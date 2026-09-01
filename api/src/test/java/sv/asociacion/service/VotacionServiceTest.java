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

    @Test
    void opensVotacionWhenAtLeastTwoOptionsExist() {
        VotacionResponse res = service.create(new VotacionRequest(
            "Votación Válida",
            "Con dos opciones",
            null,
            LocalDateTime.now().plusDays(1),
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
            LocalDateTime.now().plusDays(1),
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
            LocalDateTime.now().plusDays(1),
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
            LocalDateTime.now().plusDays(1),
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
            LocalDateTime.now().plusDays(1),
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
