package sv.asociacion.middleware;

import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;

public class AuthMiddleware {
    private final String expectedSecret;

    public AuthMiddleware(String expectedSecret) {
        this.expectedSecret = expectedSecret;
    }

    public void authenticate(Context context) {
        String authorization = context.header("Authorization");
        String suppliedSecret = authorization != null && authorization.startsWith("Bearer ")
            ? authorization.substring("Bearer ".length()).trim()
            : "";

        boolean matches = MessageDigest.isEqual(
            expectedSecret.getBytes(StandardCharsets.UTF_8),
            suppliedSecret.getBytes(StandardCharsets.UTF_8)
        );
        if (!matches) {
            context.header("WWW-Authenticate", "Bearer")
                .status(HttpStatus.UNAUTHORIZED)
                .json(Map.of("error", "No autorizado."));
            context.skipRemainingHandlers();
        }
    }
}