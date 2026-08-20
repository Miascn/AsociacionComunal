package controller;

import java.util.Objects;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.layout.StackPane;
import javafx.geometry.Insets;
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
    @FXML private HBox loginShell;
    @FXML private VBox loginBrandPanel;

    private final AuthService authService = new AuthService();
    private Runnable onAuthenticated;

    @FXML
    private void initialize() {
        ocultarError();
        actualizarBotonTema();
        loginShell.sceneProperty().addListener((observable, previous, scene) -> {
            if (scene == null) return;
            scene.widthProperty().addListener((obs, oldWidth, width) -> adaptar(width.doubleValue(), scene.getHeight()));
            scene.heightProperty().addListener((obs, oldHeight, height) -> adaptar(scene.getWidth(), height.doubleValue()));
            adaptar(scene.getWidth(), scene.getHeight());
        });
    }

    private void adaptar(double width, double height) {
        boolean compacto = width > 0 && width < 980;
        loginBrandPanel.setVisible(!compacto);
        loginBrandPanel.setManaged(!compacto);
        var root = loginShell.getScene().getRoot();
        root.getStyleClass().removeAll("login-compact", "login-low-height");
        if (compacto) root.getStyleClass().add("login-compact");
        boolean pocaAltura = height > 0 && height < 680;
        if (pocaAltura) root.getStyleClass().add("login-low-height");
        StackPane.setMargin(loginShell, pocaAltura
            ? new Insets(58, 24, 20, 24)
            : new Insets(72, 40, 40, 40));
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

        SessionManager.getInstance().start(result.user());
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
