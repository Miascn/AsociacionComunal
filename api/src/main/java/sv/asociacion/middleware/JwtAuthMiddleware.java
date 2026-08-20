package sv.asociacion.middleware;

import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import sv.asociacion.service.AuthService;
import java.util.Map;

public class JwtAuthMiddleware {
    private final AuthService authService;

    public JwtAuthMiddleware(AuthService authService) {
        this.authService = authService;
    }

    public void authenticate(Context context) {
        String path = context.path();
        if (path.equals("/api/admin/auth/login")
            || path.equals("/api/me")
            || path.startsWith("/api/auth/")
            || path.startsWith("/api/updates/")
            || path.startsWith("/api/mobile/updates/")) {
            return;
        }

        String authorization = context.header("Authorization");
        String token = authorization != null && authorization.startsWith("Bearer ")
            ? authorization.substring("Bearer ".length()).trim()
            : "";

        AuthService.JwtClaims claims = authService.validateToken(token);
        if (claims == null) {
            context.header("WWW-Authenticate", "Bearer")
                .status(HttpStatus.UNAUTHORIZED)
                .json(Map.of("error", "Token inválido o expirado."));
            context.skipRemainingHandlers();
            return;
        }
        context.attribute("idUsuario", claims.idUsuario());
        context.attribute("nombreUsuario", claims.nombreUsuario());
        context.attribute("displayName", claims.displayName());
        context.attribute("role", claims.role());
    }
}
