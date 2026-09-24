package controller;

import java.io.IOException;
import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
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
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import models.AuthUser;
import models.CargoModel;
import security.SessionManager;
import service.HeroIcon;
import service.ResponsiveWindowService;
import service.CargoApiClient;

public class CargosController {
    @FXML private TableView<CargoModel> tablaCargos;
    @FXML private TableColumn<CargoModel, String>  columnaJerarquia;
    @FXML private TableColumn<CargoModel, String>  columnaNombre;
    @FXML private TableColumn<CargoModel, String>  columnaDescripcion;
    @FXML private TableColumn<CargoModel, Integer> columnaAsignaciones;
    @FXML private TableColumn<CargoModel, String>  columnaEstado;
    @FXML private TableColumn<CargoModel, Void>    columnaAcciones;

    @FXML private Label lblCargosActivos;
    @FXML private Label lblTotalCargos;
    @FXML private Label lblTotalAsignaciones;
    @FXML private Label lblContadorFiltrados;

    @FXML private ComboBox<String> comboEstado;
    @FXML private TextField campoBusqueda;

    @FXML private Button btnNuevoCargo;    // opcional
    @FXML private Button btnEditar;        // opcional
    @FXML private Button btnToggleActivo;  // opcional
    @FXML private Button btnEliminar;      // opcional
    @FXML private Button btnRefrescar;     // opcional

    private final ObservableList<CargoModel> cargos = FXCollections.observableArrayList();
    private FilteredList<CargoModel> cargosFiltrados;
    private boolean puedeGestionar = false;

    @FXML
    private void initialize() {
        AuthUser user = SessionManager.getInstance().requireCurrentUser();
        puedeGestionar = isAdministrator(user.getRole()) || "PRESIDENTE".equalsIgnoreCase(user.getRole());

        if (btnNuevoCargo != null) {
            btnNuevoCargo.setVisible(puedeGestionar);
            btnNuevoCargo.setManaged(puedeGestionar);
        }
        if (btnEditar != null) {
            btnEditar.setVisible(puedeGestionar);
            btnEditar.setManaged(puedeGestionar);
        }
        if (btnToggleActivo != null) {
            btnToggleActivo.setVisible(puedeGestionar);
            btnToggleActivo.setManaged(puedeGestionar);
        }
        if (btnEliminar != null) {
            btnEliminar.setVisible(puedeGestionar);
            btnEliminar.setManaged(puedeGestionar);
        }

        columnaJerarquia.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getNivelDisplay()));
        columnaNombre.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getNombre()));
        columnaDescripcion.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getDescripcion()));
        columnaAsignaciones.setCellValueFactory(cell -> new SimpleIntegerProperty(cell.getValue().getTotalAsignaciones()).asObject());
        columnaEstado.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getEstadoDisplay()));

        columnaEstado.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String estado, boolean empty) {
                super.updateItem(estado, empty);
                if (empty || estado == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(estado);
                    if ("Activo".equalsIgnoreCase(estado)) {
                        setStyle("-fx-text-fill: -color-success-fg; -fx-font-weight: bold;");
                    } else {
                        setStyle("-fx-text-fill: -color-fg-muted; -fx-font-style: italic;");
                    }
                }
            }
        });

        cargosFiltrados = new FilteredList<>(cargos, c -> true);
        tablaCargos.setItems(cargosFiltrados);

        comboEstado.getItems().setAll("TODOS", "ACTIVOS", "INACTIVOS");
        comboEstado.setValue("TODOS");

        comboEstado.valueProperty().addListener((obs, o, n) -> aplicarFiltro());
        campoBusqueda.textProperty().addListener((obs, o, n) -> aplicarFiltro());

        if (btnEditar      != null) btnEditar.disableProperty().bind(tablaCargos.getSelectionModel().selectedItemProperty().isNull());
        if (btnToggleActivo!= null) btnToggleActivo.disableProperty().bind(tablaCargos.getSelectionModel().selectedItemProperty().isNull());
        if (btnEliminar    != null) btnEliminar.disableProperty().bind(tablaCargos.getSelectionModel().selectedItemProperty().isNull());

        tablaCargos.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && btnToggleActivo != null) {
                btnToggleActivo.setText(newVal.isActivo() ? "Desactivar" : "Activar");
            } else if (btnToggleActivo != null) {
                btnToggleActivo.setText("Desactivar");
            }
        });

        tablaCargos.setRowFactory(tv -> {
            TableRow<CargoModel> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && (!row.isEmpty()) && puedeGestionar) {
                    abrirFormularioEditar();
                }
            });
            return row;
        });

        // Columna ACCIONES inline (solo si el usuario puede gestionar)
        if (columnaAcciones != null) {
            columnaAcciones.setCellFactory(col -> new TableCell<>() {
                private final Button btnEdit   = new Button();
                private final Button btnToggle = new Button();
                private final Button btnDel    = new Button();
                private final HBox box = new HBox(8, btnEdit, btnToggle, btnDel);
                {
                    box.getStyleClass().add("row-actions-box");
                    box.setAlignment(Pos.CENTER);

                    btnEdit.getStyleClass().addAll("btn-row-action", "btn-action-edit");
                    btnEdit.setGraphic(HeroIcon.create(HeroIcon.PENCIL, HeroIcon.AMBER_600, 18));
                    btnEdit.setTooltip(new Tooltip("Editar cargo"));
                    btnEdit.setOnAction(e -> { CargoModel item = getTableView().getItems().get(getIndex()); if (item != null) abrirFormulario(item); });

                    btnToggle.getStyleClass().addAll("btn-row-action", "btn-action-view");
                    btnToggle.setTooltip(new Tooltip("Activar / Desactivar"));
                    btnToggle.setOnAction(e -> { CargoModel item = getTableView().getItems().get(getIndex()); if (item != null) { tablaCargos.getSelectionModel().select(item); toggleActivo(); } });

                    btnDel.getStyleClass().addAll("btn-row-action", "btn-action-delete");
                    btnDel.setGraphic(HeroIcon.create(HeroIcon.TRASH, HeroIcon.RED_600, 18));
                    btnDel.setTooltip(new Tooltip("Eliminar cargo"));
                    btnDel.setOnAction(e -> { CargoModel item = getTableView().getItems().get(getIndex()); if (item != null) { tablaCargos.getSelectionModel().select(item); eliminarCargo(); } });
                }

                @Override
                protected void updateItem(Void item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || getIndex() < 0 || getIndex() >= getTableView().getItems().size()) {
                        setGraphic(null);
                    } else {
                        if (!puedeGestionar) { setGraphic(null); return; }
                        CargoModel cargo = getTableView().getItems().get(getIndex());
                        btnToggle.setGraphic(cargo.isActivo()
                            ? HeroIcon.create(HeroIcon.ARCHIVE, HeroIcon.SLATE_700, 18)
                            : HeroIcon.create(HeroIcon.REFRESH, HeroIcon.GREEN_600, 18));
                        btnToggle.setTooltip(new Tooltip(cargo.isActivo() ? "Desactivar cargo" : "Activar cargo"));
                        setGraphic(box);
                    }
                }
            });
        }

        cargarCargos();
    }

    private void cargarCargos() {
        Task<List<CargoModel>> task = new Task<>() {
            @Override
            protected List<CargoModel> call() throws Exception {
                return new CargoApiClient().findAll();
            }
        };

        task.setOnSucceeded(event -> {
            cargos.setAll(task.getValue());
            aplicarFiltro();
            actualizarMetricas();
        });

        task.setOnFailed(event -> {
            tablaCargos.setPlaceholder(new Label("No fue posible obtener los cargos directivos."));
        });

        Thread thread = new Thread(task, "cargar-cargos-api");
        thread.setDaemon(true);
        thread.start();
    }

    private void aplicarFiltro() {
        String estadoSel = comboEstado.getValue();
        String busqueda = normalizar(campoBusqueda.getText());

        cargosFiltrados.setPredicate(c -> {
            if ("ACTIVOS".equalsIgnoreCase(estadoSel) && !c.isActivo()) return false;
            if ("INACTIVOS".equalsIgnoreCase(estadoSel) && c.isActivo()) return false;

            if (!busqueda.isBlank()) {
                return contiene(c.getNombre(), busqueda)
                    || contiene(c.getDescripcion(), busqueda)
                    || contiene(c.getNivelDisplay(), busqueda);
            }
            return true;
        });

        int visibles = cargosFiltrados.size();
        lblContadorFiltrados.setText(visibles == cargos.size()
            ? cargos.size() + " cargos registrados"
            : visibles + " de " + cargos.size() + " cargos");
    }

    private void actualizarMetricas() {
        long activos = cargos.stream().filter(CargoModel::isActivo).count();
        int totalAsignaciones = cargos.stream().mapToInt(CargoModel::getTotalAsignaciones).sum();

        lblCargosActivos.setText(activos + " cargos");
        lblTotalCargos.setText(cargos.size() + " cargos");
        lblTotalAsignaciones.setText(totalAsignaciones + " asignaciones");
    }

    @FXML
    private void abrirFormularioCrear() {
        abrirFormulario(null);
    }

    @FXML
    private void abrirFormularioEditar() {
        CargoModel selected = tablaCargos.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        abrirFormulario(selected);
    }

    private void abrirFormulario(CargoModel cargo) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/views/cargo-form.fxml"));
            Parent root = loader.load();

            CargoFormController controller = loader.getController();
            controller.initData(cargo, this::cargarCargos);

            Stage dialogStage = new Stage();
            dialogStage.setTitle(cargo == null ? "Crear nuevo cargo directivo" : "Editar cargo #" + cargo.getId());
            dialogStage.initModality(Modality.WINDOW_MODAL);
            if (tablaCargos.getScene() != null) {
                dialogStage.initOwner(tablaCargos.getScene().getWindow());
            }
            dialogStage.setScene(new Scene(root));
            ResponsiveWindowService.fitModalStage(dialogStage);
            dialogStage.showAndWait();
        } catch (IOException e) {
            new Alert(Alert.AlertType.ERROR, "Error al abrir formulario de cargo: " + e.getMessage(), ButtonType.OK).showAndWait();
        }
    }

    @FXML
    private void toggleActivo() {
        CargoModel selected = tablaCargos.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        boolean nuevoEstado = !selected.isActivo();
        String accion = nuevoEstado ? "activar" : "desactivar";

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                new CargoApiClient().toggleActivo(selected.getId(), nuevoEstado);
                return null;
            }
        };

        task.setOnSucceeded(e -> cargarCargos());
        task.setOnFailed(e -> new Alert(Alert.AlertType.ERROR, "No se pudo " + accion + " el cargo: " + message(task.getException()), ButtonType.OK).showAndWait());

        Thread thread = new Thread(task, "toggle-activo-cargo");
        thread.setDaemon(true);
        thread.start();
    }

    @FXML
    private void eliminarCargo() {
        CargoModel selected = tablaCargos.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        if (selected.getTotalAsignaciones() > 0) {
            Alert advertencia = new Alert(
                Alert.AlertType.WARNING,
                "El cargo '" + selected.getNombre() + "' tiene " + selected.getTotalAsignaciones() +
                " asignaciones históricas asociadas.\n\nPor integridad comunal, no se puede eliminar físicamente. " +
                "Se recomienda desactivarlo.",
                ButtonType.OK
            );
            advertencia.setHeaderText("No es posible eliminar el cargo");
            if (tablaCargos.getScene() != null) advertencia.initOwner(tablaCargos.getScene().getWindow());
            advertencia.showAndWait();
            return;
        }

        Alert confirm = new Alert(
            Alert.AlertType.CONFIRMATION,
            "¿Estás seguro de eliminar el cargo directivo '" + selected.getNombre() + "'?\nEsta acción no se puede revertir.",
            ButtonType.YES, ButtonType.NO
        );
        confirm.setHeaderText("Confirmar eliminación de cargo directivo");
        if (tablaCargos.getScene() != null) confirm.initOwner(tablaCargos.getScene().getWindow());

        confirm.showAndWait().ifPresent(res -> {
            if (res == ButtonType.YES) {
                Task<Void> task = new Task<>() {
                    @Override
                    protected Void call() throws Exception {
                        new CargoApiClient().delete(selected.getId());
                        return null;
                    }
                };
                task.setOnSucceeded(e -> cargarCargos());
                task.setOnFailed(e -> new Alert(Alert.AlertType.ERROR, "No fue posible eliminar el cargo: " + message(task.getException()), ButtonType.OK).showAndWait());
                Thread thread = new Thread(task, "eliminar-cargo");
                thread.setDaemon(true);
                thread.start();
            }
        });
    }

    @FXML
    private void recargar() {
        cargarCargos();
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
