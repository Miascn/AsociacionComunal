package service;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import io.github.palexdev.materialfx.dialogs.MFXGenericDialog;
import io.github.palexdev.materialfx.dialogs.MFXStageDialog;
import javafx.application.Platform;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MaterialAlertTest {

    @BeforeAll
    static void iniciarJavaFx() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        try {
            Platform.startup(latch::countDown);
            assertTrue(latch.await(5, TimeUnit.SECONDS));
        } catch (IllegalStateException e) {
            // Ya iniciado
        }
    }

    @Test
    void testClassesExist() throws Exception {
        assertNotNull(MFXGenericDialog.class);
        assertNotNull(MFXStageDialog.class);
        assertNotNull(MaterialAlertService.class);

        CountDownLatch latch = new CountDownLatch(1);
        AtomicBoolean success = new AtomicBoolean(false);

        Platform.runLater(() -> {
            try {
                MFXGenericDialog dialog = new MFXGenericDialog();
                dialog.setHeaderText("Test Header");
                dialog.setContent(new javafx.scene.control.Label("Custom Content"));
                dialog.setHeaderIcon(new javafx.scene.shape.Circle(8));
                dialog.addActions(new javafx.scene.control.Button("Aceptar"));

                MFXStageDialog stageDialog = new MFXStageDialog(dialog);
                stageDialog.setTitle("Test Dialog");
                stageDialog.setScrimOwner(true);

                success.set(true);
            } finally {
                latch.countDown();
            }
        });

        assertTrue(latch.await(5, TimeUnit.SECONDS));
        assertTrue(success.get());
    }
}
