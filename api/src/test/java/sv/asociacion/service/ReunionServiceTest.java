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
import sv.asociacion.dao.ReunionDAO;
import sv.asociacion.domain.dto.ReunionRequest;
import sv.asociacion.domain.dto.ReunionResponse;
import sv.asociacion.domain.entity.Asistencia;
import sv.asociacion.domain.entity.Reunion;

class ReunionServiceTest {
    private MemoryReunionDAO reunionDAO;
    private MemoryAsistenciaDAO asistenciaDAO;
    private ReunionService service;

    @BeforeEach
    void setUp() {
        reunionDAO = new MemoryReunionDAO();
        asistenciaDAO = new MemoryAsistenciaDAO();
        service = new ReunionService(reunionDAO, asistenciaDAO);
    }

    @Test
    void createsReunionSuccessfullyWithProgramadaStatus() {
        ReunionRequest req = new ReunionRequest(
            "Asamblea General Ordinaria",
            "2026-10-15 14:00:00",
            "Casa Comunal",
            "ORDINARIA",
            null
        );

        ReunionResponse res = service.create(req);
        assertNotNull(res);
        assertNotNull(res.id());
        assertEquals("Asamblea General Ordinaria", res.titulo());
        assertEquals("PROGRAMADA", res.estado());
        assertEquals("ORDINARIA", res.tipo());
        assertTrue(res.editable());
    }

    @Test
    void rejectsCreationWithBlankTitle() {
        ReunionRequest req = new ReunionRequest(
            "   ",
            "2026-10-15 14:00:00",
            "Casa Comunal",
            "ORDINARIA",
            null
        );
        assertThrows(IllegalArgumentException.class, () -> service.create(req));
    }

    @Test
    void rejectsCreationWithInvalidDateFormat() {
        ReunionRequest req = new ReunionRequest(
            "Reunión Extraordinaria",
            "fecha-invalida",
            "Casa Comunal",
            "EXTRAORDINARIA",
            null
        );
        assertThrows(IllegalArgumentException.class, () -> service.create(req));
    }

    @Test
    void updatesReunionWhenProgramada() {
        Reunion r = new Reunion(1, "Reunión Inicial", LocalDateTime.now().plusDays(2), "Cancha", Reunion.Tipo.ORDINARIA, Reunion.Estado.PROGRAMADA);
        reunionDAO.save(r);

        ReunionRequest updateReq = new ReunionRequest(
            "Reunión Modificada",
            "2026-11-20 10:00:00",
            "Salón de Usos Múltiples",
            "EXTRAORDINARIA",
            null
        );

        ReunionResponse res = service.update(1, updateReq);
        assertEquals("Reunión Modificada", res.titulo());
        assertEquals("EXTRAORDINARIA", res.tipo());
        assertEquals("Salón de Usos Múltiples", res.lugar());
    }

    @Test
    void rejectsUpdateWhenRealizadaOrCancelada() {
        Reunion rRealizada = new Reunion(2, "Reunión Pasada", LocalDateTime.now().minusDays(1), "Cancha", Reunion.Tipo.ORDINARIA, Reunion.Estado.REALIZADA);
        reunionDAO.save(rRealizada);

        ReunionRequest updateReq = new ReunionRequest("Intento cambio", "2026-11-20 10:00:00", "Lugar", "ORDINARIA", null);
        assertThrows(IllegalStateException.class, () -> service.update(2, updateReq));
    }

    @Test
    void transitionsFromProgramadaToRealizada() {
        Reunion r = new Reunion(3, "Reunión de Proyectos", LocalDateTime.now().plusDays(1), "Cancha", Reunion.Tipo.ORDINARIA, Reunion.Estado.PROGRAMADA);
        reunionDAO.save(r);

        ReunionResponse res = service.marcarRealizada(3);
        assertEquals("REALIZADA", res.estado());
        assertFalse(res.editable());
    }

    @Test
    void transitionsFromProgramadaToCancelada() {
        Reunion r = new Reunion(4, "Reunión por Lluvia", LocalDateTime.now().plusDays(1), "Cancha", Reunion.Tipo.ORDINARIA, Reunion.Estado.PROGRAMADA);
        reunionDAO.save(r);

        ReunionResponse res = service.cancelar(4);
        assertEquals("CANCELADA", res.estado());
        assertFalse(res.editable());
    }

    @Test
    void rejectsCancelingRealizadaReunion() {
        Reunion r = new Reunion(5, "Reunión Concluida", LocalDateTime.now().minusDays(1), "Cancha", Reunion.Tipo.ORDINARIA, Reunion.Estado.REALIZADA);
        reunionDAO.save(r);

        assertThrows(IllegalStateException.class, () -> service.cancelar(5));
    }

    @Test
    void deletesReunionWithoutAsistencias() {
        Reunion r = new Reunion(6, "Reunión sin asistencia", LocalDateTime.now().plusDays(5), "Cancha", Reunion.Tipo.ORDINARIA, Reunion.Estado.PROGRAMADA);
        reunionDAO.save(r);

        service.delete(6);
        assertTrue(reunionDAO.findById(6).isEmpty());
    }

    @Test
    void rejectsDeleteWhenReunionHasAsistencias() {
        Reunion r = new Reunion(7, "Reunión con lista", LocalDateTime.now().plusDays(1), "Cancha", Reunion.Tipo.ORDINARIA, Reunion.Estado.PROGRAMADA);
        reunionDAO.save(r);

        Asistencia a = new Asistencia(1L, 7, 10, true, "Presente");
        asistenciaDAO.save(a);

        assertThrows(IllegalStateException.class, () -> service.delete(7));
    }

    @Test
    void calculatesAttendancePercentageCorrectly() {
        Reunion r = new Reunion(8, "Reunión Quórum", LocalDateTime.now().minusHours(2), "Cancha", Reunion.Tipo.ORDINARIA, Reunion.Estado.REALIZADA);
        reunionDAO.save(r);

        // 4 convocados, 3 asistieron -> 75.0%
        asistenciaDAO.save(new Asistencia(1L, 8, 1, true, null));
        asistenciaDAO.save(new Asistencia(2L, 8, 2, true, null));
        asistenciaDAO.save(new Asistencia(3L, 8, 3, true, null));
        asistenciaDAO.save(new Asistencia(4L, 8, 4, false, "Ausente"));

        ReunionResponse res = service.getById(8);
        assertEquals(4, res.totalConvocados());
        assertEquals(3, res.totalAsistentes());
        assertEquals(75.0, res.porcentajeAsistencia());
    }

    private static final class MemoryReunionDAO extends ReunionDAO {
        private final List<Reunion> store = new ArrayList<>();
        private int seq = 1;

        @Override
        public Reunion save(Reunion entity) {
            if (entity.getIdReunion() == null) {
                entity.setIdReunion(seq++);
            }
            store.add(entity);
            return entity;
        }

        @Override
        public Optional<Reunion> findById(Integer id) {
            return store.stream().filter(r -> r.getIdReunion().equals(id)).findFirst();
        }

        @Override
        public List<Reunion> findAll() {
            return new ArrayList<>(store);
        }

        @Override
        public List<Reunion> findFiltered(String search, Reunion.Tipo tipo, Reunion.Estado estado) {
            return store.stream()
                .filter(r -> search == null || r.getTitulo().toLowerCase().contains(search.toLowerCase()))
                .filter(r -> tipo == null || r.getTipo() == tipo)
                .filter(r -> estado == null || r.getEstado() == estado)
                .toList();
        }

        @Override
        public Reunion update(Reunion entity) {
            store.removeIf(r -> r.getIdReunion().equals(entity.getIdReunion()));
            store.add(entity);
            return entity;
        }

        @Override
        public boolean updateEstado(Integer id, Reunion.Estado nuevoEstado) {
            findById(id).ifPresent(r -> r.setEstado(nuevoEstado));
            return true;
        }

        @Override
        public boolean delete(Integer id) {
            return store.removeIf(r -> r.getIdReunion().equals(id));
        }
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
        public int countByReunion(Integer idReunion) {
            return (int) store.stream().filter(a -> a.getIdReunion().equals(idReunion)).count();
        }

        @Override
        public int countAsistieronByReunion(Integer idReunion) {
            return (int) store.stream().filter(a -> a.getIdReunion().equals(idReunion) && a.isAsistio()).count();
        }
    }
}
