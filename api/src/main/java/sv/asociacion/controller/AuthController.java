package sv.asociacion.controller;

import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import sv.asociacion.domain.dto.LoginRequest;
import sv.asociacion.domain.dto.LoginResponse;
import sv.asociacion.service.AuthService;
import java.util.Map;

public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    public void login(Context context) {
        LoginRequest request = context.bodyAsClass(LoginRequest.class);
        if (request.nombreUsuario() == null || request.nombreUsuario().isBlank()
            || request.clave() == null || request.clave().isBlank()) {
            context.status(HttpStatus.BAD_REQUEST).json(Map.of("error", "Usuario y contraseña son obligatorios."));
            return;
        }
        LoginResponse response = authService.login(request.nombreUsuario().trim(), request.clave());
        if (response == null) {
            context.status(HttpStatus.UNAUTHORIZED).json(Map.of("error", "Credenciales inválidas o cuenta sin acceso al sistema administrativo de escritorio."));
            return;
        }
        context.json(response);
    }
}