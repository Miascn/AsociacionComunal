# API del servidor

La API se ejecuta únicamente en `127.0.0.1:8080`. MySQL no debe publicarse en
Internet; ngrok reenvía HTTPS hacia esta API local.

## Variables obligatorias

- `API_SHARED_SECRET`: secreto aleatorio de al menos 32 caracteres.
- `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD`: conexión MySQL.
- `API_PORT`: opcional; el valor predeterminado es `8080`.

Los valores reales se almacenan solo en el servidor y nunca se suben a Git.

## Compilación

```powershell
.\apache-maven-3.9.16\bin\mvn.cmd -f backend\pom.xml install
.\apache-maven-3.9.16\bin\mvn.cmd -f api\pom.xml package
```

El resultado es `api/target/asociacion-api.jar`.

## Ejecución permanente en el servidor

Los archivos de `deploy/` son servicios de usuario de systemd. Las credenciales
reales se guardan exclusivamente en
`~/.config/asociacion-comunal/api.env`, con permisos `600`; ese archivo no se
copia al repositorio.

La API escucha solamente en `127.0.0.1`. El servicio de ngrok publica HTTPS en
el dominio reservado y nunca expone directamente el puerto `3306` de MySQL.

## Rutas iniciales

- `GET /health`: comprueba API y conexión MySQL sin exponer credenciales.
- `GET /api/miembros`: devuelve miembros y exige `Authorization: Bearer ...`.
