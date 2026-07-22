package models;

public record AuthResult(boolean success, String message, AuthUser user) {
    public static AuthResult success(AuthUser user) {
        return new AuthResult(true, "Acceso concedido.", user);
    }

    public static AuthResult failure(String message) {
        return new AuthResult(false, message, null);
    }
}
