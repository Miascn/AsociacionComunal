package controller;

import java.io.IOException;
import java.util.Objects;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import models.AuthUser;
import security.SessionManager;

public class MainController {
    @FXML
    private Label lblNombreUsuario;

    @FXML
    private Label lblRolUsuario;

    @FXML
    private Label lblVersion;

    @FXML
    private StackPane contentArea;

    private Runnable onLogout;

    @FXML
    private void initialize() {
        AuthUser user = SessionManager.getInstance().requireCurrentUser();
        lblNombreUsuario.setText(user.getDisplayName());
        lblRolUsuario.setText(user.getRole());
        lblVersion.setText("Versión " + System.getProperty("jpackage.app-version", "DEV"));
        mostrarDashboard();
    }

    public void setOnLogout(Runnable onLogout) {
        this.onLogout = Objects.requireNonNull(onLogout);
    }

    @FXML
    private void mostrarDashboard() {
        cargarVista("/fxml/views/dashboard.fxml");
    }

    @FXML
    private void mostrarPersonas() {
        mostrarPlaceholder("Personas");
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
        mostrarPlaceholder("Proyectos");
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
