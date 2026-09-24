package controller;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import models.RolModel;
import service.HeroIcon;
import service.ResponsiveWindowService;
import service.RolApiClient;
import service.RolApiClient.RolRequest;
import service.TablePaginator;

public class RolController {
    @FXML private TableView<RolModel> tablaRoles;
    @FXML private TableColumn<RolModel, Number> columnaId;
    @FXML private TableColumn<RolModel, String> columnaNombre;
    @FXML private TableColumn<RolModel, String> columnaDescripcion;
    @FXML private TableColumn<RolModel, Number> columnaUsuarios;
    @FXML private TableColumn<RolModel, Void>   columnaAcciones;
    @FXML private TextField campoBusqueda;
    @FXML private Label lblTotalRoles;
    @FXML private Label lblEstadoModulo;
    @FXML private HBox barraPie;
    @FXML private Button btnEditar;   // opcional
    @FXML private Button btnEliminar; // opcional

    private final ObservableList<RolModel> roles = FXCollections.observableArrayList();
    private FilteredList<RolModel> filtered;
    private TablePaginator<RolModel> paginator;

    @FXML
    private void initialize() {
        columnaId.setCellValueFactory(cell -> new SimpleIntegerProperty(cell.getValue().idRol()));
        columnaNombre.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().nombre()));
        columnaDescripcion.setCellValueFactory(cell -> new SimpleStringProperty(
            cell.getValue().descripcion() == null || cell.getValue().descripcion().isBlank()
                ? "Sin descripción"
                : cell.getValue().descripcion()
        ));
        columnaUsuarios.setCellValueFactory(cell -> new SimpleIntegerProperty(cell.getValue().getUsuariosAsociadosCount()));

        filtered = new FilteredList<>(roles, value -> true);
        paginator = new TablePaginator<>(tablaRoles, filtered, "roles", 5);
        if (barraPie != null) {
            paginator.attachTo(barraPie);
        }

        campoBusqueda.textProperty().addListener((obs, previous, value) -> filter(value));
        if (btnEditar   != null) btnEditar.disableProperty().bind(tablaRoles.getSelectionModel().selectedItemProperty().isNull());
        if (btnEliminar != null) btnEliminar.disableProperty().bind(tablaRoles.getSelectionModel().selectedItemProperty().isNull());

        // Columna ACCIONES inline: Edit (amber pencil), Delete (red trash)
        if (columnaAcciones != null) {
            columnaAcciones.setCellFactory(col -> new TableCell<>() {
                private final Button btnEdit = new Button();
                private final Button btnDel  = new Button();
                private final HBox box = new HBox(8, btnEdit, btnDel);
                {
                    box.getStyleClass().add("row-actions-box");
                    box.setAlignment(Pos.CENTER_RIGHT);

                    btnEdit.getStyleClass().addAll("btn-row-action", "btn-action-edit");
                    btnEdit.setGraphic(HeroIcon.create(HeroIcon.PENCIL, HeroIcon.AMBER_600, 18));
                    btnEdit.setTooltip(new Tooltip("Editar rol"));
                    btnEdit.setOnAction(e -> {
                        RolModel item = getTableView().getItems().get(getIndex());
                        if (item != null) openForm(item);
                    });

                    btnDel.getStyleClass().addAll("btn-row-action", "btn-action-delete");
                    btnDel.setGraphic(HeroIcon.create(HeroIcon.TRASH, HeroIcon.RED_600, 18));
                    btnDel.setTooltip(new Tooltip("Eliminar rol"));
                    btnDel.setOnAction(e -> {
                        RolModel item = getTableView().getItems().get(getIndex());
                        if (item != null) { tablaRoles.getSelectionModel().select(item); delete(); }
                    });
                }

                @Override
                protected void updateItem(Void item, boolean empty) {
                    super.updateItem(item, empty);
                    setAlignment(Pos.CENTER_RIGHT);
                    if (empty || getIndex() < 0 || getIndex() >= getTableView().getItems().size()) {
                        setGraphic(null);
                    } else {
                        setGraphic(box);
                    }
                }
            });
        }

        loadData();
    }

    @FXML
    private void create() {
        openForm(null);
    }

    @FXML
    private void edit() {
        RolModel selected = tablaRoles.getSelectionModel().getSelectedItem();
        if (selected != null) {
            openForm(selected);
        }
    }

    @FXML
    private void delete() {
        RolModel selected = tablaRoles.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
            "¿Estás seguro de eliminar el rol '" + selected.nombre() + "'?\nEsta acción retirará el catálogo si no cuenta con dependencias.",
            ButtonType.YES, ButtonType.NO);
        alert.setHeaderText("Confirmar eliminación de rol");
        if (alert.showAndWait().orElse(ButtonType.NO) != ButtonType.YES) return;

        lblEstadoModulo.setText("Eliminando rol...");
        Task<Boolean> task = new Task<>() {
            @Override
            protected Boolean call() throws Exception {
                return new RolApiClient().delete(selected.idRol());
            }
        };
        task.setOnSucceeded(event -> {
            roles.remove(selected);
            tablaRoles.refresh();
            lblEstadoModulo.setText("Rol eliminado");
            updateTotal();
        });
        task.setOnFailed(event -> {
            lblEstadoModulo.setText("No fue posible eliminar el rol");
            showError("No se pudo eliminar el rol: " + message(task.getException()));
        });
        start(task, "eliminar-rol-api");
    }

    private void openForm(RolModel original) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/views/rol-form.fxml"));
            Parent content = loader.load();
            RolFormController form = loader.getController();
            if (original != null) {
                form.setRol(original);
            }

            ButtonType saveType = new ButtonType(original == null ? "Crear rol" : "Guardar cambios", ButtonBar.ButtonData.OK_DONE);
            Dialog<Void> dialog = new Dialog<>();
            dialog.setTitle(original == null ? "Nuevo rol" : "Editar rol");
            if (tablaRoles.getScene() != null) {
                dialog.initOwner(tablaRoles.getScene().getWindow());
            }
            dialog.getDialogPane().setContent(content);
            dialog.getDialogPane().getButtonTypes().addAll(saveType, ButtonType.CANCEL);
            ResponsiveWindowService.fitDialog(dialog, tablaRoles.getScene().getWindow(), 480);

            Button save = (Button) dialog.getDialogPane().lookupButton(saveType);
            save.addEventFilter(ActionEvent.ACTION, event -> {
                event.consume();
                RolRequest request = form.validatedRequest();
                if (request != null) {
                    save.setDisable(true);
                    saveRole(original, request, form, dialog, save);
                }
            });
            dialog.show();
        } catch (Exception exception) {
            showError("No fue posible abrir el formulario: " + exception.getMessage());
        }
    }

    private void saveRole(RolModel original, RolRequest request, RolFormController form, Dialog<Void> dialog, Button button) {
        lblEstadoModulo.setText("Guardando rol...");
        Task<RolModel> task = new Task<>() {
            @Override
            protected RolModel call() throws Exception {
                RolApiClient api = new RolApiClient();
                return original == null ? api.create(request) : api.update(original.idRol(), request);
            }
        };
        task.setOnSucceeded(event -> {
            replace(original, task.getValue());
            lblEstadoModulo.setText("Rol guardado");
            dialog.close();
        });
        task.setOnFailed(event -> {
            button.setDisable(false);
            lblEstadoModulo.setText("No fue posible guardar el rol");
            form.showError(message(task.getException()));
        });
        start(task, "guardar-rol-api");
    }

    private void loadData() {
        lblEstadoModulo.setText("Cargando roles...");
        Task<List<RolModel>> task = new Task<>() {
            @Override
            protected List<RolModel> call() throws Exception {
                return new RolApiClient().findAll();
            }
        };
        task.setOnSucceeded(event -> {
            roles.setAll(task.getValue());
            paginator.updatePagination();
            lblEstadoModulo.setText("Conectado al servidor");
        });
        task.setOnFailed(event -> {
            roles.clear();
            paginator.updatePagination();
            lblEstadoModulo.setText("Sin conexión al servidor");
            tablaRoles.setPlaceholder(new Label("No fue posible obtener los roles. " + message(task.getException())));
        });
        start(task, "cargar-roles-api");
    }

    private void replace(RolModel original, RolModel updated) {
        int index = original == null ? -1 : roles.indexOf(original);
        if (index >= 0) {
            roles.set(index, updated);
        } else {
            roles.add(updated);
        }
        paginator.updatePagination();
        tablaRoles.getSelectionModel().select(updated);
    }

    private void filter(String text) {
        String query = normalize(text);
        filtered.setPredicate(rol -> query.isBlank()
            || normalize(rol.nombre()).contains(query)
            || (rol.descripcion() != null && normalize(rol.descripcion()).contains(query))
            || String.valueOf(rol.idRol()).contains(query)
        );
        paginator.updatePagination();
    }

    private void updateTotal() {
        lblTotalRoles.setText(filtered.size() + " roles");
    }

    private String normalize(String value) {
        if (value == null) return "";
        return Normalizer.normalize(value, Normalizer.Form.NFD)
            .replaceAll("\\p{M}", "")
            .toLowerCase(Locale.ROOT)
            .trim();
    }

    private String message(Throwable error) {
        return error == null || error.getMessage() == null ? "Error desconocido" : error.getMessage();
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message, ButtonType.OK);
        alert.setHeaderText("Ocurrió un problema");
        alert.showAndWait();
    }

    private static void start(Task<?> task, String name) {
        Thread thread = new Thread(task, name);
        thread.setDaemon(true);
        thread.start();
    }
}
