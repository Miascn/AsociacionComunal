package controller;

import java.io.IOException;
import java.util.Objects;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;
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
    private Label lblEstadoActualizacion;

    @FXML
    private StackPane contentArea;

    @FXML
    private Button btnTema;

    @FXML
    private Button btnBuscarActualizaciones;

    private Runnable onLogout;

    @FXML
    private void initialize() {
        AuthUser user = SessionManager.getInstance().requireCurrentUser();
        lblNombreUsuario.setText(user.getDisplayName());
        lblRolUsuario.setText(user.getRole());
        String version = System.getProperty("jpackage.app-version", "DEV");
        lblVersion.setText("Versión " + version);
        lblEstadoActualizacion.setText("DEV".equals(version)
            ? "Actualizaciones disponibles en paquete QA"
            : "Listo para comprobar actualizaciones");
        actualizarBotonTema();
        mostrarDashboard();
    }

    @FXML
    private void onBuscarActualizaciones() {
        btnBuscarActualizaciones.setDisable(true);
        lblEstadoActualizacion.setText("Buscando actualizaciones...");
        UpdateService.checkManually(btnBuscarActualizaciones.getScene().getWindow(), status -> {
            lblEstadoActualizacion.setText(status);
            btnBuscarActualizaciones.setDisable(false);
        });
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
    private void mostrarViviendas() {
        mostrarPlaceholder("Viviendas");
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
