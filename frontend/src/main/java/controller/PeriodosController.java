package controller;

import java.io.IOException;
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
import models.PeriodoDirectivaModel;
import security.SessionManager;
import service.ResponsiveWindowService;
import service.PeriodoApiClient;

public class PeriodosController {
    @FXML private TableView<PeriodoDirectivaModel> tablaPeriodos;
    @FXML private TableColumn<PeriodoDirectivaModel, String> columnaNombre;
    @FXML private TableColumn<PeriodoDirectivaModel, String> columnaInicio;
    @FXML private TableColumn<PeriodoDirectivaModel, String> columnaFin;
    @FXML private TableColumn<PeriodoDirectivaModel, String> columnaRango;
    @FXML private TableColumn<PeriodoDirectivaModel, String> columnaEstado;

    @FXML private Label lblPeriodoActivo;
    @FXML private Label lblVigenciaActiva;
    @FXML private Label lblTotalPlanificados;
    @FXML private Label lblTotalFinalizados;
    @FXML private Label lblContadorFiltrados;

    @FXML private ComboBox<String> comboEstado;
    @FXML private TextField campoBusqueda;

    @FXML private Button btnNuevoPeriodo;
    @FXML private Button btnActivar;
    @FXML private Button btnFinalizar;
    @FXML private Button btnEditar;
    @FXML private Button btnEliminar;
    @FXML private Button btnRefrescar;

    private final ObservableList<PeriodoDirectivaModel> periodos = FXCollections.observableArrayList();
    private FilteredList<PeriodoDirectivaModel> periodosFiltrados;
    private boolean puedeGestionar = false;

    @FXML
    private void initialize() {
        AuthUser user = SessionManager.getInstance().requireCurrentUser();
        puedeGestionar = isAdministrator(user.getRole()) || "PRESIDENTE".equalsIgnoreCase(user.getRole());

        btnNuevoPeriodo.setVisible(puedeGestionar);
        btnNuevoPeriodo.setManaged(puedeGestionar);
        btnActivar.setVisible(puedeGestionar);
        btnActivar.setManaged(puedeGestionar);
        btnFinalizar.setVisible(puedeGestionar);
        btnFinalizar.setManaged(puedeGestionar);
        btnEditar.setVisible(puedeGestionar);
        btnEditar.setManaged(puedeGestionar);
        btnEliminar.setVisible(puedeGestionar);
        btnEliminar.setManaged(puedeGestionar);

        columnaNombre.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getNombre()));
        columnaInicio.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getFechaInicio()));
        columnaFin.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getFechaFin()));
        columnaRango.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getRangoFechas()));
        columnaEstado.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getEstado()));

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
                        case "ACTIVO" -> setStyle("-fx-text-fill: -color-success-fg; -fx-font-weight: bold;");
                        case "PLANIFICADO" -> setStyle("-fx-text-fill: -color-accent-fg; -fx-font-weight: bold;");
                        case "FINALIZADO" -> setStyle("-fx-text-fill: -color-fg-muted; -fx-font-style: italic;");
                        default -> setStyle("");
                    }
                }
            }
        });

        periodosFiltrados = new FilteredList<>(periodos, p -> true);
        tablaPeriodos.setItems(periodosFiltrados);

        comboEstado.getItems().setAll("TODOS", "ACTIVO", "PLANIFICADO", "FINALIZADO");
        comboEstado.setValue("TODOS");

        comboEstado.valueProperty().addListener((obs, o, n) -> aplicarFiltro());
        campoBusqueda.textProperty().addListener((obs, o, n) -> aplicarFiltro());

        tablaPeriodos.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, selected) -> {
            if (selected == null) {
                btnActivar.setDisable(true);
                btnFinalizar.setDisable(true);
                btnEditar.setDisable(true);
                btnEliminar.setDisable(true);
            } else {
                String st = selected.getEstado() != null ? selected.getEstado().toUpperCase() : "";
                btnActivar.setDisable("ACTIVO".equals(st) || "FINALIZADO".equals(st));
                btnFinalizar.setDisable(!"ACTIVO".equals(st));
                btnEditar.setDisable("FINALIZADO".equals(st));
                btnEliminar.setDisable(!"PLANIFICADO".equals(st));
            }
        });

        tablaPeriodos.setRowFactory(tv -> {
            TableRow<PeriodoDirectivaModel> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && (!row.isEmpty()) && puedeGestionar) {
                    abrirFormularioEditar();
                }
            });
            return row;
        });

        cargarPeriodos();
    }

    private void cargarPeriodos() {
        Task<List<PeriodoDirectivaModel>> task = new Task<>() {
            @Override
            protected List<PeriodoDirectivaModel> call() throws Exception {
                return new PeriodoApiClient().findAll();
            }
        };

        task.setOnSucceeded(event -> {
            periodos.setAll(task.getValue());
            aplicarFiltro();
            actualizarMetricas();
        });

        task.setOnFailed(event -> {
            tablaPeriodos.setPlaceholder(new Label("No fue posible obtener los períodos de directiva."));
        });

        Thread thread = new Thread(task, "cargar-periodos-api");
        thread.setDaemon(true);
        thread.start();
    }

    private void aplicarFiltro() {
        String estadoSel = comboEstado.getValue();
        String busqueda = normalizar(campoBusqueda.getText());

        periodosFiltrados.setPredicate(p -> {
            if (estadoSel != null && !"TODOS".equalsIgnoreCase(estadoSel)) {
                if (p.getEstado() == null || !p.getEstado().equalsIgnoreCase(estadoSel)) {
                    return false;
                }
            }
            if (!busqueda.isBlank()) {
                return contiene(p.getNombre(), busqueda)
                    || contiene(p.getFechaInicio(), busqueda)
                    || contiene(p.getFechaFin(), busqueda);
            }
            return true;
        });

        int visibles = periodosFiltrados.size();
        lblContadorFiltrados.setText(visibles == periodos.size()
            ? periodos.size() + " períodos registrados"
            : visibles + " de " + periodos.size() + " períodos");
    }

    private void actualizarMetricas() {
        PeriodoDirectivaModel activo = periodos.stream().filter(PeriodoDirectivaModel::isActivo).findFirst().orElse(null);
        if (activo != null) {
            lblPeriodoActivo.setText(activo.getNombre());
            lblVigenciaActiva.setText("Vigencia: " + activo.getRangoFechas());
        } else {
            lblPeriodoActivo.setText("Sin período activo");
            lblVigenciaActiva.setText("No hay directiva en funciones");
        }

        long planificados = periodos.stream().filter(p -> "PLANIFICADO".equalsIgnoreCase(p.getEstado())).count();
        long finalizados = periodos.stream().filter(p -> "FINALIZADO".equalsIgnoreCase(p.getEstado())).count();

        lblTotalPlanificados.setText(planificados + " períodos");
        lblTotalFinalizados.setText(finalizados + " períodos");
    }

    @FXML
    private void abrirFormularioCrear() {
        abrirFormulario(null);
    }

    @FXML
    private void abrirFormularioEditar() {
        PeriodoDirectivaModel selected = tablaPeriodos.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        if ("FINALIZADO".equalsIgnoreCase(selected.getEstado())) {
            new Alert(Alert.AlertType.WARNING, "No es posible editar un período que ya ha finalizado.", ButtonType.OK).showAndWait();
            return;
        }
        abrirFormulario(selected);
    }

    private void abrirFormulario(PeriodoDirectivaModel periodo) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/views/periodo-form.fxml"));
            Parent root = loader.load();

            PeriodoFormController controller = loader.getController();
            controller.initData(periodo, this::cargarPeriodos);

            Stage dialogStage = new Stage();
            dialogStage.setTitle(periodo == null ? "Crear nuevo período directivo" : "Editar período #" + periodo.getId());
            dialogStage.initModality(Modality.WINDOW_MODAL);
            if (tablaPeriodos.getScene() != null) {
                dialogStage.initOwner(tablaPeriodos.getScene().getWindow());
            }
            dialogStage.setScene(new Scene(root));
            ResponsiveWindowService.fitModalStage(dialogStage);
            dialogStage.showAndWait();
        } catch (IOException e) {
            new Alert(Alert.AlertType.ERROR, "Error al abrir formulario de período: " + e.getMessage(), ButtonType.OK).showAndWait();
        }
    }

    @FXML
    private void activarPeriodo() {
        PeriodoDirectivaModel selected = tablaPeriodos.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        Alert confirm = new Alert(
            Alert.AlertType.CONFIRMATION,
            "Al activar el período '" + selected.getNombre() + "', cualquier directiva que esté actualmente activa pasará automáticamente al estado FINALIZADO.\n\n¿Deseas continuar?",
            ButtonType.YES, ButtonType.NO
        );
        confirm.setHeaderText("Confirmar activación de período directivo");
        if (tablaPeriodos.getScene() != null) confirm.initOwner(tablaPeriodos.getScene().getWindow());

        confirm.showAndWait().ifPresent(res -> {
            if (res == ButtonType.YES) {
                Task<Void> task = new Task<>() {
                    @Override
                    protected Void call() throws Exception {
                        new PeriodoApiClient().activar(selected.getId());
                        return null;
                    }
                };
                task.setOnSucceeded(e -> cargarPeriodos());
                task.setOnFailed(e -> new Alert(Alert.AlertType.ERROR, "No se pudo activar el período: " + message(task.getException()), ButtonType.OK).showAndWait());
                Thread thread = new Thread(task, "activar-periodo");
                thread.setDaemon(true);
                thread.start();
            }
        });
    }

    @FXML
    private void finalizarPeriodo() {
        PeriodoDirectivaModel selected = tablaPeriodos.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        Alert confirm = new Alert(
            Alert.AlertType.CONFIRMATION,
            "¿Estás seguro de finalizar el período '" + selected.getNombre() + "'?\nEsta acción es irreversible y concluirá formalmente las funciones de esta junta directiva.",
            ButtonType.YES, ButtonType.NO
        );
        confirm.setHeaderText("Confirmar finalización de período");
        if (tablaPeriodos.getScene() != null) confirm.initOwner(tablaPeriodos.getScene().getWindow());

        confirm.showAndWait().ifPresent(res -> {
            if (res == ButtonType.YES) {
                Task<Void> task = new Task<>() {
                    @Override
                    protected Void call() throws Exception {
                        new PeriodoApiClient().finalizar(selected.getId());
                        return null;
                    }
                };
                task.setOnSucceeded(e -> cargarPeriodos());
                task.setOnFailed(e -> new Alert(Alert.AlertType.ERROR, "No se pudo finalizar el período: " + message(task.getException()), ButtonType.OK).showAndWait());
                Thread thread = new Thread(task, "finalizar-periodo");
                thread.setDaemon(true);
                thread.start();
            }
        });
    }

    @FXML
    private void eliminarPeriodo() {
        PeriodoDirectivaModel selected = tablaPeriodos.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        Alert confirm = new Alert(
            Alert.AlertType.CONFIRMATION,
            "¿Estás seguro de eliminar el período directivo '" + selected.getNombre() + "'?\nEsta acción no se puede deshacer.",
            ButtonType.YES, ButtonType.NO
        );
        confirm.setHeaderText("Confirmar eliminación de período");
        if (tablaPeriodos.getScene() != null) confirm.initOwner(tablaPeriodos.getScene().getWindow());

        confirm.showAndWait().ifPresent(res -> {
            if (res == ButtonType.YES) {
                Task<Void> task = new Task<>() {
                    @Override
                    protected Void call() throws Exception {
                        new PeriodoApiClient().delete(selected.getId());
                        return null;
                    }
                };
                task.setOnSucceeded(e -> cargarPeriodos());
                task.setOnFailed(e -> new Alert(Alert.AlertType.ERROR, "No fue posible eliminar el período: " + message(task.getException()), ButtonType.OK).showAndWait());
                Thread thread = new Thread(task, "eliminar-periodo");
                thread.setDaemon(true);
                thread.start();
            }
        });
    }

    @FXML
    private void recargar() {
        cargarPeriodos();
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
