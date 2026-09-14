# Colecciones y estructuras de datos — evidencia y justificación

> **Avance 2 de Programación II · Criterio 7 (0.80 pts) · SCRUM-316**
> Evidencia tomada de la rama `isolated/advance2-compliance-and-fixes`.
> Todos los conteos se midieron sobre el código actual.
> **No se modificó ninguna clase Java para producir esta evidencia.**

---

## Inventario real

Medido con `grep` sobre el código fuente. Se dan dos alcances porque cambian el número y conviene que el revisor pueda reproducir cualquiera de los dos:

| Estructura | `api` + `frontend` (solo `src/main/java`) | Repositorio completo (incluye pruebas) |
|---|---:|---:|
| `List<...>` | **281** | 358 |
| `new ArrayList<>` | **61** | 98 |
| `Map<...>` | **31** | 35 |
| `new HashMap<>` | **16** | 20 |
| `Set<...>` | **3** | 3 |
| `Optional<...>` | **46** | 98 |
| `.stream()` | **78** | 197 |
| `Collectors.` | 5 | — |
| `Arrays.` | 14 | — |
| `Collections.swap` | 2 | — |
| `ObservableList<...>` (JavaFX) | **16** | — |
| `FXCollections.observableArrayList()` | **22** | — |
| `FilteredList<...>` (JavaFX) | **18** | — |

**Estructuras que el proyecto NO usa:** `HashSet`, `TreeSet`, `LinkedHashSet`, `TreeMap`, `LinkedHashMap`, `EnumMap`, `EnumSet`, `LinkedList`, `Queue`, `Deque`, `ArrayDeque`, `Iterator` explícito. **Cero ocurrencias de cada una.**

Esto último no es una carencia: es coherente con el problema. La asociación maneja **listados ordenados que se muestran en una tabla** y **búsquedas por identificador que resuelve MySQL**, no colas de trabajo ni índices ordenados en memoria. Meter un `TreeMap` donde basta una `List` sería complicar el código para lucir variedad.

> **Corrección respecto a la ficha de Jira.** La descripción de SCRUM-316 registraba `List<>` 266, `ArrayList` 100, `Optional<>` 45, `stream()` 60 y `HashMap` 23. Ninguno de esos números coincide exactamente con ninguno de los dos alcances medidos hoy; eran estimaciones. Solo `Map<>` (31) coincidió. Se documentan los valores medidos.

---

## Cuatro cosas distintas que conviene no confundir

La rúbrica habla de «estructuras de datos adecuadas». El proyecto usa cuatro herramientas que se parecen al leer el código pero **no son lo mismo**, y presentarlas como si lo fueran sería un error técnico:

| | Qué es | Papel en este sistema |
|---|---|---|
| **`Collection`** (`List`, `Set`, `Map`*) | Estructuras que **almacenan** elementos. Tienen tamaño, se recorren, se modifican. | Guardar el padrón, las opciones de una papeleta, los intentos de login. |
| **`Optional<T>`** | **No es una colección.** Es un contenedor de cero o un valor que obliga a quien llama a considerar la ausencia. | Lo que devuelven los `DAO` cuando un registro puede no existir. |
| **`Stream<T>`** | **No es una colección.** Es una secuencia de operaciones sobre datos; no almacena nada y se consume una sola vez. | Transformar entidades en DTO de respuesta. |
| **Arreglos (`T[]`)** | Estructura de tamaño **fijo**, del lenguaje, anterior a las colecciones. | Solo en la frontera con Jackson, al deserializar JSON. |

\* `Map` no implementa `java.util.Collection` en sentido estricto —es una interfaz hermana del *Collections Framework*— pero sí es una estructura de datos que almacena. Se documenta como tal.

### Dónde se ve la frontera entre arreglo y colección

`frontend/src/main/java/service/MiembroApiClient.java`, línea 32

```java
public List<MiembroModel> findAll() throws IOException, InterruptedException {
    HttpResponse<String> response = send(request("/api/miembros").GET().build());
    ensure(response, 200);
    return Arrays.stream(json.readValue(response.body(), MiembroResponse[].class))
        .map(MiembroResponse::toModel).toList();
}
```

Jackson entrega un **arreglo** (`MiembroResponse[]`) porque un JSON de tamaño conocido se deserializa así. En la misma línea ese arreglo se convierte en `List`, y **de ahí en adelante el sistema ya no ve arreglos**. La razón: un arreglo tiene tamaño fijo, no se puede filtrar ni añadir sin copiarlo entero, y no encaja con `ObservableList` ni con la tabla de JavaFX. El arreglo vive tres líneas; la lista vive todo el flujo.

El mismo patrón se repite en los 14 usos de `Arrays.` del cliente (`CargoApiClient:55`, `ProyectoApiClient:55`, `VotacionApiClient:58`, …). **No hay un solo arreglo almacenado como estado en el proyecto.**

---

## 1. El contrato de acceso a datos: `List<T>` y `Optional<T>`

`api/src/main/java/sv/asociacion/dao/DAO.java`

```java
public interface DAO<T, ID> {
    Optional<T> findById(ID id);   // línea 7
    List<T> findAll();             // línea 8
    T save(T entity);
    T update(T entity);
    boolean delete(ID id);
}
```

Es la decisión estructural más repetida del sistema: **los 15 DAO la cumplen**.

### `List<T>` para `findAll()` — implementación en `MiembroDAO:28`

```java
@Override
public List<Miembro> findAll() {
    List<Miembro> miembros = new ArrayList<>();
    String sql = "SELECT * FROM miembro";
    try (Connection conn = DBConnection.getInstance().getConnection();
         Statement stmt = conn.createStatement();
         ResultSet rs = stmt.executeQuery(sql)) {
        while (rs.next()) {
            miembros.add(mapResultSet(rs));      // crecimiento dinámico
        }
    } catch (SQLException e) { e.printStackTrace(); }
    return miembros;
}
```

- **Qué contiene:** los miembros del padrón de la asociación.
- **Por qué `List`:** al empezar el bucle **no se sabe cuántas filas trae el `ResultSet`**. Una asociación comunal crece: hoy 40 miembros, el próximo año 60. `ArrayList` crece sola.
- **Qué propiedad se aprovecha:** **crecimiento dinámico** y **orden de inserción preservado**. El orden importa: es el que MySQL devolvió y es el que verá el secretario en la tabla.
- **Por qué una alternativa sería peor:** un arreglo `Miembro[]` obligaría a recorrer el `ResultSet` dos veces —una para contar y otra para llenar— o a redimensionar a mano. Un `Set` sería peor todavía: **perdería el orden** y obligaría a escribir `equals`/`hashCode` en `Miembro`, con el riesgo de que dos miembros distintos con datos parecidos se consideren iguales y uno desaparezca de la lista silenciosamente.

### `Optional<T>` para `findById()` — `MiembroDAO:12`

```java
@Override
public Optional<Miembro> findById(Integer id) {
    ...
    if (rs.next()) return Optional.of(mapResultSet(rs));
    ...
    return Optional.empty();
}
```

- **Qué contiene:** un miembro, o nada.
- **Por qué `Optional` y no `null`:** buscar un miembro por un DUI que no existe **es un caso normal**, no un error. `Optional` obliga al que llama a decidir qué hacer con la ausencia. Con `null`, el compilador no obliga a nada y el fallo aparece más tarde, en otra capa, como `NullPointerException` sin contexto.
- **Cómo lo usa el servicio** (`RolService:36`):
  ```java
  return rolDAO.findById(id).map(this::toResponse).orElse(null);
  ```
  y donde el caso es realmente excepcional, `VotoService:49` lo convierte en un mensaje para la persona:
  ```java
  Miembro m = miembroDAO.findById(idMiembro)
      .orElseThrow(() -> new IllegalArgumentException("Miembro no encontrado con ID: " + idMiembro));
  ```
  La decisión de si la ausencia es «normal» o «error» se toma **en el servicio**, que es quien conoce el dominio, no en el DAO.

---

## 2. El padrón en pantalla: `ObservableList` + `FilteredList`

`frontend/src/main/java/controller/MiembroController.java`

```java
private final ObservableList<MiembroModel> miembros = FXCollections.observableArrayList();  // línea 56
private FilteredList<MiembroModel> miembrosFiltrados;                                        // línea 57

// en initialize():
miembrosFiltrados = new FilteredList<>(miembros, miembro -> true);                            // línea 69
tablaMiembros.setItems(miembrosFiltrados);                                                    // línea 70
```

- **Qué contiene:** el padrón que el secretario ve en la tabla.
- **Por qué `ObservableList` y no `ArrayList`:** una `ObservableList` **avisa a quien la observa cuando cambia**. La tabla de JavaFX está suscrita a ella. Por eso basta con:
  ```java
  miembros.setAll(task.getValue());      // línea 384 — llega la respuesta de la API
  miembros.set(index, updated);          // línea 283 — se editó un miembro
  miembros.add(task.getValue().member()); // línea 330 — se registró uno nuevo
  ```
  y la tabla se repinta sola. **No hay una sola línea de código que redibuje la tabla.**
- **Qué propiedad se aprovecha:** **notificación de cambios** (además del orden y el crecimiento dinámico de toda `List`).
- **Por qué `FilteredList` para el buscador** (`línea 411`):
  ```java
  private void filtrar(String texto) {
      String criterio = normalizar(texto);
      miembrosFiltrados.setPredicate(miembro -> criterio.isBlank()
          || contiene(miembro.getDui(), criterio)
          || contiene(miembro.getNombres(), criterio)
          || contiene(miembro.getApellidos(), criterio)
          || contiene(miembro.getCorreo(), criterio));
      actualizarTotal();
  }
  ```
  `FilteredList` es una **vista** sobre `miembros`: no copia los elementos, solo decide cuáles se ven. Cambiar el predicado reevalúa la vista y la tabla se actualiza.
- **Por qué una alternativa sería peor:** con un `ArrayList` habría que mantener **dos listas a mano** —la completa y la filtrada— y sincronizarlas en cada alta, edición y baja. Ese es exactamente el tipo de duplicación donde se cuelan los errores: registrar un miembro y que no aparezca hasta recargar la pantalla. Además `actualizarTotal()` (línea 405) puede comparar `miembrosFiltrados.size()` contra `miembros.size()` para mostrar *«12 de 47 miembros»* precisamente porque la lista original sigue intacta detrás de la vista.

---

## 3. El orden ES el dato: la papeleta de votación

`frontend/src/main/java/controller/OpcionesVotacionModalController.java`

```java
private final ObservableList<OpcionVotacionModel> opciones = FXCollections.observableArrayList();  // línea 34

private void subirOpcion(OpcionVotacionModel item) {       // línea 177
    int idx = opciones.indexOf(item);
    if (idx <= 0) return;
    Collections.swap(opciones, idx, idx - 1);               // línea 180
    guardarReordenamiento();
}

private void bajarOpcion(OpcionVotacionModel item) {       // línea 184
    int idx = opciones.indexOf(item);
    if (idx < 0 || idx >= opciones.size() - 1) return;
    Collections.swap(opciones, idx, idx + 1);               // línea 187
    guardarReordenamiento();
}

private void guardarReordenamiento() {                      // línea 191
    List<Integer> ids = new ArrayList<>();
    for (OpcionVotacionModel op : opciones) {
        ids.add(op.getIdOpcion());                          // el orden del recorrido ES el mensaje
    }
    ... apiClient.reordenarOpciones(votacion.getId(), ids);
}
```

- **Qué contiene:** las alternativas de una papeleta de votación.
- **Por qué `List` y no `Set` ni `Map`:** aquí el **índice es información del negocio**. La posición de una alternativa en la papeleta la decide la directiva y afecta cómo vota la gente. `List` es la única estructura de las tres que garantiza orden posicional estable y acceso por índice.
- **Qué propiedad se aprovecha:** **orden posicional** (`indexOf`, `swap`, recorrido en secuencia) y, otra vez, **notificación de cambios** para que la lista de la pantalla refleje el movimiento al instante.
- **El detalle bonito de la línea 191:** para persistir el nuevo orden no se envía ningún campo «posición». Se envía **una `List<Integer>` de identificadores**, y el orden de esa lista *es* el orden nuevo. La estructura transporta la información sin necesidad de un campo extra.
- **Por qué una alternativa sería peor:** con un `Set` no existiría `indexOf` ni `swap` — el concepto mismo de «subir una opción» no tendría sentido. Con un `Map<Integer, Opcion>` indexado por posición, cada intercambio obligaría a reescribir las claves de los elementos afectados y a recordar reindexar tras cada alta o baja; un `swap` de una línea se convertiría en un bucle propenso a dejar huecos o posiciones duplicadas.

---

## 4. Unicidad y pertenencia: los roles del sistema

`api/src/main/java/sv/asociacion/service/RolService.java`, línea 13

```java
public static final Set<String> ROLES_BASE = Set.of(
    "ADMIN", "ADMINISTRADOR", "PRESIDENTE", "SECRETARIO", "TESORERO", "SINDICO", "MIEMBRO"
);
```

`api/src/main/java/sv/asociacion/api/auth/AuthService.java`, línea 13

```java
public static final Set<String> SYSTEM_ROLES = Set.of(
    "MIEMBRO", "ADMIN", "ADMINISTRADOR", "DIRECTIVO", "PRESIDENTE", "SECRETARIO", "TESORERO", "SINDICO"
);
```

Y el consumidor, `AuthorizationService.java` línea 6:

```java
public void requireAnyRole(AuthPrincipal principal, Set<String> allowedRoles) { ... }
```

- **Qué contiene:** los cargos de la junta directiva que el sistema reconoce.
- **Por qué `Set`:** las dos únicas preguntas que se le hacen a esta estructura son **«¿este rol existe?»** y **«¿este usuario tiene alguno de estos roles?»**. Las dos son preguntas de **pertenencia**, no de orden ni de posición.
- **Qué propiedad se aprovecha:** **unicidad** —`Set.of` rechaza duplicados en tiempo de construcción, así que un rol repetido por descuido revienta al arrancar en vez de causar comportamiento raro— y **pertenencia directa** (`contains`), que no depende de recorrer.
- **Por qué `Set.of` y no `new HashSet<>`:** `Set.of` devuelve un conjunto **inmutable**. Estos son los roles del sistema: que nadie pueda añadir «SUPERADMIN» en tiempo de ejecución es una propiedad de seguridad, no una preferencia de estilo. Un `HashSet` público y mutable sería un agujero.
- **Por qué una alternativa sería peor:** con una `List<String>` la pregunta «¿existe?» seguiría funcionando, pero nada impediría que el mismo rol apareciera dos veces, y el orden sugeriría una jerarquía que no existe —`PRESIDENTE` no está «antes» de `TESORERO` en ningún sentido real—. La estructura estaría comunicando algo falso.

Estos son los **únicos 3 usos de `Set`** del proyecto, y es correcto que sean pocos: es la única parte del dominio donde lo que importa es la pertenencia y no el orden.

---

## 5. Acceso por clave: el bloqueo por intentos fallidos

`frontend/src/main/java/services/auth/AuthService.java`

```java
private static final int MAX_ATTEMPTS = 5;                                   // línea 13
private static final long LOCKOUT_MILLIS = 30_000L;                          // línea 14

private final Map<String, Integer> failedAttempts = new HashMap<>();          // línea 16
private final Map<String, Long>    lockedUntil    = new HashMap<>();          // línea 17

private void registerFailure(String username) {                               // línea 52
    int attempts = failedAttempts.getOrDefault(username, 0) + 1;              // línea 53
    failedAttempts.put(username, attempts);
    if (attempts >= MAX_ATTEMPTS) {
        lockedUntil.put(username, System.currentTimeMillis() + LOCKOUT_MILLIS);
    }
}

private boolean isLocked(String username) {                                   // línea 39
    Long until = lockedUntil.get(username);
    if (until == null) return false;
    if (System.currentTimeMillis() > until) {
        lockedUntil.remove(username);
        failedAttempts.remove(username);
        return false;
    }
    return true;
}
```

- **Qué contiene:** cuántas veces falló cada usuario, y hasta cuándo está bloqueado cada usuario.
- **Por qué `Map` y no `List`:** la pregunta siempre es **«¿cuántos intentos lleva *este* usuario?»**. Es una búsqueda por clave, y la clave natural es el nombre de usuario ya normalizado (`normalize()`, línea 60, lo pasa a minúsculas y le quita espacios — sin eso, `Carlos` y `carlos ` serían dos entradas distintas y el bloqueo no serviría de nada).
- **Qué propiedad se aprovecha:** **acceso por clave en tiempo constante**, y sobre todo **la clave como identidad**: no hay forma de que un usuario tenga dos contadores.
- **El detalle de la línea 53:** `getOrDefault(username, 0)` resuelve en una sola expresión el caso «este usuario nunca ha fallado». Sin él haría falta un `if (containsKey(...))` antes de cada incremento.
- **Por qué una alternativa sería peor:** con una `List<IntentoFallido>` cada comprobación tendría que recorrer la lista buscando el usuario, y habría que escribir a mano la lógica de «si ya existe una entrada, actualízala; si no, créala» — exactamente lo que `put` y `getOrDefault` ya hacen. Peor aún, un descuido dejaría dos entradas para el mismo usuario y el bloqueo dejaría de contar bien.

Los **dos mapas separados** en vez de uno solo con un objeto de valor también es una decisión: cada uno responde una pregunta distinta y con ciclos de vida distintos. Se limpian juntos (líneas 29-30 tras un login correcto, 45-46 al expirar el bloqueo).

---

## 6. La lista como unidad de trabajo: `AbstractArchivoDAO`

`api/src/main/java/sv/asociacion/dao/archivo/AbstractArchivoDAO.java`

> Este es el **único ejemplo de código nuevo del Avance 2** en el documento, y va al final a propósito: los cinco casos anteriores ya existían en `develop` (`777ba35`). Las colecciones son una práctica establecida del sistema, no algo añadido para cumplir la rúbrica.

```java
@Override
public T save(T entidad) {
    List<T> registros = leerTodo();                    // 1. cargar toda la lista
    if (idDe(entidad) == null) asignarIdentidad(entidad, registros);
    ...
    if (indiceDe(registros, id) >= 0) {
        throw new IllegalStateException("Ya existe un registro con el identificador " + id + ".");
    }
    registros.add(entidad);                            // 2. modificar en memoria
    escribirTodo(registros);                           // 3. persistir la lista completa
    return entidad;
}

@Override
public T update(T entidad) {
    List<T> registros = leerTodo();
    int posicion = indiceDe(registros, id);
    if (posicion < 0) throw new IllegalStateException("No existe un registro con el identificador " + id + ".");
    registros.set(posicion, entidad);                  // reemplazo por índice
    escribirTodo(registros);
    return entidad;
}

@Override
public boolean delete(ID id) {
    List<T> registros = leerTodo();
    int posicion = indiceDe(registros, id);
    if (posicion < 0) return false;
    registros.remove(posicion);                        // eliminación por índice
    escribirTodo(registros);
    return true;
}
```

- **Qué contiene:** todos los registros del archivo `.dat`.
- **Por qué `List<T>`:** la persistencia por serialización de Java escribe y lee **un objeto completo**. La `List` es ese objeto: la unidad que se carga entera, se modifica en memoria y se vuelve a escribir entera. Las tres operaciones de escritura siguen el mismo ciclo *leer → modificar → escribir*, y la lista es lo que hace que ese ciclo sea uniforme.
- **Qué propiedad se aprovecha:** **acceso y modificación por índice** (`set`, `remove(int)`) y que `ArrayList` sea `Serializable`, lo que permite escribirla de una sola llamada.
- **Por qué una alternativa sería peor:** un `Map<ID, T>` daría búsquedas más rápidas, pero **perdería el orden de los registros** —que aquí es el orden de alta— y obligaría a duplicar el identificador: una vez como clave del mapa y otra dentro de la entidad, con el riesgo de que se desincronicen. Con `List`, el identificador vive en un solo sitio y `idDe(entidad)` es la única forma de obtenerlo.
- **Coherencia deliberada:** `findAll()` devuelve `List<T>` y `findById()` devuelve `Optional<T>` exactamente como `MiembroDAO` contra MySQL. Por eso `CensoMiembrosService` funciona igual con cualquiera de los dos orígenes sin un solo `instanceof` — es la evidencia de polimorfismo de SCRUM-314, y descansa en que **ambas implementaciones eligieron las mismas estructuras**.

---

## Streams: transformación, no almacenamiento

Los 78 usos de `.stream()` cumplen casi siempre la misma función: **convertir entidades del dominio en DTO de respuesta**.

```java
// MiembroService.java:21
return miembroDAO.findAll().stream().map(MiembroResponse::from).toList();

// ViviendaService.java:21
var residents = dao.residents(id).stream()
    .map(r -> new ResidentResponse(r.getIdResidente(), r.getIdMiembro(), r.getNombreCompleto(),
        r.getTipoPersona() == null ? null : r.getTipoPersona().name(), r.isRepresentante()))
    .toList();
```

**El `Stream` no guarda nada.** Empieza en una `List` (la del DAO) y termina en otra `List` (`toList()`, que además devuelve una lista **inmutable**). Lo que aporta es que la transformación se lea como una sola frase en vez de como un bucle con una lista acumuladora declarada tres líneas antes.

Conviene notar que **el proyecto no abusa de ellos**: solo 5 usos de `Collectors.` en todo el código, y ninguna agregación compleja en memoria. Las sumas y los conteos se piden a MySQL, que es quien debe hacerlos:

```java
// AportacionService.java:91-92
int total = aportacionDAO.countFiltered(...);
BigDecimal totalRecaudado = aportacionDAO.sumFiltered(...);
```

Sumar en Java lo que la base suma mejor obligaría a traer todas las aportaciones a memoria. La decisión correcta es la que está.

---

## Hallazgos detectados — se registran, no se corrigen aquí

Al recorrer el código para este inventario aparecieron dos puntos relacionados con colecciones. **No se ha modificado nada.** Se proponen para **SCRUM-311**.

### Hallazgo 3 — Consulta N+1 en el listado de votaciones

`api/src/main/java/sv/asociacion/service/VotacionService.java`, líneas 33-37 y 200-217

```java
public List<VotacionResponse> findFiltered(...) {
    return votacionDAO.findFiltered(estado, idProyecto, busqueda).stream()
        .map(this::mapToResponse)          // ← una vez por votación
        .toList();
}

private VotacionResponse mapToResponse(Votacion v) {
    List<OpcionVotacion> ops = opcionDAO.findByVotacion(v.getIdVotacion());   // 1 consulta
    for (OpcionVotacion op : ops) {
        int votosOp = votoDAO.countByOpcion(op.getIdOpcion());                // 1 consulta POR OPCIÓN
        ...
    }
}
```

Listar **V** votaciones con **M** opciones cada una cuesta aproximadamente **V × (2 + M) consultas**. Con 10 votaciones de 4 opciones son ~60 viajes a MySQL para pintar una pantalla.

La mejora natural es de estructura de datos: una sola consulta agrupada (`GROUP BY id_opcion`) cargada en un `Map<Integer, Integer>` de *idOpcion → conteo*, consultado en memoria. Reduce ~60 consultas a 3.

**No se corrige aquí** porque SCRUM-316 es exclusivamente documental y esto es un cambio funcional que necesita su propia prueba.

### Hallazgo 4 — Mapas de bloqueo sin caducidad ni alcance estable

`frontend/src/main/java/services/auth/AuthService.java`, líneas 16-17

Dos cuestiones distintas sobre los mismos mapas:

1. **Crecen sin límite.** Una entrada de `failedAttempts` solo se elimina si el usuario acaba entrando (línea 29) o si llegó a bloquearse y el bloqueo expiró (línea 46). Quien falla 3 veces y se va deja su entrada para siempre. En un cliente de escritorio con un usuario por sesión el impacto es despreciable, pero la estructura no tiene política de caducidad.
2. **El bloqueo es por instancia.** `LoginController:44` crea `new AuthService()` como campo del controlador. Si la pantalla de login se reconstruye, los mapas nacen vacíos y el contador de intentos vuelve a cero. **El bloqueo de 30 segundos puede ser evitable** reabriendo la ventana.

El segundo punto es el que importa, y es de seguridad más que de colecciones. **Se registra para SCRUM-311** junto con los demás hallazgos de la evaluación técnica; no se toca en este ticket.

---

## Cómo demostrarlo durante la defensa

Tres archivos, en este orden. Cada uno responde una pregunta distinta, así no se repite el argumento.

**1 — «¿Por qué `List` y no otra cosa?» → `MiembroDAO.java:28`**
Abrir `findAll()`. El bucle `while (rs.next()) miembros.add(...)` lo dice solo: **no se sabe cuántos miembros hay hasta terminar de leer**. Un arreglo obligaría a contar primero; un `Set` perdería el orden que devolvió MySQL y exigiría `equals`/`hashCode` en `Miembro`. La `ArrayList` crece sola y conserva el orden. Dos líneas más arriba está `findById` devolviendo `Optional` — sirve para la pregunta que casi siempre sigue.

**2 — «¿Hay algún caso donde la estructura aporte algo que no sea guardar?» → `OpcionesVotacionModalController.java:177-195`**
Es el mejor ejemplo del proyecto. Mostrar `Collections.swap(opciones, idx, idx - 1)` y después `guardarReordenamiento()`: al servidor **no se le manda ningún campo de posición**, se le manda una `List<Integer>` de identificadores y el orden de esa lista *es* el orden nuevo. Aquí el índice no es un detalle de implementación: es el dato del negocio. Con un `Set` la operación «subir una opción» ni siquiera se podría escribir.

**3 — Si preguntan por `Map` o por búsqueda por clave → `services/auth/AuthService.java:16, 52-58`**
`failedAttempts.getOrDefault(username, 0) + 1`. La pregunta siempre es «¿cuántos intentos lleva *este* usuario?» — es acceso por clave, y la clave es la identidad. Con una `List` habría que recorrerla en cada intento y escribir a mano el «si existe actualiza, si no crea».

**Si preguntan por variedad de estructuras** —por qué no hay `TreeMap`, `Queue` o `LinkedList`— la respuesta honesta es mejor que inventar un uso: *el sistema maneja listados ordenados que se muestran en tabla y búsquedas por identificador que resuelve MySQL. No hay colas de trabajo ni necesidad de índices ordenados en memoria. Cada estructura que sí está, está porque el problema la pedía.* Las tres únicas apariciones de `Set` (`RolService:13`, `AuthService:13`) sirven justo para eso: demostrar que cuando el dominio sí pide unicidad y pertenencia, la estructura cambia.

---

## Nota de alcance

**No se modificó ningún archivo `.java` para producir este documento.** Cinco de los seis casos documentados ya existían en `develop` (`777ba35`); el sexto (`AbstractArchivoDAO`) proviene de SCRUM-39 y se incluye al final, marcado como tal, para no dar la impresión de que las colecciones son una práctica introducida por el Avance 2. Los dos hallazgos de la sección anterior se documentan deliberadamente sin corregir: pertenecen a SCRUM-311.
