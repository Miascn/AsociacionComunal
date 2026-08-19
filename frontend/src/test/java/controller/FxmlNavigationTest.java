package controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import models.AuthUser;
import security.SessionManager;

class FxmlNavigationTest {

    @BeforeAll
    static void inicializarJavaFx() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        new Thread(() -> {
            javafx.application.Application.launch(TestApp.class);
            latch.countDown();
        }).start();
        latch.await(5, TimeUnit.SECONDS);
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
            assertEquals(9, botones.size());

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
                } else if ("Proyectos".equals(boton.getText())) {
                    boton.fire();
                    assertTrue(buscarEtiquetas(root).stream()
                            .map(Label::getText)
                            .anyMatch("Proyectos comunales"::equals));
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

    private static List<Node> recorrer(Parent parent) {
        List<Node> nodes = new java.util.ArrayList<>();
        for (Node node : parent.getChildrenUnmodifiable()) {
            nodes.add(node);
            if (node instanceof Parent child) {
                nodes.addAll(recorrer(child));
            }
        }
        return nodes;
    }

    private static void ejecutarEnJavaFx(ExceptionThrowingRunnable runnable) throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        var ref = new Object() { Exception exception; };
        javafx.application.Platform.runLater(() -> {
            try {
                runnable.run();
            } catch (Exception e) {
                ref.exception = e;
            } finally {
                latch.countDown();
            }
        });
        latch.await(10, TimeUnit.SECONDS);
        if (ref.exception != null) throw ref.exception;
    }

    @FunctionalInterface
    private interface ExceptionThrowingRunnable {
        void run() throws Exception;
    }
}
class TestApp extends javafx.application.Application {
    @Override
    public void start(javafx.stage.Stage stage) {}
}
