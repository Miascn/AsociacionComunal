package models;

public record AuthResult(boolean success, String message, AuthUser user, String token) {
    public static AuthResult success(AuthUser user, String token) {
        return new AuthResult(true, "Acceso concedido.", user, token);
    }

    public static AuthResult failure(String message) {
        return new AuthResult(false, message, null, null);
    }
}