package sv.asociacion.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import sv.asociacion.dao.RolDAO;
import sv.asociacion.dao.UsuarioDAO;
import sv.asociacion.domain.dto.RolRequest;
import sv.asociacion.domain.dto.RolResponse;
import sv.asociacion.domain.entity.Rol;

class RolServiceTest {
    private MemoryRolDAO rolDAO;
    private MemoryUsuarioDAO usuarioDAO;
    private RolService service;

    @BeforeEach
    void setUp() {
        rolDAO = new MemoryRolDAO();
        usuarioDAO = new MemoryUsuarioDAO();
        service = new RolService(rolDAO, usuarioDAO);

        // Seed base roles
        rolDAO.save(new Rol(1, "ADMINISTRADOR", "Administrador del sistema"));
        rolDAO.save(new Rol(2, "MIEMBRO", "Miembro general"));
        rolDAO.save(new Rol(3, "TESORERO", "Encargado de finanzas"));
    }

    @Test
    void createsAndRetrievesRolesWithUserCount() {
        usuarioDAO.setUserCount(1, 3);
        usuarioDAO.setUserCount(2, 10);

        var response = service.create(new RolRequest("COORDINADOR", "Coordina comisiones"));
        assertNotNull(response.idRol());
        assertEquals("COORDINADOR", response.nombre());
        assertEquals(0, response.usuariosAsociados());

        List<RolResponse> all = service.findAll();
        assertEquals(4, all.size());

        RolResponse admin = service.findById(1);
        assertNotNull(admin);
        assertEquals(3, admin.usuariosAsociados());
    }

    @Test
    void rejectsDuplicateRoleName() {
        assertThrows(IllegalStateException.class,
            () -> service.create(new RolRequest("ADMINISTRADOR", "Duplicado")));
        assertThrows(IllegalStateException.class,
            () -> service.create(new RolRequest("administrador", "Mismo nombre en minusculas")));
    }

    @Test
    void rejectsInvalidRoleName() {
        assertThrows(IllegalArgumentException.class,
            () -> service.create(new RolRequest("", "Vacio")));
        assertThrows(IllegalArgumentException.class,
            () -> service.create(new RolRequest("A", "Demasiado corto")));
    }

    @Test
    void updatesRoleSuccessfully() {
        var created = service.create(new RolRequest("AUDITOR", "Auditor de cuentas"));
        var updated = service.update(created.idRol(), new RolRequest("AUDITOR_GENERAL", "Auditor senior"));

        assertEquals("AUDITOR_GENERAL", updated.nombre());
        assertEquals("Auditor senior", updated.descripcion());
    }

    @Test
    void preventsRenamingBaseRole() {
        assertThrows(IllegalStateException.class,
            () -> service.update(1, new RolRequest("SUPER_ADMIN", "Intento de cambiar nombre")));
    }

    @Test
    void preventsDeletingBaseRole() {
        assertThrows(IllegalStateException.class,
            () -> service.delete(1));
        assertThrows(IllegalStateException.class,
            () -> service.delete(2));
    }

    @Test
    void preventsDeletingRoleWithUsers() {
        var created = service.create(new RolRequest("VOCAL", "Vocal de comite"));
        usuarioDAO.setUserCount(created.idRol(), 2);

        IllegalStateException ex = assertThrows(IllegalStateException.class,
            () -> service.delete(created.idRol()));
        assertTrue(ex.getMessage().contains("usuarios asociados"));
    }

    @Test
    void deletesUnassignedCustomRole() {
        var created = service.create(new RolRequest("TEMPORAL", "Rol que se borrara"));
        assertTrue(service.delete(created.idRol()));
        assertThrows(NoSuchElementException.class,
            () -> service.delete(created.idRol()));
    }

    private static final class MemoryRolDAO extends RolDAO {
        private final List<Rol> roles = new ArrayList<>();

        @Override
        public List<Rol> findAll() {
            return new ArrayList<>(roles);
        }

        @Override
        public Optional<Rol> findById(Integer id) {
            return roles.stream().filter(r -> r.getIdRol().equals(id)).findFirst();
        }

        @Override
        public Optional<Rol> findByNombre(String nombre) {
            if (nombre == null) return Optional.empty();
            String trimmed = nombre.trim();
            return roles.stream()
                .filter(r -> r.getNombre() != null && r.getNombre().equalsIgnoreCase(trimmed))
                .findFirst();
        }

        @Override
        public Rol save(Rol entity) {
            if (entity.getIdRol() == null) {
                entity.setIdRol(roles.size() + 1);
            }
            roles.add(entity);
            return entity;
        }

        @Override
        public Rol update(Rol entity) {
            for (int i = 0; i < roles.size(); i++) {
                if (roles.get(i).getIdRol().equals(entity.getIdRol())) {
                    roles.set(i, entity);
                    return entity;
                }
            }
            return null;
        }

        @Override
        public boolean delete(Integer id) {
            return roles.removeIf(r -> r.getIdRol().equals(id));
        }
    }

    private static final class MemoryUsuarioDAO extends UsuarioDAO {
        private final Map<Integer, Integer> userCounts = new HashMap<>();

        void setUserCount(Integer idRol, int count) {
            userCounts.put(idRol, count);
        }

        @Override
        public int countByRol(Integer idRol) {
            return userCounts.getOrDefault(idRol, 0);
        }
    }
}
