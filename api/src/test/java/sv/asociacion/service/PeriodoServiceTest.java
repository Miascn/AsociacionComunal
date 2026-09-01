package sv.asociacion.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import sv.asociacion.dao.MiembroCargoDAO;
import sv.asociacion.dao.PeriodoDirectivaDAO;
import sv.asociacion.domain.dto.PeriodoRequest;
import sv.asociacion.domain.dto.PeriodoResponse;
import sv.asociacion.domain.entity.MiembroCargo;
import sv.asociacion.domain.entity.PeriodoDirectiva;

class PeriodoServiceTest {
    private MemoryPeriodoDAO periodoDAO;
    private MemoryMiembroCargoDAO miembroCargoDAO;
    private PeriodoService service;

    @BeforeEach
    void setUp() {
        periodoDAO = new MemoryPeriodoDAO();
        miembroCargoDAO = new MemoryMiembroCargoDAO();
        service = new PeriodoService(periodoDAO, miembroCargoDAO);
    }

    @Test
    void createsPeriodoSuccessfully() {
        PeriodoRequest req = new PeriodoRequest(
            "Período 2024-2026",
            LocalDate.of(2024, 1, 1),
            LocalDate.of(2025, 12, 31),
            PeriodoDirectiva.Estado.PLANIFICADO
        );
        PeriodoResponse res = service.create(req);

        assertNotNull(res);
        assertNotNull(res.id());
        assertEquals("Período 2024-2026", res.nombre());
        assertEquals("PLANIFICADO", res.estado());
        assertEquals("2024-01-01", res.fechaInicio());
        assertEquals("2025-12-31", res.fechaFin());
    }

    @Test
    void rejectsInvertedDates() {
        PeriodoRequest req = new PeriodoRequest(
            "Período Inválido",
            LocalDate.of(2025, 1, 1),
            LocalDate.of(2024, 1, 1),
            PeriodoDirectiva.Estado.PLANIFICADO
        );
        assertThrows(IllegalArgumentException.class, () -> service.create(req));
    }

    @Test
    void rejectsOverlappingDates() {
        service.create(new PeriodoRequest(
            "Período 2022-2024",
            LocalDate.of(2022, 1, 1),
            LocalDate.of(2024, 6, 30),
            PeriodoDirectiva.Estado.PLANIFICADO
        ));

        // Intento de solapamiento: inicia antes de que termine el anterior
        PeriodoRequest overlap = new PeriodoRequest(
            "Período Solapado",
            LocalDate.of(2024, 1, 1),
            LocalDate.of(2026, 1, 1),
            PeriodoDirectiva.Estado.PLANIFICADO
        );
        assertThrows(IllegalStateException.class, () -> service.create(overlap));
    }

    @Test
    void allowsAdjacentNonOverlappingPeriods() {
        PeriodoResponse p1 = service.create(new PeriodoRequest(
            "Período 1",
            LocalDate.of(2020, 1, 1),
            LocalDate.of(2021, 12, 31),
            PeriodoDirectiva.Estado.FINALIZADO
        ));

        PeriodoResponse p2 = service.create(new PeriodoRequest(
            "Período 2",
            LocalDate.of(2022, 1, 1),
            LocalDate.of(2023, 12, 31),
            PeriodoDirectiva.Estado.PLANIFICADO
        ));

        assertNotNull(p1);
        assertNotNull(p2);
    }

    @Test
    void activatesPeriodoAndFinalizesPreviousActive() {
        // Período 1 activo
        PeriodoResponse p1 = service.create(new PeriodoRequest(
            "Gestión 2022-2024",
            LocalDate.of(2022, 1, 1),
            LocalDate.of(2024, 5, 31),
            PeriodoDirectiva.Estado.ACTIVO
        ));
        assertEquals("ACTIVO", p1.estado());

        // Período 2 planificado
        PeriodoResponse p2 = service.create(new PeriodoRequest(
            "Gestión 2024-2026",
            LocalDate.of(2024, 6, 1),
            LocalDate.of(2026, 5, 31),
            PeriodoDirectiva.Estado.PLANIFICADO
        ));
        assertEquals("PLANIFICADO", p2.estado());

        // Al activar Período 2, Período 1 debe pasar automáticamente a FINALIZADO
        PeriodoResponse p2Activado = service.activar(p2.id());
        assertEquals("ACTIVO", p2Activado.estado());

        PeriodoResponse p1Actualizado = service.findById(p1.id());
        assertEquals("FINALIZADO", p1Actualizado.estado());

        // Solo p2 es activo
        PeriodoResponse activoActual = service.findActivo();
        assertNotNull(activoActual);
        assertEquals(p2.id(), activoActual.id());
    }

    @Test
    void finalizesActivePeriodoSuccessfully() {
        PeriodoResponse p = service.create(new PeriodoRequest(
            "Período Actual",
            LocalDate.of(2024, 1, 1),
            LocalDate.of(2025, 12, 31),
            PeriodoDirectiva.Estado.ACTIVO
        ));

        PeriodoResponse finalizado = service.finalizar(p.id());
        assertEquals("FINALIZADO", finalizado.estado());
    }

    @Test
    void rejectsReactivatingFinalizedPeriodo() {
        PeriodoResponse p = service.create(new PeriodoRequest(
            "Período Concluido",
            LocalDate.of(2020, 1, 1),
            LocalDate.of(2021, 12, 31),
            PeriodoDirectiva.Estado.FINALIZADO
        ));

        assertThrows(IllegalStateException.class, () -> service.activar(p.id()));
    }

    @Test
    void rejectsEditingFinalizedPeriodo() {
        PeriodoResponse p = service.create(new PeriodoRequest(
            "Período Antiguo",
            LocalDate.of(2018, 1, 1),
            LocalDate.of(2019, 12, 31),
            PeriodoDirectiva.Estado.FINALIZADO
        ));

        assertThrows(IllegalStateException.class, () -> service.update(p.id(), new PeriodoRequest(
            "Nombre Modificado",
            LocalDate.of(2018, 1, 1),
            LocalDate.of(2019, 12, 31),
            PeriodoDirectiva.Estado.FINALIZADO
        )));
    }

    @Test
    void deletesPlannedPeriodoWithoutAssignments() {
        PeriodoResponse p = service.create(new PeriodoRequest(
            "Período Futuro Descartado",
            LocalDate.of(2030, 1, 1),
            LocalDate.of(2032, 1, 1),
            PeriodoDirectiva.Estado.PLANIFICADO
        ));

        assertTrue(service.delete(p.id()));
    }

    @Test
    void rejectsDeletingActiveOrFinalizedPeriodo() {
        PeriodoResponse activo = service.create(new PeriodoRequest(
            "Período Activo No Borrable",
            LocalDate.of(2024, 1, 1),
            LocalDate.of(2025, 1, 1),
            PeriodoDirectiva.Estado.ACTIVO
        ));
        assertThrows(IllegalStateException.class, () -> service.delete(activo.id()));
    }

    @Test
    void rejectsDeletingPeriodoWithAssignments() {
        PeriodoResponse p = service.create(new PeriodoRequest(
            "Período Con Directiva",
            LocalDate.of(2026, 1, 1),
            LocalDate.of(2028, 1, 1),
            PeriodoDirectiva.Estado.PLANIFICADO
        ));

        MiembroCargo mc = new MiembroCargo();
        mc.setIdPeriodo(p.id());
        miembroCargoDAO.save(mc);

        assertThrows(IllegalStateException.class, () -> service.delete(p.id()));
    }

    private static final class MemoryPeriodoDAO extends PeriodoDirectivaDAO {
        private final List<PeriodoDirectiva> store = new ArrayList<>();
        private int seq = 1;

        @Override
        public PeriodoDirectiva save(PeriodoDirectiva entity) {
            entity.setIdPeriodo(seq++);
            store.add(entity);
            return entity;
        }

        @Override
        public Optional<PeriodoDirectiva> findById(Integer id) {
            return store.stream().filter(p -> p.getIdPeriodo().equals(id)).findFirst();
        }

        @Override
        public Optional<PeriodoDirectiva> findActivo() {
            return store.stream().filter(p -> p.getEstado() == PeriodoDirectiva.Estado.ACTIVO).findFirst();
        }

        @Override
        public List<PeriodoDirectiva> findAll() {
            return new ArrayList<>(store);
        }

        @Override
        public List<PeriodoDirectiva> findFiltered(PeriodoDirectiva.Estado estado, String busqueda) {
            return store.stream()
                .filter(p -> estado == null || p.getEstado() == estado)
                .filter(p -> {
                    if (busqueda == null || busqueda.isBlank()) return true;
                    return p.getNombre().toLowerCase().contains(busqueda.toLowerCase());
                })
                .toList();
        }

        @Override
        public List<PeriodoDirectiva> findOverlapping(LocalDate inicio, LocalDate fin, Integer excludeId) {
            return store.stream()
                .filter(p -> excludeId == null || !p.getIdPeriodo().equals(excludeId))
                .filter(p -> !p.getFechaInicio().isAfter(fin) && !p.getFechaFin().isBefore(inicio))
                .toList();
        }

        @Override
        public void finalizarActivosExcepto(Integer excludeId) {
            for (PeriodoDirectiva p : store) {
                if (p.getEstado() == PeriodoDirectiva.Estado.ACTIVO && (excludeId == null || !p.getIdPeriodo().equals(excludeId))) {
                    p.setEstado(PeriodoDirectiva.Estado.FINALIZADO);
                }
            }
        }

        @Override
        public boolean updateEstado(Integer id, PeriodoDirectiva.Estado nuevoEstado) {
            return findById(id).map(p -> {
                p.setEstado(nuevoEstado);
                return true;
            }).orElse(false);
        }

        @Override
        public PeriodoDirectiva update(PeriodoDirectiva entity) {
            for (int i = 0; i < store.size(); i++) {
                if (store.get(i).getIdPeriodo().equals(entity.getIdPeriodo())) {
                    store.set(i, entity);
                    break;
                }
            }
            return entity;
        }

        @Override
        public boolean delete(Integer id) {
            return store.removeIf(p -> p.getIdPeriodo().equals(id));
        }
    }

    private static final class MemoryMiembroCargoDAO extends MiembroCargoDAO {
        private final List<MiembroCargo> store = new ArrayList<>();

        @Override
        public MiembroCargo save(MiembroCargo entity) {
            store.add(entity);
            return entity;
        }

        @Override
        public List<MiembroCargo> findByPeriodo(Integer idPeriodo) {
            return store.stream().filter(mc -> mc.getIdPeriodo().equals(idPeriodo)).toList();
        }
    }
}
