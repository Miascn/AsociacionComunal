#!/usr/bin/env bash
set -euo pipefail

CONFIG_FILE="$HOME/.config/asociacion-comunal/api.env"
DB_APP_USER="asociacion_api"

if [[ ! -f "$CONFIG_FILE" ]]; then
    echo "No existe la configuracion privada de la API." >&2
    exit 1
fi

db_password="$(openssl rand -hex 32)"

sudo mysql --batch <<SQL
CREATE USER IF NOT EXISTS '${DB_APP_USER}'@'localhost' IDENTIFIED BY '${db_password}';
ALTER USER '${DB_APP_USER}'@'localhost' IDENTIFIED BY '${db_password}';
REVOKE ALL PRIVILEGES, GRANT OPTION FROM '${DB_APP_USER}'@'localhost';
GRANT SELECT, INSERT, UPDATE, DELETE ON asociacion_comunal.* TO '${DB_APP_USER}'@'localhost';
FLUSH PRIVILEGES;
SQL

sed -i -E "s/^DB_USER=.*/DB_USER=${DB_APP_USER}/" "$CONFIG_FILE"
sed -i -E "s/^DB_PASSWORD=.*/DB_PASSWORD=${db_password}/" "$CONFIG_FILE"
chmod 600 "$CONFIG_FILE"
unset db_password

systemctl --user daemon-reload
systemctl --user enable --now asociacion-api.service asociacion-ngrok.service

echo "Usuario MySQL creado y servicios iniciados."
curl --fail --silent --show-error http://127.0.0.1:8080/health
echo
