package sv.asociacion.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import sv.asociacion.dao.CargoDAO;
import sv.asociacion.dao.MiembroCargoDAO;
import sv.asociacion.domain.dto.CargoRequest;
import sv.asociacion.domain.dto.CargoResponse;
import sv.asociacion.domain.entity.Cargo;

class CargoServiceTest {
    private MemoryCargoDAO cargoDAO;
    private MemoryMiembroCargoDAO miembroCargoDAO;
    private CargoService service;

    @BeforeEach
    void setUp() {
        cargoDAO = new MemoryCargoDAO();
        miembroCargoDAO = new MemoryMiembroCargoDAO();
        service = new CargoService(cargoDAO, miembroCargoDAO);
    }

    @Test
    void createsCargoSuccessfully() {
        CargoRequest req = new CargoRequest("Presidente", "Máxima autoridad de la directiva", 1, true);
        CargoResponse res = service.create(req);

        assertNotNull(res);
        assertNotNull(res.id());
        assertEquals("Presidente", res.nombre());
        assertEquals(1, res.nivelJerarquico());
        assertTrue(res.activo());
        assertEquals(0, res.totalAsignaciones());
    }

    @Test
    void rejectsBlankName() {
        CargoRequest req = new CargoRequest("   ", "Sin nombre", 2, true);
        assertThrows(IllegalArgumentException.class, () -> service.create(req));
    }

    @Test
    void rejectsNonPositiveHierarchy() {
        CargoRequest reqCero = new CargoRequest("Tesorero", "Finanzas", 0, true);
        assertThrows(IllegalArgumentException.class, () -> service.create(reqCero));

        CargoRequest reqNegativo = new CargoRequest("Secretario", "Actas", -1, true);
        assertThrows(IllegalArgumentException.class, () -> service.create(reqNegativo));
    }

    @Test
    void rejectsDuplicateName() {
        service.create(new CargoRequest("Síndico", "Fiscalización", 3, true));

        CargoRequest dup = new CargoRequest("síndico", "Duplicado", 4, true);
        assertThrows(IllegalStateException.class, () -> service.create(dup));
    }

    @Test
    void updatesCargoSuccessfully() {
        CargoResponse c = service.create(new CargoRequest("Vocal", "Apoyo general", 5, true));

        CargoResponse updated = service.update(c.id(), new CargoRequest("Primer Vocal", "Apoyo en obras", 5, true));
        assertEquals("Primer Vocal", updated.nombre());
        assertEquals("Apoyo en obras", updated.descripcion());
    }

    @Test
    void togglesActivoSuccessfully() {
        CargoResponse c = service.create(new CargoRequest("Vocal Suplente", "Temporal", 6, true));
        assertTrue(c.activo());

        CargoResponse desactivado = service.toggleActivo(c.id(), false);
        assertFalse(desactivado.activo());

        CargoResponse reactivado = service.toggleActivo(c.id(), true);
        assertTrue(reactivado.activo());
    }

    @Test
    void deletesCargoWithoutAssignments() {
        CargoResponse c = service.create(new CargoRequest("Comisión Festejos", "Ad-hoc", 7, true));
        assertTrue(service.delete(c.id()));
    }

    @Test
    void blocksDeletingCargoWithAssignments() {
        CargoResponse c = service.create(new CargoRequest("Tesorero General", "Custodia fondos", 2, true));
        miembroCargoDAO.setCountForCargo(c.id(), 3); // 3 asignaciones históricas asociadas

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> service.delete(c.id()));
        assertTrue(ex.getMessage().contains("asignaciones"));
    }

    @Test
    void findFilteredByActivoAndBusqueda() {
        service.create(new CargoRequest("Presidente", "Directiva", 1, true));
        CargoResponse inactivo = service.create(new CargoRequest("Asesor Jurídico", "Legal", 4, true));
        service.toggleActivo(inactivo.id(), false);

        List<CargoResponse> soloActivos = service.findFiltered(true, null);
        assertEquals(1, soloActivos.size());
        assertEquals("Presidente", soloActivos.get(0).nombre());

        List<CargoResponse> porBusqueda = service.findFiltered(null, "jurídico");
        assertEquals(1, porBusqueda.size());
        assertEquals("Asesor Jurídico", porBusqueda.get(0).nombre());
    }

    private static final class MemoryCargoDAO extends CargoDAO {
        private final List<Cargo> store = new ArrayList<>();
        private int seq = 1;

        @Override
        public Cargo save(Cargo entity) {
            entity.setIdCargo(seq++);
            store.add(entity);
            return entity;
        }

        @Override
        public Optional<Cargo> findById(Integer id) {
            return store.stream().filter(c -> c.getIdCargo().equals(id)).findFirst();
        }

        @Override
        public Optional<CargoDetail> findByIdWithDetail(Integer id) {
            return findById(id).map(c -> new CargoDetail(c, 0));
        }

        @Override
        public Optional<Cargo> findByNombre(String nombre) {
            return store.stream().filter(c -> c.getNombre().equalsIgnoreCase(nombre)).findFirst();
        }

        @Override
        public List<CargoDetail> findFiltered(Boolean activo, String busqueda) {
            return store.stream()
                .filter(c -> activo == null || c.getActivo().equals(activo))
                .filter(c -> {
                    if (busqueda == null || busqueda.isBlank()) return true;
                    String b = busqueda.toLowerCase();
                    return c.getNombre().toLowerCase().contains(b)
                        || (c.getDescripcion() != null && c.getDescripcion().toLowerCase().contains(b));
                })
                .map(c -> new CargoDetail(c, 0))
                .toList();
        }

        @Override
        public Cargo update(Cargo entity) {
            for (int i = 0; i < store.size(); i++) {
                if (store.get(i).getIdCargo().equals(entity.getIdCargo())) {
                    store.set(i, entity);
                    break;
                }
            }
            return entity;
        }

        @Override
        public boolean toggleActivo(Integer id, boolean activo) {
            return findById(id).map(c -> {
                c.setActivo(activo);
                return true;
            }).orElse(false);
        }

        @Override
        public boolean delete(Integer id) {
            return store.removeIf(c -> c.getIdCargo().equals(id));
        }
    }

    private static final class MemoryMiembroCargoDAO extends MiembroCargoDAO {
        private int countToReturn = 0;

        void setCountForCargo(Integer idCargo, int count) {
            this.countToReturn = count;
        }

        @Override
        public int countByCargo(Integer idCargo) {
            return countToReturn;
        }
    }
}
