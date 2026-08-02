# Asociación Comunal

Sistema de escritorio para la administración de una asociación comunal, desarrollado con JavaFX y Maven.

## Estado actual

El frontend cuenta con un flujo base funcional compuesto por:

- Inicio de sesión definido mediante FXML.
- Autenticación temporal con usuarios simulados y contraseñas procesadas con PBKDF2.
- Gestión de la sesión autenticada mediante `SessionManager`.
- Dashboard con indicadores simulados.
- Menú lateral y navegación entre módulos.
- Cierre de sesión con retorno al login.
- Vistas placeholder para los módulos que todavía no han sido implementados.

Los controladores JavaFX contienen la interacción de la interfaz, mientras los datos del dashboard y la autenticación permanecen separados en servicios. La autenticación simulada deberá sustituirse posteriormente por un repositorio JDBC conectado a MySQL.

## Interfaz

Las pantallas activas se encuentran en `frontend/src/main/resources/fxml` y utilizan:

- JavaFX 21.0.2.
- AtlantaFX para el tema visual.
- Ikonli Feather para los iconos.
- CSS propio en `frontend/src/main/resources/css/app.css`.

Las vistas construidas programáticamente que sirvieron como referencia durante la migración fueron retiradas después de comprobar el flujo FXML.

## Credenciales de prueba

La autenticación actual utiliza usuarios simulados. Para probar el inicio de sesión se puede usar cualquiera de estas cuentas:

| Usuario | Contraseña | Rol |
| --- | --- | --- |
| `admin` | `Admin2026!` | Administrador |
| `secretaria` | `Secretaria2026!` | Secretaria |
| `tesorero` | `Tesorero2026!` | Tesorero |

Estas credenciales son exclusivamente para desarrollo y pruebas. Después de cinco intentos fallidos, la cuenta queda bloqueada durante 30 segundos.

## Verificación

Desde la carpeta `frontend`:

```bash
mvn test
```

Las pruebas cargan los FXML, verifican sus controladores, recorren los botones de navegación y comprueban el inicio y cierre de sesión.

Para ejecutar la aplicación:

```bash
mvn javafx:run
```

## Nota sobre Maven

La carpeta local `apache-maven-3.9.16/` no forma parte del proyecto y está excluida mediante `.gitignore`. Cada integrante debe utilizar una instalación local de Maven o el Maven Wrapper cuando este sea incorporado al repositorio.

## Backend y persistencia

El módulo `backend` contiene las entidades del dominio y una capa DAO basada en JDBC para MySQL. El frontend declara una dependencia Maven hacia este módulo, por lo que primero debe instalarse localmente:

La estructura del esquema, sus relaciones y la configuración segura de acceso se
documentan en [`backend/README.md`](backend/README.md).

```bash
cd backend
mvn clean install
```

La conexión no contiene credenciales dentro del repositorio. Para configurarla:

1. Copiar `database-example.properties` como `database-local.properties`.
2. Completar en el archivo local el servidor, usuario y contraseña de MySQL.
3. Ejecutar el backend desde la raíz del repositorio o desde la carpeta `backend`.

`database-local.properties` está ignorado por Git y nunca debe subirse. También se
pueden utilizar las variables de entorno `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`,
`DB_PASSWORD` y `DB_PARAMETERS`; estas tienen prioridad sobre el archivo local.
Si el archivo se encuentra en otra ubicación, `DB_CONFIG_FILE` permite indicar su
ruta completa.

Para verificar la conexión:

```bash
cd backend
mvn test-compile exec:java \
  -Dexec.mainClass=sv.asociacion.backend.DatabaseConnectionTest \
  -Dexec.classpathScope=test
```
