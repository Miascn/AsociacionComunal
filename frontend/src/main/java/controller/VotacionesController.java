package controller;

import java.util.List;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import models.VotacionModel;
import security.SessionManager;
import service.VotacionApiClient;

public class VotacionesController {
    @FXML private Button btnNuevaVotacion;
    @FXML private Label lblTotalAbiertas;
    @FXML private Label lblAbiertasDetalle;
    @FXML private Label lblTotalBorradores;
    @FXML private Label lblTotalCerradas;
    @FXML private Label lblTotalVotos;
    @FXML private Label lblContadorFiltrados;

    @FXML private TextField txtBuscar;
    @FXML private ComboBox<String> comboEstado;

    @FXML private TableView<VotacionModel> tablaVotaciones;
    @FXML private TableColumn<VotacionModel, String> colTitulo;
    @FXML private TableColumn<VotacionModel, String> colProyecto;
    @FXML private TableColumn<VotacionModel, String> colFechas;
    @FXML private TableColumn<VotacionModel, String> colOpcionesVotos;
    @FXML private TableColumn<VotacionModel, String> colEstado;
    @FXML private TableColumn<VotacionModel, Void> colAcciones;

    @FXML private VBox pnlDetalleOpciones;
    @FXML private Label lblDetalleTitulo;
    @FXML private Label lblDetalleEstado;
    @FXML private FlowPane flowOpciones;

    private final VotacionApiClient apiClient = new VotacionApiClient();

    @FXML
    public void initialize() {
        boolean canManage = canManage();
        btnNuevaVotacion.setVisible(canManage);
        btnNuevaVotacion.setManaged(canManage);

        configurarFiltros();
        configurarTabla();
        cargarDatos();
    }

    private void configurarFiltros() {
        comboEstado.setItems(FXCollections.observableArrayList("TODOS", "ABIERTA", "BORRADOR", "CERRADA", "CANCELADA"));
        comboEstado.setValue("TODOS");
        comboEstado.valueProperty().addListener((obs, oldVal, newVal) -> cargarDatos());

        txtBuscar.textProperty().addListener((obs, oldVal, newVal) -> cargarDatos());
    }

    private void configurarTabla() {
        colTitulo.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getTitulo()));
        colTitulo.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    VotacionModel m = getTableRow().getItem();
                    VBox box = new VBox(2);
                    Label lblTitulo = new Label(m.getTitulo());
                    lblTitulo.setStyle("-fx-font-weight: bold; -fx-text-fill: #1e293b;");
                    String desc = m.getDescripcion();
                    if (desc != null && !desc.isBlank()) {
                        Label lblDesc = new Label(desc);
                        lblDesc.setStyle("-fx-text-fill: #64748b; -fx-font-size: 11px;");
                        box.getChildren().addAll(lblTitulo, lblDesc);
                    } else {
                        box.getChildren().add(lblTitulo);
                    }
                    setGraphic(box);
                    setText(null);
                }
            }
        });

        colProyecto.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getProyectoDisplay()));
        colProyecto.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    Label lbl = new Label(item);
                    if ("Consulta General".equalsIgnoreCase(item)) {
                        lbl.setStyle("-fx-background-color: #f1f5f9; -fx-text-fill: #475569; -fx-padding: 2 8 2 8; -fx-background-radius: 6; -fx-font-size: 11px;");
                    } else {
                        lbl.setStyle("-fx-background-color: #eff6ff; -fx-text-fill: #1d4ed8; -fx-padding: 2 8 2 8; -fx-background-radius: 6; -fx-font-size: 11px; -fx-font-weight: bold;");
                    }
                    setGraphic(lbl);
                    setText(null);
                }
            }
        });

        colFechas.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getRangoFechas()));

        colOpcionesVotos.setCellValueFactory(cellData -> {
            VotacionModel m = cellData.getValue();
            return new SimpleStringProperty(m.getTotalOpciones() + " opciones / " + m.getTotalVotos() + " votos");
        });

        colEstado.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getEstado()));
        colEstado.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    Label badge = new Label(item);
                    String style;
                    switch (item.toUpperCase()) {
                        case "ABIERTA" -> style = "-fx-background-color: #dcfce7; -fx-text-fill: #15803d; -fx-font-weight: bold; -fx-padding: 3 8 3 8; -fx-background-radius: 6;";
                        case "BORRADOR", "PROGRAMADA" -> style = "-fx-background-color: #fef3c7; -fx-text-fill: #b45309; -fx-font-weight: bold; -fx-padding: 3 8 3 8; -fx-background-radius: 6;";
                        case "CERRADA" -> style = "-fx-background-color: #f1f5f9; -fx-text-fill: #475569; -fx-font-weight: bold; -fx-padding: 3 8 3 8; -fx-background-radius: 6;";
                        case "CANCELADA" -> style = "-fx-background-color: #fee2e2; -fx-text-fill: #b91c1c; -fx-font-weight: bold; -fx-padding: 3 8 3 8; -fx-background-radius: 6;";
                        default -> style = "-fx-background-color: #e2e8f0; -fx-text-fill: #334155; -fx-padding: 3 8 3 8; -fx-background-radius: 6;";
                    }
                    badge.setStyle(style);
                    setGraphic(badge);
                    setText(null);
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
                    VotacionModel m = getTableRow().getItem();
                    HBox box = new HBox(6);
                    box.setAlignment(Pos.CENTER_LEFT);

                    if (canManage()) {
                        if (m.isBorrador()) {
                            Button btnAbrir = new Button("Abrir");
                            btnAbrir.setStyle("-fx-background-color: #10b981; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 3 8 3 8; -fx-background-radius: 4; -fx-cursor: hand;");
                            btnAbrir.setOnAction(e -> abrirVotacion(m));

                            Button btnEditar = new Button("Editar");
                            btnEditar.setStyle("-fx-background-color: #f1f5f9; -fx-text-fill: #334155; -fx-padding: 3 8 3 8; -fx-background-radius: 4; -fx-cursor: hand;");
                            btnEditar.setOnAction(e -> abrirFormularioEditar(m));

                            Button btnEliminar = new Button("Eliminar");
                            btnEliminar.setStyle("-fx-background-color: #fee2e2; -fx-text-fill: #b91c1c; -fx-padding: 3 8 3 8; -fx-background-radius: 4; -fx-cursor: hand;");
                            btnEliminar.setOnAction(e -> eliminarVotacion(m));

                            box.getChildren().addAll(btnAbrir, btnEditar, btnEliminar);
                        } else if (m.isAbierta()) {
                            Button btnCerrar = new Button("Cerrar votación");
                            btnCerrar.setStyle("-fx-background-color: #ef4444; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 3 8 3 8; -fx-background-radius: 4; -fx-cursor: hand;");
                            btnCerrar.setOnAction(e -> cerrarVotacion(m));
                            box.getChildren().add(btnCerrar);
                        }
                    }

                    Button btnDetalle = new Button("Ver opciones");
                    btnDetalle.setStyle("-fx-background-color: #e0e7ff; -fx-text-fill: #4338ca; -fx-padding: 3 8 3 8; -fx-background-radius: 4; -fx-cursor: hand;");
                    btnDetalle.setOnAction(e -> mostrarDetalle(m));
                    box.getChildren().add(btnDetalle);

                    if (canManage()) {
                        Button btnOpciones = new Button("Opciones");
                        btnOpciones.setStyle("-fx-background-color: #f1f5f9; -fx-text-fill: #1e293b; -fx-padding: 3 8 3 8; -fx-background-radius: 4; -fx-cursor: hand;");
                        btnOpciones.setOnAction(e -> abrirModalOpciones(m));
                        box.getChildren().add(btnOpciones);
                    }

                    setGraphic(box);
                }
            }
        });

        tablaVotaciones.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                mostrarDetalle(newVal);
            }
        });
    }

    @FXML
    public void cargarDatos() {
        String estado = comboEstado.getValue();
        String busqueda = txtBuscar.getText();

        Task<List<VotacionModel>> task = new Task<>() {
            @Override
            protected List<VotacionModel> call() throws Exception {
                return apiClient.findFiltered(estado, null, busqueda);
            }
        };

        task.setOnSucceeded(e -> {
            List<VotacionModel> lista = task.getValue();
            tablaVotaciones.setItems(FXCollections.observableArrayList(lista));
            lblContadorFiltrados.setText(lista.size() + " procesos registrados");

            actualizarResumen(lista);

            if (!lista.isEmpty()) {
                tablaVotaciones.getSelectionModel().selectFirst();
            } else {
                limpiarDetalle();
            }
        });

        task.setOnFailed(e -> {
            lblContadorFiltrados.setText("Error al cargar votaciones");
        });

        new Thread(task).start();
    }

    private void actualizarResumen(List<VotacionModel> lista) {
        long abiertas = lista.stream().filter(VotacionModel::isAbierta).count();
        long borradores = lista.stream().filter(VotacionModel::isBorrador).count();
        long cerradas = lista.stream().filter(VotacionModel::isCerrada).count();
        int totalVotos = lista.stream().mapToInt(VotacionModel::getTotalVotos).sum();

        lblTotalAbiertas.setText(abiertas + " activas");
        lblTotalBorradores.setText(borradores + " procesos");
        lblTotalCerradas.setText(cerradas + " concluidas");
        lblTotalVotos.setText(totalVotos + " votos");

        if (abiertas > 0) {
            lblAbiertasDetalle.setText("Recepción de votos en curso");
        } else {
            lblAbiertasDetalle.setText("Sin votaciones abiertas");
        }
    }

    private void mostrarDetalle(VotacionModel m) {
        lblDetalleTitulo.setText("Opciones y resultados: " + m.getTitulo());
        lblDetalleEstado.setText("Estado: " + m.getEstado() + " | Total votos: " + m.getTotalVotos());
        flowOpciones.getChildren().clear();

        if (m.getOpciones().isEmpty()) {
            Label lblVacio = new Label("No hay opciones registradas en esta votación.");
            lblVacio.setStyle("-fx-text-fill: #64748b; -fx-font-style: italic;");
            flowOpciones.getChildren().add(lblVacio);
            return;
        }

        int total = m.getTotalVotos();
        for (VotacionModel.OpcionModel op : m.getOpciones()) {
            VBox card = new VBox(4);
            card.setStyle("-fx-background-color: white; -fx-border-color: #cbd5e1; -fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 10 14 10 14; -fx-min-width: 170;");

            Label lblDesc = new Label(op.getDescripcion());
            lblDesc.setStyle("-fx-font-weight: bold; -fx-text-fill: #1e293b;");

            int votos = op.getVotos();
            double pct = total > 0 ? ((double) votos / total) * 100.0 : 0.0;
            Label lblVotos = new Label(votos + " votos (" + String.format("%.1f", pct) + "%)");
            lblVotos.setStyle("-fx-text-fill: #2563eb; -fx-font-size: 11px;");

            ProgressBar bar = new ProgressBar(total > 0 ? (double) votos / total : 0.0);
            bar.setPrefWidth(150);

            card.getChildren().addAll(lblDesc, lblVotos, bar);
            flowOpciones.getChildren().add(card);
        }
    }

    private void limpiarDetalle() {
        lblDetalleTitulo.setText("Selecciona una votación para ver sus opciones y resultados");
        lblDetalleEstado.setText("");
        flowOpciones.getChildren().clear();
    }

    @FXML
    public void abrirFormularioCrear() {
        abrirModal(null);
    }

    private void abrirFormularioEditar(VotacionModel model) {
        abrirModal(model);
    }

    private void abrirModal(VotacionModel model) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/views/votacion-form.fxml"));
            Parent root = loader.load();

            VotacionFormController ctrl = loader.getController();
            ctrl.setVotacionEdicion(model);
            ctrl.setOnSavedCallback(this::cargarDatos);

            Stage stage = new Stage();
            stage.setTitle(model == null ? "Nueva votación comunal" : "Editar votación comunal");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(root));
            stage.setResizable(false);
            stage.showAndWait();
        } catch (Exception e) {
            e.printStackTrace();
            mostrarAlerta(Alert.AlertType.ERROR, "Error", "No se pudo abrir el formulario: " + e.getMessage());
        }
    }

    public void abrirModalOpciones(VotacionModel v) {
        if (v == null) return;
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/views/opciones-votacion-modal.fxml"));
            Parent root = loader.load();

            OpcionesVotacionModalController ctrl = loader.getController();
            ctrl.setVotacion(v);
            ctrl.setOnCloseCallback(this::cargarDatos);

            Stage stage = new Stage();
            stage.setTitle("Opciones de votación: " + v.getTitulo());
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(root));
            stage.setResizable(false);
            stage.showAndWait();
        } catch (Exception e) {
            e.printStackTrace();
            mostrarAlerta(Alert.AlertType.ERROR, "Error", "No se pudo abrir el gestor de opciones: " + e.getMessage());
        }
    }

    private void abrirVotacion(VotacionModel m) {
        if (m.getTotalOpciones() < 2) {
            mostrarAlerta(Alert.AlertType.WARNING, "Requisito de opciones",
                "Para abrir este proceso de votación comunal se requieren al menos dos opciones registradas.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Abrir proceso de votación");
        confirm.setHeaderText("¿Deseas abrir la votación \"" + m.getTitulo() + "\"?");
        confirm.setContentText("Al abrirla, quedará disponible para que los miembros activos puedan emitir sus votos.");

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                Task<Void> task = new Task<>() {
                    @Override
                    protected Void call() throws Exception {
                        apiClient.abrir(m.getId());
                        return null;
                    }
                };
                task.setOnSucceeded(e -> cargarDatos());
                task.setOnFailed(e -> mostrarAlerta(Alert.AlertType.ERROR, "Error",
                    task.getException() != null ? task.getException().getMessage() : "Error al abrir la votación."));
                new Thread(task).start();
            }
        });
    }

    private void cerrarVotacion(VotacionModel m) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Cerrar proceso de votación");
        confirm.setHeaderText("¿Deseas cerrar definitivamente la votación \"" + m.getTitulo() + "\"?");
        confirm.setContentText("ATENCIÓN: El cierre es irreversible. No se admitirán nuevos votos tras el cierre.");

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                Task<Void> task = new Task<>() {
                    @Override
                    protected Void call() throws Exception {
                        apiClient.cerrar(m.getId());
                        return null;
                    }
                };
                task.setOnSucceeded(e -> cargarDatos());
                task.setOnFailed(e -> mostrarAlerta(Alert.AlertType.ERROR, "Error",
                    task.getException() != null ? task.getException().getMessage() : "Error al cerrar la votación."));
                new Thread(task).start();
            }
        });
    }

    private void eliminarVotacion(VotacionModel m) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Eliminar votación");
        confirm.setHeaderText("¿Eliminar la votación en borrador \"" + m.getTitulo() + "\"?");
        confirm.setContentText("Esta acción eliminará el proceso y sus opciones asociadas.");

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                Task<Void> task = new Task<>() {
                    @Override
                    protected Void call() throws Exception {
                        apiClient.delete(m.getId());
                        return null;
                    }
                };
                task.setOnSucceeded(e -> cargarDatos());
                task.setOnFailed(e -> mostrarAlerta(Alert.AlertType.ERROR, "Error",
                    task.getException() != null ? task.getException().getMessage() : "No se pudo eliminar la votación."));
                new Thread(task).start();
            }
        });
    }

    private void mostrarAlerta(Alert.AlertType type, String title, String msg) {
        Platform.runLater(() -> {
            Alert alert = new Alert(type);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(msg);
            alert.showAndWait();
        });
    }

    private boolean canManage() {
        return SessionManager.getInstance().getCurrentUser()
            .map(u -> {
                String r = u.getRole();
                return r != null && ("ADMIN".equalsIgnoreCase(r) || "ADMINISTRADOR".equalsIgnoreCase(r) || "PRESIDENTE".equalsIgnoreCase(r));
            })
            .orElse(false);
    }
}
