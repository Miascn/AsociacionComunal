### Funcionalidad desarrollada

Se implemento la capa de persistencia del sistema `AsociacionComunal` siguiendo el patron DAO (Data Access Object) con conexion JDBC directa a MySQL 8.0, sin uso de ORM.

**Componentes creados en el backend:**
- **14 Entidades:** Rol, Miembro, Usuario, Cargo, PeriodoDirectiva, MiembroCargo, Aportacion, Proyecto, Votacion, OpcionVotacion, Voto, Reunion, Asistencia, Bitacora
- **14 DAOs:** Implementacion del patron `DAO<T, ID>` con `Optional<T>` para manejo seguro de nulos
- **DAOFactory:** Factory para obtencion de instancias de DAOs
- **DBConnection:** Singleton para gestion de conexiones JDBC con `config.properties`
- **DateUtils:** Utilidades para conversion de fechas
- **config.properties** para configuracion de conexion a la base de datos

### Archivos principales modificados

| Categoria | Archivos |
|-----------|----------|
| **Entidades** | `backend/src/main/java/sv/asociacion/backend/entity/*.java` (14 archivos) |
| **DAOs** | `backend/src/main/java/sv/asociacion/backend/dao/*DAO.java` (15 archivos) |
| **Database** | `DBConnection.java`, `DatabaseInitializer.java` |
| **Config** | `backend/pom.xml` (sv.asociacion:backend, sin JavaFX, solo MySQL JDBC), `config.properties` |
| **Paquetes Frontend** | Refactorizacion de `app.*`, `views.*`, `services.*`, `models.*`, `components.*` a `sv.asociacion.frontend.*` |
| **POMs** | `backend/pom.xml` configurado como libreria JAR, `frontend/pom.xml` con dependencia al backend |
| **Documentacion** | `README.md` con instrucciones de compilacion |

### Estructura de paquetes resultante

```
backend/  (sv.asociacion:backend - Libreria JAR)
└── sv.asociacion.backend/
    ├── dao/       (DAO<T,ID>, DAOFactory, 14 *DAO)
    ├── database/  (DBConnection, DatabaseInitializer)
    ├── entity/    (14 entidades)
    └── util/      (DateUtils)

frontend/  (sv.asociacion:frontend - Depende de backend)
└── sv.asociacion.frontend/
    ├── app/       (Main)
    ├── views/     (DashboardView, DashboardContent)
    ├── services/   (MockDashboardService)
    ├── models/    (DashboardData)
    └── components/ (Sidebar, Header, DashboardCard)
```

### Como se puede probar

**1. Compilar e instalar el backend:**
```bash
cd backend
mvn clean install
```

**2. Compilar el frontend:**
```bash
cd frontend
mvn compile
```

**3. Verificar la conexion a la base de datos (con MySQL corriendo):**
```bash
java -cp "backend/target/classes:$(mvn dependency:build-classpath -q -DincludeScope=runtime -Dmdep.outputFile=/dev/stdout)" sv.asociacion.backend.DatabaseConnectionTest
```

### Aspectos pendientes

| Aspecto | Estado |
|---------|--------|
| Login/autenticacion | No implementado (necesita `UsuarioDAO.findByNombreUsuario()` y verificacion de password) |
| Logica de negocio (services en backend) | No existe - solo capa DAO |
| Controllers y logica JavaFX | No implementado (scope de otro modulo) |
| Tests unitarios | No se crearon tests formales |
| Empaquetado .exe | Pendiente cuando el frontend este completo |

### Notas adicionales

| Nota | Detalle |
|------|---------|
| Arquitectura | El modulo `backend` esta configurado como **libreria JAR** (`sv.asociacion:backend`) |
| Dependencia | El modulo `frontend` tiene como dependencia Maven al backend (`sv.asociacion:frontend` depende de `sv.asociacion:backend`) |
| Configuracion | El `config.properties` viaja dentro del JAR del backend - para cambiar credenciales en produccion es necesario recompilar |
| Prueba de conexion | La conexion a la base de datos fue probada exitosamente con 14 tablas y datos iniciales |
