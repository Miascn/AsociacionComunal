# Asociación Comunal

Sistema para administrar una asociación comunal salvadoreña: padrón de miembros y viviendas, junta directiva y cargos, aportaciones y cuota de mantenimiento, proyectos comunitarios, asambleas con control de asistencia y votaciones.

Proyecto de **Programación II** — Universidad Evangélica de El Salvador, Facultad de Ingeniería. Facilitador: Ing. Daniel Enrique Guevara Gómez.

**Repositorio:** <https://github.com/Miascn/AsociacionComunal>

## Contenido

- [Integrantes](#integrantes)
- [Tecnologías](#tecnologías)
- [Arquitectura](#arquitectura)
- [Requisitos](#requisitos)
- [Inicio rápido](#inicio-rápido)
- [Configuración de la base de datos](#configuración-de-la-base-de-datos)
- [DataSource y pool de conexiones](#datasource-y-pool-de-conexiones)
- [Ejecución](#ejecución)
- [Usuarios de prueba](#usuarios-de-prueba)
- [Programación concurrente](#programación-concurrente)
- [Manejo de errores](#manejo-de-errores)
- [Pruebas](#pruebas)
- [Rutas HTTP principales](#rutas-http-principales)
- [Seguridad](#seguridad)
- [Documentación adicional](#documentación-adicional)

## Integrantes

| Integrante | Rol en el equipo |
| --- | --- |
| Josué Jeremías Castillo Nieves | Scrum Master y coordinador; desarrollo frontend, backend y app móvil |
| Carlos Josué Samayoa Contreras | Product Owner y arquitecto; desarrollo backend y documentación |
| Jonathan Alexis Amaya Hernández | QA |
| Génesis Michelle Flores Cruz | Business Analyst |
| David Rafael Coto Garza | QA |
| Rodrigo Gilberto Sánchez Ávalos | Developer |
| Gerson Otoniel Bermúdez Ramírez | Developer |
| Edgard Antonio Mendoza Vaquero | Base de datos |

Los roles corresponden a la distribución Scrum de los avances 1 y 2. La participación de cada integrante en la entrega final se declara en el documento final (`docs/documento-final.pdf`).

## Tecnologías

| Tecnología | Versión | Uso |
| --- | ---: | --- |
| Java (JDK) | 21+ | API, escritorio y pruebas (compilado con `release 21`) |
| Maven Wrapper | 3.9.x | Construcción reproducible (`mvnw` / `mvnw.cmd`) |
| Javalin | 7.2.2 | API REST (Jetty embebido) |
| MySQL | 8.x | Base de datos relacional InnoDB `utf8mb4` |
| MySQL Connector/J | 8.3.0 | Controlador JDBC |
| Tomcat JDBC Pool | 11.0.27 | `DataSource` con pool de conexiones |
| Jackson | 2.21.3 | JSON |
| JJWT | 0.12.6 | Tokens JWT del escritorio |
| JavaFX | 21 | Cliente de escritorio (FXML, AtlantaFX, Ikonli) |
| Flutter / Dart | 3.x | App móvil de residentes (complementaria) |
| JUnit Jupiter | 5.14.2 | Pruebas unitarias |
| Mockito | 5.24.0 | Dobles de prueba para aislar dependencias |
| Docker Compose | — | MySQL local opcional |

## Arquitectura

```text
Cliente JavaFX (escritorio, directiva) ─┐
                                        ├── HTTP/JSON ──> API Javalin ──> DataSource (pool) ──> MySQL
App Flutter (móvil, residentes) ────────┘
```

Dentro de la API (`api/src/main/java/sv/asociacion`):

| Paquete | Responsabilidad |
| --- | --- |
| `controller` | Rutas HTTP: leer la petición, llamar al servicio y elegir el código de éxito |
| `service` | Reglas de negocio, validaciones, transacciones y el proceso concurrente de convocatoria |
| `dao` | Acceso a datos JDBC (`DAO<T,ID>`, `AbstractJdbcDAO<T,ID>`, `RowMapper<T>`) y persistencia en archivos `.dat` (`dao/archivo`) |
| `domain/entity` | Entidades (`Persona` abstracta, `Miembro`, `Residente`, `Reunion`, ...) |
| `domain/dto` | Objetos de entrada y salida de la API (records) |
| `config` | `DataSourceConfig` (pool), `DBConnection`, `AppConfig` |
| `exception` | Jerarquía de excepciones propias y traducción de `SQLException` |
| `util` | Fechas, hash y cifrado de credenciales, generador de claves provisionales |
| `middleware` / `api/auth` | Autenticación (JWT del escritorio, sesiones de la app móvil, token técnico) |

`ApiServer` es la raíz de composición: crea los DAO y servicios y los inyecta por constructor.

```text
AsociacionComunal/
├── api/                  API REST (Java 21, Javalin, JDBC)
├── frontend/             cliente de escritorio JavaFX
├── flutter-app/          app móvil Flutter para residentes
├── database/
│   ├── schema.sql        esquema completo para instalaciones nuevas
│   ├── data.sql          datos y usuarios de demostración
│   └── migrations/       migraciones V002-V011 para bases existentes
├── docs/                 documentación (avances, arquitectura, operación)
├── scripts/              run.sh y scripts de empaquetado QA (PowerShell)
├── docker-compose.yml    MySQL 8 local
└── pom.xml               reactor Maven (api + frontend)
```

## Requisitos

- **JDK 21 o superior** (`java -version`). No hace falta instalar Maven: se usa el wrapper incluido.
- **MySQL 8**: con Docker (`docker compose`) o una instalación propia.
- Git Bash, Linux o macOS para `scripts/run.sh` (en Windows también se pueden usar los comandos `mvnw.cmd` equivalentes).
- Flutter SDK solo si se quiere ejecutar la app móvil.

## Inicio rápido

```bash
scripts/run.sh db        # MySQL 8 en Docker con schema.sql + data.sql
scripts/run.sh api       # API en http://localhost:8080 (salud: /health)
scripts/run.sh desktop   # en otra terminal: escritorio JavaFX contra la API local
scripts/run.sh test      # todas las pruebas
```

La primera ejecución crea un `.env` local (ignorado por Git) con secretos aleatorios y la conexión a la base de Docker.

## Configuración de la base de datos

### Instalación nueva

Con Docker, `scripts/run.sh db` (o `docker compose up -d mysql`) crea el contenedor, la base `asociacion_comunal`, el usuario de aplicación `asociacion_app` y ejecuta `database/schema.sql` y `database/data.sql` la primera vez que se crea el volumen.

Sin Docker, en un MySQL 8 propio:

```bash
mysql -u root -p < database/schema.sql
mysql -u root -p asociacion_comunal < database/data.sql   # opcional: datos de demostración
```

`database/schema.sql` es el esquema efectivo (equivale al esquema base con las migraciones V002–V011 aplicadas). Una base que ya existe se actualiza con `database/migrations/`, en orden, nunca con `schema.sql`.

### Conexión

La API lee la conexión de variables de entorno, de `.env` o de `database-local.properties` (ignorados por Git), en ese orden de prioridad:

| Variable | Predeterminado | Descripción |
| --- | --- | --- |
| `DB_HOST` | `localhost` | Servidor MySQL |
| `DB_PORT` | `3306` | Puerto |
| `DB_NAME` | `asociacion_comunal` | Base de datos |
| `DB_USER` | — (obligatoria) | Usuario de aplicación |
| `DB_PASSWORD` | vacía | Contraseña |
| `DB_PARAMETERS` | zona horaria y UTF-8 | Parámetros JDBC adicionales |
| `API_PORT` | `8080` | Puerto HTTP de la API |
| `API_SHARED_SECRET` | — (obligatoria, ≥32) | Token técnico del escritorio y de publicación |
| `JWT_SECRET` | — (obligatoria, ≥32) | Firma de los JWT del escritorio |
| `CREDENTIALS_KEY` | derivada de `JWT_SECRET` | Llave de cifrado de claves provisionales |

Plantilla: `.env.example`. Ningún secreto real se guarda en el repositorio.

## DataSource y pool de conexiones

`config/DataSourceConfig` construye un `org.apache.tomcat.jdbc.pool.DataSource`. `DBConnection.getConnection()` conserva su firma, pero ahora **presta** conexiones del pool: el `close()` del try-with-resources devuelve la conexión en lugar de cerrar el socket. El pool se crea al primer uso.

| Parámetro | Valor | Motivo |
| --- | ---: | --- |
| `validationQuery` | `SELECT 1` | Consulta mínima para comprobar que MySQL responde |
| `initialSize` | 4 | Conexiones listas al iniciar; cubren el uso típico de la directiva |
| `minIdle` | 4 | Mínimo de inactivas que se conservan (igual a `initialSize`) |
| `maxIdle` | 8 | Exceso tras una ráfaga que se cierra |
| `maxActive` | 20 | Tope de conexiones prestadas; muy por debajo de las 151 de MySQL |
| `maxWait` | 10000 ms | Espera máxima por una conexión; luego error 503 en vez de colgarse |
| `testOnBorrow` | `true` | Nunca entregar una conexión que MySQL ya cerró (`wait_timeout`) |
| `testOnReturn` | `false` | Ya se valida al prestar; validar al devolver duplicaría el costo |
| `testWhileIdle` | `true` | El evictor descarta inactivas rotas |
| `testOnConnect` | `true` | Detecta de inmediato credenciales o red incorrectas |
| `validationInterval` | 30000 ms | Evita ejecutar `SELECT 1` en cada préstamo |
| `timeBetweenEvictionRunsMillis` | 30000 ms | Frecuencia del evictor |
| `minEvictableIdleTimeMillis` | 60000 ms | Inactividad tras la cual una conexión puede cerrarse |
| `removeAbandoned` | `true` | Recupera conexiones que el código no cerró |
| `removeAbandonedTimeout` | 60 s | Las operaciones duran milisegundos; 60 s solo los supera una fuga |

Además: interceptores `ConnectionState` (restablece `autoCommit` en cada préstamo) y `StatementFinalizer`. El endpoint `/health` informa conexiones activas, inactivas y el máximo. La justificación completa está en el Javadoc de `DataSourceConfig`.

## Ejecución

### API

```bash
scripts/run.sh api
# o manualmente:
./mvnw -B -pl api -am package -DskipTests
java -jar api/target/asociacion-api.jar
```

Comprobación: `curl http://localhost:8080/health`

```json
{"service":"asociacion-api","database":"available","pool":{"activas":0,"inactivas":4,"maxActivas":20}}
```

La API escucha en todas las interfaces (`0.0.0.0`) del puerto configurado. Fuera de un equipo local debe publicarse detrás de un proxy o túnel HTTPS; MySQL nunca debe exponerse a Internet.

### Escritorio JavaFX

```bash
scripts/run.sh desktop
# o manualmente (Windows PowerShell):
$env:QA_API_URL="http://localhost:8080"; $env:QA_API_TOKEN="<valor de API_SHARED_SECRET>"
.\mvnw.cmd -pl frontend javafx:run
```

El cliente acepta `http://` solo hacia `localhost` o `127.0.0.1`; cualquier otro destino exige HTTPS. El inicio de sesión se valida contra la API (usuarios de MySQL). Las cuentas con rol `MIEMBRO` no pueden entrar al escritorio: son para la app móvil.

### App móvil (Flutter)

```bash
cd flutter-app
flutter pub get
flutter test
flutter run
```

La app usa la URL de `lib/core/constants/api_constants.dart` (por defecto, el servidor del equipo). Es un cliente complementario y no forma parte del alcance Java evaluado.

## Usuarios de prueba

Creados por `database/data.sql`. **Solo para una base local de demostración**: no cargar `data.sql` en ambientes compartidos.

| Usuario | Contraseña | Rol | Dónde |
| --- | --- | --- | --- |
| `demo.admin` | `Demo.Admin.2026!` | ADMIN | Escritorio JavaFX y API |
| `demo.directiva` | `Demo.Directiva.2026!` | DIRECTIVO | Escritorio JavaFX |
| `demo.miembro` | `Demo.Miembro.2026!` | MIEMBRO | App móvil (`/api/auth/login`) |

La convocatoria a reuniones requiere un rol de gestión (ADMIN, PRESIDENTE o SECRETARIO): para la demostración use `demo.admin`.

## Programación concurrente

**Proceso:** convocatoria masiva de miembros a una asamblea (`service/ConvocatoriaConcurrenteService`).

- **Qué hace:** registra una asistencia pendiente por cada miembro activo (o por los indicados) para una reunión.
- **Por qué es concurrente:** antes se hacían, en serie y por miembro, una consulta de existencia, otra de estado y una inserción. Ahora la lista se divide en lotes de 25 que se escriben en paralelo.
- **Mecanismo:** `ExecutorService` de tamaño fijo (4 hilos, menor que `maxActive`), `invokeAll` con tiempo máximo de 60 s, resultados con `Future`/`ExecutionException` y cierre del executor con try-with-resources.
- **Interacción con la base:** cada lote pide **su propia conexión** al `DataSource` con pool (`dao/JdbcConvocatoriaRepository`), ejecuta un batch de `PreparedStatement` en una transacción y devuelve la conexión.
- **Control de problemas de concurrencia:** la restricción única `uk_asistencia_reunion_miembro` y un `INSERT IGNORE ... SELECT` que solo inscribe miembros activos hacen la operación **idempotente**: dos coordinadores que convocan a la vez no duplican registros. No se depende de `synchronized` en memoria. Un lote fallido no deshace los demás; la respuesta indica los miembros pendientes y repetir la convocatoria es seguro.

Pruébelo con la reunión 1 de los datos de demostración:

```bash
TOKEN=$(curl -s -X POST http://localhost:8080/api/admin/auth/login -H 'Content-Type: application/json' \
  -d '{"nombreUsuario":"demo.admin","clave":"Demo.Admin.2026!"}' | sed -E 's/.*"token":"([^"]+)".*/\1/')
curl -s -X POST http://localhost:8080/api/reuniones/1/asistencias/convocatoria -H "Authorization: Bearer $TOKEN"
```

Respuesta esperada sobre una base recién cargada (los 7 miembros activos; el inactivo no se convoca; `duracionMs` varía). Una segunda llamada devuelve `"convocados":0,"omitidos":7`:

```json
{"idReunion":1,"solicitados":7,"convocados":7,"omitidos":0,"lotes":1,"lotesFallidos":0,"hilos":1,"duracionMs":12,"miembrosPendientes":[],"completa":true}
```

## Manejo de errores

Paquete `exception`: `NegocioException` (abstracta) con `ValidacionException`, `RecursoNoEncontradoException` y `ConflictoException` (de la que hereda `VotoDuplicadoException`); `PersistenciaException` con `ConexionBDException` y `ConvocatoriaIncompletaException`. `TraductorSQLException` convierte cada `SQLException` en la excepción adecuada (SQLState `08xxx` o pool agotado → base no disponible). Las consultas y escrituras de los DAO ya no silencian ninguna `SQLException`. La única tolerancia que queda es la de algunos mapeadores que leen columnas opcionales.

`ApiServer.configurarManejoErrores` centraliza la traducción a HTTP: validación 400, no encontrado 404, conflicto 409, base no disponible 503 y fallo de persistencia 500. El detalle técnico (SQL, SQLState) se registra en el servidor y nunca se envía al cliente.

## Pruebas

```bash
scripts/run.sh test                 # todo el reactor
./mvnw -B -pl api test              # solo la API
```

- **API:** 385 pruebas JUnit 5; Mockito aísla DAO y JDBC (por ejemplo `MiembroServiceTest`, `AsistenciaDaoJdbcTest`, `JdbcConvocatoriaRepositoryTest`). No requieren MySQL.
- **Escritorio:** 70 pruebas. Dos de navegación FXML necesitan una configuración de API sintácticamente válida; en una terminal limpia defina antes `QA_API_URL=http://localhost:8080` y `QA_API_TOKEN` (32+ caracteres). No hace falta que la API esté corriendo.
- Reportes de Surefire: `api/target/surefire-reports/` y `frontend/target/surefire-reports/`.
- `DatabaseConnectionTest` es una herramienta manual (`main`) para diagnosticar una base real; no forma parte de la suite.

## Rutas HTTP principales

| Método | Ruta | Uso |
| --- | --- | --- |
| `GET` | `/health` | Salud de la API, de la base y del pool |
| `POST` | `/api/admin/auth/login` | Login del escritorio (JWT) |
| `POST` | `/api/auth/login` · `/refresh` · `/logout` · `/change-password` | Sesiones de la app móvil |
| `GET/POST/PUT/PATCH` | `/api/miembros` | Padrón de miembros y credenciales |
| `GET/POST/PUT/DELETE` | `/api/viviendas` | Viviendas y censo |
| `GET/POST/PUT/PATCH` | `/api/aportaciones` | Aportaciones y cuota de mantenimiento |
| `GET/POST/PUT/PATCH/DELETE` | `/api/proyectos` | Proyectos comunitarios |
| `GET/POST/PUT/PATCH/DELETE` | `/api/reuniones` | Asambleas |
| `POST` | `/api/reuniones/{id}/asistencias/convocar` | Convocatoria (devuelve la lista de asistencia) |
| `POST` | `/api/reuniones/{id}/asistencias/convocatoria` | Convocatoria concurrente con resultado agregado |
| `GET/POST/PATCH/DELETE` | `/api/votaciones`, `/api/votos` | Votaciones, opciones y votos |
| `GET/POST/PATCH/DELETE` | `/api/cargos`, `/api/periodos`, `/api/asignaciones-cargo` | Junta directiva |

La lista completa está en `ApiServer.java`.

## Seguridad

- Contraseñas con PBKDF2-HMAC-SHA256 y sal aleatoria; los tokens de sesión se guardan solo como hash SHA-256.
- La contraseña provisional (`usuario.clave_temporal`) se guarda **cifrada** con AES-256-GCM: el escritorio debe poder mostrarla hasta que el miembro la cambie, por eso no es un hash. Se borra al cambiar la contraseña.
- Toda consulta usa `PreparedStatement`; los mensajes de error al cliente no contienen SQL.
- `.env`, `database-local.properties`, `qa-local.properties` y `qa-token.upload` están ignorados por Git.

## Documentación adicional

- `docs/documento-final.pdf` (y `.docx`) — documento final; fuente editable en `docs/avance3/DOCUMENTO_FINAL.md`.
- `docs/uml.png`, `docs/uml-excepciones.png`, `docs/arquitectura.png` — diagramas (fuentes draw.io en `docs/avance3/`).
- `docs/avance2/` — documento y evidencias del Avance 2.
- `docs/FLUJO_GIT.md` — estrategia de ramas.
- `docs/GUIA_COMPLETA_OPERACION_Y_ACTUALIZACIONES.md` — servidor, paquete QA de Windows y actualizaciones.
- `docs/AUTENTICACION_API.md` — autenticación de la API.
