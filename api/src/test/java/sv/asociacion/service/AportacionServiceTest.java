package sv.asociacion.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import sv.asociacion.dao.AportacionDAO;
import sv.asociacion.dao.MiembroDAO;
import sv.asociacion.dao.ProyectoDAO;
import sv.asociacion.domain.dto.AportacionPageResponse;
import sv.asociacion.domain.dto.AportacionRequest;
import sv.asociacion.domain.dto.AportacionResponse;
import sv.asociacion.domain.entity.Aportacion;
import sv.asociacion.domain.entity.Miembro;
import sv.asociacion.domain.entity.Proyecto;

class AportacionServiceTest {
    private MemoryAportacionDAO aportacionDAO;
    private MemoryMiembroDAO miembroDAO;
    private MemoryProyectoDAO proyectoDAO;
    private AportacionService service;

    @BeforeEach
    void setUp() {
        aportacionDAO = new MemoryAportacionDAO();
        miembroDAO = new MemoryMiembroDAO();
        proyectoDAO = new MemoryProyectoDAO();
        service = new AportacionService(aportacionDAO, miembroDAO, proyectoDAO);

        Miembro m = new Miembro();
        m.setIdMiembro(1);
        m.setNombres("Carlos");
        m.setApellidos("Martínez");
        m.setDui("01234567-8");
        miembroDAO.save(m);

        Proyecto p = new Proyecto();
        p.setIdProyecto(10);
        p.setNombre("Pavimentación calle principal");
        proyectoDAO.save(p);
    }

    @Test
    void createsAportacionSuccessfully() {
        AportacionRequest req = new AportacionRequest(
            1, 10, "2026-03", new BigDecimal("15.50"),
            LocalDate.now(), Aportacion.MetodoPago.EFECTIVO, "REC-001"
        );
        AportacionResponse res = service.create(req);

        assertNotNull(res);
        assertNotNull(res.idAportacion());
        assertEquals(1, res.idMiembro());
        assertEquals("Carlos Martínez", res.nombreMiembro());
        assertEquals("01234567-8", res.duiMiembro());
        assertEquals(10, res.idProyecto());
        assertEquals("Pavimentación calle principal", res.nombreProyecto());
        assertEquals("2026-03", res.periodoMes());
        assertEquals(new BigDecimal("15.50"), res.monto());
        assertEquals("EFECTIVO", res.metodoPago());
        assertEquals("REGISTRADA", res.estado());
    }

    @Test
    void rejectsZeroOrNegativeAmount() {
        AportacionRequest reqZero = new AportacionRequest(
            1, null, "2026-03", BigDecimal.ZERO,
            LocalDate.now(), Aportacion.MetodoPago.EFECTIVO, null
        );
        assertThrows(IllegalArgumentException.class, () -> service.create(reqZero));

        AportacionRequest reqNegative = new AportacionRequest(
            1, null, "2026-03", new BigDecimal("-5.00"),
            LocalDate.now(), Aportacion.MetodoPago.EFECTIVO, null
        );
        assertThrows(IllegalArgumentException.class, () -> service.create(reqNegative));
    }

    @Test
    void rejectsFuturePaymentDate() {
        AportacionRequest req = new AportacionRequest(
            1, null, "2026-03", new BigDecimal("10.00"),
            LocalDate.now().plusDays(2), Aportacion.MetodoPago.EFECTIVO, null
        );
        assertThrows(IllegalArgumentException.class, () -> service.create(req));
    }

    @Test
    void rejectsInvalidPeriodFormat() {
        AportacionRequest req = new AportacionRequest(
            1, null, "Marzo-2026", new BigDecimal("10.00"),
            LocalDate.now(), Aportacion.MetodoPago.EFECTIVO, null
        );
        assertThrows(IllegalArgumentException.class, () -> service.create(req));
    }

    @Test
    void rejectsDuplicatePaymentForSamePeriod() {
        AportacionRequest req1 = new AportacionRequest(
            1, null, "2026-03", new BigDecimal("10.00"),
            LocalDate.now(), Aportacion.MetodoPago.EFECTIVO, null
        );
        service.create(req1);

        AportacionRequest req2 = new AportacionRequest(
            1, null, "2026-03", new BigDecimal("12.00"),
            LocalDate.now(), Aportacion.MetodoPago.TRANSFERENCIA, null
        );
        assertThrows(IllegalStateException.class, () -> service.create(req2));
    }

    @Test
    void rejectsNonExistentMember() {
        AportacionRequest req = new AportacionRequest(
            999, null, "2026-03", new BigDecimal("10.00"),
            LocalDate.now(), Aportacion.MetodoPago.EFECTIVO, null
        );
        assertThrows(IllegalArgumentException.class, () -> service.create(req));
    }

    @Test
    void updatesAportacionSuccessfully() {
        AportacionRequest req1 = new AportacionRequest(
            1, null, "2026-03", new BigDecimal("10.00"),
            LocalDate.now(), Aportacion.MetodoPago.EFECTIVO, "REC-01"
        );
        AportacionResponse created = service.create(req1);

        AportacionRequest req2 = new AportacionRequest(
            1, null, "2026-03", new BigDecimal("20.00"),
            LocalDate.now(), Aportacion.MetodoPago.TRANSFERENCIA, "REC-01-CORREGIDO"
        );
        AportacionResponse updated = service.update(created.idAportacion(), req2);

        assertEquals(new BigDecimal("20.00"), updated.monto());
        assertEquals("TRANSFERENCIA", updated.metodoPago());
        assertEquals("REC-01-CORREGIDO", updated.referencia());
    }

    @Test
    void anulatesAportacionSuccessfully() {
        AportacionRequest req = new AportacionRequest(
            1, null, "2026-03", new BigDecimal("10.00"),
            LocalDate.now(), Aportacion.MetodoPago.EFECTIVO, null
        );
        AportacionResponse created = service.create(req);
        assertTrue(service.anular(created.idAportacion()));

        AportacionResponse found = service.findById(created.idAportacion());
        assertEquals("ANULADA", found.estado());

        // Editar aportación anulada debe fallar
        assertThrows(IllegalStateException.class, () -> service.update(created.idAportacion(), req));
    }

    @Test
    void calculatesTotalRecaudadoCorrectly() {
        service.create(new AportacionRequest(1, null, "2026-01", new BigDecimal("10.00"), LocalDate.now(), Aportacion.MetodoPago.EFECTIVO, null));
        service.create(new AportacionRequest(1, null, "2026-02", new BigDecimal("15.50"), LocalDate.now(), Aportacion.MetodoPago.EFECTIVO, null));
        AportacionResponse apo3 = service.create(new AportacionRequest(1, null, "2026-03", new BigDecimal("50.00"), LocalDate.now(), Aportacion.MetodoPago.EFECTIVO, null));
        service.anular(apo3.idAportacion());

        AportacionPageResponse page = service.findPage(null, null, null, null, null, null, null, null, 1, 10);
        assertEquals(3, page.total());
        // Solo las REGISTRADAS deben sumar en la recaudación (10.00 + 15.50 = 25.50)
        assertEquals(new BigDecimal("25.50"), page.totalRecaudado());
    }

    private static final class MemoryAportacionDAO extends AportacionDAO {
        private final List<Aportacion> store = new ArrayList<>();
        private long seq = 1;

        @Override
        public Aportacion save(Aportacion entity) {
            entity.setIdAportacion(seq++);
            store.add(entity);
            return entity;
        }

        @Override
        public Optional<Aportacion> findById(Long id) {
            return store.stream().filter(a -> a.getIdAportacion().equals(id)).findFirst();
        }

        @Override
        public Optional<AportacionDetail> findByIdWithDetails(Long id) {
            return findById(id).map(a -> new AportacionDetail(
                a, "Carlos Martínez", "01234567-8",
                a.getIdProyecto() != null ? "Pavimentación calle principal" : null
            ));
        }

        @Override
        public Aportacion update(Aportacion entity) {
            for (int i = 0; i < store.size(); i++) {
                if (store.get(i).getIdAportacion().equals(entity.getIdAportacion())) {
                    store.set(i, entity);
                    break;
                }
            }
            return entity;
        }

        @Override
        public boolean anular(Long id) {
            return findById(id).map(a -> {
                a.setEstado(Aportacion.Estado.ANULADA);
                return true;
            }).orElse(false);
        }

        @Override
        public boolean existsByMiembroAndPeriodo(Integer idMiembro, String periodoMes, Long excludeId) {
            return store.stream().anyMatch(a ->
                a.getIdMiembro().equals(idMiembro)
                && a.getPeriodoMes().equals(periodoMes)
                && a.getEstado() == Aportacion.Estado.REGISTRADA
                && (excludeId == null || !a.getIdAportacion().equals(excludeId))
            );
        }

        @Override
        public int countFiltered(Integer idMiembro, Integer idProyecto, String periodo,
                                 String desde, String hasta, String metodo, String estado, String busqueda) {
            return (int) filter(idMiembro, idProyecto, periodo, metodo, estado).count();
        }

        @Override
        public BigDecimal sumFiltered(Integer idMiembro, Integer idProyecto, String periodo,
                                      String desde, String hasta, String metodo, String estado, String busqueda) {
            return filter(idMiembro, idProyecto, periodo, metodo, estado)
                .filter(a -> a.getEstado() == Aportacion.Estado.REGISTRADA)
                .map(Aportacion::getMonto)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        }

        @Override
        public List<AportacionDetail> findFiltered(Integer idMiembro, Integer idProyecto, String periodo,
                                                  String desde, String hasta, String metodo, String estado,
                                                  String busqueda, int offset, int limit) {
            return filter(idMiembro, idProyecto, periodo, metodo, estado)
                .skip(offset)
                .limit(limit)
                .map(a -> new AportacionDetail(
                    a, "Carlos Martínez", "01234567-8",
                    a.getIdProyecto() != null ? "Pavimentación calle principal" : null
                ))
                .toList();
        }

        private java.util.stream.Stream<Aportacion> filter(Integer idMiembro, Integer idProyecto, String periodo,
                                                           String metodo, String estado) {
            return store.stream().filter(a -> {
                if (idMiembro != null && !a.getIdMiembro().equals(idMiembro)) return false;
                if (idProyecto != null && !idProyecto.equals(a.getIdProyecto())) return false;
                if (periodo != null && !a.getPeriodoMes().equals(periodo)) return false;
                if (metodo != null && !a.getMetodoPago().name().equalsIgnoreCase(metodo)) return false;
                if (estado != null && !a.getEstado().name().equalsIgnoreCase(estado)) return false;
                return true;
            });
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

    private static final class MemoryProyectoDAO extends ProyectoDAO {
        private final List<Proyecto> store = new ArrayList<>();

        @Override
        public Proyecto save(Proyecto entity) {
            store.add(entity);
            return entity;
        }

        @Override
        public Optional<Proyecto> findById(Integer id) {
            return store.stream().filter(p -> p.getIdProyecto().equals(id)).findFirst();
        }
    }
}
