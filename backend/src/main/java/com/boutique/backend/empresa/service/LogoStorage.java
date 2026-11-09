package com.boutique.backend.empresa.service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.UUID;
import java.util.regex.Pattern;

import com.boutique.backend.shared.exception.ServicioExternoException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class LogoStorage {

    private static final Logger log = LoggerFactory.getLogger(LogoStorage.class);
    private static final int MAX_BYTES = 2 * 1024 * 1024;
    private static final byte[] PNG_SIGNATURE = HexFormat.of().parseHex("89504e470d0a1a0a");
    private static final byte[] JPEG_SIGNATURE = HexFormat.of().parseHex("ffd8ff");
    private static final byte[] RIFF_SIGNATURE = HexFormat.of().parseHex("52494646");
    private static final byte[] WEBP_SIGNATURE = HexFormat.of().parseHex("57454250");
    private static final Pattern NOMBRE_LOGO = Pattern.compile(
        "[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}\\.(png|jpg|webp)");

    private final String url;
    private final String key;
    private final String bucket;
    private final RestClient client;

    @Autowired
    public LogoStorage(@Value("${SUPABASE_URL:}") String url,
        @Value("${SUPABASE_SECRET_KEY:}") String key,
        @Value("${SUPABASE_STORAGE_BUCKET:}") String bucket) {
        this(url, key, bucket, crearCliente());
    }

    LogoStorage(String url, String key, String bucket, RestClient client) {
        this.url = url.strip();
        this.key = key.strip();
        this.bucket = bucket.strip();
        this.client = client;
    }

    private static RestClient crearCliente() {
        HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
        var factory = new JdkClientHttpRequestFactory(http);
        factory.setReadTimeout(Duration.ofSeconds(10));
        return RestClient.builder().requestFactory(factory).build();
    }

    public String subir(MultipartFile logo, String urlAnterior) {
        if (logo.getSize() > MAX_BYTES) {
            throw new ResponseStatusException(HttpStatus.CONTENT_TOO_LARGE, "El logo admite hasta 2 MB.");
        }
        byte[] bytes = leer(logo);
        Formato formato = detectarFormato(bytes);
        URI proyecto = proyectoConfigurado();
        String nombre = UUID.randomUUID() + "." + formato.extension();
        URI objeto = ruta(proyecto, nombre, false);
        String publica = ruta(proyecto, nombre, true).toString();
        if (publica.length() > 500) {
            throw new ServicioExternoException("La URL del logo supera la longitud admitida.");
        }
        URI anterior = objetoAdministrado(proyecto, urlAnterior);

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status == STATUS_ROLLED_BACK) {
                    limpiar(objeto);
                } else if (status == STATUS_COMMITTED && anterior != null) {
                    limpiar(anterior);
                }
            }
        });
        try {
            client.post().uri(objeto)
                .header("apikey", key)
                .contentType(formato.contentType())
                .body(bytes)
                .retrieve()
                .onStatus(status -> !status.is2xxSuccessful(), (request, response) -> {
                    log.warn("Supabase rechazó la subida del logo con estado {}", response.getStatusCode().value());
                    throw new ServicioExternoException("Supabase rechazó la subida del logo.");
                })
                .toBodilessEntity();
            return publica;
        } catch (RestClientException exception) {
            throw new ServicioExternoException("No se pudo conectar con Supabase para subir el logo.");
        }
    }

    private URI proyectoConfigurado() {
        URI proyecto;
        try {
            proyecto = URI.create(url);
        } catch (IllegalArgumentException exception) {
            throw new ServicioExternoException("SUPABASE_URL no tiene un formato válido.");
        }
        if (!"https".equalsIgnoreCase(proyecto.getScheme()) || proyecto.getHost() == null
            || proyecto.getUserInfo() != null || proyecto.getQuery() != null || proyecto.getFragment() != null
            || !proyecto.getPath().matches("/?") || !key.startsWith("sb_secret_")
            || bucket.isBlank() || bucket.contains("/") || bucket.contains("\\")
            || ".".equals(bucket) || "..".equals(bucket)) {
            throw new ServicioExternoException("Revisa las variables de Supabase del backend.");
        }
        return proyecto;
    }

    private URI ruta(URI proyecto, String nombre, boolean publica) {
        var ruta = UriComponentsBuilder.fromUri(proyecto).pathSegment("storage", "v1", "object");
        if (publica) {
            ruta.pathSegment("public");
        }
        return ruta.pathSegment(bucket, "logos", nombre).build().encode().toUri();
    }

    private URI objetoAdministrado(URI proyecto, String urlAnterior) {
        if (urlAnterior == null) {
            return null;
        }
        String nombre = urlAnterior.substring(urlAnterior.lastIndexOf('/') + 1);
        if (NOMBRE_LOGO.matcher(nombre).matches() && ruta(proyecto, nombre, true).toString().equals(urlAnterior)) {
            return ruta(proyecto, nombre, false);
        }
        return null;
    }

    private void limpiar(URI objeto) {
        try {
            client.delete().uri(objeto).header("apikey", key).exchange((request, response) -> {
                var estado = response.getStatusCode();
                if (!estado.is2xxSuccessful() && estado.value() != 404) {
                    log.warn("Supabase rechazó la eliminación del logo {} con estado {}", objeto, estado.value());
                }
                return null;
            });
        } catch (RestClientException exception) {
            log.warn("No se pudo contactar a Supabase para eliminar el logo {}", objeto);
        }
    }

    private byte[] leer(MultipartFile logo) {
        try {
            return logo.getBytes();
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No se pudo leer el archivo del logo.");
        }
    }

    private Formato detectarFormato(byte[] bytes) {
        if (coincide(bytes, 0, PNG_SIGNATURE)) {
            return new Formato("png", MediaType.IMAGE_PNG);
        }
        if (coincide(bytes, 0, JPEG_SIGNATURE)) {
            return new Formato("jpg", MediaType.IMAGE_JPEG);
        }
        if (coincide(bytes, 0, RIFF_SIGNATURE) && coincide(bytes, 8, WEBP_SIGNATURE)) {
            return new Formato("webp", MediaType.parseMediaType("image/webp"));
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Elige una imagen PNG, JPG o WebP válida.");
    }

    private boolean coincide(byte[] bytes, int inicio, byte[] firma) {
        int fin = inicio + firma.length;
        return bytes.length >= fin && Arrays.equals(bytes, inicio, fin, firma, 0, firma.length);
    }

    private record Formato(String extension, MediaType contentType) {}
}
