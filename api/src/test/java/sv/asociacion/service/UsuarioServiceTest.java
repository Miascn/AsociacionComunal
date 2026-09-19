package sv.asociacion.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import sv.asociacion.dao.MiembroDAO;
import sv.asociacion.dao.RolDAO;
import sv.asociacion.dao.UsuarioDAO;
import sv.asociacion.domain.dto.CreateUsuarioRequest;
import sv.asociacion.domain.dto.UpdateUsuarioRequest;
import sv.asociacion.domain.entity.Miembro;
import sv.asociacion.domain.entity.Rol;
import sv.asociacion.domain.entity.Usuario;

class UsuarioServiceTest {
    @Test
    void createsUpdatesAndDeactivatesWithoutPhysicalDeletion() {
        MemoryUsuarioDAO users = new MemoryUsuarioDAO();
        UsuarioService service = new UsuarioService(users, new ExistingRolDAO(), new ExistingMiembroDAO());

        var created = service.create(new CreateUsuarioRequest("maria.lopez", "segura123", 1, 10));
        assertEquals("ACTIVO", created.estado());

        var updated = service.update(created.idUsuario(), new UpdateUsuarioRequest(
            "maria.admin", null, 1, 0, "BLOQUEADO"));
        assertEquals("maria.admin", updated.nombreUsuario());
        assertEquals(null, updated.idMiembro());
        assertEquals("BLOQUEADO", updated.estado());

        assertTrue(service.delete(created.idUsuario()));
        assertEquals("INACTIVO", service.findById(created.idUsuario()).estado());
        assertEquals(1, users.findAll().size());
    }

    @Test
    void rejectsInvalidDataAndDuplicateUsername() {
        MemoryUsuarioDAO users = new MemoryUsuarioDAO();
        UsuarioService service = new UsuarioService(users, new ExistingRolDAO(), new ExistingMiembroDAO());
        service.create(new CreateUsuarioRequest("usuario.uno", "segura123", 1, null));

        assertThrows(IllegalArgumentException.class,
            () -> service.create(new CreateUsuarioRequest("x", "123", 1, null)));
        assertThrows(IllegalStateException.class,
            () -> service.create(new CreateUsuarioRequest("usuario.uno", "otraClave9", 1, null)));
        assertThrows(IllegalArgumentException.class,
            () -> service.changeState(1, "DESCONOCIDO"));
    }

    @Test
    void filtersByRoleAndStateCorrectly() {
        MemoryUsuarioDAO users = new MemoryUsuarioDAO();
        UsuarioService service = new UsuarioService(users, new ExistingRolDAO(), new ExistingMiembroDAO());
        var u1 = service.create(new CreateUsuarioRequest("admin.user", "segura123", 1, null));
        var u2 = service.create(new CreateUsuarioRequest("member.user", "segura123", 2, 10));
        service.changeState(u2.idUsuario(), "BLOQUEADO");

        assertEquals(2, service.findAll().size());
        assertEquals(1, service.findAll("1", null).size());
        assertEquals("admin.user", service.findAll("ADMINISTRADOR", null).get(0).nombreUsuario());
        assertEquals(1, service.findAll(null, "BLOQUEADO").size());
        assertEquals("member.user", service.findAll(null, "BLOQUEADO").get(0).nombreUsuario());
        assertEquals(0, service.findAll("ADMINISTRADOR", "BLOQUEADO").size());
    }

    @Test
    void rejectsShortPassword() {
        MemoryUsuarioDAO users = new MemoryUsuarioDAO();
        UsuarioService service = new UsuarioService(users, new ExistingRolDAO(), new ExistingMiembroDAO());
        assertThrows(IllegalArgumentException.class,
            () -> service.create(new CreateUsuarioRequest("valid.user", "short", 1, null)));
    }

    @Test
    void resetsPasswordSuccessfully() {
        MemoryUsuarioDAO users = new MemoryUsuarioDAO();
        UsuarioService service = new UsuarioService(users, new ExistingRolDAO(), new ExistingMiembroDAO());
        var u = service.create(new CreateUsuarioRequest("carlos.test", "segura123", 1, null));
        var result = service.resetPassword(u.idUsuario());
        assertEquals("carlos.test", result.nombreUsuario());
        assertTrue(result.temporaryPassword().startsWith("Tmp#"));
        assertTrue(result.temporaryPassword().length() >= 12);
    }

    @Test
    void throwsWhenResettingNonExistentUser() {
        MemoryUsuarioDAO users = new MemoryUsuarioDAO();
        UsuarioService service = new UsuarioService(users, new ExistingRolDAO(), new ExistingMiembroDAO());
        assertThrows(java.util.NoSuchElementException.class, () -> service.resetPassword(999));
    }

    private static final class MemoryUsuarioDAO extends UsuarioDAO {
        private final List<Usuario> values = new ArrayList<>();
        @Override public List<Usuario> findAll() { return new ArrayList<>(values); }
        @Override public Optional<Usuario> findById(Integer id) {
            return values.stream().filter(value -> value.getIdUsuario().equals(id)).findFirst();
        }
        @Override public Usuario findByNombreUsuario(String username) {
            return values.stream().filter(value -> value.getNombreUsuario().equals(username)).findFirst().orElse(null);
        }
        @Override public Usuario save(Usuario value) {
            value.setIdUsuario(values.size() + 1);
            values.add(value);
            return value;
        }
        @Override public Usuario update(Usuario value) { return findById(value.getIdUsuario()).isPresent() ? value : null; }
        @Override public boolean delete(Integer id) {
            Optional<Usuario> value = findById(id);
            value.ifPresent(user -> user.setEstado(Usuario.Estado.INACTIVO));
            return value.isPresent();
        }
        @Override public boolean resetPassword(Integer id, String claveHash, boolean requiereCambioClave) {
            Optional<Usuario> value = findById(id);
            if (value.isPresent()) {
                value.get().setClaveHash(claveHash);
                return true;
            }
            return false;
        }
    }

    private static final class ExistingRolDAO extends RolDAO {
        @Override public Optional<Rol> findById(Integer id) {
            if (id != null && id == 1) return Optional.of(new Rol(1, "ADMINISTRADOR", "Admin"));
            if (id != null && id == 2) return Optional.of(new Rol(2, "MIEMBRO", "Miembro"));
            return Optional.empty();
        }
        @Override public List<Rol> findAll() {
            return List.of(new Rol(1, "ADMINISTRADOR", "Admin"), new Rol(2, "MIEMBRO", "Miembro"));
        }
    }

    private static final class ExistingMiembroDAO extends MiembroDAO {
        @Override public Optional<Miembro> findById(Integer id) {
            return id != null && id == 10 ? Optional.of(new Miembro()) : Optional.empty();
        }
    }
}
