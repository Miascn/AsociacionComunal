#!/usr/bin/env bash
set -euo pipefail

APP_HOME="$HOME/apps/asociacion-api"
CONFIG_HOME="$HOME/.config/asociacion-comunal"
SYSTEMD_HOME="$HOME/.config/systemd/user"

install -d -m 700 "$APP_HOME/releases/1.0.0" "$CONFIG_HOME" "$SYSTEMD_HOME"
install -m 500 "$HOME/asociacion-api.jar.upload" "$APP_HOME/releases/1.0.0/asociacion-api.jar"
ln -sfn "$APP_HOME/releases/1.0.0" "$APP_HOME/current"
install -m 600 "$HOME/asociacion-api.service.upload" "$SYSTEMD_HOME/asociacion-api.service"
install -m 600 "$HOME/asociacion-ngrok.service.upload" "$SYSTEMD_HOME/asociacion-ngrok.service"

if [[ ! -f "$CONFIG_HOME/api.env" ]]; then
    umask 077
    secret="$(openssl rand -hex 32)"
    printf '%s\n' \
        'API_PORT=8080' \
        "API_SHARED_SECRET=$secret" \
        'DB_HOST=127.0.0.1' \
        'DB_PORT=3306' \
        'DB_NAME=asociacion_comunal' \
        'DB_USER=REEMPLAZAR' \
        'DB_PASSWORD=REEMPLAZAR' \
        > "$CONFIG_HOME/api.env"
fi

chmod 600 "$CONFIG_HOME/api.env"
rm -f "$HOME/asociacion-api.jar.upload" \
    "$HOME/asociacion-api.service.upload" \
    "$HOME/asociacion-ngrok.service.upload"

systemctl --user daemon-reload
echo 'SERVER_CONFIGURATION_READY'
