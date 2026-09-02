package controller;

import java.util.Objects;
import java.util.prefs.Preferences;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.layout.StackPane;
import javafx.geometry.Insets;
import org.kordamp.ikonli.javafx.FontIcon;
import models.AuthResult;
import services.auth.AuthService;
import security.SessionManager;
import service.ThemeService;

public class LoginController {
    private static final String PREF_REMEMBER_USER = "remember_user";
    private static final String PREF_SAVED_USERNAME = "saved_username";

    @FXML
    private TextField campoUsuario;

    @FXML
    private PasswordField campoContrasena;

    @FXML
    private TextField campoContrasenaVisible;

    @FXML
    private Button btnVerContrasena;

    @FXML
    private FontIcon iconoVerContrasena;

    @FXML
    private Label mensajeError;

    @FXML
    private CheckBox chkRecordarme;

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
        cargarUsuarioRecordado();
        if (campoContrasenaVisible != null && campoContrasena != null) {
            campoContrasenaVisible.textProperty().bindBidirectional(campoContrasena.textProperty());
        }
        loginShell.sceneProperty().addListener((observable, previous, scene) -> {
            if (scene == null) return;
            scene.widthProperty().addListener((obs, oldWidth, width) -> adaptar(width.doubleValue(), scene.getHeight()));
            scene.heightProperty().addListener((obs, oldHeight, height) -> adaptar(scene.getWidth(), height.doubleValue()));
            adaptar(scene.getWidth(), scene.getHeight());
        });
    }

    private void cargarUsuarioRecordado() {
        if (chkRecordarme == null || campoUsuario == null) return;
        try {
            Preferences prefs = Preferences.userNodeForPackage(LoginController.class);
            boolean remember = prefs.getBoolean(PREF_REMEMBER_USER, false);
            String savedUsername = prefs.get(PREF_SAVED_USERNAME, "");
            if (remember && !savedUsername.isBlank()) {
                campoUsuario.setText(savedUsername);
                chkRecordarme.setSelected(true);
                Platform.runLater(() -> {
                    if (campoContrasena != null) {
                        campoContrasena.requestFocus();
                    }
                });
            }
        } catch (Exception ignored) {
        }
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

    @FXML
    private void onAlternarVerContrasena() {
        if (campoContrasenaVisible == null || campoContrasena == null) return;
        boolean mostrar = !campoContrasenaVisible.isVisible();
        campoContrasenaVisible.setVisible(mostrar);
        campoContrasenaVisible.setManaged(mostrar);
        campoContrasena.setVisible(!mostrar);
        campoContrasena.setManaged(!mostrar);
        if (iconoVerContrasena != null) {
            iconoVerContrasena.setIconLiteral(mostrar ? "fth-eye-off" : "fth-eye");
        }
        if (mostrar) {
            campoContrasenaVisible.requestFocus();
            campoContrasenaVisible.positionCaret(campoContrasenaVisible.getText() == null ? 0 : campoContrasenaVisible.getText().length());
        } else {
            campoContrasena.requestFocus();
            campoContrasena.positionCaret(campoContrasena.getText() == null ? 0 : campoContrasena.getText().length());
        }
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
        guardarPreferenciaUsuario(username);
        campoContrasena.clear();
        ocultarError();
        if (onAuthenticated != null) {
            onAuthenticated.run();
        }
    }

    private void guardarPreferenciaUsuario(String username) {
        try {
            Preferences prefs = Preferences.userNodeForPackage(LoginController.class);
            if (chkRecordarme != null && chkRecordarme.isSelected()) {
                prefs.putBoolean(PREF_REMEMBER_USER, true);
                prefs.put(PREF_SAVED_USERNAME, username.trim());
            } else {
                prefs.putBoolean(PREF_REMEMBER_USER, false);
                prefs.remove(PREF_SAVED_USERNAME);
            }
        } catch (Exception ignored) {
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