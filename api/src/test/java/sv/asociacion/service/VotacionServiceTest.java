package sv.asociacion.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import sv.asociacion.dao.OpcionVotacionDAO;
import sv.asociacion.dao.ProyectoDAO;
import sv.asociacion.dao.VotacionDAO;
import sv.asociacion.dao.VotoDAO;
import sv.asociacion.domain.dto.VotacionRequest;
import sv.asociacion.domain.dto.VotacionResponse;
import sv.asociacion.domain.entity.OpcionVotacion;
import sv.asociacion.domain.entity.Proyecto;
import sv.asociacion.domain.entity.Votacion;
import sv.asociacion.domain.entity.Voto;

class VotacionServiceTest {
    private MemoryVotacionDAO votacionDAO;
    private MemoryOpcionDAO opcionDAO;
    private MemoryVotoDAO votoDAO;
    private MemoryProyectoDAO proyectoDAO;
    private VotacionService service;

    @BeforeEach
    void setUp() {
        votacionDAO = new MemoryVotacionDAO();
        opcionDAO = new MemoryOpcionDAO();
        votoDAO = new MemoryVotoDAO();
        proyectoDAO = new MemoryProyectoDAO();
        service = new VotacionService(votacionDAO, opcionDAO, votoDAO, proyectoDAO);
    }

    @Test
    void createsVotacionSuccessfully() {
        VotacionRequest req = new VotacionRequest(
            "Consulta sobre Proyecto de Agua",
            "Votación comunal para aprobar la compra de tubería principal",
            null,
            LocalDateTime.now().plusDays(1),
            LocalDateTime.now().plusDays(5),
            List.of("A favor", "En contra")
        );

        VotacionResponse res = service.create(req);
        assertNotNull(res);
        assertNotNull(res.id());
        assertEquals("BORRADOR", res.estado());
        assertEquals("Consulta sobre Proyecto de Agua", res.titulo());
        assertEquals(2, res.totalOpciones());
    }

    @Test
    void rejectsInvertedDates() {
        VotacionRequest req = new VotacionRequest(
            "Fechas Inválidas",
            "Descripción",
            null,
            LocalDateTime.now().plusDays(5),
            LocalDateTime.now().plusDays(1), // Inicio después de Fin
            null
        );

        assertThrows(IllegalArgumentException.class, () -> service.create(req));
    }

    @Test
    void rejectsOpeningWithoutMinimumTwoOptions() {
        // Votación sin opciones
        VotacionResponse res = service.create(new VotacionRequest(
            "Votación Vacía",
            "Sin opciones",
            null,
            LocalDateTime.now().plusDays(1),
            LocalDateTime.now().plusDays(3),
            null
        ));

        assertThrows(IllegalStateException.class, () -> service.abrir(res.id()));

        // Con una sola opción tampoco debe permitirse
        OpcionVotacion op1 = new OpcionVotacion();
        op1.setIdVotacion(res.id());
        op1.setDescripcion("Única opción");
        op1.setOrden((short) 1);
        opcionDAO.save(op1);

        assertThrows(IllegalStateException.class, () -> service.abrir(res.id()));
    }

    // Las cinco pruebas que siguen necesitan alcanzar ABIERTA, de modo que su ventana
    // de fechas debe estar vigente. Antes usaban fechaInicio en el futuro como relleno
    // arbitrario, porque las fechas no gobernaban ninguna transición. Solo cambia el
    // montaje: las aserciones y el propósito son los originales.

    @Test
    void opensVotacionWhenAtLeastTwoOptionsExist() {
        VotacionResponse res = service.create(new VotacionRequest(
            "Votación Válida",
            "Con dos opciones",
            null,
            LocalDateTime.now().minusHours(1),
            LocalDateTime.now().plusDays(3),
            List.of("Opción A", "Opción B")
        ));

        VotacionResponse abierta = service.abrir(res.id());
        assertEquals("ABIERTA", abierta.estado());
    }

    @Test
    void closesVotacionSuccessfully() {
        VotacionResponse res = service.create(new VotacionRequest(
            "Votación para Cerrar",
            "Descripción",
            null,
            LocalDateTime.now().minusHours(1),
            LocalDateTime.now().plusDays(3),
            List.of("Sí", "No")
        ));
        service.abrir(res.id());

        VotacionResponse cerrada = service.cerrar(res.id());
        assertEquals("CERRADA", cerrada.estado());
    }

    @Test
    void rejectsReopeningClosedVotacion() {
        VotacionResponse res = service.create(new VotacionRequest(
            "Votación Concluida",
            "Descripción",
            null,
            LocalDateTime.now().minusHours(1),
            LocalDateTime.now().plusDays(3),
            List.of("Opción 1", "Opción 2")
        ));
        service.abrir(res.id());
        service.cerrar(res.id());

        assertThrows(IllegalStateException.class, () -> service.abrir(res.id()));
    }

    @Test
    void rejectsEditingClosedVotacion() {
        VotacionResponse res = service.create(new VotacionRequest(
            "Votación Inmutable",
            "Descripción",
            null,
            LocalDateTime.now().minusHours(1),
            LocalDateTime.now().plusDays(3),
            List.of("Opción 1", "Opción 2")
        ));
        service.abrir(res.id());
        service.cerrar(res.id());

        assertThrows(IllegalStateException.class, () -> service.update(res.id(), new VotacionRequest(
            "Título Modificado",
            "Nueva descripción",
            null,
            LocalDateTime.now().plusDays(2),
            LocalDateTime.now().plusDays(4),
            null
        )));
    }

    @Test
    void cancelsVotacionSuccessfully() {
        VotacionResponse res = service.create(new VotacionRequest(
            "Votación Descartada",
            "Descripción",
            null,
            LocalDateTime.now().plusDays(1),
            LocalDateTime.now().plusDays(3),
            null
        ));

        VotacionResponse cancelada = service.cancelar(res.id());
        assertEquals("CANCELADA", cancelada.estado());
    }

    @Test
    void deletesDraftVotacion() {
        VotacionResponse res = service.create(new VotacionRequest(
            "Votación Borrador Eliminable",
            "Descripción",
            null,
            LocalDateTime.now().plusDays(1),
            LocalDateTime.now().plusDays(3),
            List.of("Opción 1", "Opción 2")
        ));

        assertTrue(service.delete(res.id()));
        assertTrue(opcionDAO.findByVotacion(res.id()).isEmpty());
    }

    @Test
    void rejectsDeletingOpenOrClosedVotacion() {
        VotacionResponse res = service.create(new VotacionRequest(
            "Votación No Borrable",
            "Descripción",
            null,
            LocalDateTime.now().minusHours(1),
            LocalDateTime.now().plusDays(3),
            List.of("Opción 1", "Opción 2")
        ));
        service.abrir(res.id());

        assertThrows(IllegalStateException.class, () -> service.delete(res.id()));
    }

    @Test
    void rejectsDeletingVotacionWithCastVotes() {
        VotacionResponse res = service.create(new VotacionRequest(
            "Votación con Votos",
            "Descripción",
            null,
            LocalDateTime.now().plusDays(1),
            LocalDateTime.now().plusDays(3),
            List.of("Opción 1", "Opción 2")
        ));

        Voto v = new Voto();
        v.setIdVotacion(res.id());
        votoDAO.save(v);

        assertThrows(IllegalStateException.class, () -> service.delete(res.id()));
    }

    // ==================================================================
    // SCRUM-262 — edición solo de lo no iniciado
    // ==================================================================

    @Test
    void permiteEditarVotacionEnBorrador() {
        VotacionResponse res = service.create(programadaEnElFuturo("Editable en borrador"));
        assertEquals("BORRADOR", res.estado());

        VotacionResponse editada = service.update(res.id(), new VotacionRequest(
            "Título corregido", "Descripción nueva", null,
            LocalDateTime.now().plusDays(2), LocalDateTime.now().plusDays(6), null));

        assertEquals("Título corregido", editada.titulo());
    }

    @Test
    void permiteEditarVotacionProgramada() {
        VotacionResponse res = service.create(programadaEnElFuturo("Editable programada"));
        VotacionResponse programada = service.abrir(res.id());
        assertEquals("PROGRAMADA", programada.estado());

        VotacionResponse editada = service.update(res.id(), new VotacionRequest(
            "Programada corregida", "Otra descripción", null,
            LocalDateTime.now().plusDays(2), LocalDateTime.now().plusDays(6), null));

        assertEquals("Programada corregida", editada.titulo());
        assertEquals("PROGRAMADA", editada.estado());
    }

    @Test
    void rechazaEditarVotacionAbierta() {
        // El hueco principal del criterio: antes se podían cambiar título, proyecto y
        // fechas con la recepción de votos en curso.
        VotacionResponse res = service.create(ventanaVigente("Abierta inmutable"));
        service.abrir(res.id());

        IllegalStateException error = assertThrows(IllegalStateException.class,
            () -> service.update(res.id(), new VotacionRequest(
                "Intento de cambio", "Descripción", null,
                LocalDateTime.now().minusHours(1), LocalDateTime.now().plusDays(3), null)));
        assertTrue(error.getMessage().contains("no iniciada"),
            "El mensaje debe señalar que solo se edita lo no iniciado: " + error.getMessage());
    }

    @Test
    void rechazaEditarVotacionCancelada() {
        VotacionResponse res = service.create(programadaEnElFuturo("Cancelada inmutable"));
        service.cancelar(res.id());

        assertThrows(IllegalStateException.class, () -> service.update(res.id(), new VotacionRequest(
            "Intento de cambio", "Descripción", null,
            LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(3), null)));
    }

    // ==================================================================
    // SCRUM-262 — las fechas gobiernan las transiciones
    // ==================================================================

    @Test
    void abrirAntesDelInicioDejaVotacionProgramada() {
        VotacionResponse res = service.create(programadaEnElFuturo("Aún no comienza"));

        VotacionResponse abierta = service.abrir(res.id());

        assertEquals("PROGRAMADA", abierta.estado(),
            "Antes de la fecha de inicio la apertura debe dejarla programada, no activa.");
    }

    @Test
    void abrirDentroDeLaVentanaActivaLaVotacion() {
        VotacionResponse res = service.create(ventanaVigente("En curso"));

        assertEquals("ABIERTA", service.abrir(res.id()).estado());
    }

    @Test
    void programadaPasaAAbiertaCuandoLlegaLaVentana() {
        // No se puede esperar en tiempo real. Se traslada la ventana al presente
        // editando la votación, algo legítimo porque PROGRAMADA es editable, y se
        // vuelve a invocar la apertura.
        VotacionResponse res = service.create(programadaEnElFuturo("Llega su momento"));
        assertEquals("PROGRAMADA", service.abrir(res.id()).estado());

        service.update(res.id(), new VotacionRequest(
            "Llega su momento", "Descripción", null,
            LocalDateTime.now().minusHours(1), LocalDateTime.now().plusDays(3), null));

        assertEquals("ABIERTA", service.abrir(res.id()).estado());
    }

    @Test
    void abrirProgramadaAntesDeTiempoEsIdempotente() {
        VotacionResponse res = service.create(programadaEnElFuturo("Sigue esperando"));
        assertEquals("PROGRAMADA", service.abrir(res.id()).estado());

        assertEquals("PROGRAMADA", service.abrir(res.id()).estado(),
            "Reintentar la apertura antes de tiempo no debe cambiar el estado.");
    }

    @Test
    void rechazaAbrirVotacionVencida() {
        VotacionResponse res = service.create(new VotacionRequest(
            "Ventana agotada", "Descripción", null,
            LocalDateTime.now().minusDays(5), LocalDateTime.now().minusDays(1),
            List.of("Opción 1", "Opción 2")));

        IllegalStateException error =
            assertThrows(IllegalStateException.class, () -> service.abrir(res.id()));
        assertTrue(error.getMessage().contains("finalización"),
            "El mensaje debe señalar la fecha de finalización: " + error.getMessage());
    }

    // ==================================================================
    // SCRUM-262 — cierre y publicación de resultados
    // ==================================================================

    @Test
    void rechazaCerrarVotacionQueNoEstaAbierta() {
        VotacionResponse borrador = service.create(programadaEnElFuturo("Nunca abierta"));
        assertThrows(IllegalStateException.class, () -> service.cerrar(borrador.id()));

        VotacionResponse programada = service.create(programadaEnElFuturo("Solo programada"));
        service.abrir(programada.id());
        assertThrows(IllegalStateException.class, () -> service.cerrar(programada.id()));

        VotacionResponse cancelada = service.create(programadaEnElFuturo("Descartada"));
        service.cancelar(cancelada.id());
        assertThrows(IllegalStateException.class, () -> service.cerrar(cancelada.id()));
    }

    @Test
    void laVotacionAbiertaNoExponeConteos() {
        VotacionResponse res = service.create(ventanaVigente("Con votos en curso"));
        service.abrir(res.id());
        registrarVotos(res, 3);

        VotacionResponse enCurso = service.findById(res.id());
        assertEquals(0, enCurso.totalVotos(),
            "Una votación abierta no debe publicar el total de votos.");
        assertTrue(enCurso.opciones().stream().allMatch(o -> o.votos() == 0),
            "Ninguna opción debe exponer su conteo antes del cierre.");
        assertEquals(2, enCurso.totalOpciones(),
            "Las opciones siguen visibles: lo que se oculta es el resultado, no la papeleta.");
    }

    @Test
    void laVotacionProgramadaNoExponeConteos() {
        VotacionResponse res = service.create(programadaEnElFuturo("Programada sin resultados"));
        service.abrir(res.id());
        registrarVotos(res, 2);

        VotacionResponse programada = service.findById(res.id());
        assertEquals("PROGRAMADA", programada.estado());
        assertEquals(0, programada.totalVotos());
    }

    @Test
    void laVotacionCanceladaNoExponeConteos() {
        VotacionResponse res = service.create(ventanaVigente("Anulada con votos"));
        service.abrir(res.id());
        registrarVotos(res, 4);
        service.cancelar(res.id());

        VotacionResponse cancelada = service.findById(res.id());
        assertEquals("CANCELADA", cancelada.estado());
        assertEquals(0, cancelada.totalVotos(),
            "Una votación cancelada no publica resultados.");
    }

    @Test
    void laVotacionCerradaExponeConteosReales() {
        VotacionResponse res = service.create(ventanaVigente("Concluida"));
        service.abrir(res.id());
        registrarVotos(res, 5);

        VotacionResponse cerrada = service.cerrar(res.id());

        assertEquals("CERRADA", cerrada.estado());
        assertEquals(5, cerrada.totalVotos(),
            "El cierre publica los resultados reales.");
        assertEquals(5, cerrada.opciones().stream().mapToInt(VotacionResponse.OpcionDetalle::votos).sum());
    }

    // ------------------------------------------------------------------
    // Utilidades de montaje. Las ventanas son amplias a propósito para que las
    // pruebas no dependan del instante exacto de ejecución.
    // ------------------------------------------------------------------

    /** Ventana futura: la apertura debe dejar la votación PROGRAMADA. */
    private static VotacionRequest programadaEnElFuturo(String titulo) {
        return new VotacionRequest(titulo, "Descripción", null,
            LocalDateTime.now().plusDays(2), LocalDateTime.now().plusDays(6),
            List.of("Opción 1", "Opción 2"));
    }

    /** Ventana vigente: la apertura debe activar la votación. */
    private static VotacionRequest ventanaVigente(String titulo) {
        return new VotacionRequest(titulo, "Descripción", null,
            LocalDateTime.now().minusHours(1), LocalDateTime.now().plusDays(3),
            List.of("Opción 1", "Opción 2"));
    }

    /** Reparte votos entre las opciones de la votación, directamente en el almacén. */
    private void registrarVotos(VotacionResponse votacion, int cantidad) {
        List<OpcionVotacion> opciones = opcionDAO.findByVotacion(votacion.id());
        for (int i = 0; i < cantidad; i++) {
            Voto voto = new Voto();
            voto.setIdVotacion(votacion.id());
            voto.setIdOpcion(opciones.get(i % opciones.size()).getIdOpcion());
            votoDAO.save(voto);
        }
    }

    private static final class MemoryVotacionDAO extends VotacionDAO {
        private final List<Votacion> store = new ArrayList<>();
        private int seq = 1;

        @Override
        public Votacion save(Votacion entity) {
            entity.setIdVotacion(seq++);
            store.add(entity);
            return entity;
        }

        @Override
        public Optional<Votacion> findById(Integer id) {
            return store.stream().filter(v -> v.getIdVotacion().equals(id)).findFirst();
        }

        @Override
        public List<Votacion> findAll() {
            return new ArrayList<>(store);
        }

        @Override
        public List<Votacion> findFiltered(Votacion.Estado estado, Integer idProyecto, String busqueda) {
            return store.stream()
                .filter(v -> estado == null || v.getEstado() == estado)
                .filter(v -> idProyecto == null || idProyecto.equals(v.getIdProyecto()))
                .filter(v -> {
                    if (busqueda == null || busqueda.isBlank()) return true;
                    return v.getTitulo().toLowerCase().contains(busqueda.toLowerCase());
                })
                .toList();
        }

        @Override
        public boolean updateEstado(Integer id, Votacion.Estado nuevoEstado) {
            return findById(id).map(v -> {
                v.setEstado(nuevoEstado);
                return true;
            }).orElse(false);
        }

        @Override
        public Votacion update(Votacion entity) {
            for (int i = 0; i < store.size(); i++) {
                if (store.get(i).getIdVotacion().equals(entity.getIdVotacion())) {
                    store.set(i, entity);
                    break;
                }
            }
            return entity;
        }

        @Override
        public boolean delete(Integer id) {
            return store.removeIf(v -> v.getIdVotacion().equals(id));
        }
    }

    private static final class MemoryOpcionDAO extends OpcionVotacionDAO {
        private final List<OpcionVotacion> store = new ArrayList<>();
        private int seq = 1;

        @Override
        public OpcionVotacion save(OpcionVotacion entity) {
            entity.setIdOpcion(seq++);
            store.add(entity);
            return entity;
        }

        @Override
        public List<OpcionVotacion> findByVotacion(Integer idVotacion) {
            return store.stream().filter(o -> o.getIdVotacion().equals(idVotacion)).toList();
        }

        @Override
        public boolean delete(Integer id) {
            return store.removeIf(o -> o.getIdOpcion().equals(id));
        }
    }

    private static final class MemoryVotoDAO extends VotoDAO {
        private final List<Voto> store = new ArrayList<>();
        private long seq = 1L;

        @Override
        public Voto save(Voto entity) {
            entity.setIdVoto(seq++);
            store.add(entity);
            return entity;
        }

        @Override
        public int countByVotacion(Integer idVotacion) {
            return (int) store.stream().filter(v -> v.getIdVotacion().equals(idVotacion)).count();
        }

        @Override
        public int countByOpcion(Integer idOpcion) {
            return (int) store.stream().filter(v -> v.getIdOpcion().equals(idOpcion)).count();
        }
    }

    private static final class MemoryProyectoDAO extends ProyectoDAO {
        @Override
        public Optional<Proyecto> findById(Integer id) {
            return Optional.empty();
        }
    }
}
