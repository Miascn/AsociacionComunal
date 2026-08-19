package controller;

import java.util.Objects;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.Button;
import models.AuthResult;
import services.auth.AuthService;
import security.SessionManager;
import service.ThemeService;

public class LoginController {
    @FXML
    private TextField campoUsuario;

    @FXML
    private PasswordField campoContrasena;

    @FXML
    private Label mensajeError;

    @FXML
    private Button btnTema;

    private final AuthService authService = new AuthService();
    private Runnable onAuthenticated;

    @FXML
    private void initialize() {
        ocultarError();
        actualizarBotonTema();
    }

    @FXML
    private void onAlternarTema() {
        ThemeService.toggle(btnTema.getScene());
        actualizarBotonTema();
    }

    private void actualizarBotonTema() {
        btnTema.setText(ThemeService.isDark() ? "Modo claro" : "Modo oscuro");
    }

    public void setOnAuthenticated(Runnable onAuthenticated) {
        this.onAuthenticated = Objects.requireNonNull(onAuthenticated);
    }

    @FXML
    private void onIniciarSesion() {
        String username = campoUsuario.getText();
        String password = campoContrasena.getText();

        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            mostrarError("Ingresa el usuario y la contraseña.");
            return;
        }

        AuthResult result = authService.authenticate(username, password);
        if (!result.success()) {
            campoContrasena.clear();
            mostrarError(result.message());
            return;
        }

        SessionManager.getInstance().start(result.user(), result.token());
        campoContrasena.clear();
        ocultarError();
        if (onAuthenticated != null) {
            onAuthenticated.run();
        }
    }

    private void mostrarError(String mensaje) {
        mensajeError.setText(mensaje);
        mensajeError.setManaged(true);
        mensajeError.setVisible(true);
    }

    private void ocultarError() {
        mensajeError.setText("");
        mensajeError.setManaged(false);
        mensajeError.setVisible(false);
    }
}