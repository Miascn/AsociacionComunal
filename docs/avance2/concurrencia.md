# Concurrencia — evidencia y justificación en el código

> **Avance 2 de Programación II · Criterio 5 (1.20 pts) · SCRUM-315**
> Evidencia tomada de la rama `isolated/advance2-compliance-and-fixes`.
> Todos los conteos de este documento se midieron sobre el código actual, no sobre estimaciones previas.
> **No se modificó ninguna clase Java para producir esta evidencia.**

---

## Resumen medible

Medido sobre `frontend/src/main/java` y `api/src/main/java` (sin contar pruebas):

| Mecanismo | Ocurrencias | Dónde |
|---|---:|---|
| `new Task<>` de JavaFX | **58** | 24 controladores |
| `new Thread(...)` | **55** | los mismos controladores + `UpdateService` |
| Hilos con nombre propio (`new Thread(task, "...")`) | **29** | p. ej. `cargar-miembros-api`, `guardar-asignacion` |
| `setDaemon(true)` | **33** | 18 archivos |
| `Platform.runLater` | **9** | 5 archivos |
| `ScheduledExecutorService` | **1** | `UpdateService` |
| `AtomicBoolean` / `volatile` | **3** | `UpdateService` |
| Hilos virtuales (`Thread.ofVirtual()`) | **3** | `UpdateService` (2), `DesktopShortcutService` (1) |
| `synchronized` | **3** | `VotoService`, `DBConnection`, `DAOFactory` |

> **Corrección respecto al conteo anterior.** La ficha de SCRUM-315 registraba *82* usos de `Task<>`; el conteo real sobre el código es **58**. Los demás valores sí coincidieron. Se deja constancia porque la evidencia debe medirse, no estimarse.

---

## Concurrencia no es paralelismo

Esta distinción es la que sostiene todo lo demás, y conviene decirla antes de los ejemplos.

**Concurrencia** es estructurar el programa en tareas que progresan de forma independiente, de modo que ninguna tenga que esperar a que otra termine. **Paralelismo** es ejecutar varias de esas tareas literalmente al mismo tiempo en núcleos distintos para terminar antes.

**Este sistema usa concurrencia. No usa paralelismo, y no lo necesita.**

La razón es concreta: prácticamente todo el trabajo que se saca del hilo de interfaz es **espera de entrada/salida** —una petición HTTP a la API, una consulta a MySQL, una descarga de archivo—. Durante esa espera el hilo no calcula nada: está bloqueado. Repartir esa espera entre ocho núcleos no la acorta ni un milisegundo, porque el cuello de botella está en la red y en la base de datos, no en el procesador.

Lo que sí se gana es que **la aplicación siga atendiendo a la persona mientras espera**. No es más velocidad; es que no hay congelamiento.

Por eso en el código:

- No hay `parallelStream()`, ni `ForkJoinPool`, ni particionado de trabajo por núcleos.
- Sí hay un `Executors.newSingleThreadScheduledExecutor` — **un solo hilo**, deliberadamente.
- Los `Task<>` de JavaFX no dividen un cálculo: apartan una llamada bloqueante.

Afirmar en la defensa que el sistema "aprovecha varios núcleos" sería falso y el código lo desmiente en un minuto. La afirmación correcta y defendible es: **el sistema nunca bloquea a su usuario esperando por la red.**

---

## 1. El actualizador automático — el caso más fuerte

`frontend/src/main/java/service/UpdateService.java`

Es el ejemplo que debe presentarse primero, porque es el único que **no es plantilla del framework**: aquí hay decisiones de diseño concurrente tomadas a mano.

### El planificador

```java
private static final ScheduledExecutorService SCHEDULER =
    Executors.newSingleThreadScheduledExecutor(runnable -> {   // línea 34
        Thread thread = new Thread(runnable, "qa-update-scheduler");
        thread.setDaemon(true);                                // línea 37
        return thread;
    });

public static void startAutomatic(Window owner) {              // línea 47
    if (!STARTED.compareAndSet(false, true)) return;            // línea 48
    SCHEDULER.schedule(() -> checkAsync(owner), 3, TimeUnit.SECONDS);
    SCHEDULER.scheduleWithFixedDelay(() -> checkAsync(owner), 15, 15, TimeUnit.MINUTES);
}
```

Cuatro decisiones, cada una con su motivo:

1. **`newSingleThreadScheduledExecutor` — un solo hilo.** Comprobar una versión cada 15 minutos no requiere más. Un pool de varios hilos permitiría comprobaciones solapadas, que es justamente lo que no se quiere.
2. **El hilo es `daemon`.** Un hilo no-daemon mantiene viva la JVM aunque se cierre la última ventana. Con un planificador que se repite cada 15 minutos, eso significaría que **la aplicación no cerraría nunca**: el proceso quedaría colgado en segundo plano. `setDaemon(true)` es lo que permite que al cerrar la ventana el proceso realmente termine.
3. **El hilo tiene nombre (`"qa-update-scheduler"`).** En un volcado de hilos o en el depurador aparece identificado, no como `Thread-7`.
4. **`STARTED.compareAndSet(false, true)`** garantiza que el actualizador arranque **una sola vez** aunque `startAutomatic` se llame desde dos sitios. `compareAndSet` es una operación atómica: lee y escribe en un solo paso indivisible. Con un `boolean` normal, dos hilos podrían leer `false` a la vez y arrancar dos planificadores.

### El guardia contra comprobaciones solapadas

```java
private static final AtomicBoolean CHECKING = new AtomicBoolean(false);  // línea 31
private static volatile String lastOfferedVersion;                        // línea 32

private static void checkAsync(Window owner) {                            // línea 67
    if (!CHECKING.compareAndSet(false, true)) return;                     // línea 68

    Thread.ofVirtual().start(() -> {                                      // línea 70
        try {
            ...
            if (compare(manifest.version(), currentVersion) > 0
                && !manifest.version().equals(lastOfferedVersion)) {       // línea 99
                lastOfferedVersion = manifest.version();                   // línea 101
                Platform.runLater(() -> offerUpdate(owner, config, client, manifest));
            }
        } catch (Exception exception) {
            ...
        } finally {
            CHECKING.set(false);                                           // línea 107
        }
    });
}
```

**Qué se está evitando concretamente.** La comprobación periódica se dispara cada 15 minutos, pero una comprobación puede tardar más que eso si la red va mal (el *timeout* del manifiesto es de 20 segundos, pero el del binario es de **3 minutos**). Sin `CHECKING`, dos descargas podrían solaparse y el usuario vería **dos diálogos de actualización encima del otro**.

**Por qué `finally`.** `CHECKING.set(false)` está en el bloque `finally`, no al final del `try`. Si la petición HTTP lanza excepción, la bandera se libera igual. Si estuviera en el `try`, un solo fallo de red dejaría el actualizador bloqueado para siempre.

**Por qué `volatile` en `lastOfferedVersion`.** Este campo lo escribe el hilo de comprobación (línea 101) y lo lee ese mismo tipo de hilo en ejecuciones posteriores, que pueden ser hilos distintos. Sin `volatile`, la JVM puede mantener el valor en la caché de un núcleo y otro hilo seguiría viendo el valor viejo — es un problema de **visibilidad**, no de exclusión mutua. El efecto práctico sería ofrecer al usuario la misma versión una y otra vez. `volatile` obliga a que la escritura se publique y las lecturas posteriores la vean.

### Hilos virtuales (Java 21)

```java
Thread.ofVirtual().start(() -> { ... });   // línea 70 y línea 123
```

La comprobación y la instalación corren en **hilos virtuales**, no en hilos del sistema operativo. Es la elección correcta para este trabajo: un hilo virtual que se bloquea esperando una respuesta HTTP **libera** el hilo portador en vez de mantenerlo ocupado. Para tareas que son casi por completo espera de red, cuesta una fracción de lo que cuesta un hilo de plataforma.

`DesktopShortcutService.ensureAsync()` (línea 11) usa el mismo recurso para crear el acceso directo del escritorio sin retrasar el arranque de la aplicación.

**Beneficio para el usuario:** la aplicación se actualiza sola, sin que nadie tenga que descargar nada a mano, sin interrumpir lo que esté haciendo, y sin impedir que la aplicación cierre.

---

## 2. El pase de lista en asamblea

`frontend/src/main/java/controller/AsistenciasModalController.java`

Este es el caso con la justificación de dominio más clara, y el que se demuestra en vivo.

```java
public void cargarDatos() {                                    // línea 164
    if (reunion == null) return;

    Task<List<AsistenciaModel>> task = new Task<>() {           // línea 167
        @Override
        protected List<AsistenciaModel> call() throws Exception {
            return apiClient.getByReunion(reunion.getId());     // llamada HTTP bloqueante
        }
    };

    task.setOnSucceeded(e -> {                                  // línea 174
        asistenciasList.setAll(task.getValue());
        actualizarMetricas();
    });

    task.setOnFailed(e -> {                                     // línea 179
        Throwable ex = task.getException();
        mostrarAlerta(Alert.AlertType.ERROR, "Error",
            "No se pudo cargar la lista de asistencia: " + ...);
    });

    new Thread(task).start();                                   // línea 185
}
```

**Qué corre concurrentemente.** El método `call()` — y solo él — corre en un hilo aparte. Ahí dentro está `apiClient.getByReunion(...)`, que abre una conexión HTTP contra la API y espera la respuesta.

**Por qué no puede correr en el hilo de JavaFX.** JavaFX tiene un único hilo (*JavaFX Application Thread*) que dibuja la ventana y procesa cada clic. Si esa llamada HTTP se ejecutara ahí, durante todo el tiempo de espera la ventana no repintaría, no respondería a clics, y Windows la marcaría como *"no responde"*. En una asamblea con varias decenas de convocados, con la gente esperando, eso es inaceptable.

**Cómo vuelve a la interfaz de forma segura.** `setOnSucceeded` y `setOnFailed` **no** corren en el hilo de fondo: JavaFX garantiza que ambos manejadores se ejecutan en el hilo de interfaz. Por eso `asistenciasList.setAll(...)` y `actualizarMetricas()` — que tocan la tabla y las etiquetas — son seguros ahí y no necesitan `Platform.runLater`.

**Qué evita la condición de carrera.** La regla que impone JavaFX es estricta: **ningún nodo de la escena puede tocarse fuera del hilo de interfaz.** El patrón `Task` + `setOnSucceeded` respeta esa regla por construcción — el trabajo sucio está en `call()`, la actualización de la vista está en el manejador. No hay un solo campo compartido que dos hilos escriban a la vez, y por eso no hace falta ningún candado.

**Este patrón es el mismo en los 24 controladores** y en las 58 tareas. Las variantes nombradas lo hacen explícito:

```java
Thread thread = new Thread(task, "cargar-miembros-api");   // MiembroController.java:399
thread.setDaemon(true);
thread.start();
```

**Beneficio para el usuario:** el secretario puede seguir buscando un nombre, desplazar la tabla o mover la ventana mientras la lista se está trayendo del servidor.

---

## 3. El retorno controlado al hilo de interfaz

`frontend/src/main/java/service/UpdateService.java`, método `download` (línea 234)

Hay un caso donde `setOnSucceeded` no sirve: cuando hay que informar del **progreso** mientras el trabajo todavía corre. Ahí es donde aparece `Platform.runLater`.

```java
while ((read = input.read(buffer)) >= 0) {
    output.write(buffer, 0, read);
    downloaded += read;
    long done = downloaded;                                  // línea 242
    Platform.runLater(() -> {                                // línea 243
        ProgressBar bar = (ProgressBar) window.getProperties().get("progress");
        Label status  = (Label) window.getProperties().get("status");
        if (total > 0) {
            bar.setProgress(Math.min(1d, (double) done / total));
            status.setText("Descargando cambios: " + (done * 100 / total) + "%");
        }
    });
}
```

**Qué hace `Platform.runLater`.** Encola la acción para que el hilo de interfaz la ejecute en su próximo ciclo. Es el único mecanismo legítimo para tocar la interfaz desde un hilo de fondo.

**El detalle de la línea 242 es lo interesante.** `downloaded` es una variable que cambia en cada vuelta del bucle; una lambda de Java no puede capturarla. Se copia a `done`, que es efectivamente final. Esto no es un tecnicismo del compilador: **garantiza que cada actualización de la barra muestre el valor que tenía en ese instante**, y no el que la variable tenga cuando el hilo de interfaz llegue a ejecutar la lambda —que es más tarde, y para entonces ya habrá cambiado—. Sin esa copia, el porcentaje mostrado sería inconsistente con el momento que representa.

Los demás `Platform.runLater` del proyecto cumplen la misma función de cruce de hilos: `UpdateService:192` (cerrar la aplicación tras lanzar el instalador), `UpdateService:198` (mostrar el error de instalación), `AsistenciasModalController:293`, `ReunionesController:386`, `VotacionesController:455`.

---

## 4. La emisión de votos — y el límite honesto de `synchronized`

`api/src/main/java/sv/asociacion/service/VotoService.java`

```java
public synchronized EmitirVotoResponse emitirVoto(EmitirVotoRequest req, Integer idMiembroSesion) {  // línea 32
    ...
    // 4. Validar que no haya votado previamente
    if (votoDAO.existsByVotacionAndMiembro(req.idVotacion(), idMiembro)) {    // línea 70
        throw new IllegalStateException("El miembro ya ha emitido su voto en esta votación. ...");
    }

    // 5. Registrar voto
    ...
    Voto saved = votoDAO.save(nuevo);                                          // línea 81
}
```

**Por qué existe.** La API corre sobre Javalin/Jetty, que **sí atiende varias peticiones HTTP a la vez, en hilos distintos**. Este es el único punto del sistema donde hay verdadera concurrencia de servidor. El método hace una comprobación (línea 70) y después una escritura (línea 81); entre ambas hay una ventana en la que otro hilo podría colarse. `synchronized` cierra esa ventana: solo un hilo a la vez puede estar dentro de `emitirVoto` **sobre esta instancia de `VotoService`**.

**Lo que `synchronized` NO garantiza — y presentarlo como garantía completa sería incorrecto:**

- Protege **solo dentro de un mismo proceso de la JVM**. Si mañana la API se levantara en dos instancias detrás de un balanceador, cada una tendría su propio candado y el doble voto volvería a ser posible.
- Serializa **todas** las emisiones de voto del sistema, incluso las de votaciones distintas que no compiten entre sí. Es un candado más grueso de lo necesario.

**Dónde está la garantía real:**

```sql
CONSTRAINT uk_voto_votacion_miembro UNIQUE (id_votacion, id_miembro)
```
`schema.sql`, línea 172

Esa restricción la impone **MySQL**, no la aplicación. Es la que hace imposible el doble voto pase lo que pase: sobreviva o no el proceso, haya una instancia o diez. `synchronized` convierte lo que sería un error de base de datos en un mensaje claro para el usuario; la integridad la sostiene la base.

**Beneficio para el usuario:** un miembro no puede votar dos veces en la misma votación, y si lo intenta recibe un mensaje explicativo en vez de un error técnico.

---

## 5. Inicialización perezosa sincronizada

Dos singletons del backend usan `synchronized` por un motivo distinto: no proteger datos, sino **proteger la creación**.

```java
public static synchronized DBConnection getInstance() {   // DBConnection.java:31
    if (instance == null) {
        instance = new DBConnection();
    }
    return instance;
}
```
El mismo patrón en `DAOFactory.java`, línea 8.

Sin `synchronized`, dos hilos de Jetty que pidan la instancia a la vez pueden ver ambos `instance == null` y construir dos objetos. Es el caso clásico de *check-then-act*: la comprobación y la acción deben ser un solo paso indivisible.

Conviene notar que `DBConnection.getConnection()` (línea 38) **no** está sincronizado, y está bien que no lo esté: abre una conexión JDBC nueva en cada llamada, así que cada hilo trabaja con su propia conexión y no hay estado compartido que proteger.

---

## Hallazgos técnicos detectados — se registran, no se corrigen aquí

Al verificar el código para este documento aparecieron dos puntos que **no corresponden a SCRUM-315** (que es exclusivamente documental) y que **no se han modificado**. Se proponen para registrarse en **SCRUM-311**, junto con los demás hallazgos de la evaluación técnica.

### Hallazgo 1 — Hilos de interfaz sin `setDaemon(true)`

De los 55 hilos que crea el cliente de escritorio, **33 son daemon y 22 no lo son**. El criterio no es uniforme entre controladores:

| Controlador | Hilos | Daemon |
|---|---:|---:|
| `MiembroController`, `PeriodosController`, `DirectivaController`, `CargosController`, … | 33 | **33** |
| `AsistenciasModalController` | 4 | **0** |
| `OpcionesVotacionModalController` | 5 | **0** |
| `ReunionesController` | 4 | **0** |
| `VotacionesController` | 4 | **0** |
| `VotacionFormController`, `PapeletaVotacionModalController`, `ReunionFormController` | 5 | **0** |

**Riesgo real:** un hilo no-daemon mantiene viva la JVM hasta que su `call()` termina. Si el usuario cierra la aplicación mientras una petición está esperando su *timeout*, el proceso puede sobrevivir a la ventana durante ese tiempo. No hay pérdida de datos ni corrupción; es un cierre sucio.

**Por qué no se corrige aquí:** SCRUM-315 exige explícitamente que no haya cambios de código, y añadir `setDaemon(true)` en 22 puntos es una modificación funcional que merece su propia prueba y su propio commit.

### Hallazgo 2 — Doble alternancia rápida en el pase de lista

`AsistenciasModalController.toggleAsistenciaServidor` (línea 237) lanza un `Task` por cada clic en la casilla de asistencia, sin desactivar el control mientras la petición viaja. Si se hace clic dos veces rápido sobre el mismo miembro, salen dos peticiones y **el orden en que el servidor las atienda no está garantizado**: el estado final podría no coincidir con el último clic. El manejador `setOnFailed` (línea 251) además revierte el valor visual, lo que con dos peticiones en vuelo puede dejar la casilla mostrando lo contrario de lo que quedó guardado.

En la práctica es poco probable —la operación es rápida y el usuario hace un clic por miembro—, pero es una condición de carrera real. Otros controladores del proyecto ya aplican la mitigación correcta: `AsistenciasModalController.autoConvocar` desactiva el botón antes de lanzar la tarea (`btnAutoConvocar.setDisable(true)`, línea 211) y lo reactiva en ambos manejadores.

**No se corrige en este ticket.** Se propone para SCRUM-311.

---

## Cómo demostrarlo durante la defensa

El orden importa: si se empieza por los `Task<>` de los controladores, un evaluador puede objetar con razón que eso es *el patrón que JavaFX obliga a usar*. Por eso el recorrido arranca por el actualizador, que sí es diseño propio.

**Paso 1 — Abrir `UpdateService.java` y explicar el planificador (líneas 30–51 y 67–70).**
Es el ejemplo que no es plantilla. Los cuatro puntos a decir, en este orden:
un solo hilo porque no hace falta más; **daemon**, porque si no la aplicación no cerraría nunca;
`compareAndSet` para que el actualizador arranque una sola vez; `CHECKING` liberado en `finally`
para que un fallo de red no lo deje bloqueado para siempre.

**Paso 2 — La demostración en vivo: el pase de lista.**
Abrir el modal de asistencias de una reunión con varios convocados y, mientras la tabla se está cargando,
**mover la ventana, desplazar la tabla y escribir en el filtro**. La aplicación responde.
Después señalar `AsistenciasModalController.java:167` — la llamada HTTP está dentro de `call()`— y
`:174` — la tabla se actualiza en `setOnSucceeded`, que corre en el hilo de interfaz.
Es la prueba visible de que el hilo de interfaz nunca se bloqueó.

**Paso 3 — Si preguntan por sincronización y datos compartidos: `VotoService.java:32`.**
Decir las dos cosas, no solo la primera: `synchronized` cierra la ventana entre comprobar y escribir
**dentro de un proceso**, y la garantía definitiva es `uk_voto_votacion_miembro` en `schema.sql:172`.
Reconocer el límite es más sólido que ocultarlo, y es lo que diferencia una respuesta estudiada de una memorizada.

**Paso 4 — Si preguntan por rendimiento o por núcleos: responder la distinción, no el número.**
«El sistema no usa paralelismo y no lo necesita: lo que se saca del hilo de interfaz es espera de red,
no cálculo. Repartirla entre núcleos no la acortaría. Lo que se gana es que la aplicación no se congela.»
El código respalda esa respuesta: no hay `parallelStream` ni `ForkJoinPool` en ninguna parte, y el
planificador es explícitamente de **un solo hilo**.

**Si piden ver un problema de visibilidad concreto:** `UpdateService.java:32`, el campo `volatile`.
Explicar que sin `volatile` el valor puede quedarse en la caché de un núcleo y otro hilo seguiría
viendo el anterior — es visibilidad, no exclusión mutua, y son problemas distintos.

---

## Nota de alcance

**No se modificó ningún archivo `.java` para producir este documento.** La concurrencia descrita ya existía en `develop` (`777ba35`) y este trabajo se limita a localizarla, medirla y justificarla. Los dos hallazgos de la sección anterior se documentan deliberadamente sin corregir: pertenecen a SCRUM-311, la fase de endurecimiento posterior al Avance 2.
