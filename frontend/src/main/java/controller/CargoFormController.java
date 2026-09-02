package controller;

import java.util.ArrayList;
import java.util.List;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import models.CargoModel;
import service.CargoApiClient;

public class CargoFormController {
    public record JerarquiaItem(int nivel, String etiqueta) {
        @Override
        public String toString() {
            return etiqueta;
        }
    }

    @FXML private Label lblTitulo;
    @FXML private Label lblSubtitulo;
    @FXML private TextField campoNombre;
    @FXML private ComboBox<JerarquiaItem> comboJerarquia;
    @FXML private TextArea campoDescripcion;
    @FXML private CheckBox checkActivo;
    @FXML private Label lblError;
    @FXML private Button btnGuardar;

    private CargoModel cargoActual;
    private Runnable onSuccessCallback;

    @FXML
    private void initialize() {
        List<JerarquiaItem> items = new ArrayList<>();
        items.add(new JerarquiaItem(1, "1 - Nivel Máximo (Presidencia)"));
        items.add(new JerarquiaItem(2, "2 - Nivel Alto (Vicepresidencia)"));
        items.add(new JerarquiaItem(3, "3 - Nivel Medio-Alto (Secretaría)"));
        items.add(new JerarquiaItem(4, "4 - Nivel Medio (Tesorería)"));
        items.add(new JerarquiaItem(5, "5 - Nivel Operativo (Vocalía / Síndico)"));
        items.add(new JerarquiaItem(6, "6 - Nivel de Apoyo (Vocal 2)"));
        items.add(new JerarquiaItem(7, "7 - Nivel de Apoyo (Vocal 3)"));
        items.add(new JerarquiaItem(8, "8 - Nivel de Apoyo (Suplente)"));
        items.add(new JerarquiaItem(9, "9 - Nivel de Apoyo"));
        items.add(new JerarquiaItem(10, "10 - Nivel de Apoyo"));
        comboJerarquia.getItems().setAll(items);
        comboJerarquia.getSelectionModel().selectFirst();
    }

    public void initData(CargoModel cargo, Runnable onSuccess) {
        this.cargoActual = cargo;
        this.onSuccessCallback = onSuccess;

        if (cargo != null) {
            lblTitulo.setText("Editar cargo #" + cargo.getId());
            lblSubtitulo.setText("Modifica el nombre, nivel jerárquico o responsabilidades del cargo");
            btnGuardar.setText("Guardar cambios");

            campoNombre.setText(cargo.getNombre());
            if (cargo.getNivelJerarquico() != null && comboJerarquia != null) {
                int nivel = cargo.getNivelJerarquico();
                JerarquiaItem matching = comboJerarquia.getItems().stream()
                    .filter(it -> it.nivel() == nivel)
                    .findFirst()
                    .orElseGet(() -> {
                        JerarquiaItem custom = new JerarquiaItem(nivel, CargoModel.getDescripcionJerarquia(nivel));
                        comboJerarquia.getItems().add(custom);
                        return custom;
                    });
                comboJerarquia.getSelectionModel().select(matching);
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

        JerarquiaItem selectedJerarquia = comboJerarquia.getValue();
        Integer jerarquia = selectedJerarquia != null ? selectedJerarquia.nivel() : 1;

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
