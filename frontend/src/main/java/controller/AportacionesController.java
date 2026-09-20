package controller;

import java.io.IOException;
import java.math.BigDecimal;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.stage.Modality;
import javafx.stage.Stage;
import models.AportacionModel;
import models.AuthUser;
import security.SessionManager;
import service.ResponsiveWindowService;
import service.AportacionApiClient;

public class AportacionesController {
    @FXML private TableView<AportacionModel> tablaAportaciones;
    @FXML private TableColumn<AportacionModel, String> columnaFecha;
    @FXML private TableColumn<AportacionModel, String> columnaMiembro;
    @FXML private TableColumn<AportacionModel, String> columnaDui;
    @FXML private TableColumn<AportacionModel, String> columnaPeriodo;
    @FXML private TableColumn<AportacionModel, String> columnaMonto;
    @FXML private TableColumn<AportacionModel, String> columnaMetodo;
    @FXML private TableColumn<AportacionModel, String> columnaProyecto;
    @FXML private TableColumn<AportacionModel, String> columnaReferencia;
    @FXML private TableColumn<AportacionModel, String> columnaEstado;

    @FXML private Label lblTotalRecaudado;
    @FXML private Label lblTotalRegistros;
    @FXML private Label lblPeriodoActual;
    @FXML private Label lblPaginacion;

    @FXML private TextField campoFiltroPeriodo;
    @FXML private ComboBox<String> comboMetodo;
    @FXML private ComboBox<String> comboEstado;
    @FXML private TextField campoBusqueda;

    @FXML private Button btnNuevaAportacion;
    @FXML private Button btnVerRecibo;
    @FXML private Button btnEditar;
    @FXML private Button btnAnular;
    @FXML private Button btnRefrescar;
    @FXML private Button btnAnterior;
    @FXML private Button btnSiguiente;

    private final ObservableList<AportacionModel> aportaciones = FXCollections.observableArrayList();
    private int currentPage = 1;
    private final int pageSize = 25;
    private int totalPages = 1;
    private int totalRegistros = 0;
    private BigDecimal totalRecaudado = BigDecimal.ZERO;
    private boolean puedeGestionar = false;

    @FXML
    private void initialize() {
        AuthUser user = SessionManager.getInstance().requireCurrentUser();
        puedeGestionar = isAdministrator(user.getRole()) || "TESORERO".equalsIgnoreCase(user.getRole());

        if (btnNuevaAportacion != null) {
            btnNuevaAportacion.setVisible(false);
            btnNuevaAportacion.setManaged(false);
        }
        if (btnEditar != null) {
            btnEditar.setVisible(false);
            btnEditar.setManaged(false);
        }
        if (btnAnular != null) {
            btnAnular.setVisible(puedeGestionar);
            btnAnular.setManaged(puedeGestionar);
        }

        columnaFecha.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getFechaPago()));
        columnaMiembro.setCellValueFactory(cell -> new SimpleStringProperty(
            cell.getValue().getNombreMiembro() != null ? cell.getValue().getNombreMiembro() : "Miembro #" + cell.getValue().getIdMiembro()
        ));
        columnaDui.setCellValueFactory(cell -> new SimpleStringProperty(
            cell.getValue().getDuiMiembro() != null ? cell.getValue().getDuiMiembro() : "-"
        ));
        columnaPeriodo.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getPeriodoMes()));
        columnaMonto.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getMontoFormateado()));
        columnaMetodo.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getMetodoPago()));
        columnaProyecto.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getProyectoDisplay()));
        columnaReferencia.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getReferenciaDisplay()));
        columnaEstado.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getEstado()));

        // Estilos para la columna de estado
        columnaEstado.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    if ("REGISTRADA".equalsIgnoreCase(item)) {
                        setStyle("-fx-text-fill: -color-success-fg; -fx-font-weight: bold;");
                    } else {
                        setStyle("-fx-text-fill: -color-danger-fg; -fx-font-style: italic;");
                    }
                }
            }
        });

        tablaAportaciones.setItems(aportaciones);

        comboMetodo.getItems().setAll("TODOS", "EFECTIVO", "TRANSFERENCIA", "OTRO");
        comboMetodo.setValue("TODOS");

        comboEstado.getItems().setAll("TODOS", "REGISTRADA", "ANULADA");
        comboEstado.setValue("TODOS");

        comboMetodo.valueProperty().addListener((obs, o, n) -> { currentPage = 1; loadPage(); });
        comboEstado.valueProperty().addListener((obs, o, n) -> { currentPage = 1; loadPage(); });
        campoFiltroPeriodo.textProperty().addListener((obs, o, n) -> { currentPage = 1; loadPage(); });
        campoBusqueda.textProperty().addListener((obs, o, n) -> { currentPage = 1; loadPage(); });

        if (btnVerRecibo != null) {
            btnVerRecibo.disableProperty().bind(tablaAportaciones.getSelectionModel().selectedItemProperty().isNull());
        }
        if (btnEditar != null) {
            btnEditar.disableProperty().bind(tablaAportaciones.getSelectionModel().selectedItemProperty().isNull());
        }
        if (btnAnular != null) {
            btnAnular.disableProperty().bind(tablaAportaciones.getSelectionModel().selectedItemProperty().isNull());
        }

        tablaAportaciones.setRowFactory(tv -> {
            TableRow<AportacionModel> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && (!row.isEmpty())) {
                    verRecibo();
                }
            });
            return row;
        });

        loadPage();
    }

    @FXML
    private void abrirFormularioCrear() {
        abrirFormulario(null);
    }

    @FXML
    private void abrirFormularioEditar() {
        AportacionModel selected = tablaAportaciones.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        if ("ANULADA".equalsIgnoreCase(selected.getEstado())) {
            new Alert(Alert.AlertType.WARNING, "No es posible editar una aportación anulada.", ButtonType.OK).showAndWait();
            return;
        }
        abrirFormulario(selected);
    }

    private void abrirFormulario(AportacionModel aportacion) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/views/aportacion-form.fxml"));
            Parent root = loader.load();

            AportacionFormController controller = loader.getController();
            controller.initData(aportacion, this::loadPage);

            Stage dialogStage = new Stage();
            dialogStage.setTitle(aportacion == null ? "Registrar nueva aportación" : "Editar aportación #" + aportacion.getIdAportacion());
            dialogStage.initModality(Modality.WINDOW_MODAL);
            if (tablaAportaciones.getScene() != null) {
                dialogStage.initOwner(tablaAportaciones.getScene().getWindow());
            }
            dialogStage.setScene(new Scene(root));
            ResponsiveWindowService.fitModalStage(dialogStage);
            dialogStage.showAndWait();
        } catch (IOException e) {
            new Alert(Alert.AlertType.ERROR, "Error al abrir formulario: " + e.getMessage(), ButtonType.OK).showAndWait();
        }
    }

    @FXML
    private void verRecibo() {
        AportacionModel selected = tablaAportaciones.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        ReciboAportacionModal.mostrar(selected, tablaAportaciones.getScene() != null ? (Stage) tablaAportaciones.getScene().getWindow() : null);
    }

    @FXML
    private void anularAportacion() {
        AportacionModel selected = tablaAportaciones.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        if ("ANULADA".equalsIgnoreCase(selected.getEstado())) {
            new Alert(Alert.AlertType.INFORMATION, "Esta aportación ya se encuentra anulada.", ButtonType.OK).showAndWait();
            return;
        }

        Alert confirm = new Alert(
            Alert.AlertType.CONFIRMATION,
            "¿Estás seguro de anular la aportación de " + selected.getMontoFormateado() + " del período " + selected.getPeriodoMes() + "?\nEsta acción no se puede deshacer.",
            ButtonType.YES, ButtonType.NO
        );
        confirm.setHeaderText("Confirmar anulación de aportación");
        if (tablaAportaciones.getScene() != null) {
            confirm.initOwner(tablaAportaciones.getScene().getWindow());
        }

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.YES) {
                Task<Void> task = new Task<>() {
                    @Override
                    protected Void call() throws Exception {
                        new AportacionApiClient().anular(selected.getIdAportacion());
                        return null;
                    }
                };
                task.setOnSucceeded(e -> loadPage());
                task.setOnFailed(e -> new Alert(Alert.AlertType.ERROR, "No fue posible anular la aportación: " + message(task.getException()), ButtonType.OK).showAndWait());
                Thread thread = new Thread(task, "anular-aportacion");
                thread.setDaemon(true);
                thread.start();
            }
        });
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
        String periodo = campoFiltroPeriodo.getText();
        String metodo = comboMetodo.getValue();
        String estado = comboEstado.getValue();
        String busqueda = campoBusqueda.getText();

        Task<AportacionModel.Page> task = new Task<>() {
            @Override
            protected AportacionModel.Page call() throws Exception {
                return new AportacionApiClient().findPage(null, null, periodo, null, null, metodo, estado, busqueda, currentPage, pageSize);
            }
        };

        task.setOnSucceeded(event -> {
            AportacionModel.Page page = task.getValue();
            aportaciones.setAll(page.getItems());
            totalRegistros = page.getTotal();
            totalRecaudado = page.getTotalRecaudado() != null ? page.getTotalRecaudado() : BigDecimal.ZERO;
            totalPages = Math.max(1, page.getTotalPages());
            currentPage = page.getPage();

            lblTotalRecaudado.setText(String.format("$%.2f", totalRecaudado));
            lblTotalRegistros.setText(totalRegistros + " aportaciones");
            lblPeriodoActual.setText(periodo != null && !periodo.isBlank() ? "Período: " + periodo : "Todos los períodos");
            lblPaginacion.setText("Página " + currentPage + " de " + totalPages + " (" + totalRegistros + " registros)");

            btnAnterior.setDisable(currentPage <= 1);
            btnSiguiente.setDisable(currentPage >= totalPages);
            tablaAportaciones.refresh();
        });

        task.setOnFailed(event -> {
            tablaAportaciones.setPlaceholder(new Label("Error al cargar aportaciones: " + message(task.getException())));
        });

        Thread thread = new Thread(task, "cargar-aportaciones-api");
        thread.setDaemon(true);
        thread.start();
    }

    private static boolean isAdministrator(String role) {
        return role != null && ("ADMIN".equalsIgnoreCase(role) || "ADMINISTRADOR".equalsIgnoreCase(role));
    }

    private String message(Throwable error) {
        return error == null || error.getMessage() == null ? "Error de conexión" : error.getMessage();
    }
}
