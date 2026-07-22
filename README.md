# AsociacionComunal
Sistema ERP para la administracion de una Asociacion Comunal

## Estructura del Proyecto

```
AsociacionComunal/
├── backend/                  # Modulo libreria (Java SE + JDBC)
│   ├── src/main/java/       # Codigo fuente (sv.asociacion.backend)
│   └── src/main/resources/  # config.properties
│
├── frontend/                # Modulo aplicacion (JavaFX)
│   └── src/main/
│       ├── java/            # Codigo fuente (sv.asociacion.frontend)
│       └── resources/       # FXML, CSS, iconos
│
├── .env                    # Variables de entorno (NO committing)
└── .env.example            # Template de variables
```

## Modulos

### Backend (`sv.asociacion:backend`)
- **Tipo:** Libreria JAR
- **Paquete base:** `sv.asociacion.backend`
- **Contiene:** DAOs, Entities, Database, Utilidades
- **Dependencias:** MySQL Connector (solo runtime JDBC)

### Frontend (`sv.asociacion:frontend`)
- **Tipo:** Aplicacion JavaFX
- **Paquete base:** `sv.asociacion.frontend`
- **Contiene:** Controllers, Views, Services, Models, Components
- **Dependencias:** JavaFX 21, Backend JAR

## Compilacion

### Paso 1: Compilar e instalar el Backend

```bash
cd backend
mvn clean install
```

Esto genera el JAR en el repositorio local de Maven.

### Paso 2: Compilar el Frontend

```bash
cd ../frontend
mvn compile
```

O para empaquetar:
```bash
mvn package
```

### Ejecutar la aplicacion

```bash
cd frontend
mvn javafx:run
```

## Configuracion

### Variables de entorno

El Backend usa variables de entorno para la conexion a la base de datos:

| Variable | Descripcion | Valor por defecto |
|----------|-------------|-------------------|
| `DB_URL` | URL de conexion JDBC | `jdbc:mysql://localhost:3306/asociacion_db` |
| `DB_USER` | Usuario de MySQL | `root` |
| `DB_PASSWORD` | Contrasena de MySQL | `secret` |

### Archivo .env

Copia `.env.example` a `.env` y ajusta los valores:

```bash
cp .env.example .env
```

**Importante:** No hagas commit del archivo `.env` (esta en `.gitignore`).

## Tecnologias

- **Java:** 21
- **JavaFX:** 21
- **Maven:** 3.x
- **MySQL:** 8.0
- **JDBC:** Conexion directa (sin ORM)
