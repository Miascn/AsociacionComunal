package sv.asociacion.service;

import java.util.List;
import sv.asociacion.api.auth.MemberProvisioningService;
import sv.asociacion.dao.MiembroDAO;
import sv.asociacion.domain.dto.CreateMemberRequest;
import sv.asociacion.domain.dto.CreateMemberResponse;
import sv.asociacion.domain.dto.MiembroResponse;
import sv.asociacion.domain.entity.Miembro;

public class MiembroService {
    private final MiembroDAO miembroDAO;
    private final MemberProvisioningService provisioning;
    private final sv.asociacion.dao.UsuarioDAO usuarioDAO;

    public MiembroService(MiembroDAO miembroDAO, MemberProvisioningService provisioning) {
        this(miembroDAO, provisioning, new sv.asociacion.dao.UsuarioDAO());
    }

    public MiembroService(MiembroDAO miembroDAO, MemberProvisioningService provisioning, sv.asociacion.dao.UsuarioDAO usuarioDAO) {
        this.miembroDAO = miembroDAO;
        this.provisioning = provisioning;
        this.usuarioDAO = usuarioDAO != null ? usuarioDAO : new sv.asociacion.dao.UsuarioDAO();
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

    public sv.asociacion.domain.dto.CredencialesMiembroResponse getCredenciales(int idMiembro) {
        if (usuarioDAO == null) return null;
        var opt = usuarioDAO.findByIdMiembro(idMiembro);
        if (opt.isEmpty()) return null;
        sv.asociacion.domain.entity.Usuario u = opt.get();
        boolean req = Boolean.TRUE.equals(u.getRequiereCambioClave());
        String claveTemp = u.getClaveTemporal();
        if (req && (claveTemp == null || claveTemp.isBlank())) {
            String chars = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789!@#$%";
            java.security.SecureRandom random = new java.security.SecureRandom();
            StringBuilder sb = new StringBuilder("Tmp#");
            for (int i = 0; i < 12; i++) sb.append(chars.charAt(random.nextInt(chars.length())));
            claveTemp = sb.toString();
            String hashed = sv.asociacion.util.PasswordHasher.hash(claveTemp);
            usuarioDAO.resetPassword(u.getIdUsuario(), hashed, true, claveTemp);
        }
        return new sv.asociacion.domain.dto.CredencialesMiembroResponse(
            idMiembro,
            u.getNombreUsuario(),
            req ? claveTemp : null,
            req,
            req ? "Contraseña provisional pendiente de cambio en celular" : "Contraseña ya cambiada por el miembro en el celular"
        );
    }

    public sv.asociacion.domain.dto.CredencialesMiembroResponse generarCredenciales(int idMiembro) {
        var miembroOpt = miembroDAO.findById(idMiembro);
        if (miembroOpt.isEmpty()) return null;
        var miembro = miembroOpt.get();

        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789!@#$%";
        java.security.SecureRandom random = new java.security.SecureRandom();
        StringBuilder sb = new StringBuilder("Tmp!");
        for (int i = 0; i < 12; i++) sb.append(chars.charAt(random.nextInt(chars.length())));
        String temporary = sb.toString();
        String hashed = sv.asociacion.util.PasswordHasher.hash(temporary);

        String username = (miembro.getDui() != null && !miembro.getDui().isBlank())
            ? miembro.getDui().trim()
            : ("miembro" + idMiembro);

        var usuarioOpt = usuarioDAO.findByIdMiembro(idMiembro);
        if (usuarioOpt.isPresent()) {
            var u = usuarioOpt.get();
            usuarioDAO.resetPassword(u.getIdUsuario(), hashed, true, temporary);
            return new sv.asociacion.domain.dto.CredencialesMiembroResponse(
                idMiembro,
                u.getNombreUsuario(),
                temporary,
                true,
                "Nueva contraseña provisional generada exitosamente"
            );
        } else {
            try (java.sql.Connection conn = sv.asociacion.config.DBConnection.getInstance().getConnection()) {
                String sql = "INSERT INTO usuario (id_rol, id_miembro, nombre_usuario, clave_hash, estado, requiere_cambio_clave, clave_temporal) " +
                             "SELECT id_rol, ?, ?, ?, 'ACTIVO', TRUE, ? FROM rol WHERE nombre = 'MIEMBRO'";
                try (java.sql.PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setInt(1, idMiembro);
                    ps.setString(2, username);
                    ps.setString(3, hashed);
                    ps.setString(4, temporary);
                    int affected = ps.executeUpdate();
                    if (affected == 0) {
                        String fallback = "INSERT INTO usuario (id_rol, id_miembro, nombre_usuario, clave_hash, estado, requiere_cambio_clave, clave_temporal) " +
                                          "SELECT MIN(id_rol), ?, ?, ?, 'ACTIVO', TRUE, ? FROM rol";
                        try (java.sql.PreparedStatement psFb = conn.prepareStatement(fallback)) {
                            psFb.setInt(1, idMiembro);
                            psFb.setString(2, username);
                            psFb.setString(3, hashed);
                            psFb.setString(4, temporary);
                            psFb.executeUpdate();
                        }
                    }
                } catch (java.sql.SQLException sqle) {
                    String fallbackSinCol = "INSERT INTO usuario (id_rol, id_miembro, nombre_usuario, clave_hash, estado, requiere_cambio_clave) " +
                                            "SELECT id_rol, ?, ?, ?, 'ACTIVO', TRUE FROM rol WHERE nombre = 'MIEMBRO'";
                    try (java.sql.PreparedStatement psFb = conn.prepareStatement(fallbackSinCol)) {
                        psFb.setInt(1, idMiembro);
                        psFb.setString(2, username);
                        psFb.setString(3, hashed);
                        psFb.executeUpdate();
                    }
                }
            } catch (Exception ex) {
                throw new IllegalStateException("Error al aprovisionar el usuario del miembro: " + ex.getMessage(), ex);
            }

            return new sv.asociacion.domain.dto.CredencialesMiembroResponse(
                idMiembro,
                username,
                temporary,
                true,
                "Acceso generado exitosamente con contraseña provisional"
            );
        }
    }

    private static String clean(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
