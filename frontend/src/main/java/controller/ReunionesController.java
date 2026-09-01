package controller;

import java.util.List;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import models.ReunionModel;
import security.SessionManager;
import service.ReunionApiClient;

public class ReunionesController {
    @FXML private Button btnNuevaReunion;
    @FXML private Label lblTotalReuniones;
    @FXML private Label lblTotalProgramadas;
    @FXML private Label lblTotalRealizadas;
    @FXML private Label lblTotalCanceladas;

    @FXML private TextField txtBuscar;
    @FXML private ComboBox<String> cbFiltroTipo;
    @FXML private ComboBox<String> cbFiltroEstado;

    @FXML private TableView<ReunionModel> tablaReuniones;
    @FXML private TableColumn<ReunionModel, String> colTitulo;
    @FXML private TableColumn<ReunionModel, String> colFecha;
    @FXML private TableColumn<ReunionModel, String> colLugar;
    @FXML private TableColumn<ReunionModel, String> colTipo;
    @FXML private TableColumn<ReunionModel, String> colEstado;
    @FXML private TableColumn<ReunionModel, String> colQuorum;
    @FXML private TableColumn<ReunionModel, Void> colAcciones;

    private final ReunionApiClient apiClient = new ReunionApiClient();
    private final ObservableList<ReunionModel> reunionesList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        cbFiltroTipo.setItems(FXCollections.observableArrayList("TODOS", "ORDINARIA", "EXTRAORDINARIA"));
        cbFiltroTipo.getSelectionModel().select("TODOS");

        cbFiltroEstado.setItems(FXCollections.observableArrayList("TODOS", "PROGRAMADA", "REALIZADA", "CANCELADA"));
        cbFiltroEstado.getSelectionModel().select("TODOS");

        configurarTabla();
        configurarPermisos();

        txtBuscar.textProperty().addListener((obs, oldVal, newVal) -> cargarDatos());
        cbFiltroTipo.valueProperty().addListener((obs, oldVal, newVal) -> cargarDatos());
        cbFiltroEstado.valueProperty().addListener((obs, oldVal, newVal) -> cargarDatos());

        cargarDatos();
    }

    private void configurarPermisos() {
        boolean canManage = canManage();
        btnNuevaReunion.setVisible(canManage);
        btnNuevaReunion.setManaged(canManage);
    }

    private boolean canManage() {
        return SessionManager.getInstance().getCurrentUser()
            .map(u -> {
                String r = u.getRole() != null ? u.getRole().toUpperCase() : "";
                return r.contains("ADMIN") || r.contains("PRESIDENTE") || r.contains("SECRETARI");
            }).orElse(false);
    }

    private void configurarTabla() {
        colTitulo.setCellValueFactory(cellData -> cellData.getValue().tituloProperty());
        colTitulo.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    Label lbl = new Label(item);
                    lbl.setStyle("-fx-font-weight: bold; -fx-text-fill: #1e293b;");
                    lbl.setWrapText(true);
                    setGraphic(lbl);
                }
            }
        });

        colFecha.setCellValueFactory(cellData -> cellData.getValue().fechaHoraProperty());
        colFecha.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.replace("T", " "));
                }
            }
        });

        colLugar.setCellValueFactory(cellData -> cellData.getValue().lugarProperty());
        colLugar.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setText(null);
                } else {
                    setText((item != null && !item.isBlank()) ? item : "Por definir");
                }
            }
        });

        colTipo.setCellValueFactory(cellData -> cellData.getValue().tipoProperty());
        colTipo.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    Label badge = new Label(item);
                    badge.setStyle("EXTRAORDINARIA".equalsIgnoreCase(item)
                        ? "-fx-background-color: #fef3c7; -fx-text-fill: #b45309; -fx-font-weight: bold; -fx-padding: 3 8 3 8; -fx-background-radius: 6; -fx-font-size: 11px;"
                        : "-fx-background-color: #e0e7ff; -fx-text-fill: #4338ca; -fx-font-weight: bold; -fx-padding: 3 8 3 8; -fx-background-radius: 6; -fx-font-size: 11px;");
                    setGraphic(badge);
                }
            }
        });

        colEstado.setCellValueFactory(cellData -> cellData.getValue().estadoProperty());
        colEstado.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    Label badge = new Label(item);
                    if ("REALIZADA".equalsIgnoreCase(item)) {
                        badge.setStyle("-fx-background-color: #dcfce7; -fx-text-fill: #15803d; -fx-font-weight: bold; -fx-padding: 3 8 3 8; -fx-background-radius: 6; -fx-font-size: 11px;");
                    } else if ("PROGRAMADA".equalsIgnoreCase(item)) {
                        badge.setStyle("-fx-background-color: #e0f2fe; -fx-text-fill: #0369a1; -fx-font-weight: bold; -fx-padding: 3 8 3 8; -fx-background-radius: 6; -fx-font-size: 11px;");
                    } else {
                        badge.setStyle("-fx-background-color: #fee2e2; -fx-text-fill: #b91c1c; -fx-font-weight: bold; -fx-padding: 3 8 3 8; -fx-background-radius: 6; -fx-font-size: 11px;");
                    }
                    setGraphic(badge);
                }
            }
        });

        colQuorum.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getQuorumDisplay()));
        colQuorum.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item);
                }
            }
        });

        colAcciones.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null);
                } else {
                    ReunionModel m = getTableRow().getItem();
                    HBox box = new HBox(6);
                    box.setAlignment(Pos.CENTER_LEFT);

                    if (canManage()) {
                        if (m.isProgramada()) {
                            Button btnRealizada = new Button("Realizada");
                            btnRealizada.setStyle("-fx-background-color: #10b981; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 3 8 3 8; -fx-background-radius: 4; -fx-cursor: hand;");
                            btnRealizada.setOnAction(e -> marcarRealizada(m));

                            Button btnEditar = new Button("Editar");
                            btnEditar.setStyle("-fx-background-color: #f1f5f9; -fx-text-fill: #1e293b; -fx-padding: 3 8 3 8; -fx-background-radius: 4; -fx-cursor: hand;");
                            btnEditar.setOnAction(e -> abrirFormularioEditar(m));

                            Button btnCancelar = new Button("Cancelar");
                            btnCancelar.setStyle("-fx-background-color: #fee2e2; -fx-text-fill: #b91c1c; -fx-padding: 3 8 3 8; -fx-background-radius: 4; -fx-cursor: hand;");
                            btnCancelar.setOnAction(e -> cancelarReunion(m));

                            Button btnEliminar = new Button("Eliminar");
                            btnEliminar.setStyle("-fx-background-color: #f87171; -fx-text-fill: white; -fx-padding: 3 8 3 8; -fx-background-radius: 4; -fx-cursor: hand;");
                            btnEliminar.setOnAction(e -> eliminarReunion(m));

                            box.getChildren().addAll(btnRealizada, btnEditar, btnCancelar, btnEliminar);
                        }
                    }

                    Button btnAsistencias = new Button("Asistencias");
                    btnAsistencias.setStyle("-fx-background-color: #e0e7ff; -fx-text-fill: #4338ca; -fx-font-weight: bold; -fx-padding: 3 8 3 8; -fx-background-radius: 4; -fx-cursor: hand;");
                    btnAsistencias.setOnAction(e -> abrirGestionAsistencias(m));
                    box.getChildren().add(btnAsistencias);

                    setGraphic(box);
                }
            }
        });

        tablaReuniones.setItems(reunionesList);
    }

    public void cargarDatos() {
        String search = txtBuscar.getText();
        String tipo = cbFiltroTipo.getValue();
        String estado = cbFiltroEstado.getValue();

        Task<List<ReunionModel>> task = new Task<>() {
            @Override
            protected List<ReunionModel> call() throws Exception {
                return apiClient.getAll(search, tipo, estado);
            }
        };

        task.setOnSucceeded(e -> {
            reunionesList.setAll(task.getValue());
            actualizarResumen(task.getValue());
        });

        task.setOnFailed(e -> {
            Throwable ex = task.getException();
            mostrarAlerta(Alert.AlertType.ERROR, "Error de carga",
                "No se pudieron consultar las reuniones comunitarias: " + (ex != null ? ex.getMessage() : "Desconocido"));
        });

        new Thread(task).start();
    }

    private void actualizarResumen(List<ReunionModel> list) {
        int total = list.size();
        long programadas = list.stream().filter(ReunionModel::isProgramada).count();
        long realizadas = list.stream().filter(ReunionModel::isRealizada).count();
        long canceladas = list.stream().filter(ReunionModel::isCancelada).count();

        lblTotalReuniones.setText(total + " registradas");
        lblTotalProgramadas.setText(programadas + " activas");
        lblTotalRealizadas.setText(realizadas + " sesiones");
        lblTotalCanceladas.setText(canceladas + " suspendidas");
    }

    @FXML
    public void limpiarFiltros() {
        txtBuscar.clear();
        cbFiltroTipo.getSelectionModel().select("TODOS");
        cbFiltroEstado.getSelectionModel().select("TODOS");
    }

    @FXML
    public void abrirFormularioCrear() {
        abrirModalFormulario(null);
    }

    public void abrirFormularioEditar(ReunionModel m) {
        if (m == null) return;
        abrirModalFormulario(m);
    }

    private void abrirModalFormulario(ReunionModel m) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/views/reunion-form.fxml"));
            Parent root = loader.load();

            ReunionFormController ctrl = loader.getController();
            ctrl.setReunion(m);
            ctrl.setOnSaveCallback(this::cargarDatos);

            Stage stage = new Stage();
            stage.setTitle(m == null ? "Nueva Reunión Comunal" : "Editar Reunión Comunal");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(root));
            stage.setResizable(false);
            stage.showAndWait();
        } catch (Exception e) {
            e.printStackTrace();
            mostrarAlerta(Alert.AlertType.ERROR, "Error", "No se pudo abrir el formulario: " + e.getMessage());
        }
    }

    private void marcarRealizada(ReunionModel m) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Marcar reunión como Realizada");
        confirm.setHeaderText("¿Confirmas que la reunión \"" + m.getTitulo() + "\" ha concluido?");
        confirm.setContentText("Al marcarla como REALIZADA quedará registrada de forma definitiva.");

        confirm.showAndWait().ifPresent(res -> {
            if (res == ButtonType.OK) {
                Task<Void> task = new Task<>() {
                    @Override
                    protected Void call() throws Exception {
                        apiClient.marcarRealizada(m.getId());
                        return null;
                    }
                };
                task.setOnSucceeded(e -> cargarDatos());
                task.setOnFailed(e -> mostrarAlerta(Alert.AlertType.ERROR, "Error",
                    "No se pudo marcar como realizada: " + task.getException().getMessage()));
                new Thread(task).start();
            }
        });
    }

    private void cancelarReunion(ReunionModel m) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Cancelar reunión");
        confirm.setHeaderText("¿Estás seguro de suspender la reunión \"" + m.getTitulo() + "\"?");
        confirm.setContentText("La reunión pasará a estado CANCELADA.");

        confirm.showAndWait().ifPresent(res -> {
            if (res == ButtonType.OK) {
                Task<Void> task = new Task<>() {
                    @Override
                    protected Void call() throws Exception {
                        apiClient.cancelar(m.getId());
                        return null;
                    }
                };
                task.setOnSucceeded(e -> cargarDatos());
                task.setOnFailed(e -> mostrarAlerta(Alert.AlertType.ERROR, "Error",
                    "No se pudo cancelar la reunión: " + task.getException().getMessage()));
                new Thread(task).start();
            }
        });
    }

    private void eliminarReunion(ReunionModel m) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Eliminar reunión");
        confirm.setHeaderText("¿Eliminar definitivamente la reunión \"" + m.getTitulo() + "\"?");
        confirm.setContentText("Solo se puede eliminar si no posee registros de asistencia.");

        confirm.showAndWait().ifPresent(res -> {
            if (res == ButtonType.OK) {
                Task<Void> task = new Task<>() {
                    @Override
                    protected Void call() throws Exception {
                        apiClient.delete(m.getId());
                        return null;
                    }
                };
                task.setOnSucceeded(e -> cargarDatos());
                task.setOnFailed(e -> mostrarAlerta(Alert.AlertType.ERROR, "Error",
                    "No se pudo eliminar la reunión: " + task.getException().getMessage()));
                new Thread(task).start();
            }
        });
    }

    public void abrirGestionAsistencias(ReunionModel m) {
        if (m == null) return;
        mostrarAlerta(Alert.AlertType.INFORMATION, "Control de Asistencia",
            "Reunión: " + m.getTitulo() + "\nFecha: " + m.getFechaDisplay() +
            "\nEstado: " + m.getEstado() + "\nQuórum: " + m.getQuorumDisplay());
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
