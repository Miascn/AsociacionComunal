# Asociación Comunal

Sistema de administración para una asociación comunal con API REST (Javalin) y cliente de escritorio (JavaFX).

## Arquitectura

```
asociacion-api/          ← Servicio web (fat JAR autocontenido)
  └── sv.asociacion/
      ├── controller/    ← Controladores HTTP
      ├── service/       ← Lógica de negocio
      ├── dao/           ← Acceso a datos JDBC
      ├── domain/
      │   ├── entity/    ← Entidades de dominio
      │   └── dto/       ← Objetos de transferencia
      ├── config/        ← Conexión a base de datos
      ├── middleware/     ← Autenticación JWT y shared secret
      └── util/          ← PasswordHasher (PBKDF2), DateUtils

frontend/                ← Cliente de escritorio JavaFX
  └── src/main/java/
      ├── controller/    ← Controladores JavaFX
      ├── service/       ← Clientes HTTP hacia la API
      ├── services/      ← AuthService (vía API, no mock)
      ├── models/        ← Modelos propios (sin dependencia a entidades del backend)
      ├── security/      ← SessionManager (guarda JWT)
      └── app/           ← Punto de entrada
```

## Requisitos

- Java 21 (Temurin JDK recomendado)
- Docker + Docker Compose (para MySQL)
- Apache Maven 3.9+

## Inicio rápido

```bash
# 1. Levantar MySQL con el esquema y datos iniciales
docker-compose up -d

# 2. Configurar variables de entorno
export API_SHARED_SECRET="clave-secreta-de-al-menos-32-caracteres"
export JWT_SECRET="otra-clave-secreta-de-al-menos-32-caracteres"

# 3. Iniciar la API
cd api
mvn package -DskipTests
java -jar target/asociacion-api.jar

# 4. En otra terminal, iniciar el cliente de escritorio
cd frontend
mvn javafx:run
```

## Credenciales por defecto (desarrollo)

La base de datos se inicializa con un usuario administrador:

| Usuario | Contraseña  | Rol   |
|---------|-------------|-------|
| `admin` | `Admin2026!`| ADMIN |

La contraseña está hasheada con PBKDF2WithHmacSHA256 (120.000 iteraciones, salt de 16 bytes)
directamente en `schema.sql`. No hay usuarios simulados en el frontend.

## Pruebas

### Frontend (unitarias, sin base de datos)

```bash
cd frontend
mvn test
```

### API (integración, requiere MySQL corriendo)

```bash
docker-compose up -d

cd api
mvn test
```

Las pruebas de integración verifican:
- Conexión a la base de datos
- Inicio de sesión con credenciales válidas (retorna JWT)
- Rechazo de credenciales inválidas (HTTP 401)

## Endpoints de la API

| Método | Path                    | Autenticación     | Descripción                |
|--------|-------------------------|-------------------|----------------------------|
| GET    | `/health`               | Pública           | Estado del servicio        |
| POST   | `/api/auth/login`       | Pública           | Iniciar sesión, retorna JWT|
| GET    | `/api/miembros`         | JWT               | Listar miembros            |
| POST   | `/api/miembros`         | JWT               | Registrar miembro          |
| GET    | `/api/proyectos`        | JWT               | Listar proyectos           |
| GET    | `/api/usuarios`         | JWT               | Listar usuarios            |
| GET    | `/api/usuarios/{id}`    | JWT               | Obtener usuario            |
| POST   | `/api/usuarios`         | JWT               | Crear usuario              |
| PUT    | `/api/usuarios/{id}`    | JWT               | Actualizar usuario         |
| DELETE | `/api/usuarios/{id}`    | JWT               | Eliminar usuario           |
| GET    | `/api/updates/windows/*`| Shared Secret     | Actualizaciones QA         |

## Variables de entorno

La API lee la configuración en este orden de prioridad:

1. Variables de entorno del sistema (`export VAR=valor`)
2. Archivo `.env` en la raíz del proyecto (cargado automáticamente si existe)
3. Valores por defecto documentados abajo

| Variable           | Obligatoria | Default | Descripción                              |
|--------------------|-------------|---------|------------------------------------------|
| `API_PORT`         | No          | `8080`  | Puerto del servidor                      |
| `API_SHARED_SECRET`| Sí          | —       | Token compartido para endpoints de update|
| `JWT_SECRET`       | No          | fallback a `API_SHARED_SECRET` | Clave para firmar JWT |
| `DB_HOST`          | No          | `localhost` | Host de MySQL                        |
| `DB_PORT`          | No          | `3306`  | Puerto de MySQL                          |
| `DB_NAME`          | No          | `asociacion_comunal` | Base de datos                  |
| `DB_USER`          | Sí          | —       | Usuario de MySQL                         |
| `DB_PASSWORD`      | Sí          | —       | Contraseña de MySQL                      |
| `DB_PARAMETERS`    | No          | `serverTimezone=America/El_Salvador&useUnicode=true&characterEncoding=UTF-8` | Parámetros JDBC |

El archivo `.env` de ejemplo se encuentra en `.env.example`. Copie y complete:

```bash
cp .env.example .env
# edite .env con sus valores locales
```

`.env` está en `.gitignore` y nunca debe subirse al repositorio.