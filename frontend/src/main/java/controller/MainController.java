package controller;

import java.io.IOException;
import java.util.Objects;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import models.AuthUser;
import security.SessionManager;
import service.ThemeService;
import service.UpdateService;

public class MainController {
    @FXML
    private Label lblNombreUsuario;

    @FXML
    private Label lblRolUsuario;

    @FXML
    private Label lblVersion;

    @FXML
    private StackPane contentArea;

    @FXML
    private Button btnTema;
    @FXML private VBox sidebar;
    @FXML private Button btnUsuarios;

    private Runnable onLogout;

    @FXML
    private void initialize() {
        AuthUser user = SessionManager.getInstance().requireCurrentUser();
        lblNombreUsuario.setText(user.getDisplayName());
        lblRolUsuario.setText(user.getRole());
        lblVersion.setText("Versión " + UpdateService.currentVersion());
        boolean administrator = isAdministrator(user.getRole());
        btnUsuarios.setVisible(administrator);
        btnUsuarios.setManaged(administrator);
        actualizarBotonTema();
        mostrarDashboard();
        sidebar.sceneProperty().addListener((observable, previous, scene) -> {
            if (scene == null) return;
            scene.widthProperty().addListener((obs, oldWidth, width) -> adaptarBarra(width.doubleValue()));
            adaptarBarra(scene.getWidth());
        });
    }

    private void adaptarBarra(double width) {
        if (width > 0 && width < 1000) sidebar.setPrefWidth(185);
        else if (width > 0 && width < 1200) sidebar.setPrefWidth(210);
        else sidebar.setPrefWidth(240);
    }

    @FXML
    private void onAlternarTema() {
        ThemeService.toggle(btnTema.getScene());
        actualizarBotonTema();
    }

    private void actualizarBotonTema() {
        btnTema.setText(ThemeService.isDark() ? "Modo claro" : "Modo oscuro");
    }

    public void setOnLogout(Runnable onLogout) {
        this.onLogout = Objects.requireNonNull(onLogout);
    }

    @FXML
    private void mostrarDashboard() {
        cargarVista("/fxml/views/dashboard.fxml");
    }

    @FXML
    private void mostrarMiembros() {
        cargarVista("/fxml/views/miembros.fxml");
    }

    @FXML
    private void mostrarUsuarios() {
        cargarVista("/fxml/views/usuarios.fxml");
    }

    static boolean isAdministrator(String role) {
        return role != null && ("ADMIN".equalsIgnoreCase(role) || "ADMINISTRADOR".equalsIgnoreCase(role));
    }

    @FXML
    private void mostrarViviendas() {
        cargarVista("/fxml/views/viviendas.fxml");
    }

    @FXML
    private void mostrarAportaciones() {
        mostrarPlaceholder("Aportaciones");
    }

    @FXML
    private void mostrarProyectos() {
        cargarVista("/fxml/views/proyectos.fxml");
    }

    @FXML
    private void mostrarReuniones() {
        mostrarPlaceholder("Reuniones");
    }

    @FXML
    private void mostrarVotaciones() {
        mostrarPlaceholder("Votaciones");
    }

    @FXML
    private void mostrarReportes() {
        mostrarPlaceholder("Reportes");
    }

    @FXML
    private void onCerrarSesion() {
        SessionManager.getInstance().clear();
        if (onLogout != null) {
            onLogout.run();
        }
    }

    private void mostrarPlaceholder(String titulo) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/views/placeholder.fxml"));
            Parent view = loader.load();
            PlaceholderController controller = loader.getController();
            controller.setTitulo(titulo + " - módulo en construcción");
            contentArea.getChildren().setAll(view);
        } catch (IOException exception) {
            throw new IllegalStateException("No fue posible abrir el módulo " + titulo + ".", exception);
        }
    }

    private void cargarVista(String resourcePath) {
        try {
            Parent view = FXMLLoader.load(Objects.requireNonNull(getClass().getResource(resourcePath)));
            contentArea.getChildren().setAll(view);
        } catch (IOException exception) {
            throw new IllegalStateException("No fue posible cargar la vista " + resourcePath + ".", exception);
        }
    }
}
