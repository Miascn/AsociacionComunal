package sv.asociacion.service;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import sv.asociacion.dao.RolDAO;
import sv.asociacion.dao.UsuarioDAO;
import sv.asociacion.domain.dto.RolRequest;
import sv.asociacion.domain.dto.RolResponse;
import sv.asociacion.domain.entity.Rol;

public class RolService {
    /**
     * Nombres de rol protegidos frente a renombrado y eliminacion.
     *
     * <p>Cubre los tres roles que siembra {@code schema.sql} —{@code ADMIN},
     * {@code DIRECTIVO} y {@code MIEMBRO}— mas los nombres que la capa de
     * autorizacion trata como privilegiados aunque la semilla no los cree.
     *
     * <p>Esos ultimos se conservan deliberadamente (SCRUM-329, criterio 8): si un rol
     * con uno de esos nombres existiera y pudiera renombrarse, perderia sus permisos
     * en silencio. La divergencia de fondo entre el modelo de roles y el de cargos
     * queda registrada en SCRUM-179 y se resuelve por separado.
     */
    public static final Set<String> ROLES_BASE = Set.of(
        "ADMIN", "ADMINISTRADOR", "DIRECTIVO", "PRESIDENTE", "SECRETARIO", "TESORERO", "SINDICO", "MIEMBRO"
    );

    private final RolDAO rolDAO;
    private final UsuarioDAO usuarioDAO;

    public RolService(RolDAO rolDAO) {
        this(rolDAO, new UsuarioDAO());
    }

    public RolService(RolDAO rolDAO, UsuarioDAO usuarioDAO) {
        this.rolDAO = rolDAO;
        this.usuarioDAO = usuarioDAO;
    }

    public List<RolResponse> findAll() {
        return rolDAO.findAll().stream()
            .map(this::toResponse)
            .toList();
    }

    public RolResponse findById(Integer id) {
        if (id == null) return null;
        return rolDAO.findById(id)
            .map(this::toResponse)
            .orElse(null);
    }

    public RolResponse create(RolRequest request) {
        String error = request.validationError();
        if (error != null) throw new IllegalArgumentException(error);

        String nombre = request.nombre().trim();
        if (rolDAO.findByNombre(nombre).isPresent()) {
            throw new IllegalStateException("Ya existe un rol con ese nombre.");
        }

        Rol rol = new Rol();
        rol.setNombre(nombre);
        rol.setDescripcion(clean(request.descripcion()));
        rol = rolDAO.save(rol);

        return toResponse(rol);
    }

    public RolResponse update(Integer id, RolRequest request) {
        if (id == null) throw new IllegalArgumentException("ID de rol inválido.");
        Rol rol = rolDAO.findById(id)
            .orElseThrow(() -> new NoSuchElementException("Rol no encontrado."));

        String error = request.validationError();
        if (error != null) throw new IllegalArgumentException(error);

        String nuevoNombre = request.nombre().trim();
        if (isBaseRole(rol.getNombre()) && !rol.getNombre().equalsIgnoreCase(nuevoNombre)) {
            throw new IllegalStateException("No se puede cambiar el nombre de un rol base del sistema.");
        }

        var duplicado = rolDAO.findByNombre(nuevoNombre);
        if (duplicado.isPresent() && !duplicado.get().getIdRol().equals(id)) {
            throw new IllegalStateException("Ya existe un rol con ese nombre.");
        }

        rol.setNombre(nuevoNombre);
        rol.setDescripcion(clean(request.descripcion()));
        rol = rolDAO.update(rol);

        return toResponse(rol);
    }

    public boolean delete(Integer id) {
        if (id == null) throw new IllegalArgumentException("ID de rol inválido.");
        Rol rol = rolDAO.findById(id)
            .orElseThrow(() -> new NoSuchElementException("Rol no encontrado."));

        if (isBaseRole(rol.getNombre())) {
            throw new IllegalStateException("No se puede eliminar un rol base del sistema.");
        }

        int usuariosAsociados = usuarioDAO.countByRol(id);
        if (usuariosAsociados > 0) {
            throw new IllegalStateException("No se puede eliminar el rol porque tiene usuarios asociados (" + usuariosAsociados + ").");
        }

        return rolDAO.delete(id);
    }

    public static boolean isBaseRole(String nombre) {
        if (nombre == null) return false;
        return ROLES_BASE.contains(nombre.trim().toUpperCase());
    }

    private RolResponse toResponse(Rol rol) {
        int count = usuarioDAO.countByRol(rol.getIdRol());
        return RolResponse.from(rol, count);
    }

    private static String clean(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
