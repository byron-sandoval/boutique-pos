#!/bin/bash
# El subshell evita alterar el entorno del entrypoint si este carga el script.
(
  set -euo pipefail

  : "${APP_DB_PASSWORD:?APP_DB_PASSWORD is required}"
  : "${KEYCLOAK_DB_PASSWORD:?KEYCLOAK_DB_PASSWORD is required}"

  # psql escapa los valores como literales SQL, incluidas comillas y simbolos.
  psql --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" \
    --set=ON_ERROR_STOP=1 \
    --set=app_password="$APP_DB_PASSWORD" \
    --set=keycloak_password="$KEYCLOAK_DB_PASSWORD" <<'SQL'
SELECT format('CREATE ROLE boutique_user LOGIN PASSWORD %L', :'app_password') \gexec
SELECT format('CREATE ROLE keycloak_user LOGIN PASSWORD %L', :'keycloak_password') \gexec

CREATE DATABASE boutique_pos OWNER boutique_user;
CREATE DATABASE keycloak OWNER keycloak_user;

REVOKE ALL ON DATABASE boutique_pos FROM PUBLIC;
REVOKE ALL ON DATABASE keycloak FROM PUBLIC;
SQL
)
