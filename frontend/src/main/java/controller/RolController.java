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
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import models.RolModel;
import service.RolApiClient;
import service.RolApiClient.RolRequest;

public class RolController {
    @FXML private TableView<RolModel> tablaRoles;
    @FXML private TableColumn<RolModel, Number> columnaId;
    @FXML private TableColumn<RolModel, String> columnaNombre;
    @FXML private TableColumn<RolModel, String> columnaDescripcion;
    @FXML private TableColumn<RolModel, Number> columnaUsuarios;
    @FXML private TextField campoBusqueda;
    @FXML private Label lblTotalRoles;
    @FXML private Label lblEstadoModulo;
    @FXML private Button btnEditar;
    @FXML private Button btnEliminar;

    private final ObservableList<RolModel> roles = FXCollections.observableArrayList();
    private FilteredList<RolModel> filtered;

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
        tablaRoles.setItems(filtered);

        campoBusqueda.textProperty().addListener((obs, previous, value) -> filter(value));
        btnEditar.disableProperty().bind(tablaRoles.getSelectionModel().selectedItemProperty().isNull());
        btnEliminar.disableProperty().bind(tablaRoles.getSelectionModel().selectedItemProperty().isNull());

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
            dialog.getDialogPane().setPrefWidth(480);

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
            tablaRoles.refresh();
            lblEstadoModulo.setText("Conectado al servidor");
            updateTotal();
        });
        task.setOnFailed(event -> {
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
        tablaRoles.getSelectionModel().select(updated);
        tablaRoles.refresh();
        updateTotal();
    }

    private void filter(String text) {
        String query = normalize(text);
        filtered.setPredicate(rol -> query.isBlank()
            || normalize(rol.nombre()).contains(query)
            || (rol.descripcion() != null && normalize(rol.descripcion()).contains(query))
            || String.valueOf(rol.idRol()).contains(query)
        );
        updateTotal();
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
