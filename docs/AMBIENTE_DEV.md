# Ambiente de desarrollo

El ambiente de desarrollo utiliza el esquema MySQL `asociacion_comunal` y una
cuenta con permisos limitados a ese esquema. Las credenciales no se guardan en
Git.

## Configuración

1. Copiar `config/dev/database.properties.example` a una ubicación privada; por
   ejemplo, `database-dev.properties` fuera del repositorio.
2. Completar el host, usuario y contraseña asignados al ambiente DEV.
3. Indicar la ruta absoluta del archivo antes de ejecutar el backend:

   ```powershell
   $env:DB_CONFIG_FILE = "C:\ruta\privada\database-dev.properties"
   ```

También se pueden definir `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`,
`DB_PASSWORD` y `DB_PARAMETERS`. Estas variables tienen prioridad sobre el
archivo configurado.

## Verificación

Desde la raíz del repositorio:

```powershell
.\apache-maven-3.9.16\bin\mvn.cmd -f backend\pom.xml test-compile `
  org.codehaus.mojo:exec-maven-plugin:3.5.0:java `
  "-Dexec.mainClass=sv.asociacion.backend.DatabaseConnectionTest" `
  "-Dexec.classpathScope=test"
```

La prueba valida la conexión y consulta la estructura sin modificar datos. No
debe utilizarse el usuario administrativo de MySQL desde la aplicación.
