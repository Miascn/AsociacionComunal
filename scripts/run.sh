#!/usr/bin/env bash
# =============================================================================
# Asociacion Comunal - script de ejecucion (Linux, macOS o Git Bash en Windows)
#
# Uso:
#   scripts/run.sh db        Levanta MySQL 8 con Docker y carga schema.sql + data.sql
#   scripts/run.sh api       Compila y arranca la API REST (puerto 8080 por defecto)
#   scripts/run.sh desktop   Arranca el cliente de escritorio JavaFX contra la API local
#   scripts/run.sh test      Ejecuta todas las pruebas (JUnit 5 + Mockito)
#   scripts/run.sh all       db + api (el escritorio se abre en otra terminal)
#
# Requisitos: JDK 21 o superior. Docker solo para "db".
# La primera vez crea un .env local (ignorado por Git) con secretos aleatorios.
# =============================================================================
set -euo pipefail

RAIZ="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$RAIZ"
MVNW="$RAIZ/mvnw"

info()  { printf '\033[1;34m==>\033[0m %s\n' "$*"; }
falla() { printf '\033[1;31mError:\033[0m %s\n' "$*" >&2; exit 1; }

verificar_java() {
  command -v java >/dev/null 2>&1 || falla "No se encontro 'java'. Instale un JDK 21 o superior."
  local version
  version="$(java -version 2>&1 | awk -F'"' '/version/ {print $2}' | cut -d. -f1)"
  [ "${version:-0}" -ge 21 ] 2>/dev/null || falla "Se requiere JDK 21 o superior (encontrado: ${version:-desconocido})."
  [ -x "$MVNW" ] || chmod +x "$MVNW"
}

secreto_aleatorio() {
  if command -v openssl >/dev/null 2>&1; then
    openssl rand -hex 32
  else
    od -An -tx1 -N32 /dev/urandom | tr -d ' \n'
  fi
}

# Crea .env con valores locales si no existe. No sobrescribe un .env existente.
preparar_env() {
  if [ ! -f .env ]; then
    info "Creando .env local con secretos aleatorios (ignorado por Git)"
    umask 077
    {
      echo "API_PORT=8080"
      echo "API_SHARED_SECRET=$(secreto_aleatorio)"
      echo "JWT_SECRET=$(secreto_aleatorio)"
      echo "DB_HOST=localhost"
      echo "DB_PORT=3306"
      echo "DB_NAME=asociacion_comunal"
      echo "DB_USER=asociacion_app"
      echo "DB_PASSWORD=asociacion_local"
    } > .env
  fi
  # Exporta las variables de .env sin pisar las que ya existan en el entorno.
  while IFS='=' read -r clave valor; do
    [[ -z "$clave" || "$clave" == \#* ]] && continue
    if [ -z "${!clave:-}" ]; then export "$clave=$valor"; fi
  done < .env
}

cmd_db() {
  command -v docker >/dev/null 2>&1 || falla "Docker no esta instalado. Alternativa: cargar database/schema.sql y database/data.sql en un MySQL 8 propio."
  preparar_env
  info "Levantando MySQL 8 (docker compose)"
  docker compose up -d mysql
  info "Esperando a que MySQL este listo"
  for _ in $(seq 1 30); do
    estado="$(docker inspect -f '{{.State.Health.Status}}' asociacion_mysql 2>/dev/null || echo desconocido)"
    if [ "$estado" = "healthy" ]; then info "MySQL listo en localhost:3306 (base asociacion_comunal)"; return 0; fi
    sleep 2
  done
  falla "MySQL no quedo listo a tiempo. Revise: docker compose logs mysql"
}

cmd_api() {
  verificar_java
  preparar_env
  info "Compilando la API (sin pruebas; use 'scripts/run.sh test' para ejecutarlas)"
  "$MVNW" -B -q -pl api -am package -DskipTests
  info "API en http://localhost:${API_PORT:-8080}  (salud: /health)"
  exec java -jar api/target/asociacion-api.jar
}

cmd_desktop() {
  verificar_java
  preparar_env
  # El cliente acepta http:// solo para localhost/127.0.0.1; fuera de la maquina exige HTTPS.
  export QA_API_URL="${QA_API_URL:-http://localhost:${API_PORT:-8080}}"
  export QA_API_TOKEN="${QA_API_TOKEN:-$API_SHARED_SECRET}"
  info "Escritorio JavaFX contra $QA_API_URL"
  "$MVNW" -B -q -pl frontend javafx:run
}

cmd_test() {
  verificar_java
  info "Ejecutando todas las pruebas"
  "$MVNW" -B test
  info "Reportes: api/target/surefire-reports y frontend/target/surefire-reports"
}

case "${1:-}" in
  db)      cmd_db ;;
  api)     cmd_api ;;
  desktop) cmd_desktop ;;
  test)    cmd_test ;;
  all)     cmd_db; cmd_api ;;
  *)       sed -n '2,14p' "$0"; exit 1 ;;
esac
