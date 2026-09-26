package sv.asociacion.service;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import sv.asociacion.dao.RolDAO;
import sv.asociacion.dao.UsuarioDAO;
import sv.asociacion.dao.MiembroDAO;
import sv.asociacion.domain.dto.LoginResponse;
import sv.asociacion.domain.entity.Rol;
import sv.asociacion.domain.entity.Miembro;
import sv.asociacion.domain.entity.Usuario;
import sv.asociacion.util.PasswordHasher;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

public class AuthService {
    private final UsuarioDAO usuarioDAO;
    private final RolDAO rolDAO;
    private final MiembroDAO miembroDAO;
    private final SecretKey jwtKey;
    private final long expirationMs;

    public AuthService(UsuarioDAO usuarioDAO, RolDAO rolDAO, MiembroDAO miembroDAO, String jwtSecret) {
        this.usuarioDAO = usuarioDAO;
        this.rolDAO = rolDAO;
        this.miembroDAO = miembroDAO;
        this.jwtKey = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = 86_400_000L;
    }

    public LoginResponse login(String nombreUsuario, String clave) {
        Usuario usuario = usuarioDAO.findByNombreUsuario(nombreUsuario);
        if (usuario == null || !PasswordHasher.verify(clave, usuario.getClaveHash())) {
            return null;
        }
        if (usuario.getEstado() != Usuario.Estado.ACTIVO) {
            return null;
        }

        String role = "";
        if (usuario.getIdRol() != null) {
            Rol rol = rolDAO.findById(usuario.getIdRol()).orElse(null);
            if (rol != null) role = rol.getNombre();
        }

        // Separación estricta: las cuentas de miembros residentes están destinadas
        // exclusivamente a la aplicación móvil y no pueden acceder al sistema de escritorio.
        if ("MIEMBRO".equalsIgnoreCase(role)) {
            return null;
        }

        String displayName = usuario.getNombreUsuario();
        if (usuario.getIdMiembro() != null) {
            Miembro miembro = miembroDAO.findById(usuario.getIdMiembro()).orElse(null);
            if (miembro != null) {
                displayName = (miembro.getNombres() + " " + miembro.getApellidos()).trim();
            }
        }

        long now = System.currentTimeMillis();
        String token = Jwts.builder()
            .subject(usuario.getIdUsuario().toString())
            .claim("nombreUsuario", usuario.getNombreUsuario())
            .claim("idRol", usuario.getIdRol())
            .claim("displayName", displayName)
            .claim("role", role)
            .issuedAt(new Date(now))
            .expiration(new Date(now + expirationMs))
            .signWith(jwtKey)
            .compact();

        return new LoginResponse(token, displayName, role, usuario.getIdUsuario());
    }

    public JwtClaims validateToken(String token) {
        try {
            var claims = Jwts.parser().verifyWith(jwtKey).build()
                .parseSignedClaims(token).getPayload();
            return new JwtClaims(
                Integer.parseInt(claims.getSubject()),
                claims.get("nombreUsuario", String.class),
                claims.get("displayName", String.class),
                claims.get("role", String.class)
            );
        } catch (Exception e) {
            return null;
        }
    }

    public record JwtClaims(Integer idUsuario, String nombreUsuario, String displayName, String role) {}
}