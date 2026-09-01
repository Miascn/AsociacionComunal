package sv.asociacion.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import sv.asociacion.dao.CargoDAO;
import sv.asociacion.dao.MiembroCargoDAO;
import sv.asociacion.dao.MiembroDAO;
import sv.asociacion.dao.PeriodoDirectivaDAO;
import sv.asociacion.domain.dto.AsignacionCargoRequest;
import sv.asociacion.domain.dto.AsignacionCargoResponse;
import sv.asociacion.domain.dto.RevocarAsignacionRequest;
import sv.asociacion.domain.entity.Cargo;
import sv.asociacion.domain.entity.Miembro;
import sv.asociacion.domain.entity.MiembroCargo;
import sv.asociacion.domain.entity.PeriodoDirectiva;

class AsignacionCargoServiceTest {
    private MemoryMiembroCargoDAO miembroCargoDAO;
    private MemoryMiembroDAO miembroDAO;
    private MemoryCargoDAO cargoDAO;
    private MemoryPeriodoDAO periodoDAO;
    private AsignacionCargoService service;

    private Miembro miembroActivo1;
    private Miembro miembroActivo2;
    private Miembro miembroInactivo;
    private Cargo cargoPresidente;
    private Cargo cargoSecretario;
    private Cargo cargoInactivo;
    private PeriodoDirectiva periodoActivo;
    private PeriodoDirectiva periodoFinalizado;

    @BeforeEach
    void setUp() {
        miembroCargoDAO = new MemoryMiembroCargoDAO();
        miembroDAO = new MemoryMiembroDAO();
        cargoDAO = new MemoryCargoDAO();
        periodoDAO = new MemoryPeriodoDAO();
        service = new AsignacionCargoService(miembroCargoDAO, miembroDAO, cargoDAO, periodoDAO);

        miembroActivo1 = new Miembro(1, "01234567-8", "Carlos", "Hernández", "7000-0001", "carlos@mail.com", "Calle Principal #1", LocalDate.now(), Miembro.Estado.ACTIVO);
        miembroActivo2 = new Miembro(2, "08765432-1", "Elena", "Gómez", "7000-0002", "elena@mail.com", "Calle Principal #2", LocalDate.now(), Miembro.Estado.ACTIVO);
        miembroInactivo = new Miembro(3, "00000000-0", "Inactivo", "Pérez", "7000-0003", "inactivo@mail.com", "Calle Principal #3", LocalDate.now(), Miembro.Estado.INACTIVO);
        miembroDAO.save(miembroActivo1);
        miembroDAO.save(miembroActivo2);
        miembroDAO.save(miembroInactivo);

        cargoPresidente = new Cargo(1, "Presidente", "Líder de la directiva", 1, true);
        cargoSecretario = new Cargo(2, "Secretario", "Actas y correspondencia", 2, true);
        cargoInactivo = new Cargo(3, "Vocal Suplente Antiguo", "Obsoleto", 5, false);
        cargoDAO.save(cargoPresidente);
        cargoDAO.save(cargoSecretario);
        cargoDAO.save(cargoInactivo);

        periodoActivo = new PeriodoDirectiva(1, "Gestión 2024-2026", LocalDate.of(2024, 1, 1), LocalDate.of(2025, 12, 31), PeriodoDirectiva.Estado.ACTIVO);
        periodoFinalizado = new PeriodoDirectiva(2, "Gestión 2022-2024", LocalDate.of(2022, 1, 1), LocalDate.of(2023, 12, 31), PeriodoDirectiva.Estado.FINALIZADO);
        periodoDAO.save(periodoActivo);
        periodoDAO.save(periodoFinalizado);
    }

    @Test
    void createsAssignmentSuccessfully() {
        AsignacionCargoRequest req = new AsignacionCargoRequest(
            miembroActivo1.getIdMiembro(),
            cargoPresidente.getIdCargo(),
            periodoActivo.getIdPeriodo(),
            LocalDate.of(2024, 1, 15),
            null
        );

        AsignacionCargoResponse res = service.create(req);
        assertNotNull(res);
        assertNotNull(res.id());
        assertEquals("Presidente", res.nombreCargo());
        assertEquals("ACTIVO", res.estado());
    }

    @Test
    void rejectsInactiveMember() {
        AsignacionCargoRequest req = new AsignacionCargoRequest(
            miembroInactivo.getIdMiembro(),
            cargoPresidente.getIdCargo(),
            periodoActivo.getIdPeriodo(),
            LocalDate.of(2024, 1, 15),
            null
        );

        assertThrows(IllegalStateException.class, () -> service.create(req));
    }

    @Test
    void rejectsInactiveCargo() {
        AsignacionCargoRequest req = new AsignacionCargoRequest(
            miembroActivo1.getIdMiembro(),
            cargoInactivo.getIdCargo(),
            periodoActivo.getIdPeriodo(),
            LocalDate.of(2024, 1, 15),
            null
        );

        assertThrows(IllegalStateException.class, () -> service.create(req));
    }

    @Test
    void rejectsFinalizedPeriod() {
        AsignacionCargoRequest req = new AsignacionCargoRequest(
            miembroActivo1.getIdMiembro(),
            cargoPresidente.getIdCargo(),
            periodoFinalizado.getIdPeriodo(),
            LocalDate.of(2022, 2, 1),
            null
        );

        assertThrows(IllegalStateException.class, () -> service.create(req));
    }

    @Test
    void rejectsAssignmentBeforePeriodStart() {
        AsignacionCargoRequest req = new AsignacionCargoRequest(
            miembroActivo1.getIdMiembro(),
            cargoPresidente.getIdCargo(),
            periodoActivo.getIdPeriodo(),
            LocalDate.of(2023, 12, 31), // Antes del 2024-01-01
            null
        );

        assertThrows(IllegalArgumentException.class, () -> service.create(req));
    }

    @Test
    void rejectsAssignmentAfterPeriodEnd() {
        AsignacionCargoRequest req = new AsignacionCargoRequest(
            miembroActivo1.getIdMiembro(),
            cargoPresidente.getIdCargo(),
            periodoActivo.getIdPeriodo(),
            LocalDate.of(2026, 1, 1), // Después del 2025-12-31
            null
        );

        assertThrows(IllegalArgumentException.class, () -> service.create(req));
    }

    @Test
    void rejectsDuplicateCargoInSamePeriod() {
        // Asignar miembro 1 a Presidente
        service.create(new AsignacionCargoRequest(
            miembroActivo1.getIdMiembro(),
            cargoPresidente.getIdCargo(),
            periodoActivo.getIdPeriodo(),
            LocalDate.of(2024, 1, 1),
            null
        ));

        // Intento de asignar miembro 2 también a Presidente en el mismo período
        AsignacionCargoRequest dupCargo = new AsignacionCargoRequest(
            miembroActivo2.getIdMiembro(),
            cargoPresidente.getIdCargo(),
            periodoActivo.getIdPeriodo(),
            LocalDate.of(2024, 1, 1),
            null
        );

        assertThrows(IllegalStateException.class, () -> service.create(dupCargo));
    }

    @Test
    void rejectsDoubleCargoForSameMember() {
        // Asignar miembro 1 a Presidente
        service.create(new AsignacionCargoRequest(
            miembroActivo1.getIdMiembro(),
            cargoPresidente.getIdCargo(),
            periodoActivo.getIdPeriodo(),
            LocalDate.of(2024, 1, 1),
            null
        ));

        // Intento de asignar el mismo miembro 1 a Secretario a la vez
        AsignacionCargoRequest doubleCargo = new AsignacionCargoRequest(
            miembroActivo1.getIdMiembro(),
            cargoSecretario.getIdCargo(),
            periodoActivo.getIdPeriodo(),
            LocalDate.of(2024, 1, 1),
            null
        );

        assertThrows(IllegalStateException.class, () -> service.create(doubleCargo));
    }

    @Test
    void allowsNewAssignmentAfterPreviousRevoked() {
        // Miembro 1 asume como Presidente
        AsignacionCargoResponse p1 = service.create(new AsignacionCargoRequest(
            miembroActivo1.getIdMiembro(),
            cargoPresidente.getIdCargo(),
            periodoActivo.getIdPeriodo(),
            LocalDate.of(2024, 1, 1),
            null
        ));

        // Miembro 1 renuncia / es revocado
        service.revocar(p1.id(), new RevocarAsignacionRequest(LocalDate.of(2024, 6, 1), "Renuncia por motivos personales"));

        // Ahora Miembro 2 puede asumir legalmente la presidencia para el resto del período
        AsignacionCargoResponse p2 = service.create(new AsignacionCargoRequest(
            miembroActivo2.getIdMiembro(),
            cargoPresidente.getIdCargo(),
            periodoActivo.getIdPeriodo(),
            LocalDate.of(2024, 6, 2),
            null
        ));

        assertNotNull(p2);
        assertEquals("ACTIVO", p2.estado());
        assertEquals("Elena Gómez", p2.nombreMiembro());
    }

    @Test
    void revokesActiveAssignmentSuccessfully() {
        AsignacionCargoResponse a = service.create(new AsignacionCargoRequest(
            miembroActivo1.getIdMiembro(),
            cargoPresidente.getIdCargo(),
            periodoActivo.getIdPeriodo(),
            LocalDate.of(2024, 1, 1),
            null
        ));

        AsignacionCargoResponse rev = service.revocar(a.id(), new RevocarAsignacionRequest(
            LocalDate.of(2024, 8, 15), "Cambio de domicilio"
        ));

        assertEquals("REVOCADO", rev.estado());
        assertEquals("2024-08-15", rev.fechaFin());
        assertEquals("Cambio de domicilio", rev.motivoSalida());
    }

    @Test
    void rejectsRevokingNonActiveAssignment() {
        AsignacionCargoResponse a = service.create(new AsignacionCargoRequest(
            miembroActivo1.getIdMiembro(),
            cargoPresidente.getIdCargo(),
            periodoActivo.getIdPeriodo(),
            LocalDate.of(2024, 1, 1),
            null
        ));
        service.finalizar(a.id());

        assertThrows(IllegalStateException.class, () -> service.revocar(a.id(), new RevocarAsignacionRequest(
            LocalDate.of(2024, 8, 15), "Intento revocación de finalizado"
        )));
    }

    @Test
    void rejectsRevocationDateBeforeAssignmentDate() {
        AsignacionCargoResponse a = service.create(new AsignacionCargoRequest(
            miembroActivo1.getIdMiembro(),
            cargoPresidente.getIdCargo(),
            periodoActivo.getIdPeriodo(),
            LocalDate.of(2024, 5, 1),
            null
        ));

        assertThrows(IllegalArgumentException.class, () -> service.revocar(a.id(), new RevocarAsignacionRequest(
            LocalDate.of(2024, 4, 1), "Fecha anterior a inicio"
        )));
    }

    private static final class MemoryMiembroCargoDAO extends MiembroCargoDAO {
        private final List<MiembroCargo> store = new ArrayList<>();
        private int seq = 1;

        @Override
        public MiembroCargo save(MiembroCargo entity) {
            entity.setIdMiembroCargo(seq++);
            store.add(entity);
            return entity;
        }

        @Override
        public Optional<MiembroCargo> findById(Integer id) {
            return store.stream().filter(mc -> mc.getIdMiembroCargo().equals(id)).findFirst();
        }

        @Override
        public Optional<MiembroCargo> findActiveByCargoAndPeriodo(Integer idCargo, Integer idPeriodo, Integer excludeId) {
            return store.stream()
                .filter(mc -> excludeId == null || !mc.getIdMiembroCargo().equals(excludeId))
                .filter(mc -> mc.getIdCargo().equals(idCargo) && mc.getIdPeriodo().equals(idPeriodo) && mc.getEstado() == MiembroCargo.Estado.ACTIVO)
                .findFirst();
        }

        @Override
        public Optional<MiembroCargo> findActiveByMiembroAndPeriodo(Integer idMiembro, Integer idPeriodo, Integer excludeId) {
            return store.stream()
                .filter(mc -> excludeId == null || !mc.getIdMiembroCargo().equals(excludeId))
                .filter(mc -> mc.getIdMiembro().equals(idMiembro) && mc.getIdPeriodo().equals(idPeriodo) && mc.getEstado() == MiembroCargo.Estado.ACTIVO)
                .findFirst();
        }

        @Override
        public boolean revocar(Integer id, LocalDate fechaFin, String motivo) {
            return findById(id).map(mc -> {
                mc.setFechaFin(fechaFin);
                mc.setMotivoSalida(motivo);
                mc.setEstado(MiembroCargo.Estado.REVOCADO);
                return true;
            }).orElse(false);
        }

        @Override
        public MiembroCargo update(MiembroCargo entity) {
            for (int i = 0; i < store.size(); i++) {
                if (store.get(i).getIdMiembroCargo().equals(entity.getIdMiembroCargo())) {
                    store.set(i, entity);
                    break;
                }
            }
            return entity;
        }

        @Override
        public boolean delete(Integer id) {
            return store.removeIf(mc -> mc.getIdMiembroCargo().equals(id));
        }

        @Override
        public Optional<AsignacionCargoResponse> findDetalladoById(Integer id) {
            return findById(id).map(mc -> new AsignacionCargoResponse(
                mc.getIdMiembroCargo(),
                mc.getIdMiembro(),
                mc.getIdMiembro() == 1 ? "Carlos Hernández" : "Elena Gómez",
                "00000000-0",
                "7000-0000",
                mc.getIdCargo(),
                mc.getIdCargo() == 1 ? "Presidente" : "Secretario",
                1,
                mc.getIdPeriodo(),
                "Gestión 2024-2026",
                "ACTIVO",
                mc.getFechaAsignacion().toString(),
                mc.getFechaFin() != null ? mc.getFechaFin().toString() : null,
                mc.getMotivoSalida(),
                mc.getEstado().name()
            ));
        }

        @Override
        public List<AsignacionCargoResponse> findDetalladoFiltered(Integer idPeriodo, String estado, String busqueda) {
            return store.stream()
                .filter(mc -> idPeriodo == null || mc.getIdPeriodo().equals(idPeriodo))
                .filter(mc -> estado == null || mc.getEstado().name().equalsIgnoreCase(estado))
                .map(mc -> findDetalladoById(mc.getIdMiembroCargo()).orElseThrow())
                .toList();
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
    }

    private static final class MemoryCargoDAO extends CargoDAO {
        private final List<Cargo> store = new ArrayList<>();

        @Override
        public Cargo save(Cargo entity) {
            store.add(entity);
            return entity;
        }

        @Override
        public Optional<Cargo> findById(Integer id) {
            return store.stream().filter(c -> c.getIdCargo().equals(id)).findFirst();
        }
    }

    private static final class MemoryPeriodoDAO extends PeriodoDirectivaDAO {
        private final List<PeriodoDirectiva> store = new ArrayList<>();

        @Override
        public PeriodoDirectiva save(PeriodoDirectiva entity) {
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
    }
}
