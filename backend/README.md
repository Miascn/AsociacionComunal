# Base de datos de Asociacion Comunal

El modulo `backend` utiliza JDBC para conectarse al esquema MySQL
`asociacion_comunal`. La base de datos se aloja en el servidor del equipo y no
debe duplicarse ni reemplazarse por una base local con una estructura diferente.

## Tecnologias

- MySQL 8.4.
- Motor de almacenamiento InnoDB.
- Codificacion `utf8mb4`.
- MySQL Connector/J 8.3.0.
- Java 21 y Maven.

## Estructura del esquema

La base contiene 14 tablas:

| Tabla | Proposito |
| --- | --- |
| `rol` | Catalogo de perfiles y permisos. |
| `miembro` | Censo de integrantes de la asociacion. |
| `usuario` | Credenciales y acceso al sistema. |
| `cargo` | Catalogo de cargos de la directiva. |
| `periodo_directiva` | Periodos de vigencia de cada directiva. |
| `miembro_cargo` | Historial de cargos asignados a miembros. |
| `aportacion` | Aportaciones mensuales de los miembros. |
| `proyecto` | Proyectos de mejora comunitaria. |
| `votacion` | Procesos de votacion asociados con proyectos. |
| `opcion_votacion` | Opciones disponibles en cada votacion. |
| `voto` | Votos emitidos por los miembros. |
| `reunion` | Reuniones ordinarias y extraordinarias. |
| `asistencia` | Participacion de miembros en reuniones. |
| `bitacora` | Registro de acciones importantes del sistema. |

## Relaciones principales

- Un `rol` puede pertenecer a muchos `usuario`.
- Un `miembro` puede tener como maximo un `usuario`.
- Un `miembro` puede ocupar cargos en diferentes periodos.
- Un `miembro` puede registrar una aportacion por mes.
- Un `proyecto` puede tener varias votaciones.
- Una `votacion` puede tener varias opciones y votos.
- Un miembro solo puede emitir un voto por votacion.
- Una `reunion` puede registrar la asistencia de muchos miembros.
- Un usuario puede generar muchos registros de `bitacora`.

Las llaves foraneas, restricciones `UNIQUE` y validaciones `CHECK` protegen la
integridad de estas relaciones. No se deben desactivar para insertar datos.

## Roles iniciales

El esquema contiene los siguientes roles:

- `ADMINISTRADOR`
- `PRESIDENTE`
- `SECRETARIO`
- `TESORERO`
- `SINDICO`
- `MIEMBRO`

## Configuracion de la conexion

Las credenciales nunca deben escribirse en clases Java ni subirse a Git. Cada
integrante debe copiar el archivo de ejemplo desde la raiz del repositorio:

```powershell
Copy-Item database-example.properties database-local.properties
```

Luego debe completar localmente:

```properties
db.host=IP_O_NOMBRE_DEL_SERVIDOR
db.port=3306
db.name=asociacion_comunal
db.user=USUARIO_MYSQL
db.password=CONTRASENA_MYSQL
db.parameters=serverTimezone=America/El_Salvador&useUnicode=true&characterEncoding=UTF-8
```

`database-local.properties` esta excluido mediante `.gitignore`. La aplicacion
tambien acepta estas variables de entorno, que tienen prioridad sobre el archivo:

| Variable | Descripcion |
| --- | --- |
| `DB_HOST` | IP o nombre del servidor MySQL. |
| `DB_PORT` | Puerto; normalmente `3306`. |
| `DB_NAME` | Nombre del esquema: `asociacion_comunal`. |
| `DB_USER` | Usuario autorizado de MySQL. |
| `DB_PASSWORD` | Contrasena del usuario. |
| `DB_PARAMETERS` | Parametros adicionales de la URL JDBC. |
| `DB_CONFIG_FILE` | Ruta alternativa del archivo de propiedades. |

## Compilacion y verificacion

Desde la raiz del repositorio:

```powershell
.\apache-maven-3.9.16\bin\mvn.cmd -f backend\pom.xml clean install
```

Para comprobar la conexion, el esquema, las tablas y los roles:

```powershell
.\apache-maven-3.9.16\bin\mvn.cmd -f backend\pom.xml test-compile `
  org.codehaus.mojo:exec-maven-plugin:3.5.0:java `
  "-Dexec.mainClass=sv.asociacion.backend.DatabaseConnectionTest" `
  "-Dexec.classpathScope=test"
```

La prueba es de lectura: selecciona la base activa, enumera las tablas y consulta
los roles iniciales sin insertar, actualizar ni eliminar registros.

## Capa DAO

Cada entidad posee un DAO en
`src/main/java/sv/asociacion/backend/dao`. Todos utilizan
`DBConnection.getInstance().getConnection()` y sentencias preparadas para reducir
el riesgo de inyeccion SQL.

Antes de agregar una operacion nueva:

1. Confirmar que la tabla y columnas existen en el servidor.
2. Utilizar `PreparedStatement` para todos los valores proporcionados por usuarios.
3. Ejecutar pagos y votos dentro de transacciones cuando intervengan varias consultas.
4. Respetar las restricciones de unicidad y las llaves foraneas.
5. No eliminar fisicamente miembros que posean historial; cambiar su estado a
   `INACTIVO`.

## Seguridad

- No subir contrasenas, direcciones IP privadas ni archivos locales de configuracion.
- No conceder al usuario de la aplicacion permisos sobre otros esquemas.
- No utilizar la cuenta administrativa de MySQL desde la aplicacion.
- Cambiar inmediatamente cualquier credencial expuesta por accidente.
- No modificar la base `losolivares`; este proyecto utiliza exclusivamente
  `asociacion_comunal`.
