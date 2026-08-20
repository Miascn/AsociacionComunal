package sv.asociacion.api.auth;

import java.io.Console;

public final class PasswordHashTool {
    private PasswordHashTool() { }

    public static void main(String[] args) {
        Console console = System.console();
        if (console == null) throw new IllegalStateException("Ejecute esta herramienta desde una terminal interactiva.");
        char[] first = console.readPassword("Contraseña nueva: ");
        char[] second = console.readPassword("Confirmar contraseña: ");
        try {
            String password = new String(first);
            if (password.length() < 12) throw new IllegalArgumentException("La contraseña debe tener al menos 12 caracteres.");
            if (!password.equals(new String(second))) throw new IllegalArgumentException("Las contraseñas no coinciden.");
            console.writer().println(new PasswordService().hash(password));
        } finally {
            java.util.Arrays.fill(first, '\0');
            java.util.Arrays.fill(second, '\0');
        }
    }
}
