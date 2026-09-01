package controller;

import java.util.List;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import models.AsistenciaModel;
import models.ReunionModel;
import service.AsistenciaApiClient;

public class AsistenciasModalController {
    @FXML private Label lblTituloReunion;
    @FXML private Label lblDetallesReunion;
    @FXML private Label lblBadgeEstado;

    @FXML private Label lblConvocados;
    @FXML private Label lblPresentes;
    @FXML private Label lblAusentes;
    @FXML private Label lblQuorum;

    @FXML private TextField txtBuscar;
    @FXML private Button btnAutoConvocar;

    @FXML private TableView<AsistenciaModel> tablaAsistencias;
    @FXML private TableColumn<AsistenciaModel, String> colMiembro;
    @FXML private TableColumn<AsistenciaModel, String> colDui;
    @FXML private TableColumn<AsistenciaModel, String> colTelefono;
    @FXML private TableColumn<AsistenciaModel, Boolean> colAsistio;
    @FXML private TableColumn<AsistenciaModel, String> colObservacion;

    private final AsistenciaApiClient apiClient = new AsistenciaApiClient();
    private final ObservableList<AsistenciaModel> asistenciasList = FXCollections.observableArrayList();
    private FilteredList<AsistenciaModel> filteredList;

    private ReunionModel reunion;
    private Runnable onCloseCallback;

    @FXML
    public void initialize() {
        filteredList = new FilteredList<>(asistenciasList, p -> true);
        txtBuscar.textProperty().addListener((obs, oldVal, newVal) -> {
            filteredList.setPredicate(a -> {
                if (newVal == null || newVal.isBlank()) return true;
                String term = newVal.toLowerCase().trim();
                return (a.getNombreMiembro() != null && a.getNombreMiembro().toLowerCase().contains(term))
                    || (a.getDuiMiembro() != null && a.getDuiMiembro().toLowerCase().contains(term));
            });
            actualizarMetricas();
        });

        configurarTabla();
    }

    public void setReunion(ReunionModel r) {
        this.reunion = r;
        if (r == null) return;

        lblTituloReunion.setText(r.getTitulo());
        lblDetallesReunion.setText("Fecha: " + r.getFechaDisplay() + " | Lugar: " + r.getLugarDisplay() + " | Tipo: " + r.getTipo());
        lblBadgeEstado.setText(r.getEstado());

        if (r.isCancelada()) {
            lblBadgeEstado.setStyle("-fx-background-color: #fee2e2; -fx-text-fill: #b91c1c; -fx-font-weight: bold; -fx-padding: 3 10 3 10; -fx-background-radius: 6;");
            btnAutoConvocar.setDisable(true);
        } else if (r.isRealizada()) {
            lblBadgeEstado.setStyle("-fx-background-color: #dcfce7; -fx-text-fill: #15803d; -fx-font-weight: bold; -fx-padding: 3 10 3 10; -fx-background-radius: 6;");
        }

        cargarDatos();
    }

    public void setOnCloseCallback(Runnable callback) {
        this.onCloseCallback = callback;
    }

    private void configurarTabla() {
        colMiembro.setCellValueFactory(cellData -> cellData.getValue().nombreMiembroProperty());
        colMiembro.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item);
                    setStyle("-fx-font-weight: bold; -fx-text-fill: #1e293b;");
                }
            }
        });

        colDui.setCellValueFactory(cellData -> cellData.getValue().duiMiembroProperty());
        colTelefono.setCellValueFactory(cellData -> cellData.getValue().telefonoMiembroProperty());

        colAsistio.setCellValueFactory(cellData -> cellData.getValue().asistioProperty());
        colAsistio.setCellFactory(col -> new TableCell<>() {
            private final CheckBox checkBox = new CheckBox();

            {
                checkBox.setAlignment(Pos.CENTER);
                checkBox.setOnAction(e -> {
                    if (getTableRow() != null && getTableRow().getItem() != null) {
                        AsistenciaModel a = getTableRow().getItem();
                        boolean nuevoValor = checkBox.isSelected();
                        a.setAsistio(nuevoValor);
                        toggleAsistenciaServidor(a, nuevoValor);
                    }
                });
            }

            @Override
            protected void updateItem(Boolean item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    checkBox.setSelected(item);
                    checkBox.setDisable(reunion != null && reunion.isCancelada());
                    setGraphic(checkBox);
                    setAlignment(Pos.CENTER);
                }
            }
        });

        colObservacion.setCellValueFactory(cellData -> cellData.getValue().observacionProperty());
        colObservacion.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    AsistenciaModel a = getTableRow().getItem();
                    HBox box = new HBox(8);
                    box.setAlignment(Pos.CENTER_LEFT);

                    Label lblObs = new Label((item != null && !item.isBlank()) ? item : "-");
                    lblObs.setStyle("-fx-text-fill: #64748b; -fx-font-size: 11px;");

                    if (reunion != null && !reunion.isCancelada()) {
                        Button btnEditar = new Button("Nota");
                        btnEditar.setStyle("-fx-background-color: #f1f5f9; -fx-text-fill: #334155; -fx-padding: 2 6 2 6; -fx-background-radius: 4; -fx-font-size: 10px;");
                        btnEditar.setOnAction(e -> editarObservacion(a));
                        box.getChildren().addAll(lblObs, btnEditar);
                    } else {
                        box.getChildren().add(lblObs);
                    }

                    setGraphic(box);
                }
            }
        });

        tablaAsistencias.setItems(filteredList);
    }

    public void cargarDatos() {
        if (reunion == null) return;

        Task<List<AsistenciaModel>> task = new Task<>() {
            @Override
            protected List<AsistenciaModel> call() throws Exception {
                return apiClient.getByReunion(reunion.getId());
            }
        };

        task.setOnSucceeded(e -> {
            asistenciasList.setAll(task.getValue());
            actualizarMetricas();
        });

        task.setOnFailed(e -> {
            Throwable ex = task.getException();
            mostrarAlerta(Alert.AlertType.ERROR, "Error",
                "No se pudo cargar la lista de asistencia: " + (ex != null ? ex.getMessage() : "Desconocido"));
        });

        new Thread(task).start();
    }

    private void actualizarMetricas() {
        int convocados = asistenciasList.size();
        long presentes = asistenciasList.stream().filter(AsistenciaModel::isAsistio).count();
        long ausentes = convocados - presentes;
        double pct = convocados > 0 ? (presentes * 100.0) / convocados : 0.0;

        lblConvocados.setText(String.valueOf(convocados));
        lblPresentes.setText(String.valueOf(presentes));
        lblAusentes.setText(String.valueOf(ausentes));
        lblQuorum.setText(String.format("%.1f%%", pct));
    }

    @FXML
    public void autoConvocar() {
        if (reunion == null || reunion.isCancelada()) return;

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Auto-convocar miembros activos");
        confirm.setHeaderText("¿Deseas generar la lista de convocatoria con todos los miembros activos?");
        confirm.setContentText("Se incorporarán a la lista de esta reunión todos los miembros registrados en estado ACTIVO.");

        confirm.showAndWait().ifPresent(res -> {
            if (res == ButtonType.OK) {
                btnAutoConvocar.setDisable(true);
                Task<List<AsistenciaModel>> task = new Task<>() {
                    @Override
                    protected List<AsistenciaModel> call() throws Exception {
                        return apiClient.convocar(reunion.getId(), null);
                    }
                };

                task.setOnSucceeded(e -> {
                    btnAutoConvocar.setDisable(false);
                    asistenciasList.setAll(task.getValue());
                    actualizarMetricas();
                    if (onCloseCallback != null) onCloseCallback.run();
                });

                task.setOnFailed(e -> {
                    btnAutoConvocar.setDisable(false);
                    mostrarAlerta(Alert.AlertType.ERROR, "Error al convocar",
                        "No se pudo autogenerar la convocatoria: " + task.getException().getMessage());
                });

                new Thread(task).start();
            }
        });
    }

    private void toggleAsistenciaServidor(AsistenciaModel a, boolean asistio) {
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                apiClient.toggle(a.getIdAsistencia(), asistio, a.getObservacion());
                return null;
            }
        };

        task.setOnSucceeded(e -> {
            actualizarMetricas();
            if (onCloseCallback != null) onCloseCallback.run();
        });

        task.setOnFailed(e -> {
            // Revertir si falló
            a.setAsistio(!asistio);
            actualizarMetricas();
            mostrarAlerta(Alert.AlertType.ERROR, "Error", "No se pudo actualizar la asistencia: " + task.getException().getMessage());
        });

        new Thread(task).start();
    }

    private void editarObservacion(AsistenciaModel a) {
        TextInputDialog dialog = new TextInputDialog(a.getObservacion());
        dialog.setTitle("Observación de Asistencia");
        dialog.setHeaderText("Miembro: " + a.getNombreMiembro());
        dialog.setContentText("Escribe la justificación o nota:");

        dialog.showAndWait().ifPresent(texto -> {
            String clean = texto != null ? texto.trim() : "";
            Task<Void> task = new Task<>() {
                @Override
                protected Void call() throws Exception {
                    apiClient.toggle(a.getIdAsistencia(), a.isAsistio(), clean);
                    return null;
                }
            };
            task.setOnSucceeded(e -> {
                a.setObservacion(clean);
                tablaAsistencias.refresh();
            });
            task.setOnFailed(e -> mostrarAlerta(Alert.AlertType.ERROR, "Error", "No se pudo guardar la nota: " + task.getException().getMessage()));
            new Thread(task).start();
        });
    }

    @FXML
    public void cerrar() {
        if (onCloseCallback != null) onCloseCallback.run();
        Stage stage = (Stage) btnAutoConvocar.getScene().getWindow();
        if (stage != null) stage.close();
    }

    private void mostrarAlerta(Alert.AlertType tipo, String titulo, String mensaje) {
        Platform.runLater(() -> {
            Alert alert = new Alert(tipo);
            alert.setTitle(titulo);
            alert.setHeaderText(null);
            alert.setContentText(mensaje);
            alert.showAndWait();
        });
    }
}
