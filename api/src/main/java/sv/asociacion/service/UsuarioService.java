package sv.asociacion.service;

import sv.asociacion.dao.UsuarioDAO;
import sv.asociacion.domain.dto.CreateUsuarioRequest;
import sv.asociacion.domain.dto.UpdateUsuarioRequest;
import sv.asociacion.domain.dto.UsuarioResponse;
import sv.asociacion.domain.entity.Usuario;
import sv.asociacion.util.DateUtils;
import sv.asociacion.util.PasswordHasher;
import java.util.List;

public class UsuarioService {
    private final UsuarioDAO usuarioDAO;

    public UsuarioService(UsuarioDAO usuarioDAO) {
        this.usuarioDAO = usuarioDAO;
    }

    public List<UsuarioResponse> findAll() {
        return usuarioDAO.findAll().stream().map(this::toResponse).toList();
    }

    public UsuarioResponse findById(Integer id) {
        return usuarioDAO.findById(id).map(this::toResponse).orElse(null);
    }

    public UsuarioResponse create(CreateUsuarioRequest request) {
        if (usuarioDAO.findByNombreUsuario(request.nombreUsuario().trim()) != null) {
            throw new IllegalStateException("Ya existe un usuario con ese nombre.");
        }
        Usuario usuario = new Usuario();
        usuario.setNombreUsuario(request.nombreUsuario().trim());
        usuario.setClaveHash(PasswordHasher.hash(request.clave()));
        usuario.setIdRol(request.idRol());
        usuario.setIdMiembro(request.idMiembro());
        usuario.setEstado(Usuario.Estado.ACTIVO);
        usuarioDAO.save(usuario);
        return toResponse(usuario);
    }

    public UsuarioResponse update(Integer id, UpdateUsuarioRequest request) {
        Usuario usuario = usuarioDAO.findById(id).orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado."));
        if (request.nombreUsuario() != null && !request.nombreUsuario().isBlank()) {
            usuario.setNombreUsuario(request.nombreUsuario().trim());
        }
        if (request.clave() != null && !request.clave().isBlank()) {
            usuario.setClaveHash(PasswordHasher.hash(request.clave()));
        }
        if (request.idRol() != null) usuario.setIdRol(request.idRol());
        if (request.idMiembro() != null) usuario.setIdMiembro(request.idMiembro());
        if (request.estado() != null) usuario.setEstado(Usuario.Estado.valueOf(request.estado()));
        usuarioDAO.update(usuario);
        return toResponse(usuario);
    }

    public boolean delete(Integer id) {
        return usuarioDAO.delete(id);
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