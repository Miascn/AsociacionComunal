package app;

import java.io.IOException;
import java.util.List;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import controller.LoginController;
import controller.MainController;
import security.SessionManager;
import service.UpdateService;
import service.DesktopShortcutService;
import service.ThemeService;
import service.ResponsiveWindowService;

public class Main extends Application {
    private Stage stage;

    @Override
    public void start(Stage stage) throws IOException {
        this.stage = stage;
        stage.setTitle("Asociación Comunal ERP");
        stage.getIcons().addAll(List.of(
                cargarIcono("/images/app-icon-32.png"),
                cargarIcono("/images/app-icon-48.png"),
                cargarIcono("/images/app-icon-256.png"),
                cargarIcono("/images/logo-asociacion-comunal.png")));
        showLogin();
        stage.show();
        DesktopShortcutService.ensureAsync();
        UpdateService.checkAsync(stage);
    }

    private void showLogin() throws IOException {
        SessionManager.getInstance().clear();

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/login.fxml"));
        Parent root = loader.load();
        LoginController controller = loader.getController();
        controller.setOnAuthenticated(this::showDashboard);

        setScene(root, 1280, 760, false);
    }

    private void showDashboard() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/main.fxml"));
            Parent root = loader.load();
            MainController controller = loader.getController();
            controller.setOnLogout(this::returnToLogin);
            setScene(root, 1400, 820, true);
        } catch (IOException exception) {
            throw new IllegalStateException("No fue posible abrir el panel principal.", exception);
        }
    }

    private void returnToLogin() {
        try {
            showLogin();
        } catch (IOException exception) {
            throw new IllegalStateException("No fue posible volver al inicio de sesión.", exception);
        }
    }

    private void setScene(Parent root, double width, double height, boolean dashboard) {
        Scene scene = new Scene(root);
        scene.getStylesheets().add(getClass().getResource("/css/app.css").toExternalForm());
        ThemeService.apply(scene);
        stage.setScene(scene);
        ResponsiveWindowService.fit(stage, width, height, dashboard);
    }

    private Image cargarIcono(String ruta) {
        return new Image(getClass().getResourceAsStream(ruta));
    }

    public static void main(String[] args) {
        launch(args);
    }
}
