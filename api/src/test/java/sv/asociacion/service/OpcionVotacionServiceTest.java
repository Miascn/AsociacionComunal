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
import sv.asociacion.dao.VotacionDAO;
import sv.asociacion.dao.VotoDAO;
import sv.asociacion.domain.dto.OpcionVotacionRequest;
import sv.asociacion.domain.dto.OpcionVotacionResponse;
import sv.asociacion.domain.entity.OpcionVotacion;
import sv.asociacion.domain.entity.Votacion;
import sv.asociacion.domain.entity.Voto;

class OpcionVotacionServiceTest {
    private MemoryOpcionDAO opcionDAO;
    private MemoryVotacionDAO votacionDAO;
    private MemoryVotoDAO votoDAO;
    private OpcionVotacionService service;

    private Votacion votacionBorrador;
    private Votacion votacionAbierta;

    @BeforeEach
    void setUp() {
        opcionDAO = new MemoryOpcionDAO();
        votacionDAO = new MemoryVotacionDAO();
        votoDAO = new MemoryVotoDAO();
        service = new OpcionVotacionService(opcionDAO, votacionDAO, votoDAO);

        votacionBorrador = new Votacion(1, null, "Consulta Borrador", LocalDateTime.now(), LocalDateTime.now().plusDays(2), Votacion.Estado.BORRADOR);
        votacionDAO.save(votacionBorrador);

        votacionAbierta = new Votacion(2, null, "Consulta Abierta", LocalDateTime.now(), LocalDateTime.now().plusDays(2), Votacion.Estado.ABIERTA);
        votacionDAO.save(votacionAbierta);
    }

    @Test
    void createsOptionSuccessfully() {
        OpcionVotacionRequest req = new OpcionVotacionRequest(votacionBorrador.getIdVotacion(), "A favor", (short) 1);
        OpcionVotacionResponse res = service.create(req);

        assertNotNull(res);
        assertNotNull(res.id());
        assertEquals("A favor", res.descripcion());
        assertEquals(1, res.orden());
        assertTrue(res.editable());
    }

    @Test
    void assignsAutoIncrementalOrderWhenNotSpecified() {
        service.create(new OpcionVotacionRequest(votacionBorrador.getIdVotacion(), "Opción 1", null));
        OpcionVotacionResponse op2 = service.create(new OpcionVotacionRequest(votacionBorrador.getIdVotacion(), "Opción 2", null));

        assertEquals(2, op2.orden());
    }

    @Test
    void rejectsBlankDescription() {
        assertThrows(IllegalArgumentException.class, () ->
            service.create(new OpcionVotacionRequest(votacionBorrador.getIdVotacion(), "   ", null)));
    }

    @Test
    void rejectsDuplicateDescriptionInSameVotacion() {
        service.create(new OpcionVotacionRequest(votacionBorrador.getIdVotacion(), "Opción A", null));

        assertThrows(IllegalArgumentException.class, () ->
            service.create(new OpcionVotacionRequest(votacionBorrador.getIdVotacion(), "opción a", null)));
    }

    @Test
    void rejectsAddingOptionToOpenVotacion() {
        assertThrows(IllegalStateException.class, () ->
            service.create(new OpcionVotacionRequest(votacionAbierta.getIdVotacion(), "Nueva opción tardía", null)));
    }

    @Test
    void updatesOptionSuccessfullyInDraft() {
        OpcionVotacionResponse created = service.create(new OpcionVotacionRequest(votacionBorrador.getIdVotacion(), "Original", null));

        OpcionVotacionResponse updated = service.update(created.id(), new OpcionVotacionRequest(
            votacionBorrador.getIdVotacion(), "Modificada", (short) 3));

        assertEquals("Modificada", updated.descripcion());
        assertEquals(3, updated.orden());
    }

    @Test
    void rejectsUpdatingOptionInOpenVotacion() {
        // Creamos opción en borrador
        OpcionVotacionResponse op = service.create(new OpcionVotacionRequest(votacionBorrador.getIdVotacion(), "Opción X", null));
        // Pasamos a abierta
        votacionBorrador.setEstado(Votacion.Estado.ABIERTA);

        assertThrows(IllegalStateException.class, () ->
            service.update(op.id(), new OpcionVotacionRequest(votacionBorrador.getIdVotacion(), "Cambio ilícito", null)));
    }

    @Test
    void reordersOptionsSuccessfully() {
        OpcionVotacionResponse op1 = service.create(new OpcionVotacionRequest(votacionBorrador.getIdVotacion(), "Primero", null));
        OpcionVotacionResponse op2 = service.create(new OpcionVotacionRequest(votacionBorrador.getIdVotacion(), "Segundo", null));

        // Reordenar invirtiendo
        service.reordenar(votacionBorrador.getIdVotacion(), List.of(op2.id(), op1.id()));

        List<OpcionVotacionResponse> lista = service.findByVotacion(votacionBorrador.getIdVotacion());
        assertEquals(op2.id(), lista.get(0).id());
        assertEquals(1, lista.get(0).orden());
        assertEquals(op1.id(), lista.get(1).id());
        assertEquals(2, lista.get(1).orden());
    }

    @Test
    void rejectsReorderingInOpenVotacion() {
        votacionBorrador.setEstado(Votacion.Estado.ABIERTA);

        assertThrows(IllegalStateException.class, () ->
            service.reordenar(votacionBorrador.getIdVotacion(), List.of(1, 2)));
    }

    @Test
    void deletesOptionSuccessfullyInDraft() {
        OpcionVotacionResponse op = service.create(new OpcionVotacionRequest(votacionBorrador.getIdVotacion(), "Para Borrar", null));
        assertTrue(service.delete(op.id()));
        assertTrue(service.findByVotacion(votacionBorrador.getIdVotacion()).isEmpty());
    }

    @Test
    void rejectsDeletingOptionInOpenVotacion() {
        OpcionVotacionResponse op = service.create(new OpcionVotacionRequest(votacionBorrador.getIdVotacion(), "No Borrable", null));
        votacionBorrador.setEstado(Votacion.Estado.ABIERTA);

        assertThrows(IllegalStateException.class, () -> service.delete(op.id()));
    }

    @Test
    void rejectsDeletingOptionWithVotes() {
        OpcionVotacionResponse op = service.create(new OpcionVotacionRequest(votacionBorrador.getIdVotacion(), "Con Voto", null));

        Voto voto = new Voto();
        voto.setIdVotacion(votacionBorrador.getIdVotacion());
        voto.setIdOpcion(op.id());
        votoDAO.save(voto);

        assertThrows(IllegalStateException.class, () -> service.delete(op.id()));
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
        public Optional<OpcionVotacion> findById(Integer id) {
            return store.stream().filter(o -> o.getIdOpcion().equals(id)).findFirst();
        }

        @Override
        public List<OpcionVotacion> findByVotacion(Integer idVotacion) {
            return store.stream()
                .filter(o -> o.getIdVotacion().equals(idVotacion))
                .sorted((a, b) -> Short.compare(a.getOrden(), b.getOrden()))
                .toList();
        }

        @Override
        public short findMaxOrdenByVotacion(Integer idVotacion) {
            return (short) store.stream()
                .filter(o -> o.getIdVotacion().equals(idVotacion))
                .mapToInt(OpcionVotacion::getOrden)
                .max()
                .orElse(0);
        }

        @Override
        public OpcionVotacion update(OpcionVotacion entity) {
            for (int i = 0; i < store.size(); i++) {
                if (store.get(i).getIdOpcion().equals(entity.getIdOpcion())) {
                    store.set(i, entity);
                    break;
                }
            }
            return entity;
        }

        @Override
        public boolean delete(Integer id) {
            return store.removeIf(o -> o.getIdOpcion().equals(id));
        }
    }

    private static final class MemoryVotacionDAO extends VotacionDAO {
        private final List<Votacion> store = new ArrayList<>();

        @Override
        public Votacion save(Votacion entity) {
            store.add(entity);
            return entity;
        }

        @Override
        public Optional<Votacion> findById(Integer id) {
            return store.stream().filter(v -> v.getIdVotacion().equals(id)).findFirst();
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
        public int countByOpcion(Integer idOpcion) {
            return (int) store.stream().filter(v -> v.getIdOpcion().equals(idOpcion)).count();
        }
    }
}
