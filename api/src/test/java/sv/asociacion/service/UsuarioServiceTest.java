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
    }

    private static final class ExistingRolDAO extends RolDAO {
        @Override public Optional<Rol> findById(Integer id) {
            return id != null && id == 1 ? Optional.of(new Rol(1, "ADMINISTRADOR", "Admin")) : Optional.empty();
        }
    }

    private static final class ExistingMiembroDAO extends MiembroDAO {
        @Override public Optional<Miembro> findById(Integer id) {
            return id != null && id == 10 ? Optional.of(new Miembro()) : Optional.empty();
        }
    }
}
