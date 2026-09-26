package controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import javafx.application.Platform;
import javafx.scene.control.ComboBox;
import models.MiembroModel;
import models.RolModel;
import models.UsuarioModel;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class UsuarioSegmentacionTest {

    @BeforeAll
    static void initJavaFx() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        try {
            Platform.startup(latch::countDown);
            assertTrue(latch.await(5, TimeUnit.SECONDS));
        } catch (IllegalStateException ignored) {
            // JavaFX ya iniciado
        }
    }

    @Test
    void testSeparacionUsuariosSistemaYMovil() {
        UsuarioController controller = new UsuarioController();

        UsuarioModel userJava = new UsuarioModel(1, 1, null, "admin", "ACTIVO", null, "SISTEMA_JAVA");
        UsuarioModel userMovil = new UsuarioModel(2, 2, 10, "01234567-8", "ACTIVO", null, "APP_MOVIL");

        // Verificamos detección de tipo
        assertFalse(controller.esMovil(userJava), "userJava no debe ser detectado como móvil");
        assertTrue(controller.esMovil(userMovil), "userMovil debe ser detectado como móvil");

        // 1. En segmento SISTEMA_JAVA (por defecto): sólo usuarios de escritorio Java
        controller.setSegmentoActivo(UsuarioController.SegmentoUsuario.SISTEMA_JAVA);
        assertTrue(controller.cumpleFiltros(userJava, ""));
        assertFalse(controller.cumpleFiltros(userMovil, ""));

        // 2. En segmento APP_MOVIL: sólo cuentas de la app móvil
        controller.setSegmentoActivo(UsuarioController.SegmentoUsuario.APP_MOVIL);
        assertFalse(controller.cumpleFiltros(userJava, ""));
        assertTrue(controller.cumpleFiltros(userMovil, ""));

        // 3. En segmento TODOS: ambas cuentas están presentes
        controller.setSegmentoActivo(UsuarioController.SegmentoUsuario.TODOS);
        assertTrue(controller.cumpleFiltros(userJava, ""));
        assertTrue(controller.cumpleFiltros(userMovil, ""));
    }

    @Test
    void testFiltroBusquedaTexto() {
        UsuarioController controller = new UsuarioController();
        controller.setSegmentoActivo(UsuarioController.SegmentoUsuario.TODOS);

        UsuarioModel userJava = new UsuarioModel(1, 1, null, "admin.principal", "ACTIVO", null, "SISTEMA_JAVA");
        UsuarioModel userMovil = new UsuarioModel(2, 2, 10, "residente.sur", "ACTIVO", null, "APP_MOVIL");

        // Búsqueda por texto parcial
        assertTrue(controller.cumpleFiltros(userJava, "admin"));
        assertFalse(controller.cumpleFiltros(userJava, "residente"));

        assertTrue(controller.cumpleFiltros(userMovil, "residente"));
        assertFalse(controller.cumpleFiltros(userMovil, "admin"));

        // Búsqueda por etiqueta de plataforma
        assertTrue(controller.cumpleFiltros(userMovil, "movil"));
        assertTrue(controller.cumpleFiltros(userJava, "java"));
    }

    @Test
    void testFormularioExcluyeRolMiembroParaUsuariosDeEscritorio() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                UsuarioFormController formController = new UsuarioFormController();

                // Simulamos inyección de campos FXML
                java.lang.reflect.Field fSelectorRol = UsuarioFormController.class.getDeclaredField("selectorRol");
                fSelectorRol.setAccessible(true);
                ComboBox<RolModel> comboRol = new ComboBox<>();
                fSelectorRol.set(formController, comboRol);

                java.lang.reflect.Field fSelectorMiembro = UsuarioFormController.class.getDeclaredField("selectorMiembro");
                fSelectorMiembro.setAccessible(true);
                ComboBox<MiembroModel> comboMiembro = new ComboBox<>();
                fSelectorMiembro.set(formController, comboMiembro);

                RolModel rolAdmin = new RolModel(1, "ADMINISTRADOR", "Admin");
                RolModel rolDirectivo = new RolModel(2, "DIRECTIVO", "Directivo");
                RolModel rolMiembro = new RolModel(3, "MIEMBRO", "Miembro Móvil");

                formController.setCatalogs(List.of(rolAdmin, rolDirectivo, rolMiembro), List.of());

                assertEquals(2, comboRol.getItems().size(), "Debe excluir el rol MIEMBRO del selector para escritorio");
                assertTrue(comboRol.getItems().contains(rolAdmin));
                assertTrue(comboRol.getItems().contains(rolDirectivo));
                assertFalse(comboRol.getItems().contains(rolMiembro));
            } catch (Exception e) {
                throw new RuntimeException(e);
            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }
}
