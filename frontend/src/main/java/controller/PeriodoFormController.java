package controller;

import java.time.LocalDate;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import models.PeriodoDirectivaModel;
import service.PeriodoApiClient;

public class PeriodoFormController {
    @FXML private Label lblTitulo;
    @FXML private Label lblSubtitulo;
    @FXML private TextField campoNombre;
    @FXML private DatePicker pickerInicio;
    @FXML private DatePicker pickerFin;
    @FXML private ComboBox<String> comboEstado;
    @FXML private Label lblError;
    @FXML private Button btnGuardar;

    private PeriodoDirectivaModel periodoActual;
    private Runnable onSuccessCallback;

    @FXML
    private void initialize() {
        comboEstado.getItems().setAll("PLANIFICADO", "ACTIVO");
        comboEstado.setValue("PLANIFICADO");
    }

    public void initData(PeriodoDirectivaModel periodo, Runnable onSuccess) {
        this.periodoActual = periodo;
        this.onSuccessCallback = onSuccess;

        if (periodo != null) {
            lblTitulo.setText("Editar período #" + periodo.getId());
            lblSubtitulo.setText("Modifica el nombre o las fechas de vigencia de la directiva");
            btnGuardar.setText("Guardar cambios");

            campoNombre.setText(periodo.getNombre());
            try {
                if (periodo.getFechaInicio() != null) {
                    pickerInicio.setValue(LocalDate.parse(periodo.getFechaInicio()));
                }
                if (periodo.getFechaFin() != null) {
                    pickerFin.setValue(LocalDate.parse(periodo.getFechaFin()));
                }
            } catch (Exception ignored) {}

            if (periodo.getEstado() != null) {
                if (!comboEstado.getItems().contains(periodo.getEstado().toUpperCase())) {
                    comboEstado.getItems().add(periodo.getEstado().toUpperCase());
                }
                comboEstado.setValue(periodo.getEstado().toUpperCase());
            }
        }
    }

    @FXML
    private void guardar() {
        lblError.setText("");

        String nombre = campoNombre.getText() != null ? campoNombre.getText().trim() : "";
        if (nombre.isBlank()) {
            lblError.setText("El nombre del período es obligatorio.");
            return;
        }

        LocalDate inicio = pickerInicio.getValue();
        if (inicio == null) {
            lblError.setText("La fecha de inicio del período es obligatoria.");
            return;
        }

        LocalDate fin = pickerFin.getValue();
        if (fin == null) {
            lblError.setText("La fecha de finalización del período es obligatoria.");
            return;
        }

        if (inicio.isAfter(fin)) {
            lblError.setText("La fecha de inicio debe ser anterior o igual a la fecha de finalización.");
            return;
        }

        String estado = comboEstado.getValue() != null ? comboEstado.getValue() : "PLANIFICADO";

        PeriodoDirectivaModel model = new PeriodoDirectivaModel();
        model.setNombre(nombre);
        model.setFechaInicio(inicio.toString());
        model.setFechaFin(fin.toString());
        model.setEstado(estado);

        btnGuardar.setDisable(true);
        lblError.setStyle("-fx-text-fill: -color-accent-fg;");
        lblError.setText("Guardando período...");

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                PeriodoApiClient client = new PeriodoApiClient();
                if (periodoActual == null) {
                    client.create(model);
                } else {
                    client.update(periodoActual.getId(), model);
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
            lblError.setText(error != null && error.getMessage() != null ? error.getMessage() : "Error al guardar el período.");
        });

        Thread thread = new Thread(task, "guardar-periodo");
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
