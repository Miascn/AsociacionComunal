package controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.Pane;
import models.AuthUser;
import security.SessionManager;

class FxmlNavigationTest {
    @BeforeAll
    static void iniciarJavaFx() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        Platform.startup(latch::countDown);
        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }

    @AfterAll
    static void finalizarJavaFx() {
        SessionManager.getInstance().clear();
        Platform.exit();
    }

    @Test
    void cargaLoginYControlador() throws Exception {
        ejecutarEnJavaFx(() -> {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/login.fxml"));
            Parent root = loader.load();

            assertNotNull(root);
            assertNotNull(loader.<LoginController>getController());
            return null;
        });
    }

    @Test
    void cargaFormularioRegistrarMiembro() throws Exception {
        ejecutarEnJavaFx(() -> {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/views/registrar-miembro.fxml"));
            Parent root = loader.load();
            assertNotNull(loader.<RegistrarMiembroController>getController());
            long campos = recorrer(root).stream().filter(TextField.class::isInstance).count();
            assertEquals(5, campos);
            assertTrue(recorrer(root).stream()
                    .filter(ComboBox.class::isInstance)
                    .map(ComboBox.class::cast)
                    .anyMatch(combo -> "selectorVivienda".equals(combo.getId())));
            return null;
        });
    }

    @Test
    void cargaDashboardNavegaYCierraSesion() throws Exception {
        ejecutarEnJavaFx(() -> {
            SessionManager session = SessionManager.getInstance();
            session.start(new AuthUser("admin", "Josué Romero", "Administrador"), "test-token");

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/main.fxml"));
            Parent root = loader.load();
            MainController controller = loader.getController();
            boolean[] logoutInvocado = {false};
            controller.setOnLogout(() -> logoutInvocado[0] = true);

            List<Button> botones = buscarBotones(root);
            assertEquals(12, botones.size());

            for (Button boton : botones) {
                if ("Miembros".equals(boton.getText())) {
                    boton.fire();
                    assertTrue(buscarEtiquetas(root).stream()
                            .map(Label::getText)
                            .anyMatch("Gestión de miembros"::equals));
                    TextField busqueda = recorrer(root).stream()
                            .filter(TextField.class::isInstance)
                            .map(TextField.class::cast)
                            .findFirst()
                            .orElseThrow();
                    TableView<?> tabla = recorrer(root).stream()
                            .filter(TableView.class::isInstance)
                            .map(TableView.class::cast)
                            .findFirst()
                            .orElseThrow();
                    assertEquals(0, tabla.getItems().size());
                    busqueda.setText("María");
                    assertEquals(0, tabla.getItems().size());
                    busqueda.clear();
                } else if ("Usuarios".equals(boton.getText())) {
                    boton.fire();
                    assertTrue(buscarEtiquetas(root).stream().map(Label::getText).anyMatch("Gestión de usuarios"::equals));
                    assertTrue(recorrer(root).stream().anyMatch(TableView.class::isInstance));
                } else if ("Roles".equals(boton.getText())) {
                    boton.fire();
                    assertTrue(buscarEtiquetas(root).stream().map(Label::getText).anyMatch("Catálogo de roles"::equals));
                    assertTrue(recorrer(root).stream().anyMatch(TableView.class::isInstance));
                } else if ("Bitácora".equals(boton.getText())) {
                    boton.fire();
                    assertTrue(buscarEtiquetas(root).stream().map(Label::getText).anyMatch("Bitácora del sistema"::equals));
                    assertTrue(recorrer(root).stream().anyMatch(TableView.class::isInstance));
                } else if ("Proyectos".equals(boton.getText())) {
                    boton.fire();
                    assertTrue(buscarEtiquetas(root).stream()
                            .map(Label::getText)
                            .anyMatch("Proyectos comunales"::equals));
                    assertTrue(recorrer(root).stream().anyMatch(TableView.class::isInstance));
                } else if ("Viviendas".equals(boton.getText())) {
                    boton.fire();
                    assertTrue(buscarEtiquetas(root).stream()
                            .map(Label::getText)
                            .anyMatch("Gestión de viviendas"::equals));
                    assertTrue(recorrer(root).stream().anyMatch(TableView.class::isInstance));
                } else if (!"Dashboard".equals(boton.getText()) && !"Cerrar sesión".equals(boton.getText())) {
                    boton.fire();
                    assertTrue(buscarEtiquetas(root).stream()
                            .map(Label::getText)
                            .anyMatch(texto -> texto != null && texto.contains("módulo en construcción")));
                }
            }

            botones.stream()
                    .filter(boton -> "Dashboard".equals(boton.getText()))
                    .findFirst()
                    .orElseThrow()
                    .fire();
            assertTrue(buscarEtiquetas(root).stream().anyMatch(label -> "Dashboard".equals(label.getText())));

            botones.stream()
                    .filter(boton -> "Cerrar sesión".equals(boton.getText()))
                    .findFirst()
                    .orElseThrow()
                    .fire();
            assertTrue(logoutInvocado[0]);
            assertFalse(session.isAuthenticated());
            return null;
        });
    }

    private static List<Button> buscarBotones(Parent root) {
        return recorrer(root).stream()
                .filter(Button.class::isInstance)
                .map(Button.class::cast)
                .filter(button -> button.getStyleClass().contains("nav-button")
                        || button.getStyleClass().contains("logout-button"))
                .toList();
    }

    private static List<Label> buscarEtiquetas(Parent root) {
        return recorrer(root).stream()
                .filter(Label.class::isInstance)
                .map(Label.class::cast)
                .toList();
    }

    private static List<javafx.scene.Node> recorrer(javafx.scene.Node node) {
        List<javafx.scene.Node> nodes = new java.util.ArrayList<>();
        nodes.add(node);
        if (node instanceof Pane pane) {
            pane.getChildrenUnmodifiable().forEach(child -> nodes.addAll(recorrer(child)));
        } else if (node instanceof javafx.scene.control.ScrollPane scrollPane && scrollPane.getContent() != null) {
            nodes.addAll(recorrer(scrollPane.getContent()));
        }
        return nodes;
    }

    private static <T> T ejecutarEnJavaFx(java.util.concurrent.Callable<T> action) throws Exception {
        FutureTask<T> task = new FutureTask<>(action);
        Platform.runLater(task);
        return task.get(10, TimeUnit.SECONDS);
    }
}
