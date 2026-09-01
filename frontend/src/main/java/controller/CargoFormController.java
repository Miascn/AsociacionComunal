package controller;

import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import models.CargoModel;
import service.CargoApiClient;

public class CargoFormController {
    @FXML private Label lblTitulo;
    @FXML private Label lblSubtitulo;
    @FXML private TextField campoNombre;
    @FXML private Spinner<Integer> spinnerJerarquia;
    @FXML private TextArea campoDescripcion;
    @FXML private CheckBox checkActivo;
    @FXML private Label lblError;
    @FXML private Button btnGuardar;

    private CargoModel cargoActual;
    private Runnable onSuccessCallback;

    @FXML
    private void initialize() {
        SpinnerValueFactory<Integer> valueFactory = new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 99, 1);
        spinnerJerarquia.setValueFactory(valueFactory);
    }

    public void initData(CargoModel cargo, Runnable onSuccess) {
        this.cargoActual = cargo;
        this.onSuccessCallback = onSuccess;

        if (cargo != null) {
            lblTitulo.setText("Editar cargo #" + cargo.getId());
            lblSubtitulo.setText("Modifica el nombre, nivel jerárquico o responsabilidades del cargo");
            btnGuardar.setText("Guardar cambios");

            campoNombre.setText(cargo.getNombre());
            if (cargo.getNivelJerarquico() != null && spinnerJerarquia.getValueFactory() != null) {
                spinnerJerarquia.getValueFactory().setValue(cargo.getNivelJerarquico());
            }
            campoDescripcion.setText(cargo.getDescripcion());
            checkActivo.setSelected(cargo.isActivo());
        }
    }

    @FXML
    private void guardar() {
        lblError.setText("");

        String nombre = campoNombre.getText() != null ? campoNombre.getText().trim() : "";
        if (nombre.isBlank()) {
            lblError.setText("El nombre del cargo es obligatorio.");
            return;
        }

        Integer jerarquia = spinnerJerarquia.getValue();
        if (jerarquia == null || jerarquia <= 0) {
            lblError.setText("El nivel jerárquico debe ser un número entero mayor que cero.");
            return;
        }

        String descripcion = campoDescripcion.getText() != null ? campoDescripcion.getText().trim() : "";
        if (descripcion.isBlank()) {
            lblError.setText("Las responsabilidades / descripción del cargo son obligatorias.");
            return;
        }

        boolean activo = checkActivo.isSelected();

        CargoModel model = new CargoModel();
        model.setNombre(nombre);
        model.setNivelJerarquico(jerarquia);
        model.setDescripcion(descripcion);
        model.setActivo(activo);

        btnGuardar.setDisable(true);
        lblError.setStyle("-fx-text-fill: -color-accent-fg;");
        lblError.setText("Guardando cargo...");

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                CargoApiClient client = new CargoApiClient();
                if (cargoActual == null) {
                    client.create(model);
                } else {
                    client.update(cargoActual.getId(), model);
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
            lblError.setText(error != null && error.getMessage() != null ? error.getMessage() : "Error al guardar el cargo.");
        });

        Thread thread = new Thread(task, "guardar-cargo");
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
