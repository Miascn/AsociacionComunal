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
import service.ResponsiveWindowService;
import service.ReunionApiClient;
import service.TablePaginator;

public class ReunionesController {
    @FXML private Button btnNuevaReunion;
    @FXML private Label lblTotalReuniones;
    @FXML private Label lblTotalProgramadas;
    @FXML private Label lblTotalRealizadas;
    @FXML private Label lblTotalCanceladas;
    @FXML private HBox barraPie;

    @FXML private TextField txtBuscar;
    @FXML private ComboBox<String> cbFiltroTipo;
    @FXML private ComboBox<String> cbFiltroEstado;
    @FXML private ComboBox<String> cbFiltroTemporal;

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
    private TablePaginator<ReunionModel> paginator;

    @FXML
    public void initialize() {
        cbFiltroTipo.setItems(FXCollections.observableArrayList("TODOS", "ORDINARIA", "EXTRAORDINARIA"));
        cbFiltroTipo.getSelectionModel().select("TODOS");

        cbFiltroEstado.setItems(FXCollections.observableArrayList("TODOS", "PROGRAMADA", "REALIZADA", "CANCELADA"));
        cbFiltroEstado.getSelectionModel().select("TODOS");

        cbFiltroTemporal.setItems(FXCollections.observableArrayList("TODAS", "PRÓXIMAS", "PASADAS"));
        cbFiltroTemporal.getSelectionModel().select("TODAS");

        configurarTabla();

        paginator = new TablePaginator<>(tablaReuniones, reunionesList, "reuniones", 5);
        if (barraPie != null) {
            paginator.attachTo(barraPie);
        }

        configurarPermisos();

        txtBuscar.textProperty().addListener((obs, oldVal, newVal) -> cargarDatos());
        cbFiltroTipo.valueProperty().addListener((obs, oldVal, newVal) -> cargarDatos());
        cbFiltroEstado.valueProperty().addListener((obs, oldVal, newVal) -> cargarDatos());
        cbFiltroTemporal.valueProperty().addListener((obs, oldVal, newVal) -> cargarDatos());

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
                    ReunionModel m = getTableRow() != null ? getTableRow().getItem() : null;
                    Label lbl = new Label(item);
                    if (m != null && m.isCancelada()) {
                        lbl.setStyle("-fx-font-weight: bold; -fx-text-fill: #94a3b8;");
                    } else {
                        lbl.setStyle("-fx-font-weight: bold; -fx-text-fill: #1e293b;");
                    }
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
                    Label badge = new Label();
                    if ("REALIZADA".equalsIgnoreCase(item)) {
                        badge.setText("✓ REALIZADA");
                        badge.setStyle("-fx-background-color: #f1f5f9; -fx-text-fill: #475569; -fx-font-weight: bold; -fx-padding: 3 8 3 8; -fx-background-radius: 6; -fx-font-size: 11px;");
                    } else if ("PROGRAMADA".equalsIgnoreCase(item)) {
                        badge.setText("● PROGRAMADA");
                        badge.setStyle("-fx-background-color: #dbeafe; -fx-text-fill: #1d4ed8; -fx-font-weight: bold; -fx-padding: 3 8 3 8; -fx-background-radius: 6; -fx-font-size: 11px;");
                    } else {
                        badge.setText("✕ CANCELADA");
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
            private final Button btnAsistencias = new Button();
            private final Button btnRealizada   = new Button();
            private final Button btnEditar      = new Button();
            private final Button btnCancelar    = new Button();
            private final Button btnEliminar    = new Button();
            private final HBox box = new HBox(8);

            {
                box.getStyleClass().add("row-actions-box");
                box.setAlignment(Pos.CENTER_RIGHT);

                btnAsistencias.getStyleClass().addAll("btn-row-action", "btn-action-view");
                btnAsistencias.setGraphic(service.HeroIcon.create(service.HeroIcon.USERS, service.HeroIcon.BLUE_600, 18));
                btnAsistencias.setTooltip(new Tooltip("Gestionar asistencias / Quórum"));
                btnAsistencias.setOnAction(e -> {
                    ReunionModel m = getTableView().getItems().get(getIndex());
                    if (m != null) abrirGestionAsistencias(m);
                });

                btnRealizada.getStyleClass().addAll("btn-row-action", "btn-action-view");
                btnRealizada.setGraphic(service.HeroIcon.create(service.HeroIcon.CHECK_CIRCLE, service.HeroIcon.GREEN_600, 18));
                btnRealizada.setTooltip(new Tooltip("Marcar como realizada"));
                btnRealizada.setOnAction(e -> {
                    ReunionModel m = getTableView().getItems().get(getIndex());
                    if (m != null) marcarRealizada(m);
                });

                btnEditar.getStyleClass().addAll("btn-row-action", "btn-action-edit");
                btnEditar.setGraphic(service.HeroIcon.create(service.HeroIcon.PENCIL, service.HeroIcon.AMBER_600, 18));
                btnEditar.setTooltip(new Tooltip("Editar reunión"));
                btnEditar.setOnAction(e -> {
                    ReunionModel m = getTableView().getItems().get(getIndex());
                    if (m != null) abrirFormularioEditar(m);
                });

                btnCancelar.getStyleClass().addAll("btn-row-action", "btn-action-delete");
                btnCancelar.setGraphic(service.HeroIcon.create(service.HeroIcon.BAN, service.HeroIcon.SLATE_700, 18));
                btnCancelar.setTooltip(new Tooltip("Cancelar reunión"));
                btnCancelar.setOnAction(e -> {
                    ReunionModel m = getTableView().getItems().get(getIndex());
                    if (m != null) cancelarReunion(m);
                });

                btnEliminar.getStyleClass().addAll("btn-row-action", "btn-action-delete");
                btnEliminar.setGraphic(service.HeroIcon.create(service.HeroIcon.TRASH, service.HeroIcon.RED_600, 18));
                btnEliminar.setTooltip(new Tooltip("Eliminar reunión"));
                btnEliminar.setOnAction(e -> {
                    ReunionModel m = getTableView().getItems().get(getIndex());
                    if (m != null) eliminarReunion(m);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getIndex() < 0 || getIndex() >= getTableView().getItems().size()) {
                    setGraphic(null);
                } else {
                    setAlignment(Pos.CENTER_RIGHT);
                    ReunionModel m = getTableView().getItems().get(getIndex());
                    box.getChildren().clear();

                    if (canManage() && m.isProgramada()) {
                        box.getChildren().addAll(btnRealizada, btnEditar, btnCancelar, btnEliminar);
                    }
                    box.getChildren().add(btnAsistencias);

                    setGraphic(box);
                }
            }
        });

        // tablaReuniones.setItems is managed by paginator
    }

    public void cargarDatos() {
        String search = txtBuscar.getText();
        String tipo = cbFiltroTipo.getValue();
        String estado = cbFiltroEstado.getValue();
        String temporal = cbFiltroTemporal != null ? cbFiltroTemporal.getValue() : "TODAS";

        String desde = null;
        String hasta = null;
        if ("PRÓXIMAS".equalsIgnoreCase(temporal)) {
            desde = java.time.LocalDate.now().toString();
        } else if ("PASADAS".equalsIgnoreCase(temporal)) {
            hasta = java.time.LocalDate.now().minusDays(1).toString();
        }

        final String finalDesde = desde;
        final String finalHasta = hasta;

        Task<List<ReunionModel>> task = new Task<>() {
            @Override
            protected List<ReunionModel> call() throws Exception {
                return apiClient.getAll(search, tipo, estado, finalDesde, finalHasta);
            }
        };

        task.setOnSucceeded(e -> {
            reunionesList.setAll(task.getValue());
            paginator.updatePagination();
            actualizarResumen(task.getValue());
        });

        task.setOnFailed(e -> {
            reunionesList.clear();
            paginator.updatePagination();
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
        if (cbFiltroTemporal != null) {
            cbFiltroTemporal.getSelectionModel().select("TODAS");
        }
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
            ResponsiveWindowService.fitModalStage(stage);
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
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/views/asistencias-modal.fxml"));
            Parent root = loader.load();

            AsistenciasModalController ctrl = loader.getController();
            ctrl.setReunion(m);
            ctrl.setOnCloseCallback(this::cargarDatos);

            Stage stage = new Stage();
            stage.setTitle("Control de Asistencia: " + m.getTitulo());
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(root));
            ResponsiveWindowService.fitModalStage(stage);
            stage.showAndWait();
        } catch (Exception e) {
            e.printStackTrace();
            mostrarAlerta(Alert.AlertType.ERROR, "Error", "No se pudo abrir el control de asistencia: " + e.getMessage());
        }
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
