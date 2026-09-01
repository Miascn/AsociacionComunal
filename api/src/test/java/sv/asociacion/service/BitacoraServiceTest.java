package sv.asociacion.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import sv.asociacion.dao.BitacoraDAO;
import sv.asociacion.domain.dto.BitacoraPageResponse;
import sv.asociacion.domain.dto.BitacoraResponse;
import sv.asociacion.domain.entity.Bitacora;

class BitacoraServiceTest {
    private MemoryBitacoraDAO dao;
    private BitacoraService service;

    @BeforeEach
    void setUp() {
        dao = new MemoryBitacoraDAO();
        service = new BitacoraService(dao);
    }

    @Test
    void recordsAuditEventCorrectly() {
        Bitacora b = service.registrar(1, "CREATE", "USUARIO", "42", "Creación de usuario nuevo");
        assertNotNull(b);
        assertNotNull(b.getIdBitacora());
        assertEquals(1, b.getIdUsuario());
        assertEquals("CREATE", b.getAccion());
        assertEquals("USUARIO", b.getEntidad());
        assertEquals("42", b.getIdRegistro());
        assertNotNull(b.getFechaHora());
        assertEquals("Creación de usuario nuevo", b.getDetalle());
    }

    @Test
    void filtersByEntityAndAction() {
        service.registrar(1, "CREATE", "USUARIO", "10", "Usuario 10");
        service.registrar(1, "UPDATE", "USUARIO", "10", "Modifica usuario");
        service.registrar(2, "CREATE", "ROL", "5", "Rol 5");

        BitacoraPageResponse res1 = service.findPage(null, "USUARIO", null, null, null, 1, 10);
        assertEquals(2, res1.total());
        assertEquals(2, res1.items().size());

        BitacoraPageResponse res2 = service.findPage(null, null, "CREATE", null, null, 1, 10);
        assertEquals(2, res2.total());

        BitacoraPageResponse res3 = service.findPage(null, "ROL", "CREATE", null, null, 1, 10);
        assertEquals(1, res3.total());
        assertEquals("ROL", res3.items().get(0).entidad());
    }

    @Test
    void filtersByUsernameOrId() {
        service.registrar(1, "LOGIN", "AUTH", "1", "Login exitoso");
        service.registrar(2, "LOGIN", "AUTH", "2", "Login exitoso");

        BitacoraPageResponse res = service.findPage("admin", null, null, null, null, 1, 10);
        assertEquals(1, res.total());
        assertEquals("admin", res.items().get(0).nombreUsuario());
    }

    @Test
    void handlesPaginationProperly() {
        for (int i = 1; i <= 25; i++) {
            service.registrar(1, "EVENT_" + i, "SISTEMA", String.valueOf(i), "Detalle " + i);
        }

        BitacoraPageResponse page1 = service.findPage(null, null, null, null, null, 1, 10);
        assertEquals(25, page1.total());
        assertEquals(10, page1.items().size());
        assertEquals(3, page1.totalPages());

        BitacoraPageResponse page3 = service.findPage(null, null, null, null, null, 3, 10);
        assertEquals(5, page3.items().size());
    }

    @Test
    void retrievesByIdWithUsername() {
        Bitacora b = service.registrar(1, "CREATE", "ROL", "99", "Nuevo rol");
        BitacoraResponse resp = service.findById(b.getIdBitacora());

        assertNotNull(resp);
        assertEquals(b.getIdBitacora(), resp.idBitacora());
        assertEquals("admin", resp.nombreUsuario());
        assertEquals("ROL", resp.entidad());
    }

    @Test
    void assertsImmutabilityOnDAOUpdateAndDelete() {
        assertThrows(UnsupportedOperationException.class, () -> dao.update(new Bitacora()));
        assertThrows(UnsupportedOperationException.class, () -> dao.delete(1L));
    }

    private static final class MemoryBitacoraDAO extends BitacoraDAO {
        private final List<Bitacora> store = new ArrayList<>();
        private long seq = 1;

        @Override
        public Bitacora save(Bitacora entity) {
            entity.setIdBitacora(seq++);
            store.add(entity);
            return entity;
        }

        @Override
        public Optional<Bitacora> findById(Long id) {
            return store.stream().filter(b -> b.getIdBitacora().equals(id)).findFirst();
        }

        @Override
        public Optional<BitacoraEntry> findByIdWithUser(Long id) {
            return findById(id).map(b -> new BitacoraEntry(b, b.getIdUsuario() == 1 ? "admin" : "usuario_" + b.getIdUsuario()));
        }

        @Override
        public int countFiltered(String usuario, String entidad, String accion, String fechaDesde, String fechaHasta) {
            return (int) filter(usuario, entidad, accion).count();
        }

        @Override
        public List<BitacoraEntry> findFiltered(String usuario, String entidad, String accion,
                                                String fechaDesde, String fechaHasta, int offset, int limit) {
            return filter(usuario, entidad, accion)
                .skip(offset)
                .limit(limit)
                .map(b -> new BitacoraEntry(b, b.getIdUsuario() == 1 ? "admin" : "usuario_" + b.getIdUsuario()))
                .toList();
        }

        private java.util.stream.Stream<Bitacora> filter(String usuario, String entidad, String accion) {
            return store.stream()
                .filter(b -> {
                    if (usuario != null && !usuario.isBlank()) {
                        String uName = b.getIdUsuario() == 1 ? "admin" : "usuario_" + b.getIdUsuario();
                        if (!uName.toLowerCase().contains(usuario.toLowerCase()) && !String.valueOf(b.getIdUsuario()).equals(usuario)) {
                            return false;
                        }
                    }
                    if (entidad != null && !entidad.isBlank() && !"TODAS".equalsIgnoreCase(entidad)) {
                        if (!b.getEntidad().equalsIgnoreCase(entidad)) return false;
                    }
                    if (accion != null && !accion.isBlank() && !"TODAS".equalsIgnoreCase(accion)) {
                        if (!b.getAccion().equalsIgnoreCase(accion)) return false;
                    }
                    return true;
                });
        }
    }
}
