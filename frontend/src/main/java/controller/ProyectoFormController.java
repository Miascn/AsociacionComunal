package controller;

import java.math.BigDecimal;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import models.ProyectoModel;
import service.ProyectoApiClient;

public class ProyectoFormController {
    @FXML private Label lblTitulo;
    @FXML private Label lblSubtitulo;
    @FXML private TextField campoNombre;
    @FXML private TextField campoPresupuesto;
    @FXML private TextArea campoDescripcion;
    @FXML private Label lblError;
    @FXML private Button btnGuardar;

    private ProyectoModel proyectoActual;
    private Runnable onSuccessCallback;

    public void initData(ProyectoModel proyecto, Runnable onSuccess) {
        this.proyectoActual = proyecto;
        this.onSuccessCallback = onSuccess;

        if (proyecto != null) {
            lblTitulo.setText("Editar proyecto #" + proyecto.getId());
            lblSubtitulo.setText("Modifica los datos y presupuesto de la iniciativa comunal");
            btnGuardar.setText("Guardar cambios");

            campoNombre.setText(proyecto.getNombre());
            campoPresupuesto.setText(proyecto.getPresupuesto() != null ? proyecto.getPresupuesto().toString() : "0.00");
            campoDescripcion.setText(proyecto.getDescripcion());
        } else {
            campoPresupuesto.setText("0.00");
        }
    }

    @FXML
    private void guardar() {
        lblError.setText("");

        String nombre = campoNombre.getText() != null ? campoNombre.getText().trim() : "";
        if (nombre.isBlank()) {
            lblError.setText("El nombre del proyecto es obligatorio.");
            return;
        }

        String presupuestoStr = campoPresupuesto.getText() != null ? campoPresupuesto.getText().trim() : "";
        BigDecimal presupuesto;
        try {
            presupuesto = new BigDecimal(presupuestoStr);
            if (presupuesto.compareTo(BigDecimal.ZERO) < 0) {
                lblError.setText("El presupuesto no puede ser negativo.");
                return;
            }
        } catch (Exception e) {
            lblError.setText("Ingresa un monto de presupuesto válido (ej. 1500.00).");
            return;
        }

        String descripcion = campoDescripcion.getText() != null ? campoDescripcion.getText().trim() : "";
        if (descripcion.isBlank()) {
            lblError.setText("La descripción del proyecto es obligatoria.");
            return;
        }

        ProyectoModel model = new ProyectoModel();
        model.setNombre(nombre);
        model.setPresupuesto(presupuesto);
        model.setDescripcion(descripcion);

        btnGuardar.setDisable(true);
        lblError.setStyle("-fx-text-fill: -color-accent-fg;");
        lblError.setText("Guardando proyecto...");

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                ProyectoApiClient client = new ProyectoApiClient();
                if (proyectoActual == null) {
                    model.setEstado("BORRADOR");
                    client.create(model);
                } else {
                    client.update(proyectoActual.getId(), model);
                }
                return null;
            }
        };

        task.setOnSucceeded(e -> {
            if (onSuccessCallback != null) {
                onSuccessCallback.run();
            }
            cerrar();
        });

        task.setOnFailed(e -> {
            btnGuardar.setDisable(false);
            lblError.setStyle("-fx-text-fill: -color-danger-fg;");
            Throwable error = task.getException();
            lblError.setText(error != null && error.getMessage() != null ? error.getMessage() : "Error al guardar el proyecto.");
        });

        Thread thread = new Thread(task, "guardar-proyecto");
        thread.setDaemon(true);
        thread.start();
    }

    @FXML
    private void cancelar() {
        cerrar();
    }

    private void cerrar() {
        Stage stage = (Stage) btnGuardar.getScene().getWindow();
        stage.close();
    }
}
