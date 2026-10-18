package sv.asociacion.service;

import sv.asociacion.config.DataSourceConfig;
import sv.asociacion.dao.ConvocatoriaRepository;
import sv.asociacion.domain.dto.ResultadoConvocatoria;
import sv.asociacion.exception.ConvocatoriaIncompletaException;
import sv.asociacion.exception.ValidacionException;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Convoca a los miembros de la asociacion a una reunion en paralelo.
 *
 * <h2>Por que concurrencia</h2>
 * <p>Convocar a una asamblea significa crear un registro de asistencia por cada
 * miembro activo. Antes se hacia uno por uno en el hilo de la peticion, con dos
 * consultas previas por miembro (existe ya / esta activo) y una insercion: con
 * cientos de miembros eran cientos de viajes a MySQL en serie. Ahora la lista se
 * divide en lotes que varios hilos escriben a la vez, cada uno con su propia
 * conexion del pool. El tiempo total queda acotado por el lote mas lento y no por
 * la suma de todos.</p>
 *
 * <h2>Mecanismo</h2>
 * <ul>
 *   <li>{@link ExecutorService} de tamano fijo ({@link DataSourceConfig#HILOS_CONVOCATORIA}
 *       hilos), menor que {@code maxActive} del pool: la convocatoria nunca agota las
 *       conexiones que necesitan las demas peticiones HTTP.</li>
 *   <li>Cada lote es una {@link Callable} que devuelve cuantas asistencias creo; los
 *       resultados se recogen con {@link Future}. No hay contadores compartidos que
 *       sincronizar: cada tarea devuelve su propio numero.</li>
 *   <li>{@code invokeAll} con tiempo maximo: si un lote no termina a tiempo se cancela.</li>
 *   <li>El executor se cierra siempre al terminar (try-with-resources de Java 21:
 *       {@code close()} espera a que acaben las tareas y libera los hilos).</li>
 * </ul>
 *
 * <h2>Problemas de concurrencia y como se controlan</h2>
 * <ul>
 *   <li><b>Duplicados</b> (dos hilos o dos coordinadores convocando a la misma
 *       persona): los impide la restriccion unica de la tabla; el repositorio usa una
 *       insercion idempotente.</li>
 *   <li><b>Conexiones compartidas</b>: cada tarea pide y devuelve la suya.</li>
 *   <li><b>Fallo parcial</b>: cada lote es una transaccion. Si alguno falla, los
 *       demas quedan confirmados, el resultado informa los miembros pendientes y se
 *       lanza {@link ConvocatoriaIncompletaException}. Repetir la convocatoria es seguro.</li>
 *   <li><b>Interrupcion</b>: se restaura el estado de interrupcion del hilo y se
 *       cancelan las tareas pendientes.</li>
 * </ul>
 */
public class ConvocatoriaConcurrenteService {

    /** Miembros por lote: suficientes para aprovechar el batch de JDBC, pocos para que un fallo afecte a pocos. */
    public static final int TAMANO_LOTE = 25;
    public static final Duration TIEMPO_MAXIMO = Duration.ofSeconds(60);

    private final ConvocatoriaRepository repositorio;
    private final int hilos;
    private final int tamanoLote;
    private final Duration tiempoMaximo;

    public ConvocatoriaConcurrenteService(ConvocatoriaRepository repositorio) {
        this(repositorio, DataSourceConfig.HILOS_CONVOCATORIA, TAMANO_LOTE, TIEMPO_MAXIMO);
    }

    public ConvocatoriaConcurrenteService(ConvocatoriaRepository repositorio, int hilos, int tamanoLote, Duration tiempoMaximo) {
        if (hilos < 1 || tamanoLote < 1) {
            throw new IllegalArgumentException("Los hilos y el tamaño de lote deben ser positivos.");
        }
        this.repositorio = Objects.requireNonNull(repositorio);
        this.hilos = hilos;
        this.tamanoLote = tamanoLote;
        this.tiempoMaximo = Objects.requireNonNull(tiempoMaximo);
    }

    /**
     * Convoca a la reunion a los miembros indicados o, si la lista es nula o vacia,
     * a todos los miembros activos. La existencia y el estado de la reunion los
     * valida quien llama ({@link AsistenciaService}).
     */
    public ResultadoConvocatoria convocar(int idReunion, List<Integer> idsSolicitados) {
        long inicio = System.nanoTime();
        List<Integer> ids = (idsSolicitados == null || idsSolicitados.isEmpty())
            ? repositorio.idsMiembrosActivos()
            : sinDuplicados(idsSolicitados);

        List<List<Integer>> lotes = particionar(ids, tamanoLote);
        if (lotes.isEmpty()) {
            return new ResultadoConvocatoria(idReunion, 0, 0, 0, 0, 0, 0, msDesde(inicio), List.of());
        }

        int hilosUsados = Math.min(hilos, lotes.size());
        List<Callable<Integer>> tareas = new ArrayList<>();
        for (List<Integer> lote : lotes) {
            tareas.add(() -> repositorio.convocarLote(idReunion, lote));
        }

        int convocados = 0;
        int lotesFallidos = 0;
        List<Integer> pendientes = new ArrayList<>();
        Throwable primeraCausa = null;

        try (ExecutorService executor = Executors.newFixedThreadPool(hilosUsados, fabricaDeHilos(idReunion))) {
            List<Future<Integer>> futuros = executor.invokeAll(tareas, tiempoMaximo.toMillis(), TimeUnit.MILLISECONDS);
            for (int i = 0; i < futuros.size(); i++) {
                try {
                    convocados += futuros.get(i).get();
                } catch (ExecutionException e) {
                    lotesFallidos++;
                    pendientes.addAll(lotes.get(i));
                    if (primeraCausa == null) primeraCausa = e.getCause();
                } catch (CancellationException e) {
                    lotesFallidos++;
                    pendientes.addAll(lotes.get(i));
                    if (primeraCausa == null) {
                        primeraCausa = new TimeoutException("El lote superó el tiempo máximo de " + tiempoMaximo.toSeconds() + " s.");
                    }
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("La convocatoria fue interrumpida antes de terminar.", e);
        }

        ResultadoConvocatoria resultado = new ResultadoConvocatoria(
            idReunion, ids.size(), convocados, ids.size() - convocados - pendientes.size(),
            lotes.size(), lotesFallidos, hilosUsados, msDesde(inicio), pendientes);

        if (!resultado.completa()) {
            throw new ConvocatoriaIncompletaException(resultado, primeraCausa);
        }
        return resultado;
    }

    /** Divide la lista en sublistas de como maximo {@code tamano} elementos, conservando el orden. */
    static <T> List<List<T>> particionar(List<T> elementos, int tamano) {
        List<List<T>> lotes = new ArrayList<>();
        for (int i = 0; i < elementos.size(); i += tamano) {
            lotes.add(List.copyOf(elementos.subList(i, Math.min(i + tamano, elementos.size()))));
        }
        return lotes;
    }

    private static List<Integer> sinDuplicados(List<Integer> ids) {
        LinkedHashSet<Integer> unicos = new LinkedHashSet<>();
        for (Integer id : ids) {
            if (id == null || id <= 0) {
                throw new ValidacionException("La lista de miembros contiene un identificador inválido: " + id);
            }
            unicos.add(id);
        }
        return List.copyOf(unicos);
    }

    /** Hilos con nombre (se reconocen en un volcado de hilos) y daemon (no impiden apagar la JVM). */
    private static ThreadFactory fabricaDeHilos(int idReunion) {
        AtomicInteger contador = new AtomicInteger(1);
        return tarea -> {
            Thread hilo = new Thread(tarea, "convocatoria-reunion-" + idReunion + "-" + contador.getAndIncrement());
            hilo.setDaemon(true);
            return hilo;
        };
    }

    private static long msDesde(long inicioNanos) {
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - inicioNanos);
    }
}
