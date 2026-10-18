package sv.asociacion.service;

import java.util.List;
import java.util.Objects;
import sv.asociacion.api.auth.MemberProvisioningService;
import sv.asociacion.dao.MiembroDAO;
import sv.asociacion.dao.UsuarioDAO;
import sv.asociacion.domain.dto.CreateMemberRequest;
import sv.asociacion.domain.dto.CreateMemberResponse;
import sv.asociacion.domain.dto.CredencialesMiembroResponse;
import sv.asociacion.domain.dto.MiembroResponse;
import sv.asociacion.domain.entity.Miembro;
import sv.asociacion.domain.entity.Usuario;
import sv.asociacion.util.GeneradorClaveTemporal;
import sv.asociacion.util.PasswordHasher;

/**
 * Reglas del padron de miembros y de sus credenciales de acceso.
 *
 * <p>Desde el Avance 3 no abre conexiones ni contiene SQL: la creacion de la cuenta
 * vive en {@link UsuarioDAO#crearCuentaMiembro}. Tampoco instancia sus
 * dependencias; las recibe todas por constructor desde {@code ApiServer}.</p>
 */
public class MiembroService {
    private final MiembroDAO miembroDAO;
    private final MemberProvisioningService provisioning;
    private final UsuarioDAO usuarioDAO;

    public MiembroService(MiembroDAO miembroDAO, MemberProvisioningService provisioning, UsuarioDAO usuarioDAO) {
        this.miembroDAO = Objects.requireNonNull(miembroDAO);
        this.provisioning = Objects.requireNonNull(provisioning);
        this.usuarioDAO = Objects.requireNonNull(usuarioDAO);
    }

    public List<MiembroResponse> findAll() {
        return miembroDAO.findAll().stream().map(MiembroResponse::from).toList();
    }

    public MiembroResponse findById(int id) {
        return miembroDAO.findById(id).map(MiembroResponse::from).orElse(null);
    }

    public CreateMemberResponse createMember(CreateMemberRequest request) {
        String error = request.validationError();
        if (error != null) throw new IllegalArgumentException(error);
        String document = request.documento().trim();
        if (miembroDAO.existsByDui(document)) throw new IllegalStateException("Ya existe un miembro con ese documento.");
        MemberProvisioningService.Result created = provisioning.create(
            document, request.tipoDocumento().trim(), clean(request.paisOrigen()), request.idVivienda(),
            request.nombres().trim(), request.apellidos().trim(), clean(request.telefono()), clean(request.correo())
        );
        var member = miembroDAO.findById(created.memberId())
            .orElseThrow(() -> new IllegalStateException("El miembro creado no pudo recuperarse."));
        return new CreateMemberResponse(MiembroResponse.from(member), document, created.temporaryPassword());
    }

    public MiembroResponse update(int id, CreateMemberRequest request) {
        String error = request.validationError();
        if (error != null) throw new IllegalArgumentException(error);
        Miembro member = miembroDAO.findById(id).orElse(null);
        if (member == null) return null;
        String document = request.documento().trim();
        if (miembroDAO.existsByDuiExcludingId(document, id)) {
            throw new IllegalStateException("Ya existe otro miembro con ese documento.");
        }
        member.setDui(document);
        member.setTipoDocumento(request.tipoDocumento().trim());
        member.setPaisOrigen(clean(request.paisOrigen()));
        member.setIdVivienda(request.idVivienda());
        member.setNombres(request.nombres().trim());
        member.setApellidos(request.apellidos().trim());
        member.setTelefono(clean(request.telefono()));
        member.setCorreo(clean(request.correo()));
        Miembro updated = miembroDAO.update(member);
        return updated == null ? null : MiembroResponse.from(updated);
    }

    public MiembroResponse changeState(int id, String state) {
        Miembro.Estado estado;
        try {
            estado = Miembro.Estado.valueOf(state == null ? "" : state.trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("El estado debe ser ACTIVO o INACTIVO.");
        }
        if (!miembroDAO.changeEstado(id, estado)) return null;
        return miembroDAO.findById(id).map(MiembroResponse::from).orElse(null);
    }

    public CredencialesMiembroResponse getCredenciales(int idMiembro) {
        var opt = usuarioDAO.findByIdMiembro(idMiembro);
        if (opt.isEmpty()) return null;
        Usuario u = opt.get();
        boolean req = Boolean.TRUE.equals(u.getRequiereCambioClave());
        String claveTemp = u.getClaveTemporal();
        if (req && (claveTemp == null || claveTemp.isBlank())) {
            claveTemp = GeneradorClaveTemporal.generar();
            usuarioDAO.resetPassword(u.getIdUsuario(), PasswordHasher.hash(claveTemp), true, claveTemp);
        }
        return new CredencialesMiembroResponse(
            idMiembro,
            u.getNombreUsuario(),
            req ? claveTemp : null,
            req,
            req ? "Contraseña provisional pendiente de cambio en celular" : "Contraseña ya cambiada por el miembro en el celular"
        );
    }

    /**
     * Genera una contrasena provisional nueva. Si el miembro ya tiene cuenta, la
     * restablece; si no, la crea con su documento como nombre de usuario.
     */
    public CredencialesMiembroResponse generarCredenciales(int idMiembro) {
        var miembroOpt = miembroDAO.findById(idMiembro);
        if (miembroOpt.isEmpty()) return null;
        Miembro miembro = miembroOpt.get();

        String temporary = GeneradorClaveTemporal.generar();
        String hashed = PasswordHasher.hash(temporary);

        var usuarioOpt = usuarioDAO.findByIdMiembro(idMiembro);
        if (usuarioOpt.isPresent()) {
            Usuario u = usuarioOpt.get();
            usuarioDAO.resetPassword(u.getIdUsuario(), hashed, true, temporary);
            return new CredencialesMiembroResponse(idMiembro, u.getNombreUsuario(), temporary, true,
                "Nueva contraseña provisional generada exitosamente");
        }

        String username = nombreUsuarioPara(miembro);
        if (!usuarioDAO.crearCuentaMiembro(idMiembro, username, hashed, temporary)) {
            throw new IllegalStateException("No fue posible crear la cuenta del miembro: no hay roles configurados.");
        }
        return new CredencialesMiembroResponse(idMiembro, username, temporary, true,
            "Acceso generado exitosamente con contraseña provisional");
    }

    /** El documento del miembro es su usuario; si no lo tiene, se usa un nombre derivado del id. */
    static String nombreUsuarioPara(Miembro miembro) {
        return miembro.getDui() != null && !miembro.getDui().isBlank()
            ? miembro.getDui().trim()
            : "miembro" + miembro.getIdMiembro();
    }

    private static String clean(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
