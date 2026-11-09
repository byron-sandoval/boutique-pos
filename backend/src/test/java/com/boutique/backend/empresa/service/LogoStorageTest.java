package com.boutique.backend.empresa.service;

import java.util.HexFormat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.client.RestClient;

import static org.hamcrest.Matchers.matchesPattern;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class LogoStorageTest {

    private static final String PROYECTO = "https://example.supabase.co";
    private static final String OBJETOS = PROYECTO + "/storage/v1/object/";
    private static final String NOMBRE_ANTERIOR = "9c2c705f-9320-4167-833a-fda22ded034e.png";
    private static final String URL_ANTERIOR = OBJETOS + "public/marca/logos/" + NOMBRE_ANTERIOR;
    private static final String CLAVE = "sb_secret_prueba";
    private final MockMultipartFile logo = new MockMultipartFile("logo", "nuevo.png", MediaType.IMAGE_PNG_VALUE,
        HexFormat.of().parseHex("89504e470d0a1a0a"));

    private MockRestServiceServer server;
    private LogoStorage storage;
    private String objetoNuevo;
    private boolean transaccionTerminada;

    @BeforeEach
    void preparar() {
        var builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        storage = new LogoStorage(PROYECTO, CLAVE, "marca", builder.build());
        TransactionSynchronizationManager.initSynchronization();
    }

    @AfterEach
    void limpiar() {
        TransactionSynchronizationManager.clearSynchronization();
    }

    @Test
    void eliminaElAnteriorSoloDespuesDeConfirmarElNuevo() {
        esperarSubida();
        server.expect(requestTo(OBJETOS + "marca/logos/" + NOMBRE_ANTERIOR))
            .andExpect(method(HttpMethod.DELETE))
            .andExpect(header("apikey", CLAVE))
            .andExpect(request -> assertTrue(transaccionTerminada))
            .andRespond(withSuccess());

        String url = storage.subir(logo, URL_ANTERIOR);
        assertNotEquals(URL_ANTERIOR, url);

        completar(TransactionSynchronization.STATUS_COMMITTED);
        server.verify();
    }

    @Test
    void eliminaElNuevoYConservaElAnteriorSiLaTransaccionSeRevierte() {
        esperarSubida();
        server.expect(request -> assertEquals(objetoNuevo, request.getURI().toString()))
            .andExpect(method(HttpMethod.DELETE))
            .andExpect(request -> assertTrue(transaccionTerminada))
            .andRespond(withSuccess());

        storage.subir(logo, URL_ANTERIOR);

        completar(TransactionSynchronization.STATUS_ROLLED_BACK);
        server.verify();
    }

    @Test
    void conservaUnaUrlAnteriorFueraDelBucketConfigurado() {
        esperarSubida();
        storage.subir(logo, OBJETOS + "public/otro-bucket/logos/" + NOMBRE_ANTERIOR);

        completar(TransactionSynchronization.STATUS_COMMITTED);
        server.verify();
    }

    private void esperarSubida() {
        server.expect(requestTo(matchesPattern(
                "https://example\\.supabase\\.co/storage/v1/object/marca/logos/[a-f0-9-]+\\.png")))
            .andExpect(method(HttpMethod.POST))
            .andExpect(header("apikey", CLAVE))
            .andExpect(request -> objetoNuevo = request.getURI().toString())
            .andRespond(withSuccess());
    }

    private void completar(int estado) {
        transaccionTerminada = true;
        TransactionSynchronizationManager.getSynchronizations()
            .forEach(synchronization -> synchronization.afterCompletion(estado));
    }
}
