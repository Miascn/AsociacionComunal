package sv.asociacion.middleware;

import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import java.util.Map;
import sv.asociacion.api.auth.AuthPrincipal;
import sv.asociacion.dao.UsuarioDAO;
import sv.asociacion.domain.entity.Usuario;
import sv.asociacion.service.AuthService;

public class JwtAuthMiddleware {
    private final AuthService authService;
    private final sv.asociacion.api.auth.AuthService mobileAuthService;
    private final UsuarioDAO usuarioDAO;

    public JwtAuthMiddleware(AuthService authService) {
        this(authService, null, null);
    }

    public JwtAuthMiddleware(AuthService authService, sv.asociacion.api.auth.AuthService mobileAuthService, UsuarioDAO usuarioDAO) {
        this.authService = authService;
        this.mobileAuthService = mobileAuthService;
        this.usuarioDAO = usuarioDAO;
    }

    public void authenticate(Context context) {
        String path = context.path();
        if (path.equals("/api/admin/auth/login")
            || path.equals("/api/me")
            || path.startsWith("/api/auth/")
            || path.startsWith("/api/updates/")
            || path.startsWith("/api/mobile/updates/")
            || path.equals("/health")) {
            return;
        }

        String authorization = context.header("Authorization");
        String token = authorization != null && authorization.startsWith("Bearer ")
            ? authorization.substring("Bearer ".length()).trim()
            : "";

        if (token.isEmpty()) {
            context.header("WWW-Authenticate", "Bearer")
                .status(HttpStatus.UNAUTHORIZED)
                .json(Map.of("error", "No autorizado. Token no proporcionado."));
            context.skipRemainingHandlers();
            return;
        }

        // 1. Intentar validar como JWT clásico de escritorio (Admin / Swing)
        AuthService.JwtClaims claims = authService.validateToken(token);
        if (claims != null) {
            context.attribute("idUsuario", claims.idUsuario());
            context.attribute("nombreUsuario", claims.nombreUsuario());
            context.attribute("displayName", claims.displayName());
            context.attribute("role", claims.role());
            if (usuarioDAO != null) {
                Usuario u = usuarioDAO.findById(claims.idUsuario()).orElse(null);
                if (u != null && u.getIdMiembro() != null) {
                    context.attribute("idMiembro", u.getIdMiembro());
                }
            }
            return;
        }

        // 2. Intentar validar como sesión móvil (Móvil Flutter)
        if (mobileAuthService != null) {
            try {
                AuthPrincipal principal = mobileAuthService.authenticate(token);
                if (principal != null) {
                    context.attribute("idUsuario", principal.userId());
                    context.attribute("nombreUsuario", principal.username());
                    String name = principal.memberNames() != null
                        ? (principal.memberNames() + " " + (principal.memberLastNames() != null ? principal.memberLastNames() : "")).trim()
                        : principal.username();
                    context.attribute("displayName", name);
                    context.attribute("role", principal.role());
                    context.attribute("idMiembro", principal.memberId());
                    return;
                }
            } catch (Exception ignored) {
                // Token no válido para sesión móvil
            }
        }

        context.header("WWW-Authenticate", "Bearer")
            .status(HttpStatus.UNAUTHORIZED)
            .json(Map.of("error", "Token inválido o expirado."));
        context.skipRemainingHandlers();
    }
}
