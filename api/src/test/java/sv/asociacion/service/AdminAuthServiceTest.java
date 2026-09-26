package sv.asociacion.service;

import static org.junit.jupiter.api.Assertions.*;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import sv.asociacion.dao.MiembroDAO;
import sv.asociacion.dao.RolDAO;
import sv.asociacion.dao.UsuarioDAO;
import sv.asociacion.domain.entity.Miembro;
import sv.asociacion.domain.entity.Rol;
import sv.asociacion.domain.entity.Usuario;
import sv.asociacion.util.PasswordHasher;

class AdminAuthServiceTest {

    @Test
    void rejectsMobileMemberFromDesktopLogin() {
        UsuarioDAO usuarioDAO = new UsuarioDAO() {
            @Override
            public Usuario findByNombreUsuario(String nombreUsuario) {
                if ("residente.uno".equals(nombreUsuario)) {
                    Usuario u = new Usuario();
                    u.setIdUsuario(10);
                    u.setIdRol(2); // Rol MIEMBRO
                    u.setIdMiembro(100);
                    u.setNombreUsuario("residente.uno");
                    u.setClaveHash(PasswordHasher.hash("ClaveSegura123#"));
                    u.setEstado(Usuario.Estado.ACTIVO);
                    return u;
                }
                return null;
            }
        };

        RolDAO rolDAO = new RolDAO() {
            @Override
            public Optional<Rol> findById(Integer id) {
                if (id != null && id == 2) {
                    return Optional.of(new Rol(2, "MIEMBRO", "Miembro regular de la comunidad"));
                }
                return Optional.empty();
            }
        };

        MiembroDAO miembroDAO = new MiembroDAO();

        AuthService authService = new AuthService(usuarioDAO, rolDAO, miembroDAO, "test-secret-key-12345678901234567890123456789012");

        // Intento de login en sistema de escritorio con cuenta de MIEMBRO móvil
        var response = authService.login("residente.uno", "ClaveSegura123#");

        // Debe ser RECHAZADO (retorna null) porque pertenece a la app móvil
        assertNull(response, "Las cuentas con rol MIEMBRO no deben tener acceso al sistema Java de escritorio.");
    }

    @Test
    void allowsAdminUserToLoginToDesktop() {
        UsuarioDAO usuarioDAO = new UsuarioDAO() {
            @Override
            public Usuario findByNombreUsuario(String nombreUsuario) {
                if ("admin.sistema".equals(nombreUsuario)) {
                    Usuario u = new Usuario();
                    u.setIdUsuario(1);
                    u.setIdRol(1); // Rol ADMIN
                    u.setNombreUsuario("admin.sistema");
                    u.setClaveHash(PasswordHasher.hash("AdminPass123#"));
                    u.setEstado(Usuario.Estado.ACTIVO);
                    return u;
                }
                return null;
            }
        };

        RolDAO rolDAO = new RolDAO() {
            @Override
            public Optional<Rol> findById(Integer id) {
                if (id != null && id == 1) {
                    return Optional.of(new Rol(1, "ADMIN", "Administrador del sistema"));
                }
                return Optional.empty();
            }
        };

        MiembroDAO miembroDAO = new MiembroDAO();

        AuthService authService = new AuthService(usuarioDAO, rolDAO, miembroDAO, "test-secret-key-12345678901234567890123456789012");

        var response = authService.login("admin.sistema", "AdminPass123#");

        assertNotNull(response);
        assertEquals("ADMIN", response.role());
        assertNotNull(response.token());
    }
}
