package controller;

import java.time.LocalDate;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import models.AsignacionCargoModel;
import service.DirectivaApiClient;

public class RevocarCargoModalController {
    @FXML private Label lblNombreMiembro;
    @FXML private Label lblNombreCargo;
    @FXML private Label lblNombrePeriodo;
    @FXML private DatePicker pickerFechaFin;
    @FXML private TextField campoMotivo;
    @FXML private Label lblError;
    @FXML private Button btnRevocar;

    private AsignacionCargoModel asignacion;
    private Runnable onSuccessCallback;

    @FXML
    private void initialize() {
        pickerFechaFin.setValue(LocalDate.now());
    }

    public void initData(AsignacionCargoModel asignacion, Runnable onSuccess) {
        this.asignacion = asignacion;
        this.onSuccessCallback = onSuccess;

        if (asignacion != null) {
            lblNombreMiembro.setText(asignacion.getNombreMiembro());
            lblNombreCargo.setText(asignacion.getNombreCargo());
            lblNombrePeriodo.setText(asignacion.getNombrePeriodo());
        }
    }

    @FXML
    private void revocar() {
        lblError.setText("");

        if (asignacion == null) return;

        LocalDate fechaFin = pickerFechaFin.getValue();
        if (fechaFin == null) {
            lblError.setText("La fecha de cese es obligatoria.");
            return;
        }

        String motivo = campoMotivo.getText() != null ? campoMotivo.getText().trim() : "";
        if (motivo.isBlank()) {
            lblError.setText("Debe especificar el motivo de salida o renuncia.");
            return;
        }

        btnRevocar.setDisable(true);
        lblError.setStyle("-fx-text-fill: -color-accent-fg;");
        lblError.setText("Procesando revocación...");

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                new DirectivaApiClient().revocar(asignacion.getId(), fechaFin.toString(), motivo);
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
            btnRevocar.setDisable(false);
            lblError.setStyle("-fx-text-fill: -color-danger-fg;");
            Throwable error = task.getException();
            lblError.setText(error != null && error.getMessage() != null ? error.getMessage() : "Error al revocar el cargo.");
        });

        Thread thread = new Thread(task, "revocar-cargo-task");
        thread.setDaemon(true);
        thread.start();
    }

    @FXML
    private void cancelar() {
        cerrar();
    }

    private void cerrar() {
        Stage stage = (Stage) btnRevocar.getScene().getWindow();
        stage.close();
    }
}
