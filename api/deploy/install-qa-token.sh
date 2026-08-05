#!/usr/bin/env bash
set -euo pipefail

TOKEN_UPLOAD="$HOME/qa-token.upload"
CONFIG_FILE="$HOME/.config/asociacion-comunal/api.env"
TEMP_FILE="$CONFIG_FILE.tmp"

token="$(<"$TOKEN_UPLOAD")"
if [[ ! "$token" =~ ^[a-f0-9]{64}$ ]]; then
    echo "El token QA cargado no es valido." >&2
    exit 1
fi

umask 077
awk -v token="$token" '
    /^API_SHARED_SECRET=/ { print "API_SHARED_SECRET=" token; next }
    { print }
' "$CONFIG_FILE" > "$TEMP_FILE"
mv "$TEMP_FILE" "$CONFIG_FILE"
chmod 600 "$CONFIG_FILE"
rm -f "$TOKEN_UPLOAD"
unset token

systemctl --user restart asociacion-api.service
for attempt in {1..15}; do
    if systemctl --user is-active --quiet asociacion-api.service \
        && curl --fail --silent --output /dev/null http://127.0.0.1:8080/health; then
        echo "TOKEN_QA_INSTALLED"
        exit 0
    fi
    sleep 1
done

echo "La API no reinicio correctamente." >&2
exit 1
