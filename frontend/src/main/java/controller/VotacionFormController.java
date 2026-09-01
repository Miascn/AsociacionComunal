package controller;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import models.ProyectoModel;
import models.VotacionModel;
import service.ProyectoApiClient;
import service.VotacionApiClient;

public class VotacionFormController {
    @FXML private Label lblModalTitulo;
    @FXML private TextField txtTitulo;
    @FXML private TextArea txtDescripcion;
    @FXML private ComboBox<ProyectoOption> comboProyecto;
    @FXML private DatePicker pickerFechaInicio;
    @FXML private DatePicker pickerFechaFin;
    @FXML private VBox secOpciones;
    @FXML private TextField txtNuevaOpcion;
    @FXML private Button btnAgregarOpcion;
    @FXML private ListView<String> listOpciones;
    @FXML private Label lblError;
    @FXML private Button btnGuardar;

    private final ObservableList<String> opcionesList = FXCollections.observableArrayList();
    private VotacionModel votacionEdicion;
    private Runnable onSavedCallback;

    public record ProyectoOption(Integer id, String nombre) {
        @Override
        public String toString() {
            return nombre;
        }
    }

    @FXML
    public void initialize() {
        listOpciones.setItems(opcionesList);
        listOpciones.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    HBox box = new HBox(10);
                    Label lbl = new Label(item);
                    lbl.setMaxWidth(Double.MAX_VALUE);
                    HBox.setHgrow(lbl, javafx.scene.layout.Priority.ALWAYS);
                    Button btnEliminar = new Button("✕");
                    btnEliminar.setStyle("-fx-background-color: transparent; -fx-text-fill: #ef4444; -fx-cursor: hand; -fx-padding: 0 4 0 4;");
                    btnEliminar.setOnAction(e -> opcionesList.remove(item));
                    box.getChildren().addAll(lbl, btnEliminar);
                    setGraphic(box);
                    setText(null);
                }
            }
        });

        comboProyecto.setConverter(new StringConverter<>() {
            @Override
            public String toString(ProyectoOption opt) {
                return opt != null ? opt.nombre() : "";
            }
            @Override
            public ProyectoOption fromString(String string) {
                return null;
            }
        });

        cargarProyectos();
    }

    private void cargarProyectos() {
        Task<List<ProyectoModel>> task = new Task<>() {
            @Override
            protected List<ProyectoModel> call() throws Exception {
                return new ProyectoApiClient().findAll();
            }
        };

        task.setOnSucceeded(e -> {
            List<ProyectoOption> options = new ArrayList<>();
            options.add(new ProyectoOption(null, "Sin proyecto vinculado (Consulta General)"));
            for (ProyectoModel p : task.getValue()) {
                options.add(new ProyectoOption(p.getIdProyecto(), p.getNombre()));
            }
            comboProyecto.setItems(FXCollections.observableArrayList(options));

            if (votacionEdicion != null && votacionEdicion.getIdProyecto() != null) {
                for (ProyectoOption opt : comboProyecto.getItems()) {
                    if (opt.id() != null && opt.id().equals(votacionEdicion.getIdProyecto())) {
                        comboProyecto.setValue(opt);
                        break;
                    }
                }
            } else {
                comboProyecto.getSelectionModel().selectFirst();
            }
        });

        new Thread(task).start();
    }

    public void setVotacionEdicion(VotacionModel model) {
        this.votacionEdicion = model;
        if (model != null) {
            lblModalTitulo.setText("Editar votación comunal");
            txtTitulo.setText(model.getTitulo());
            txtDescripcion.setText(model.getDescripcion());
            try {
                if (model.getFechaInicio() != null && !model.getFechaInicio().isBlank()) {
                    pickerFechaInicio.setValue(LocalDate.parse(model.getFechaInicio().substring(0, 10)));
                }
                if (model.getFechaFin() != null && !model.getFechaFin().isBlank()) {
                    pickerFechaFin.setValue(LocalDate.parse(model.getFechaFin().substring(0, 10)));
                }
            } catch (Exception ignored) {}

            // En edición, las opciones se gestionan en su respectivo CRUD/panel
            secOpciones.setVisible(false);
            secOpciones.setManaged(false);
        }
    }

    public void setOnSavedCallback(Runnable callback) {
        this.onSavedCallback = callback;
    }

    @FXML
    public void agregarOpcion() {
        String texto = txtNuevaOpcion.getText();
        if (texto != null && !texto.trim().isBlank()) {
            String opt = texto.trim();
            if (!opcionesList.contains(opt)) {
                opcionesList.add(opt);
                txtNuevaOpcion.clear();
                lblError.setText("");
            } else {
                lblError.setText("La opción ya se encuentra agregada.");
            }
        }
    }

    @FXML
    public void guardar() {
        lblError.setText("");
        String titulo = txtTitulo.getText();
        String descripcion = txtDescripcion.getText();
        LocalDate inicio = pickerFechaInicio.getValue();
        LocalDate fin = pickerFechaFin.getValue();
        ProyectoOption proyecto = comboProyecto.getValue();
        Integer idProyecto = proyecto != null ? proyecto.id() : null;

        if (titulo == null || titulo.trim().isBlank()) {
            lblError.setText("El título del proceso de votación es obligatorio.");
            return;
        }
        if (inicio == null || fin == null) {
            lblError.setText("Debe seleccionar las fechas de inicio y finalización.");
            return;
        }
        if (!inicio.isBefore(fin)) {
            lblError.setText("La fecha de inicio debe ser anterior a la fecha de finalización.");
            return;
        }

        LocalDateTime dtInicio = inicio.atTime(8, 0);
        LocalDateTime dtFin = fin.atTime(18, 0);
        String strInicio = dtInicio.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        String strFin = dtFin.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

        btnGuardar.setDisable(true);

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                VotacionApiClient client = new VotacionApiClient();
                if (votacionEdicion == null) {
                    List<String> opciones = new ArrayList<>(opcionesList);
                    client.create(titulo.trim(), descripcion != null ? descripcion.trim() : "",
                        idProyecto, strInicio, strFin, opciones);
                } else {
                    client.update(votacionEdicion.getId(), titulo.trim(), descripcion != null ? descripcion.trim() : "",
                        idProyecto, strInicio, strFin);
                }
                return null;
            }
        };

        task.setOnSucceeded(e -> {
            if (onSavedCallback != null) onSavedCallback.run();
            cerrarModal();
        });

        task.setOnFailed(e -> {
            btnGuardar.setDisable(false);
            Throwable ex = task.getException();
            lblError.setText(ex != null ? ex.getMessage() : "Error al guardar la votación.");
        });

        new Thread(task).start();
    }

    @FXML
    public void cancelar() {
        cerrarModal();
    }

    private void cerrarModal() {
        Stage stage = (Stage) btnGuardar.getScene().getWindow();
        if (stage != null) stage.close();
    }
}
