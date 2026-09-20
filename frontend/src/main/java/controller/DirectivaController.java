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
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import models.AsignacionCargoModel;
import models.AuthUser;
import models.PeriodoDirectivaModel;
import security.SessionManager;
import service.ResponsiveWindowService;
import service.DirectivaApiClient;
import service.PeriodoApiClient;

public class DirectivaController {
    @FXML private TableView<AsignacionCargoModel> tablaDirectiva;
    @FXML private TableColumn<AsignacionCargoModel, String> columnaJerarquia;
    @FXML private TableColumn<AsignacionCargoModel, String> columnaCargo;
    @FXML private TableColumn<AsignacionCargoModel, String> columnaMiembro;
    @FXML private TableColumn<AsignacionCargoModel, String> columnaContacto;
    @FXML private TableColumn<AsignacionCargoModel, String> columnaVigencia;
    @FXML private TableColumn<AsignacionCargoModel, String> columnaEstado;

    @FXML private Label lblNombrePeriodo;
    @FXML private Label lblEstadoPeriodo;
    @FXML private Label lblTotalDirectivos;
    @FXML private Label lblNombrePresidente;
    @FXML private Label lblContadorFiltrados;

    @FXML private ComboBox<PeriodoItem> comboPeriodo;
    @FXML private ComboBox<String> comboEstado;
    @FXML private TextField campoBusqueda;

    @FXML private Button btnAsignarCargo;
    @FXML private Button btnRevocar;
    @FXML private Button btnFinalizar;
    @FXML private Button btnEliminar;
    @FXML private Button btnRefrescar;

    private final ObservableList<AsignacionCargoModel> asignaciones = FXCollections.observableArrayList();
    private FilteredList<AsignacionCargoModel> asignacionesFiltradas;
    private boolean puedeGestionar = false;

    public record PeriodoItem(Integer id, String nombre, String estado) {
        @Override
        public String toString() {
            return nombre;
        }
    }

    @FXML
    private void initialize() {
        AuthUser user = SessionManager.getInstance().requireCurrentUser();
        puedeGestionar = isAdministrator(user.getRole()) || "PRESIDENTE".equalsIgnoreCase(user.getRole());

        btnAsignarCargo.setVisible(puedeGestionar);
        btnAsignarCargo.setManaged(puedeGestionar);
        btnRevocar.setVisible(puedeGestionar);
        btnRevocar.setManaged(puedeGestionar);
        btnFinalizar.setVisible(puedeGestionar);
        btnFinalizar.setManaged(puedeGestionar);
        btnEliminar.setVisible(puedeGestionar);
        btnEliminar.setManaged(puedeGestionar);

        columnaJerarquia.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getJerarquiaDisplay()));
        columnaCargo.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getNombreCargo()));
        columnaMiembro.setCellValueFactory(cell -> new SimpleStringProperty(
            cell.getValue().getNombreMiembro() + (cell.getValue().getDuiMiembro() != null ? " (" + cell.getValue().getDuiMiembro() + ")" : "")
        ));
        columnaContacto.setCellValueFactory(cell -> new SimpleStringProperty(
            cell.getValue().getTelefonoMiembro() != null && !cell.getValue().getTelefonoMiembro().isBlank()
                ? cell.getValue().getTelefonoMiembro() : "Sin teléfono"
        ));
        columnaVigencia.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getVigenciaDisplay()));
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
                        case "REVOCADO" -> setStyle("-fx-text-fill: -color-danger-fg; -fx-font-weight: bold;");
                        case "FINALIZADO" -> setStyle("-fx-text-fill: -color-fg-muted; -fx-font-style: italic;");
                        default -> setStyle("");
                    }
                }
            }
        });

        asignacionesFiltradas = new FilteredList<>(asignaciones, a -> true);
        tablaDirectiva.setItems(asignacionesFiltradas);

        comboEstado.getItems().setAll("TODOS", "ACTIVO", "REVOCADO", "FINALIZADO");
        comboEstado.setValue("TODOS");

        comboEstado.valueProperty().addListener((obs, o, n) -> aplicarFiltro());
        campoBusqueda.textProperty().addListener((obs, o, n) -> aplicarFiltro());

        tablaDirectiva.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, selected) -> {
            if (selected == null) {
                btnRevocar.setDisable(true);
                btnFinalizar.setDisable(true);
                btnEliminar.setDisable(true);
            } else {
                boolean isActivo = "ACTIVO".equalsIgnoreCase(selected.getEstado());
                btnRevocar.setDisable(!isActivo);
                btnFinalizar.setDisable(!isActivo);
                btnEliminar.setDisable(false);
            }
        });

        comboPeriodo.setConverter(new StringConverter<>() {
            @Override
            public String toString(PeriodoItem item) {
                return item != null ? item.nombre() : "";
            }
            @Override
            public PeriodoItem fromString(String string) {
                return null;
            }
        });

        comboPeriodo.valueProperty().addListener((obs, o, n) -> {
            if (n != null) {
                cargarAsignacionesPeriodo(n.id());
            }
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
            List<PeriodoDirectivaModel> list = task.getValue();
            ObservableList<PeriodoItem> items = FXCollections.observableArrayList();

            PeriodoDirectivaModel activo = list.stream().filter(PeriodoDirectivaModel::isActivo).findFirst().orElse(null);
            PeriodoItem selectedItem = null;

            for (PeriodoDirectivaModel p : list) {
                PeriodoItem item = new PeriodoItem(p.getId(), p.getNombre() + " (" + p.getEstado() + ")", p.getEstado());
                items.add(item);
                if (activo != null && p.getId().equals(activo.getId())) {
                    selectedItem = item;
                }
            }

            comboPeriodo.setItems(items);
            if (selectedItem != null) {
                comboPeriodo.setValue(selectedItem);
            } else if (!items.isEmpty()) {
                comboPeriodo.setValue(items.get(0));
            } else {
                lblNombrePeriodo.setText("Sin períodos");
                lblEstadoPeriodo.setText("No hay directivas registradas");
            }
        });

        Thread thread = new Thread(task, "cargar-periodos-combo");
        thread.setDaemon(true);
        thread.start();
    }

    private void cargarAsignacionesPeriodo(Integer idPeriodo) {
        Task<List<AsignacionCargoModel>> task = new Task<>() {
            @Override
            protected List<AsignacionCargoModel> call() throws Exception {
                return new DirectivaApiClient().findDirectivaPeriodo(idPeriodo);
            }
        };

        task.setOnSucceeded(event -> {
            asignaciones.setAll(task.getValue());
            aplicarFiltro();
            actualizarMetricas();
        });

        task.setOnFailed(event -> {
            tablaDirectiva.setPlaceholder(new Label("No fue posible cargar la junta directiva."));
        });

        Thread thread = new Thread(task, "cargar-directiva-periodo");
        thread.setDaemon(true);
        thread.start();
    }

    private void aplicarFiltro() {
        String estadoSel = comboEstado.getValue();
        String busqueda = normalizar(campoBusqueda.getText());

        asignacionesFiltradas.setPredicate(a -> {
            if (estadoSel != null && !"TODOS".equalsIgnoreCase(estadoSel)) {
                if (a.getEstado() == null || !a.getEstado().equalsIgnoreCase(estadoSel)) {
                    return false;
                }
            }
            if (!busqueda.isBlank()) {
                return contiene(a.getNombreMiembro(), busqueda)
                    || contiene(a.getNombreCargo(), busqueda)
                    || contiene(a.getDuiMiembro(), busqueda)
                    || contiene(a.getTelefonoMiembro(), busqueda);
            }
            return true;
        });

        int visibles = asignacionesFiltradas.size();
        lblContadorFiltrados.setText(visibles == asignaciones.size()
            ? asignaciones.size() + " cargos asignados"
            : visibles + " de " + asignaciones.size() + " cargos");
    }

    private void actualizarMetricas() {
        PeriodoItem periodoSel = comboPeriodo.getValue();
        if (periodoSel != null) {
            lblNombrePeriodo.setText(periodoSel.nombre());
            lblEstadoPeriodo.setText("Estado del período: " + periodoSel.estado());
        }

        long directivosActivos = asignaciones.stream().filter(AsignacionCargoModel::isActivo).count();
        lblTotalDirectivos.setText(directivosActivos + " en funciones");

        AsignacionCargoModel pres = asignaciones.stream()
            .filter(a -> a.getNombreCargo() != null && a.getNombreCargo().toLowerCase().contains("presidente") && a.isActivo())
            .findFirst().orElse(null);

        if (pres != null) {
            lblNombrePresidente.setText(pres.getNombreMiembro());
        } else {
            lblNombrePresidente.setText("Sin asignar / Vacante");
        }
    }

    @FXML
    private void abrirModalAsignacion() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/views/asignacion-cargo-form.fxml"));
            Parent root = loader.load();

            AsignacionCargoFormController controller = loader.getController();
            PeriodoItem periodoSel = comboPeriodo.getValue();
            controller.initData(periodoSel != null ? periodoSel.id() : null, this::recargar);

            Stage dialogStage = new Stage();
            dialogStage.setTitle("Asignar cargo en junta directiva");
            dialogStage.initModality(Modality.WINDOW_MODAL);
            if (tablaDirectiva.getScene() != null) {
                dialogStage.initOwner(tablaDirectiva.getScene().getWindow());
            }
            dialogStage.setScene(new Scene(root));
            ResponsiveWindowService.fitModalStage(dialogStage);
            dialogStage.showAndWait();
        } catch (IOException e) {
            new Alert(Alert.AlertType.ERROR, "Error al abrir formulario de asignación: " + e.getMessage(), ButtonType.OK).showAndWait();
        }
    }

    @FXML
    private void abrirModalRevocar() {
        AsignacionCargoModel selected = tablaDirectiva.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/views/revocar-cargo-modal.fxml"));
            Parent root = loader.load();

            RevocarCargoModalController controller = loader.getController();
            controller.initData(selected, this::recargar);

            Stage dialogStage = new Stage();
            dialogStage.setTitle("Revocar asignación de directiva");
            dialogStage.initModality(Modality.WINDOW_MODAL);
            if (tablaDirectiva.getScene() != null) {
                dialogStage.initOwner(tablaDirectiva.getScene().getWindow());
            }
            dialogStage.setScene(new Scene(root));
            ResponsiveWindowService.fitModalStage(dialogStage);
            dialogStage.showAndWait();
        } catch (IOException e) {
            new Alert(Alert.AlertType.ERROR, "Error al abrir modal de revocación: " + e.getMessage(), ButtonType.OK).showAndWait();
        }
    }

    @FXML
    private void finalizarAsignacion() {
        AsignacionCargoModel selected = tablaDirectiva.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        Alert confirm = new Alert(
            Alert.AlertType.CONFIRMATION,
            "¿Deseas finalizar formalmente la asignación de " + selected.getNombreMiembro() + " como " + selected.getNombreCargo() + "?",
            ButtonType.YES, ButtonType.NO
        );
        confirm.setHeaderText("Finalizar cargo directivo");
        if (tablaDirectiva.getScene() != null) confirm.initOwner(tablaDirectiva.getScene().getWindow());

        confirm.showAndWait().ifPresent(res -> {
            if (res == ButtonType.YES) {
                Task<Void> task = new Task<>() {
                    @Override
                    protected Void call() throws Exception {
                        new DirectivaApiClient().finalizar(selected.getId());
                        return null;
                    }
                };
                task.setOnSucceeded(e -> recargar());
                task.setOnFailed(e -> new Alert(Alert.AlertType.ERROR, "No se pudo finalizar la asignación: " + message(task.getException()), ButtonType.OK).showAndWait());
                Thread thread = new Thread(task, "finalizar-asignacion");
                thread.setDaemon(true);
                thread.start();
            }
        });
    }

    @FXML
    private void eliminarAsignacion() {
        AsignacionCargoModel selected = tablaDirectiva.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        Alert confirm = new Alert(
            Alert.AlertType.CONFIRMATION,
            "¿Estás seguro de eliminar el registro de asignación de " + selected.getNombreMiembro() + " como " + selected.getNombreCargo() + "?",
            ButtonType.YES, ButtonType.NO
        );
        confirm.setHeaderText("Confirmar eliminación");
        if (tablaDirectiva.getScene() != null) confirm.initOwner(tablaDirectiva.getScene().getWindow());

        confirm.showAndWait().ifPresent(res -> {
            if (res == ButtonType.YES) {
                Task<Void> task = new Task<>() {
                    @Override
                    protected Void call() throws Exception {
                        new DirectivaApiClient().delete(selected.getId());
                        return null;
                    }
                };
                task.setOnSucceeded(e -> recargar());
                task.setOnFailed(e -> new Alert(Alert.AlertType.ERROR, "No se pudo eliminar la asignación: " + message(task.getException()), ButtonType.OK).showAndWait());
                Thread thread = new Thread(task, "eliminar-asignacion");
                thread.setDaemon(true);
                thread.start();
            }
        });
    }

    @FXML
    private void recargar() {
        PeriodoItem sel = comboPeriodo.getValue();
        if (sel != null) {
            cargarAsignacionesPeriodo(sel.id());
        }
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
