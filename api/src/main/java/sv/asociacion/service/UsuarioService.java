package sv.asociacion.service;

import sv.asociacion.dao.UsuarioDAO;
import sv.asociacion.dao.RolDAO;
import sv.asociacion.dao.MiembroDAO;
import sv.asociacion.domain.dto.CreateUsuarioRequest;
import sv.asociacion.domain.dto.UpdateUsuarioRequest;
import sv.asociacion.domain.dto.UsuarioResponse;
import sv.asociacion.domain.entity.Usuario;
import sv.asociacion.util.DateUtils;
import sv.asociacion.util.PasswordHasher;
import java.util.List;
import java.util.NoSuchElementException;

public class UsuarioService {
    private final UsuarioDAO usuarioDAO;
    private final RolDAO rolDAO;
    private final MiembroDAO miembroDAO;

    public UsuarioService(UsuarioDAO usuarioDAO, RolDAO rolDAO, MiembroDAO miembroDAO) {
        this.usuarioDAO = usuarioDAO;
        this.rolDAO = rolDAO;
        this.miembroDAO = miembroDAO;
    }

    public List<UsuarioResponse> findAll() {
        return usuarioDAO.findAll().stream().map(this::toResponse).toList();
    }

    public UsuarioResponse findById(Integer id) {
        return usuarioDAO.findById(id).map(this::toResponse).orElse(null);
    }

    public UsuarioResponse create(CreateUsuarioRequest request) {
        String username = validateUsername(request.nombreUsuario());
        validatePassword(request.clave(), true);
        validateRelations(request.idRol(), request.idMiembro());
        if (usuarioDAO.findByNombreUsuario(username) != null) {
            throw new IllegalStateException("Ya existe un usuario con ese nombre.");
        }
        Usuario usuario = new Usuario();
        usuario.setNombreUsuario(username);
        usuario.setClaveHash(PasswordHasher.hash(request.clave()));
        usuario.setIdRol(request.idRol());
        usuario.setIdMiembro(request.idMiembro() != null && request.idMiembro() > 0 ? request.idMiembro() : null);
        usuario.setEstado(Usuario.Estado.ACTIVO);
        usuarioDAO.save(usuario);
        return toResponse(usuario);
    }

    public UsuarioResponse update(Integer id, UpdateUsuarioRequest request) {
        Usuario usuario = usuarioDAO.findById(id).orElseThrow(() -> new NoSuchElementException("Usuario no encontrado."));
        if (request.nombreUsuario() != null && !request.nombreUsuario().isBlank()) {
            String username = validateUsername(request.nombreUsuario());
            Usuario duplicate = usuarioDAO.findByNombreUsuario(username);
            if (duplicate != null && !duplicate.getIdUsuario().equals(id)) {
                throw new IllegalStateException("Ya existe un usuario con ese nombre.");
            }
            usuario.setNombreUsuario(username);
        }
        if (request.clave() != null && !request.clave().isBlank()) {
            validatePassword(request.clave(), false);
            usuario.setClaveHash(PasswordHasher.hash(request.clave()));
        }
        if (request.idRol() != null) {
            validateRole(request.idRol());
            usuario.setIdRol(request.idRol());
        }
        if (request.idMiembro() != null) {
            if (request.idMiembro() > 0 && miembroDAO.findById(request.idMiembro()).isEmpty()) {
                throw new IllegalArgumentException("El miembro seleccionado no existe.");
            }
            usuario.setIdMiembro(request.idMiembro() > 0 ? request.idMiembro() : null);
        }
        if (request.estado() != null) usuario.setEstado(parseState(request.estado()));
        if (usuarioDAO.update(usuario) == null) throw new NoSuchElementException("Usuario no encontrado.");
        return toResponse(usuario);
    }

    public UsuarioResponse changeState(Integer id, String state) {
        Usuario usuario = usuarioDAO.findById(id).orElseThrow(() -> new NoSuchElementException("Usuario no encontrado."));
        usuario.setEstado(parseState(state));
        usuarioDAO.update(usuario);
        return toResponse(usuario);
    }

    public boolean delete(Integer id) {
        return usuarioDAO.delete(id);
    }

    private void validateRelations(Integer roleId, Integer memberId) {
        validateRole(roleId);
        if (memberId != null && memberId > 0 && miembroDAO.findById(memberId).isEmpty()) {
            throw new IllegalArgumentException("El miembro seleccionado no existe.");
        }
    }

    private void validateRole(Integer roleId) {
        if (roleId == null || roleId <= 0 || rolDAO.findById(roleId).isEmpty()) {
            throw new IllegalArgumentException("Selecciona un rol válido.");
        }
    }

    private String validateUsername(String value) {
        String username = value == null ? "" : value.trim();
        if (!username.matches("[A-Za-z0-9._@-]{3,50}")) {
            throw new IllegalArgumentException("El usuario debe tener de 3 a 50 caracteres válidos.");
        }
        return username;
    }

    private void validatePassword(String value, boolean required) {
        if ((value == null || value.isBlank()) && !required) return;
        if (value == null || value.length() < 8) {
            throw new IllegalArgumentException("La contraseña debe contener al menos 8 caracteres.");
        }
    }

    private Usuario.Estado parseState(String value) {
        try {
            return Usuario.Estado.valueOf(value == null ? "" : value.trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("El estado debe ser ACTIVO, BLOQUEADO o INACTIVO.");
        }
    }

    private UsuarioResponse toResponse(Usuario u) {
        return new UsuarioResponse(
            u.getIdUsuario(),
            u.getNombreUsuario(),
            u.getIdRol(),
            u.getIdMiembro(),
            u.getEstado() == null ? null : u.getEstado().name(),
            DateUtils.formatDateTime(u.getUltimoAcceso())
        );
    }
}
