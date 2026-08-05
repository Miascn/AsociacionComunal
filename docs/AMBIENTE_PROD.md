# Ambiente de producción

Producción utiliza exclusivamente el esquema MySQL `asociacion_comunal`. La
aplicación debe conectarse con una cuenta propia y con los permisos mínimos
necesarios; no debe usar `root` ni una cuenta administradora.

## Configuración

1. Copiar `config/prod/database.properties.example` a una ruta privada del
   servidor.
2. Completar los valores reales sin modificar la plantilla versionada.
3. Restringir los permisos de lectura del archivo al usuario que ejecuta la
   aplicación.
4. Definir `DB_CONFIG_FILE` con la ruta absoluta antes de iniciar el sistema:

   ```powershell
   $env:DB_CONFIG_FILE = "C:\ruta\privada\database-prod.properties"
   ```

Las variables `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD` y
`DB_PARAMETERS` pueden sustituir valores concretos y tienen prioridad sobre el
archivo.

## Requisitos de producción

- Mantener TLS habilitado y validar el certificado del servidor MySQL.
- Limitar la cuenta al esquema `asociacion_comunal`.
- Rotar inmediatamente cualquier credencial expuesta.
- Realizar y comprobar un respaldo antes de migraciones.
- Ejecutar primero las migraciones en DEV y conservar un procedimiento de
  reversión.
- No ejecutar inicializadores DDL automáticamente al arrancar la aplicación.

La comprobación de conexión debe ejecutarse antes del despliegue. La prueba
`DatabaseConnectionTest` solo consulta el esquema y no modifica datos.
