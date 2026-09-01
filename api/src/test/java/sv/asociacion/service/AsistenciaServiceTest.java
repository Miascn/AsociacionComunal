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
import sv.asociacion.dao.AsistenciaDAO;
import sv.asociacion.dao.MiembroDAO;
import sv.asociacion.dao.ReunionDAO;
import sv.asociacion.domain.dto.AsistenciaRequest;
import sv.asociacion.domain.dto.AsistenciaResponse;
import sv.asociacion.domain.dto.ConvocatoriaMasivaRequest;
import sv.asociacion.domain.entity.Asistencia;
import sv.asociacion.domain.entity.Miembro;
import sv.asociacion.domain.entity.Reunion;

class AsistenciaServiceTest {
    private MemoryAsistenciaDAO asistenciaDAO;
    private MemoryReunionDAO reunionDAO;
    private MemoryMiembroDAO miembroDAO;
    private AsistenciaService service;

    private Reunion reunionProgramada;
    private Reunion reunionRealizada;
    private Reunion reunionCancelada;
    private Miembro miembro1;
    private Miembro miembro2;

    @BeforeEach
    void setUp() {
        asistenciaDAO = new MemoryAsistenciaDAO();
        reunionDAO = new MemoryReunionDAO();
        miembroDAO = new MemoryMiembroDAO();
        service = new AsistenciaService(asistenciaDAO, reunionDAO, miembroDAO);

        reunionProgramada = new Reunion(1, "Asamblea Programada", LocalDateTime.now().plusDays(2), "Cancha", Reunion.Tipo.ORDINARIA, Reunion.Estado.PROGRAMADA);
        reunionDAO.save(reunionProgramada);

        reunionRealizada = new Reunion(2, "Asamblea Realizada", LocalDateTime.now().minusDays(1), "Cancha", Reunion.Tipo.ORDINARIA, Reunion.Estado.REALIZADA);
        reunionDAO.save(reunionRealizada);

        reunionCancelada = new Reunion(3, "Asamblea Cancelada", LocalDateTime.now().plusDays(1), "Cancha", Reunion.Tipo.ORDINARIA, Reunion.Estado.CANCELADA);
        reunionDAO.save(reunionCancelada);

        miembro1 = new Miembro();
        miembro1.setIdMiembro(10);
        miembro1.setNombres("Juan");
        miembro1.setApellidos("Perez");
        miembro1.setEstado(Miembro.Estado.ACTIVO);
        miembroDAO.save(miembro1);

        miembro2 = new Miembro();
        miembro2.setIdMiembro(20);
        miembro2.setNombres("Ana");
        miembro2.setApellidos("Gomez");
        miembro2.setEstado(Miembro.Estado.ACTIVO);
        miembroDAO.save(miembro2);
    }

    @Test
    void convocaMiembrosActivosExitosamente() {
        List<AsistenciaResponse> list = service.convocarMiembros(1, new ConvocatoriaMasivaRequest(null));
        assertEquals(2, list.size());
        assertEquals(2, asistenciaDAO.countByReunion(1));
    }

    @Test
    void rechazaConvocatoriaEnReunionCancelada() {
        assertThrows(IllegalStateException.class, () ->
            service.convocarMiembros(3, new ConvocatoriaMasivaRequest(null)));
    }

    @Test
    void registraNuevaAsistenciaCorrectamente() {
        AsistenciaRequest req = new AsistenciaRequest(10, true, "Presente puntual");
        AsistenciaResponse res = service.registrarOActualizar(1, req);

        assertNotNull(res);
        assertEquals(10, res.idMiembro());
        assertTrue(res.asistio());
        assertEquals("Presente puntual", res.observacion());
    }

    @Test
    void actualizaAsistenciaExistente() {
        service.registrarOActualizar(1, new AsistenciaRequest(10, false, "Sin justificar"));

        AsistenciaResponse updated = service.registrarOActualizar(1, new AsistenciaRequest(10, true, "Llegó más tarde con permiso"));
        assertTrue(updated.asistio());
        assertEquals("Llegó más tarde con permiso", updated.observacion());
        assertEquals(1, asistenciaDAO.countByReunion(1));
    }

    @Test
    void toggleAsistenciaExitosamente() {
        AsistenciaResponse initial = service.registrarOActualizar(1, new AsistenciaRequest(20, false, null));
        assertFalse(initial.asistio());

        AsistenciaResponse toggled = service.toggleAsistencia(initial.idAsistencia(), true, "Marcado presente");
        assertTrue(toggled.asistio());
        assertEquals("Marcado presente", toggled.observacion());
    }

    @Test
    void rechazaModificacionEnReunionCancelada() {
        Asistencia a = new Asistencia(99L, 3, 10, false, null);
        asistenciaDAO.save(a);

        assertThrows(IllegalStateException.class, () ->
            service.toggleAsistencia(99L, true, "Intento"));
    }

    @Test
    void eliminaAsistenciaEnReunionProgramada() {
        AsistenciaResponse a = service.registrarOActualizar(1, new AsistenciaRequest(10, false, null));
        service.delete(a.idAsistencia());

        assertEquals(0, asistenciaDAO.countByReunion(1));
    }

    @Test
    void rechazaEliminarAsistenciaEnReunionRealizada() {
        Asistencia a = new Asistencia(100L, 2, 10, true, "Presente");
        asistenciaDAO.save(a);

        assertThrows(IllegalStateException.class, () -> service.delete(100L));
    }

    @Test
    void rechazaObservacionDemasiadoLarga() {
        String muyLarga = "a".repeat(205);
        AsistenciaRequest req = new AsistenciaRequest(10, true, muyLarga);
        assertThrows(IllegalArgumentException.class, () -> service.registrarOActualizar(1, req));
    }

    private static final class MemoryAsistenciaDAO extends AsistenciaDAO {
        private final List<Asistencia> store = new ArrayList<>();
        private long seq = 1L;

        @Override
        public Asistencia save(Asistencia entity) {
            if (entity.getIdAsistencia() == null) {
                entity.setIdAsistencia(seq++);
            }
            store.add(entity);
            return entity;
        }

        @Override
        public Asistencia update(Asistencia entity) {
            store.removeIf(a -> a.getIdAsistencia().equals(entity.getIdAsistencia()));
            store.add(entity);
            return entity;
        }

        @Override
        public Optional<Asistencia> findById(Long id) {
            return store.stream().filter(a -> a.getIdAsistencia().equals(id)).findFirst();
        }

        @Override
        public List<Asistencia> findByReunion(Integer idReunion) {
            return store.stream().filter(a -> a.getIdReunion().equals(idReunion)).toList();
        }

        @Override
        public Optional<Asistencia> findByReunionAndMiembro(Integer idReunion, Integer idMiembro) {
            return store.stream().filter(a -> a.getIdReunion().equals(idReunion) && a.getIdMiembro().equals(idMiembro)).findFirst();
        }

        @Override
        public boolean existsByReunionAndMiembro(Integer idReunion, Integer idMiembro) {
            return store.stream().anyMatch(a -> a.getIdReunion().equals(idReunion) && a.getIdMiembro().equals(idMiembro));
        }

        @Override
        public int countByReunion(Integer idReunion) {
            return (int) store.stream().filter(a -> a.getIdReunion().equals(idReunion)).count();
        }

        @Override
        public int countAsistieronByReunion(Integer idReunion) {
            return (int) store.stream().filter(a -> a.getIdReunion().equals(idReunion) && a.isAsistio()).count();
        }

        @Override
        public boolean delete(Long id) {
            return store.removeIf(a -> a.getIdAsistencia().equals(id));
        }
    }

    private static final class MemoryReunionDAO extends ReunionDAO {
        private final List<Reunion> store = new ArrayList<>();

        @Override
        public Reunion save(Reunion entity) {
            store.add(entity);
            return entity;
        }

        @Override
        public Optional<Reunion> findById(Integer id) {
            return store.stream().filter(r -> r.getIdReunion().equals(id)).findFirst();
        }
    }

    private static final class MemoryMiembroDAO extends MiembroDAO {
        private final List<Miembro> store = new ArrayList<>();

        @Override
        public Miembro save(Miembro entity) {
            store.add(entity);
            return entity;
        }

        @Override
        public Optional<Miembro> findById(Integer id) {
            return store.stream().filter(m -> m.getIdMiembro().equals(id)).findFirst();
        }

        @Override
        public List<Miembro> findByEstado(Miembro.Estado estado) {
            return store.stream().filter(m -> m.getEstado() == estado).toList();
        }
    }
}
