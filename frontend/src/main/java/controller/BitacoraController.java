package controller;

import java.util.List;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.geometry.Pos;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import models.BitacoraModel;
import service.HeroIcon;
import service.ResponsiveWindowService;
import service.BitacoraApiClient;

public class BitacoraController {
    @FXML private TableView<BitacoraModel> tablaBitacora;
    @FXML private TableColumn<BitacoraModel, String> columnaFecha;
    @FXML private TableColumn<BitacoraModel, String> columnaUsuario;
    @FXML private TableColumn<BitacoraModel, String> columnaAccion;
    @FXML private TableColumn<BitacoraModel, String> columnaEntidad;
    @FXML private TableColumn<BitacoraModel, String> columnaIdRegistro;
    @FXML private TableColumn<BitacoraModel, String> columnaDetalle;
    @FXML private TableColumn<BitacoraModel, Void>   columnaAcciones;

    @FXML private Label lblTotalEventos;
    @FXML private Label lblEstadoModulo;
    @FXML private Label lblPaginacion;

    @FXML private ComboBox<String> comboEntidad;
    @FXML private ComboBox<String> comboAccion;
    @FXML private TextField campoBusqueda;
    @FXML private ComboBox<Integer> comboPageSize;

    @FXML private Button btnVerDetalle;
    @FXML private Button btnRefrescar;
    @FXML private Button btnAnterior;
    @FXML private Button btnSiguiente;

    private final ObservableList<BitacoraModel> eventos = FXCollections.observableArrayList();
    private int currentPage = 1;
    private int pageSize = 5;
    private int totalPages = 1;
    private int totalRegistros = 0;

    @FXML
    private void initialize() {
        columnaFecha.setCellValueFactory(cell -> new SimpleStringProperty(
            cell.getValue().fechaHora() == null ? "-" : cell.getValue().fechaHora()
        ));
        columnaUsuario.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getUsuarioDisplay()));
        columnaAccion.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().accion()));
        columnaEntidad.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().entidad()));
        columnaIdRegistro.setCellValueFactory(cell -> new SimpleStringProperty(
            cell.getValue().idRegistro() == null || cell.getValue().idRegistro().isBlank() ? "-" : cell.getValue().idRegistro()
        ));
        columnaDetalle.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getDetalleCorto()));

        if (columnaAcciones != null) {
            columnaAcciones.setCellFactory(col -> new TableCell<>() {
                private final Button btnEye = new Button();
                private final HBox box = new HBox(8, btnEye);
                {
                    box.getStyleClass().add("row-actions-box");
                    box.setAlignment(Pos.CENTER_RIGHT);

                    btnEye.getStyleClass().addAll("btn-row-action", "btn-action-view");
                    btnEye.setGraphic(HeroIcon.create(HeroIcon.EYE, HeroIcon.BLUE_600, 18));
                    btnEye.setTooltip(new Tooltip("Ver detalle del evento"));
                    btnEye.setOnAction(e -> {
                        BitacoraModel item = getTableView().getItems().get(getIndex());
                        if (item != null) {
                            tablaBitacora.getSelectionModel().select(item);
                            verDetalle();
                        }
                    });
                }

                @Override
                protected void updateItem(Void item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || getIndex() < 0 || getIndex() >= getTableView().getItems().size()) {
                        setGraphic(null);
                    } else {
                        setAlignment(Pos.CENTER_RIGHT);
                        setGraphic(box);
                    }
                }
            });
        }

        tablaBitacora.setItems(eventos);

        comboEntidad.getItems().setAll("TODAS", "USUARIO", "ROL", "MIEMBRO", "VIVIENDA", "PROYECTO", "AUTH", "SISTEMA");
        comboEntidad.setValue("TODAS");

        comboAccion.getItems().setAll("TODAS", "CREATE", "UPDATE", "DELETE", "STATE_CHANGE", "LOGIN");
        comboAccion.setValue("TODAS");

        comboEntidad.valueProperty().addListener((obs, oldVal, newVal) -> {
            currentPage = 1;
            loadPage();
        });
        comboAccion.valueProperty().addListener((obs, oldVal, newVal) -> {
            currentPage = 1;
            loadPage();
        });
        campoBusqueda.textProperty().addListener((obs, oldVal, newVal) -> {
            currentPage = 1;
            loadPage();
        });

        if (comboPageSize != null) {
            comboPageSize.getItems().addAll(5, 10, 25, 50);
            comboPageSize.setValue(pageSize);
            comboPageSize.valueProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal != null && newVal != pageSize) {
                    pageSize = newVal;
                    currentPage = 1;
                    loadPage();
                }
            });
        }

        if (btnVerDetalle != null) {
            btnVerDetalle.disableProperty().bind(tablaBitacora.getSelectionModel().selectedItemProperty().isNull());
        }

        tablaBitacora.setRowFactory(tv -> {
            TableRow<BitacoraModel> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && (!row.isEmpty())) {
                    verDetalle();
                }
            });
            return row;
        });

        loadPage();
    }

    @FXML
    private void verDetalle() {
        BitacoraModel selected = tablaBitacora.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Detalle del evento de auditoría #" + selected.idBitacora());
        dialog.setHeaderText("Registro inmutable de bitácora");
        if (tablaBitacora.getScene() != null) {
            dialog.initOwner(tablaBitacora.getScene().getWindow());
        }

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(10);
        grid.setStyle("-fx-padding: 16px;");

        grid.add(new Label("ID de Evento:"), 0, 0);
        grid.add(new Label(String.valueOf(selected.idBitacora())), 1, 0);

        grid.add(new Label("Fecha y Hora:"), 0, 1);
        grid.add(new Label(selected.fechaHora() == null ? "-" : selected.fechaHora()), 1, 1);

        grid.add(new Label("Usuario Responsable:"), 0, 2);
        grid.add(new Label(selected.getUsuarioDisplay() + " (ID: " + selected.idUsuario() + ")"), 1, 2);

        grid.add(new Label("Acción:"), 0, 3);
        grid.add(new Label(selected.accion()), 1, 3);

        grid.add(new Label("Entidad Afectada:"), 0, 4);
        grid.add(new Label(selected.entidad() + (selected.idRegistro() != null ? " [ID: " + selected.idRegistro() + "]" : "")), 1, 4);

        grid.add(new Label("Detalle Completo:"), 0, 5);
        TextArea txtDetalle = new TextArea(selected.detalle() == null ? "Sin detalle adicional" : selected.detalle());
        txtDetalle.setEditable(false);
        txtDetalle.setWrapText(true);
        txtDetalle.setPrefRowCount(5);
        GridPane.setHgrow(txtDetalle, Priority.ALWAYS);
        grid.add(txtDetalle, 0, 6, 2, 1);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        ResponsiveWindowService.fitDialog(dialog, tablaBitacora.getScene().getWindow(), 620);
        dialog.showAndWait();
    }

    @FXML
    private void recargar() {
        loadPage();
    }

    @FXML
    private void paginaAnterior() {
        if (currentPage > 1) {
            currentPage--;
            loadPage();
        }
    }

    @FXML
    private void paginaSiguiente() {
        if (currentPage < totalPages) {
            currentPage++;
            loadPage();
        }
    }

    private void loadPage() {
        String usuario = campoBusqueda.getText();
        String entidad = comboEntidad.getValue();
        String accion = comboAccion.getValue();

        lblEstadoModulo.setText("Cargando bitácora...");
        Task<BitacoraModel.Page> task = new Task<>() {
            @Override
            protected BitacoraModel.Page call() throws Exception {
                return new BitacoraApiClient().findPage(usuario, entidad, accion, null, null, currentPage, pageSize);
            }
        };

        task.setOnSucceeded(event -> {
            BitacoraModel.Page page = task.getValue();
            eventos.setAll(page.items());
            totalRegistros = page.total();
            totalPages = Math.max(1, page.totalPages());
            currentPage = page.page();

            lblTotalEventos.setText(totalRegistros + " eventos");
            lblEstadoModulo.setText("Inmutable • Solo lectura");
            lblPaginacion.setText("Pág. " + currentPage + " de " + totalPages);

            btnAnterior.setDisable(currentPage <= 1);
            btnSiguiente.setDisable(currentPage >= totalPages);
            tablaBitacora.refresh();

            double fixedCellSize = 50.0;
            tablaBitacora.setFixedCellSize(fixedCellSize);
            double headerHeight = 47.0;
            double rowCount = Math.max(1, eventos.size());
            double targetHeight = headerHeight + (rowCount * fixedCellSize) + 3;
            tablaBitacora.setPrefHeight(targetHeight);
            tablaBitacora.setMinHeight(targetHeight);
            tablaBitacora.setMaxHeight(targetHeight);
        });

        task.setOnFailed(event -> {
            lblEstadoModulo.setText("Error al consultar bitácora");
            tablaBitacora.setPlaceholder(new Label("No fue posible cargar la bitácora: " + message(task.getException())));
        });

        Thread thread = new Thread(task, "cargar-bitacora-api");
        thread.setDaemon(true);
        thread.start();
    }

    private String message(Throwable error) {
        return error == null || error.getMessage() == null ? "Error de conexión" : error.getMessage();
    }
}
