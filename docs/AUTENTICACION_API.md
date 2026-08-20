# Autenticación oficial de la API

La API utiliza sesiones individuales reutilizables por Android, JavaFX y otros
clientes. Las rutas administrativas existentes conservan temporalmente el token
técnico `API_SHARED_SECRET` para no romper QA ni las actualizaciones.

## Flujo

1. `POST /api/auth/login` valida el usuario real de MySQL y crea una sesión.
2. El access token dura 15 minutos de forma predeterminada.
3. El refresh token dura 30 días y rota en cada uso.
4. MySQL almacena únicamente SHA-256 de ambos tokens.
5. `POST /api/auth/logout` revoca la sesión en el servidor.
6. `GET /api/me` obtiene el usuario y miembro desde el access token; nunca acepta IDs del cliente.

Las contraseñas se almacenan con el formato autocontenido
`pbkdf2_sha256$iteraciones$salt$hash`. Nunca se guarda texto plano.

## Migración

No existe Flyway ni Liquibase en el proyecto. Para mantener esta fase pequeña,
las migraciones son SQL versionadas y se aplican manualmente después de un
respaldo. La migración requerida es:

```text
database/migrations/V002__crear_sesion_usuario.sql
```

Ejemplo desde el servidor, usando las credenciales externas ya configuradas:

```bash
mysql asociacion_comunal < database/migrations/V002__crear_sesion_usuario.sql
```

No se incluyen usuarios ni contraseñas en la migración.

## Configuración

Variables existentes obligatorias:

- `API_SHARED_SECRET`
- `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD`

Variables opcionales nuevas:

- `AUTH_ACCESS_TTL_MINUTES` (predeterminado: `15`)
- `AUTH_REFRESH_TTL_DAYS` (predeterminado: `30`)

## Crear un hash sin exponer la contraseña

Desde una terminal interactiva:

```powershell
java -cp api\target\asociacion-api.jar sv.asociacion.api.auth.PasswordHashTool
```

La herramienta solicita la contraseña sin mostrarla y devuelve solamente el
hash. Este hash puede asignarse a `usuario.clave_hash` mediante un procedimiento
administrativo controlado.

## Prueba manual

```powershell
$baseUrl = "https://URL-DE-LA-API"
$login = Invoke-RestMethod -Method Post `
  -Uri "$baseUrl/api/auth/login" `
  -ContentType "application/json" `
  -Body '{"username":"USUARIO_DE_PRUEBA","password":"CONTRASEÑA_DE_PRUEBA"}'

Invoke-RestMethod -Method Get `
  -Uri "$baseUrl/api/me" `
  -Headers @{ Authorization = "Bearer $($login.accessToken)" }

$refreshBody = @{ refreshToken = $login.refreshToken } | ConvertTo-Json
$tokens = Invoke-RestMethod -Method Post `
  -Uri "$baseUrl/api/auth/refresh" `
  -ContentType "application/json" `
  -Body $refreshBody

Invoke-RestMethod -Method Post `
  -Uri "$baseUrl/api/auth/logout" `
  -ContentType "application/json" `
  -Body (@{ refreshToken = $tokens.refreshToken } | ConvertTo-Json)
```

No copies tokens o contraseñas reales en documentación, capturas o commits.
