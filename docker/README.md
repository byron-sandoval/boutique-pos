# Docker para desarrollo local

Esta carpeta contiene la configuracion de PostgreSQL y Keycloak.
El backend y la interfaz se ejecutan desde tu computadora por ahora.

## Tema de login de Keycloak

El tema `boutique` esta en `keycloak/themes/boutique/login` y extiende los
formularios de Keycloak. Los estilos propios estan en
`resources/css/boutique.css`. Las imagenes del login (logo, fondo, etc.) van
en `resources/img/`; esta carpeta puede estar vacia hasta que agregues una.
Desde el CSS puedes referenciar una imagen con `url('../img/fondo.webp')`.
Estas imagenes son recursos visuales del tema, separados de las fotos de productos.

Compose monta el tema en `/opt/keycloak/themes/boutique` en modo de solo lectura.
Si el contenedor ya estaba creado, cuando decidas aplicar este montaje ejecuta
`docker compose up -d keycloak` desde esta carpeta. Esto conserva el volumen.
Despues selecciona el realm `boutique` en la consola de Keycloak y entra en
**Realm settings > Themes > Login theme**, elige **boutique** y guarda.
Crear los archivos no activa el tema en un realm que ya existe.

El tema incluye una composicion de dos columnas y un fondo fotografico generado
en `resources/img/boutique-login.png`, servido localmente por Keycloak.
El movimiento suave del fondo es automatico y respeta la preferencia de movimiento reducido.
En pantallas pequenas se muestra el formulario. Los formularios de acceso,
recuperacion y acciones requeridas siguen siendo los originales de Keycloak.
No se habilita el registro publico ni se cambia la configuracion del correo.

## Antes de iniciar

Edita `.env` en esta carpeta y asigna tus propias contrasenas:

| Variable | Uso |
| --- | --- |
| POSTGRES_PASSWORD | Administrador postgres de PostgreSQL |
| APP_DB_PASSWORD | Usuario boutique_user de la base boutique_pos |
| KEYCLOAK_DB_PASSWORD | Usuario keycloak_user de la base keycloak |
| KEYCLOAK_ADMIN_PASSWORD | Administrador de la consola de Keycloak |

`KEYCLOAK_ADMIN_USERNAME` tiene como valor inicial `admin`; puedes cambiarlo
antes del primer arranque. `.env` esta excluido de Git. Si una contrasena
contiene `$` o `#`, usa comillas simples para el valor en `.env`.

## Cuando decidas levantarlo

Con Docker Desktop iniciado, desde la raiz de boutique-pos:

```powershell
cd docker
docker compose up -d
```

Estos comandos los ejecutas tu, despues de asignar las contrasenas.
En el primer arranque se crearan ambas bases y el realm de Keycloak.
No se importan las tablas del POS: quedan para el siguiente paso con Liquibase.
La base PostgreSQL que tengas fuera de Docker no se modifica.

## Conexiones previstas

| Uso | Direccion | Base / usuario |
| --- | --- | --- |
| Backend o pgAdmin desde Windows | localhost:5433 | boutique_pos / boutique_user |
| Keycloak dentro de Docker | postgres:5432 | keycloak / keycloak_user |
| Consola de Keycloak | http://localhost:9081/admin/ | Administrador definido en .env |

Realm: `boutique`. Cliente publico para React: `boutique-ui`, con PKCE S256
y origen `http://localhost:5173`. Roles iniciales: `ADMIN` y `CAJERO`.
No incluye usuarios del POS ni asigna roles automaticamente.
Keycloak usa `start-dev`, destinado a desarrollo local.

## Persistencia

El volumen `boutique-pos_postgres_data` conservara ambas bases.
El script de PostgreSQL solo se ejecuta cuando el directorio de datos esta
vacio. La importacion del realm no sobrescribe uno que ya exista.
Despues del primer arranque, editar `.env` no cambia las contrasenas ya
guardadas en PostgreSQL o Keycloak.

Para detener los contenedores conservando los datos, desde esta carpeta:

```powershell
docker compose down
```

Agregar `-v` eliminaria el volumen y sus bases de datos.

Spring Boot ejecutado desde el IDE no carga automaticamente este `.env`.
Su conexion y la integracion de React con Keycloak se configuraran aparte.
