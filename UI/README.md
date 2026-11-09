# Boutique POS — frontend

React y TypeScript con autenticación de Keycloak y acceso a la API de Spring Boot.

## Desarrollo

Con PostgreSQL, Keycloak y el backend ya iniciados por ti:

```powershell
cd UI
npm install
npm run dev
```

Abre http://localhost:5173. Si no tienes sesión, la aplicación te envía directamente al login de Keycloak. Después muestra el panel con tu nombre y una navegación adaptada a ADMIN o CAJERO.

El inicio es una vista de diseño: sus indicadores, gráficas y ventas todavía usan datos de ejemplo. El selector de período, los filtros y los submenús funcionan localmente. Productos → Categorías conserva la consulta real a la API. Configuración → Empresa y Sucursales ya consultan y guardan datos mediante los endpoints existentes del backend. Los demás módulos mantienen su pantalla provisional.

## Empresa y sucursales

Estas pantallas son exclusivas de ADMIN; Spring Boot sigue verificando los permisos.

- **Empresa:** configuración de un solo negocio. Solo permite el registro inicial cuando no existe una empresa; después muestra sus datos y permite editarlos. No incluye eliminación ni carga de archivos. Guardar el nombre no cambia automáticamente el tema de Keycloak.
- **Sucursales:** listado completo, registro, edición y activación/desactivación con confirmación. Se asocian automáticamente a la empresa del negocio. Se conservan los datos al desactivar.
- Se utiliza el ID real de la empresa, sin presuponer el ID 1. Si la API devuelve más de una empresa, se muestra un error para revisar los registros; no se elige una arbitrariamente. La API existente conserva sus contratos; esta pantalla no administra múltiples empresas.
- Los formularios validan los campos obligatorios, sus longitudes y el IVA entre 0 y 100 con hasta dos decimales. Ante un error se conserva lo escrito. Los guardados no se reintentan automáticamente.

Para probar con tus servicios: entra como ADMIN y abre **Configuración → Empresa**. Consulta los datos existentes o registra tu empresa si la lista está vacía. Luego abre **Sucursales** y registra un local; se asociará a tu empresa automáticamente. Las acciones de guardar y cambiar estado sí escriben en la base de datos del backend configurado.

La estructura visual se encuentra en `src/components/layout`, el inicio en `src/features/inicio` y la navegación en `src/routes`. Se reutilizan Tailwind, shadcn/ui (Base UI), Lucide y Geist, sin dependencias nuevas. El panel usa rutas como `/ventas` y `/productos/categorias`, con historial del navegador y validación de roles. Los enlaces antiguos `/#/...` se convierten a rutas limpias al entrar. Keycloak regresa al módulo solicitado sin reutilizar los parámetros de su callback.

Vite está configurado como SPA y admite abrir y recargar esas rutas en desarrollo y en `npm run preview`. Al desplegar los archivos de `dist`, el servidor web también debe devolver `index.html` para las rutas del panel que no correspondan a un archivo. Por ejemplo, en Nginx se utiliza `try_files $uri $uri/ /index.html;`. Esta configuración del alojamiento será necesaria cuando se prepare el despliegue del frontend.

Vite usa el puerto 5173 y falla si está ocupado, para conservar las rutas autorizadas en Keycloak y CORS.

## Conexión

Los valores predeterminados están en `src/config.ts`:

| Configuración pública | Valor predeterminado |
| --- | --- |
| VITE_API_URL | http://localhost:8080/api |
| VITE_KEYCLOAK_URL | http://localhost:9081 |
| VITE_KEYCLOAK_REALM | boutique |
| VITE_KEYCLOAK_CLIENT_ID | boutique-ui |

No necesitas crear un archivo de entorno para trabajar con estos valores. Puedes sobrescribirlos con variables VITE_* y reiniciar Vite. Estas variables son públicas: nunca contienen contraseñas ni secretos.

El cliente `boutique-ui` debe tener Standard flow, PKCE S256, el redirect `http://localhost:5173/*`, el origen `http://localhost:5173`, el redirect posterior al logout y el mapper de audiencia `boutique-api`. La configuración está guardada en `docker/keycloak/boutique-realm.json`; modificar ese archivo no sobrescribe un realm ya existente.

Los roles ADMIN y CAJERO se leen del realm. El backend sigue verificando los permisos y el estado del usuario: para CAJERO se necesita también su registro local vinculado mediante keycloak_id y una sucursal activa. Crear un usuario directamente en Keycloak no crea ese registro en PostgreSQL.

El adaptador oficial keycloak-js usa Authorization Code con PKCE S256. Los tokens permanecen en memoria, se renuevan antes de las peticiones y al vencer, y se envían como Bearer. No se guardan en localStorage ni sessionStorage. Cerrar sesión borra la sesión local y redirige al logout de Keycloak. Una respuesta 401 solicita iniciar sesión de nuevo; 403 muestra falta de permiso sin cerrar la sesión. Las escrituras no se reintentan automáticamente.

## Validación

```powershell
npm run build
npm run lint
npm test
```

Las pruebas del cliente HTTP usan respuestas simuladas; las de navegación verifican las rutas permitidas para ADMIN y CAJERO. Las del módulo Empresa comprueban validaciones, paginación, campos enviados y los contratos de creación, edición y cambio de estado. No se conectan a PostgreSQL ni Keycloak. `npm test` requiere Node 24 o posterior.

Para comprobar la integración real: entrar como ADMIN, consultar categorías, cerrar sesión y repetir con CAJERO. Verificar también el cierre de sesión y la renovación del token. Esta comprobación requiere tus servicios en ejecución y tus usuarios; la compilación y las pruebas aisladas no la sustituyen.
