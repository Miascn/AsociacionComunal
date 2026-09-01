package controller;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import models.ReunionModel;
import service.ReunionApiClient;

public class ReunionFormController {
    @FXML private Label lblTituloForm;
    @FXML private TextField txtTitulo;
    @FXML private DatePicker dpFecha;
    @FXML private ComboBox<String> cbHora;
    @FXML private TextField txtLugar;
    @FXML private ComboBox<String> cbTipo;
    @FXML private Label lblError;
    @FXML private Button btnGuardar;

    private final ReunionApiClient apiClient = new ReunionApiClient();
    private ReunionModel reunionExistente;
    private Runnable onSaveCallback;

    @FXML
    public void initialize() {
        cbTipo.setItems(FXCollections.observableArrayList("ORDINARIA", "EXTRAORDINARIA"));
        cbTipo.getSelectionModel().select("ORDINARIA");

        List<String> horas = new ArrayList<>();
        for (int h = 7; h <= 20; h++) {
            horas.add(String.format("%02d:00", h));
            horas.add(String.format("%02d:30", h));
        }
        cbHora.setItems(FXCollections.observableArrayList(horas));
        cbHora.getSelectionModel().select("14:00");

        dpFecha.setValue(LocalDate.now().plusDays(3));
    }

    public void setReunion(ReunionModel m) {
        this.reunionExistente = m;
        if (m == null) {
            lblTituloForm.setText("Nueva Reunión Comunal");
            btnGuardar.setText("Guardar reunión");
        } else {
            lblTituloForm.setText("Editar Reunión #" + m.getId());
            btnGuardar.setText("Actualizar reunión");

            txtTitulo.setText(m.getTitulo());
            txtLugar.setText(m.getLugar());
            if (m.getTipo() != null) {
                cbTipo.getSelectionModel().select(m.getTipo().toUpperCase());
            }

            try {
                String fh = m.getFechaHora();
                if (fh != null && !fh.isBlank()) {
                    String clean = fh.replace(" ", "T");
                    LocalDateTime ldt = LocalDateTime.parse(clean.substring(0, Math.min(clean.length(), 19)));
                    dpFecha.setValue(ldt.toLocalDate());
                    cbHora.getSelectionModel().select(String.format("%02d:%02d", ldt.getHour(), ldt.getMinute()));
                }
            } catch (Exception ignored) {}
        }
    }

    public void setOnSaveCallback(Runnable callback) {
        this.onSaveCallback = callback;
    }

    @FXML
    public void guardar() {
        lblError.setText("");

        String titulo = txtTitulo.getText() != null ? txtTitulo.getText().trim() : "";
        if (titulo.isBlank()) {
            lblError.setText("El título de la reunión es obligatorio.");
            return;
        }

        LocalDate fecha = dpFecha.getValue();
        if (fecha == null) {
            lblError.setText("Debe seleccionar la fecha de la reunión.");
            return;
        }

        String horaStr = cbHora.getValue();
        if (horaStr == null || horaStr.isBlank()) {
            lblError.setText("Debe seleccionar la hora de inicio.");
            return;
        }

        String fechaHoraStr = fecha.format(DateTimeFormatter.ISO_LOCAL_DATE) + " " + horaStr + ":00";
        String lugar = txtLugar.getText() != null ? txtLugar.getText().trim() : "";
        String tipo = cbTipo.getValue() != null ? cbTipo.getValue() : "ORDINARIA";

        btnGuardar.setDisable(true);

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                ReunionModel model = new ReunionModel();
                model.setTitulo(titulo);
                model.setFechaHora(fechaHoraStr);
                model.setLugar(lugar);
                model.setTipo(tipo);

                if (reunionExistente == null || reunionExistente.getId() == 0) {
                    apiClient.create(model);
                } else {
                    apiClient.update(reunionExistente.getId(), model);
                }
                return null;
            }
        };

        task.setOnSucceeded(e -> {
            btnGuardar.setDisable(false);
            if (onSaveCallback != null) onSaveCallback.run();
            cancelar();
        });

        task.setOnFailed(e -> {
            btnGuardar.setDisable(false);
            Throwable ex = task.getException();
            lblError.setText(ex != null ? ex.getMessage() : "Error al guardar la reunión.");
        });

        new Thread(task).start();
    }

    @FXML
    public void cancelar() {
        Stage stage = (Stage) btnGuardar.getScene().getWindow();
        if (stage != null) stage.close();
    }
}
