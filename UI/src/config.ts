// Datos públicos. Las contraseñas y los secretos nunca van en VITE_*.
export const config = {
  apiUrl: import.meta.env.VITE_API_URL || 'http://localhost:8080/api',
  keycloakUrl: import.meta.env.VITE_KEYCLOAK_URL || 'http://localhost:9081',
  keycloakRealm: import.meta.env.VITE_KEYCLOAK_REALM || 'boutique',
  keycloakClientId: import.meta.env.VITE_KEYCLOAK_CLIENT_ID || 'boutique-ui',
}
