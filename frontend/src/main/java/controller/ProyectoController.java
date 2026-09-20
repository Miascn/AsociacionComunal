package controller;

import java.io.IOException;
import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
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
import models.AuthUser;
import models.ProyectoModel;
import security.SessionManager;
import service.ResponsiveWindowService;
import service.ProyectoApiClient;

public class ProyectoController {
    @FXML private TableView<ProyectoModel> tablaProyectos;
    @FXML private TableColumn<ProyectoModel, String> columnaNombre;
    @FXML private TableColumn<ProyectoModel, String> columnaDescripcion;
    @FXML private TableColumn<ProyectoModel, String> columnaPresupuesto;
    @FXML private TableColumn<ProyectoModel, String> columnaFecha;
    @FXML private TableColumn<ProyectoModel, String> columnaCreador;
    @FXML private TableColumn<ProyectoModel, String> columnaEstado;

    @FXML private Label lblEnEjecucion;
    @FXML private Label lblAprobadosPropuestos;
    @FXML private Label lblPresupuestoTotal;
    @FXML private Label lblTotalProyectos;

    @FXML private ComboBox<String> comboEstado;
    @FXML private TextField campoBusqueda;

    @FXML private Button btnNuevoProyecto;
    @FXML private Button btnCicloVida;
    @FXML private Button btnEditar;
    @FXML private Button btnEliminar;
    @FXML private Button btnRefrescar;

    private final ObservableList<ProyectoModel> proyectos = FXCollections.observableArrayList();
    private FilteredList<ProyectoModel> proyectosFiltrados;
    private boolean puedeGestionar = false;

    @FXML
    private void initialize() {
        AuthUser user = SessionManager.getInstance().requireCurrentUser();
        puedeGestionar = isAdministrator(user.getRole())
            || "PRESIDENTE".equalsIgnoreCase(user.getRole())
            || "TESORERO".equalsIgnoreCase(user.getRole());

        btnNuevoProyecto.setVisible(puedeGestionar);
        btnNuevoProyecto.setManaged(puedeGestionar);
        btnCicloVida.setVisible(puedeGestionar);
        btnCicloVida.setManaged(puedeGestionar);
        btnEditar.setVisible(puedeGestionar);
        btnEditar.setManaged(puedeGestionar);
        btnEliminar.setVisible(puedeGestionar);
        btnEliminar.setManaged(puedeGestionar);

        columnaNombre.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getNombre()));
        columnaDescripcion.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getDescripcion()));
        columnaPresupuesto.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getPresupuestoFormateado()));
        columnaFecha.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getFechaCreacion()));
        columnaCreador.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getCreadorDisplay()));
        columnaEstado.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getEstado()));

        // Estilo visual del estado
        columnaEstado.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String estado, boolean empty) {
                super.updateItem(estado, empty);
                if (empty || estado == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(estado);
                    switch (estado.toUpperCase()) {
                        case "BORRADOR" -> setStyle("-fx-text-fill: -color-fg-muted; -fx-font-weight: bold;");
                        case "PROPUESTO" -> setStyle("-fx-text-fill: -color-accent-fg; -fx-font-weight: bold;");
                        case "APROBADO" -> setStyle("-fx-text-fill: #0284c7; -fx-font-weight: bold;");
                        case "EN_EJECUCION" -> setStyle("-fx-text-fill: -color-success-fg; -fx-font-weight: bold;");
                        case "FINALIZADO" -> setStyle("-fx-text-fill: #16a34a; -fx-font-weight: bold;");
                        case "RECHAZADO" -> setStyle("-fx-text-fill: -color-danger-fg; -fx-font-style: italic;");
                        default -> setStyle("");
                    }
                }
            }
        });

        proyectosFiltrados = new FilteredList<>(proyectos, p -> true);
        tablaProyectos.setItems(proyectosFiltrados);

        comboEstado.getItems().setAll("TODOS", "BORRADOR", "PROPUESTO", "APROBADO", "EN_EJECUCION", "FINALIZADO", "RECHAZADO");
        comboEstado.setValue("TODOS");

        comboEstado.valueProperty().addListener((obs, o, n) -> aplicarFiltro());
        campoBusqueda.textProperty().addListener((obs, o, n) -> aplicarFiltro());

        btnCicloVida.disableProperty().bind(tablaProyectos.getSelectionModel().selectedItemProperty().isNull());
        btnEditar.disableProperty().bind(tablaProyectos.getSelectionModel().selectedItemProperty().isNull());
        btnEliminar.disableProperty().bind(tablaProyectos.getSelectionModel().selectedItemProperty().isNull());

        tablaProyectos.setRowFactory(tv -> {
            TableRow<ProyectoModel> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && (!row.isEmpty())) {
                    abrirCicloVida();
                }
            });
            return row;
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

        task.setOnSucceeded(event -> {
            proyectos.setAll(task.getValue());
            aplicarFiltro();
            actualizarMetricas();
        });

        task.setOnFailed(event -> {
            tablaProyectos.setPlaceholder(new Label("No fue posible obtener los proyectos comunales."));
        });

        Thread thread = new Thread(task, "cargar-proyectos-api");
        thread.setDaemon(true);
        thread.start();
    }

    private void aplicarFiltro() {
        String estadoSel = comboEstado.getValue();
        String busqueda = normalizar(campoBusqueda.getText());

        proyectosFiltrados.setPredicate(p -> {
            if (estadoSel != null && !"TODOS".equalsIgnoreCase(estadoSel)) {
                if (p.getEstado() == null || !p.getEstado().equalsIgnoreCase(estadoSel)) {
                    return false;
                }
            }
            if (!busqueda.isBlank()) {
                return contiene(p.getNombre(), busqueda)
                    || contiene(p.getDescripcion(), busqueda)
                    || contiene(p.getCreadorDisplay(), busqueda);
            }
            return true;
        });

        int visibles = proyectosFiltrados.size();
        lblTotalProyectos.setText(visibles == proyectos.size()
            ? proyectos.size() + " proyectos registrados"
            : visibles + " de " + proyectos.size() + " proyectos");
    }

    private void actualizarMetricas() {
        long enEjecucion = proyectos.stream().filter(p -> "EN_EJECUCION".equalsIgnoreCase(p.getEstado())).count();
        long aprobadosPropuestos = proyectos.stream()
            .filter(p -> "APROBADO".equalsIgnoreCase(p.getEstado()) || "PROPUESTO".equalsIgnoreCase(p.getEstado()))
            .count();

        BigDecimal presupuestoTotal = proyectos.stream()
            .map(ProyectoModel::getPresupuesto)
            .filter(java.util.Objects::nonNull)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        lblEnEjecucion.setText(enEjecucion + " en curso");
        lblAprobadosPropuestos.setText(aprobadosPropuestos + " iniciativas");
        lblPresupuestoTotal.setText(String.format("$%.2f", presupuestoTotal));
    }

    @FXML
    private void abrirFormularioCrear() {
        abrirFormulario(null);
    }

    @FXML
    private void abrirFormularioEditar() {
        ProyectoModel selected = tablaProyectos.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        if ("FINALIZADO".equalsIgnoreCase(selected.getEstado())) {
            new Alert(Alert.AlertType.WARNING, "No es posible editar un proyecto que ya ha finalizado.", ButtonType.OK).showAndWait();
            return;
        }
        abrirFormulario(selected);
    }

    private void abrirFormulario(ProyectoModel proyecto) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/views/proyecto-form.fxml"));
            Parent root = loader.load();

            ProyectoFormController controller = loader.getController();
            controller.initData(proyecto, this::cargarProyectos);

            Stage dialogStage = new Stage();
            dialogStage.setTitle(proyecto == null ? "Crear nuevo proyecto comunal" : "Editar proyecto #" + proyecto.getId());
            dialogStage.initModality(Modality.WINDOW_MODAL);
            if (tablaProyectos.getScene() != null) {
                dialogStage.initOwner(tablaProyectos.getScene().getWindow());
            }
            dialogStage.setScene(new Scene(root));
            ResponsiveWindowService.fitModalStage(dialogStage);
            dialogStage.showAndWait();
        } catch (IOException e) {
            new Alert(Alert.AlertType.ERROR, "Error al abrir formulario: " + e.getMessage(), ButtonType.OK).showAndWait();
        }
    }

    @FXML
    private void abrirCicloVida() {
        ProyectoModel selected = tablaProyectos.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        CicloVidaProyectoModal.mostrar(
            selected,
            tablaProyectos.getScene() != null ? (Stage) tablaProyectos.getScene().getWindow() : null,
            puedeGestionar,
            this::cargarProyectos
        );
    }

    @FXML
    private void eliminarProyecto() {
        ProyectoModel selected = tablaProyectos.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        if (!"BORRADOR".equalsIgnoreCase(selected.getEstado()) && !"RECHAZADO".equalsIgnoreCase(selected.getEstado())) {
            new Alert(Alert.AlertType.WARNING, "Solo se pueden eliminar proyectos en estado BORRADOR o RECHAZADO.", ButtonType.OK).showAndWait();
            return;
        }

        Alert confirm = new Alert(
            Alert.AlertType.CONFIRMATION,
            "¿Estás seguro de eliminar el proyecto '" + selected.getNombre() + "'?\nEsta acción no se puede deshacer.",
            ButtonType.YES, ButtonType.NO
        );
        confirm.setHeaderText("Confirmar eliminación de proyecto");
        if (tablaProyectos.getScene() != null) {
            confirm.initOwner(tablaProyectos.getScene().getWindow());
        }

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.YES) {
                Task<Void> task = new Task<>() {
                    @Override
                    protected Void call() throws Exception {
                        new ProyectoApiClient().delete(selected.getId());
                        return null;
                    }
                };
                task.setOnSucceeded(e -> cargarProyectos());
                task.setOnFailed(e -> new Alert(Alert.AlertType.ERROR, "No fue posible eliminar el proyecto: " + message(task.getException()), ButtonType.OK).showAndWait());
                Thread thread = new Thread(task, "eliminar-proyecto");
                thread.setDaemon(true);
                thread.start();
            }
        });
    }

    @FXML
    private void recargar() {
        cargarProyectos();
    }

    private static boolean isAdministrator(String role) {
        return role != null && ("ADMIN".equalsIgnoreCase(role) || "ADMINISTRADOR".equalsIgnoreCase(role));
    }

    private boolean contiene(String valor, String criterio) {
        return valor != null && normalizar(valor).contains(criterio);
    }

    private String normalizar(String valor) {
        if (valor == null) return "";
        return Normalizer.normalize(valor, Normalizer.Form.NFD)
            .replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT).trim();
    }

    private String message(Throwable error) {
        return error == null || error.getMessage() == null ? "Error de conexión" : error.getMessage();
    }
}