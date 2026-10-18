package sv.asociacion.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import sv.asociacion.dao.ConvocatoriaRepository;
import sv.asociacion.domain.dto.ResultadoConvocatoria;
import sv.asociacion.exception.ConexionBDException;
import sv.asociacion.exception.ConvocatoriaIncompletaException;
import sv.asociacion.exception.PersistenciaException;
import sv.asociacion.exception.ValidacionException;

import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

class ConvocatoriaConcurrenteServiceTest {

    private static final Duration TIEMPO = Duration.ofSeconds(10);

    @Test
    @DisplayName("Convoca a todos los activos repartiendo los lotes entre hilos del pool")
    void convocaATodosEnLotesYHilos() {
        RepositorioEnMemoria repo = new RepositorioEnMemoria(miembros(100));
        var servicio = new ConvocatoriaConcurrenteService(repo, 4, 10, TIEMPO);

        ResultadoConvocatoria r = servicio.convocar(5, null);

        assertEquals(100, r.solicitados());
        assertEquals(100, r.convocados());
        assertEquals(0, r.omitidos());
        assertEquals(10, r.lotes());
        assertEquals(4, r.hilos());
        assertTrue(r.completa());
        assertEquals(100, repo.registros.size());
        assertTrue(repo.hilos.stream().allMatch(n -> n.startsWith("convocatoria-reunion-5-")),
            "las tareas corren en los hilos con nombre del executor: " + repo.hilos);
        assertTrue(repo.hilos.size() > 1, "se uso mas de un hilo");
    }

    @Test
    @DisplayName("Los lotes se ejecutan de verdad al mismo tiempo (los 4 estan activos a la vez)")
    void lasTareasSeSolapan() {
        RepositorioEnMemoria repo = new RepositorioEnMemoria(miembros(40));
        repo.esperarA = new CountDownLatch(4);
        var servicio = new ConvocatoriaConcurrenteService(repo, 4, 10, TIEMPO);

        servicio.convocar(1, null);

        assertTrue(repo.todosCoincidieron.get(), "cada lote espero a que los otros tres estuvieran en curso");
        assertEquals(4, repo.maxSimultaneos.get());
    }

    @Test
    @DisplayName("Nunca hay mas tareas simultaneas que hilos configurados: el pool de conexiones no se agota")
    void respetaElLimiteDeHilos() {
        RepositorioEnMemoria repo = new RepositorioEnMemoria(miembros(200));
        repo.pausa = Duration.ofMillis(5);
        var servicio = new ConvocatoriaConcurrenteService(repo, 3, 10, TIEMPO);

        ResultadoConvocatoria r = servicio.convocar(1, null);

        assertEquals(20, r.lotes());
        assertTrue(repo.maxSimultaneos.get() <= 3, "maximo simultaneo: " + repo.maxSimultaneos.get());
    }

    @Test
    @DisplayName("Repetir la convocatoria es idempotente: no crea duplicados")
    void idempotente() {
        RepositorioEnMemoria repo = new RepositorioEnMemoria(miembros(30));
        var servicio = new ConvocatoriaConcurrenteService(repo, 4, 7, TIEMPO);

        servicio.convocar(9, null);
        ResultadoConvocatoria segunda = servicio.convocar(9, null);

        assertEquals(0, segunda.convocados());
        assertEquals(30, segunda.omitidos());
        assertEquals(30, repo.registros.size());
    }

    @Test
    @DisplayName("Dos coordinadores convocando la misma reunion a la vez no duplican: la unicidad la decide el repositorio")
    void dosConvocatoriasSimultaneas() throws Exception {
        RepositorioEnMemoria repo = new RepositorioEnMemoria(miembros(150));
        var servicio = new ConvocatoriaConcurrenteService(repo, 4, 5, TIEMPO);

        ExecutorService coordinadores = Executors.newFixedThreadPool(2);
        try {
            Future<ResultadoConvocatoria> a = coordinadores.submit(() -> servicio.convocar(3, null));
            Future<ResultadoConvocatoria> b = coordinadores.submit(() -> servicio.convocar(3, null));
            int total = a.get(10, TimeUnit.SECONDS).convocados() + b.get(10, TimeUnit.SECONDS).convocados();

            assertEquals(150, total, "entre ambas ejecuciones cada miembro se convoco exactamente una vez");
            assertEquals(150, repo.registros.size());
        } finally {
            coordinadores.shutdownNow();
        }
    }

    @Test
    @DisplayName("Si falla un lote, los demas se confirman y el resultado informa los pendientes")
    void falloDeUnLote() {
        RepositorioEnMemoria repo = new RepositorioEnMemoria(miembros(100));
        repo.fallarConMiembro = 33;
        var servicio = new ConvocatoriaConcurrenteService(repo, 4, 10, TIEMPO);

        ConvocatoriaIncompletaException e = assertThrows(ConvocatoriaIncompletaException.class,
            () -> servicio.convocar(2, null));

        ResultadoConvocatoria r = e.resultado();
        assertFalse(r.completa());
        assertEquals(1, r.lotesFallidos());
        assertEquals(90, r.convocados());
        assertEquals(IntStream.rangeClosed(31, 40).boxed().toList(), r.miembrosPendientes());
        assertInstanceOf(PersistenciaException.class, e.getCause());
        assertFalse(e.porBaseNoDisponible());
        assertFalse(e.getMessage().contains("SQL"));
    }

    @Test
    @DisplayName("Con la base caida la convocatoria incompleta lo indica (se respondera 503)")
    void falloPorBaseCaida() {
        RepositorioEnMemoria repo = new RepositorioEnMemoria(miembros(20));
        repo.fallarConMiembro = 1;
        repo.falloDeConexion = true;
        var servicio = new ConvocatoriaConcurrenteService(repo, 2, 10, TIEMPO);

        ConvocatoriaIncompletaException e = assertThrows(ConvocatoriaIncompletaException.class,
            () -> servicio.convocar(2, null));
        assertTrue(e.porBaseNoDisponible());
    }

    @Test
    @DisplayName("Un lote que excede el tiempo maximo se cancela y queda pendiente")
    void loteLentoSeCancela() {
        RepositorioEnMemoria repo = new RepositorioEnMemoria(miembros(20));
        repo.lentoConMiembro = 15;
        var servicio = new ConvocatoriaConcurrenteService(repo, 2, 10, Duration.ofMillis(300));

        ConvocatoriaIncompletaException e = assertThrows(ConvocatoriaIncompletaException.class,
            () -> servicio.convocar(4, null));

        assertEquals(10, e.resultado().convocados(), "el lote rapido si se confirmo");
        assertEquals(10, e.resultado().miembrosPendientes().size());
        assertInstanceOf(TimeoutException.class, e.getCause());
    }

    @Test
    @DisplayName("Una lista explicita se deduplica; un id invalido es error de validacion")
    void listaExplicita() {
        RepositorioEnMemoria repo = new RepositorioEnMemoria(miembros(5));
        var servicio = new ConvocatoriaConcurrenteService(repo, 2, 10, TIEMPO);

        ResultadoConvocatoria r = servicio.convocar(1, List.of(1, 1, 2));
        assertEquals(2, r.solicitados());
        assertEquals(2, r.convocados());

        assertThrows(ValidacionException.class, () -> servicio.convocar(1, List.of(3, -1)));
    }

    @Test
    @DisplayName("Sin miembros activos no se crea ningun hilo ni lote")
    void sinMiembros() {
        RepositorioEnMemoria repo = new RepositorioEnMemoria(List.of());
        ResultadoConvocatoria r = new ConvocatoriaConcurrenteService(repo).convocar(1, null);
        assertEquals(0, r.lotes());
        assertEquals(0, r.hilos());
        assertTrue(r.completa());
        assertTrue(repo.hilos.isEmpty());
    }

    @Test
    @DisplayName("particionar respeta el orden y el tamano maximo de cada lote")
    void particionar() {
        List<List<Integer>> lotes = ConvocatoriaConcurrenteService.particionar(List.of(1, 2, 3, 4, 5, 6, 7), 3);
        assertEquals(List.of(List.of(1, 2, 3), List.of(4, 5, 6), List.of(7)), lotes);
        assertTrue(ConvocatoriaConcurrenteService.particionar(List.of(), 3).isEmpty());
    }

    private static List<Integer> miembros(int n) {
        return IntStream.rangeClosed(1, n).boxed().toList();
    }

    /**
     * Repositorio seguro para varios hilos. {@code registros.add} es atomico: reproduce
     * la restriccion unica (reunion, miembro) de la base de datos.
     */
    static final class RepositorioEnMemoria implements ConvocatoriaRepository {
        final List<Integer> activos;
        final Set<String> registros = ConcurrentHashMap.newKeySet();
        final Set<String> hilos = ConcurrentHashMap.newKeySet();
        final AtomicInteger enCurso = new AtomicInteger();
        final AtomicInteger maxSimultaneos = new AtomicInteger();
        final java.util.concurrent.atomic.AtomicBoolean todosCoincidieron = new java.util.concurrent.atomic.AtomicBoolean(true);
        volatile CountDownLatch esperarA;
        volatile Duration pausa;
        volatile Integer fallarConMiembro;
        volatile boolean falloDeConexion;
        volatile Integer lentoConMiembro;

        RepositorioEnMemoria(List<Integer> activos) {
            this.activos = activos;
        }

        @Override
        public List<Integer> idsMiembrosActivos() {
            return activos;
        }

        @Override
        public int convocarLote(int idReunion, List<Integer> ids) {
            hilos.add(Thread.currentThread().getName());
            int actuales = enCurso.incrementAndGet();
            maxSimultaneos.accumulateAndGet(actuales, Math::max);
            try {
                if (esperarA != null) {
                    esperarA.countDown();
                    if (!esperarA.await(5, TimeUnit.SECONDS)) todosCoincidieron.set(false);
                }
                if (pausa != null) Thread.sleep(pausa.toMillis());
                if (lentoConMiembro != null && ids.contains(lentoConMiembro)) Thread.sleep(5_000);
                if (fallarConMiembro != null && ids.contains(fallarConMiembro)) {
                    throw falloDeConexion
                        ? new ConexionBDException("Base no disponible.", null)
                        : new PersistenciaException("No fue posible convocar un lote.", null);
                }
                int creados = 0;
                for (Integer id : ids) {
                    if (registros.add(idReunion + ":" + id)) creados++;
                }
                return creados;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new PersistenciaException("Lote interrumpido.", e);
            } finally {
                enCurso.decrementAndGet();
            }
        }
    }
}
