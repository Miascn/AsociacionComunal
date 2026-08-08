package sv.asociacion.api.auth;

import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import io.javalin.router.JavalinDefaultRoutingApi;
import java.util.Map;

public final class AuthRoutes {
    private final AuthService auth;

    public AuthRoutes(AuthService auth) { this.auth = auth; }

    public void register(JavalinDefaultRoutingApi app) {
        app.post("/api/auth/login", context -> {
            LoginRequest request = context.bodyAsClass(LoginRequest.class);
            AuthService.LoginResult result = auth.login(request.username(), request.password());
            context.json(LoginResponse.from(result));
        });
        app.post("/api/auth/refresh", context -> {
            TokenRequest request = context.bodyAsClass(TokenRequest.class);
            context.json(auth.refresh(request.refreshToken()));
        });
        app.post("/api/auth/logout", context -> {
            TokenRequest request = context.bodyAsClass(TokenRequest.class);
            auth.logout(request.refreshToken());
            context.status(HttpStatus.NO_CONTENT);
        });
        app.get("/api/me", context -> context.json(MeResponse.from(requirePrincipal(context))));
        app.exception(AuthException.class, (exception, context) -> context.status(exception.status())
            .json(Map.of("error", exception.code(), "message", exception.getMessage())));
    }

    public AuthPrincipal requirePrincipal(Context context) {
        String authorization = context.header("Authorization");
        String token = authorization != null && authorization.startsWith("Bearer ")
            ? authorization.substring(7).trim() : "";
        return auth.authenticate(token);
    }

    public record LoginRequest(String username, String password) { }
    public record TokenRequest(String refreshToken) { }
    public record UserResponse(int id, String username, String role) { }
    public record MemberResponse(Integer id, String names, String lastNames, String status) { }
    public record LoginResponse(String accessToken, String refreshToken, long expiresIn, UserResponse user) {
        static LoginResponse from(AuthService.LoginResult result) {
            AuthPrincipal p = result.principal();
            return new LoginResponse(result.accessToken(), result.refreshToken(), result.expiresIn(), new UserResponse(p.userId(), p.username(), p.role()));
        }
    }
    public record MeResponse(UserResponse user, MemberResponse member) {
        static MeResponse from(AuthPrincipal p) {
            MemberResponse member = p.memberId() == null ? null : new MemberResponse(p.memberId(), p.memberNames(), p.memberLastNames(), p.memberStatus());
            return new MeResponse(new UserResponse(p.userId(), p.username(), p.role()), member);
        }
    }
}
