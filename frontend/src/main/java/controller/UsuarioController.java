package controller;

import java.text.Normalizer;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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
import models.MiembroModel;
import models.RolModel;
import models.UsuarioModel;
import service.ResponsiveWindowService;
import service.MiembroApiClient;
import service.RolApiClient;
import service.UsuarioApiClient;
import service.UsuarioApiClient.UsuarioRequest;

public class UsuarioController {
    @FXML private TableView<UsuarioModel> tablaUsuarios;
    @FXML private TableColumn<UsuarioModel, String> columnaUsuario;
    @FXML private TableColumn<UsuarioModel, String> columnaRol;
    @FXML private TableColumn<UsuarioModel, String> columnaMiembro;
    @FXML private TableColumn<UsuarioModel, String> columnaEstado;
    @FXML private TableColumn<UsuarioModel, String> columnaUltimoAcceso;
    @FXML private TextField campoBusqueda;
    @FXML private Label lblTotalUsuarios;
    @FXML private Label lblEstadoModulo;
    @FXML private Button btnEditar;
    @FXML private Button btnCambiarEstado;

    private final ObservableList<UsuarioModel> users = FXCollections.observableArrayList();
    private final Map<Integer, RolModel> roles = new HashMap<>();
    private final Map<Integer, MiembroModel> members = new HashMap<>();
    private FilteredList<UsuarioModel> filtered;

    @FXML private void initialize() {
        columnaUsuario.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getNombreUsuario()));
        columnaRol.setCellValueFactory(cell -> new SimpleStringProperty(roleName(cell.getValue().getIdRol())));
        columnaMiembro.setCellValueFactory(cell -> new SimpleStringProperty(memberName(cell.getValue().getIdMiembro())));
        columnaEstado.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getEstado()));
        columnaUltimoAcceso.setCellValueFactory(cell -> new SimpleStringProperty(
            cell.getValue().getUltimoAcceso() == null ? "Nunca" : cell.getValue().getUltimoAcceso()));
        filtered = new FilteredList<>(users, value -> true);
        tablaUsuarios.setItems(filtered);
        campoBusqueda.textProperty().addListener((obs, previous, value) -> filter(value));
        btnEditar.disableProperty().bind(tablaUsuarios.getSelectionModel().selectedItemProperty().isNull());
        btnCambiarEstado.disableProperty().bind(tablaUsuarios.getSelectionModel().selectedItemProperty().isNull());
        tablaUsuarios.getSelectionModel().selectedItemProperty().addListener((obs, previous, selected) ->
            btnCambiarEstado.setText(selected != null && "INACTIVO".equals(selected.getEstado()) ? "Reactivar" : "Desactivar"));
        loadData();
    }

    @FXML private void create() { openForm(null); }

    @FXML private void edit() {
        UsuarioModel selected = tablaUsuarios.getSelectionModel().getSelectedItem();
        if (selected != null) openForm(selected);
    }

    @FXML private void changeState() {
        UsuarioModel selected = tablaUsuarios.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        String state = "INACTIVO".equals(selected.getEstado()) ? "ACTIVO" : "INACTIVO";
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
            "La cuenta cambiará a " + state + ". El registro y su historial se conservarán.", ButtonType.YES, ButtonType.NO);
        alert.setHeaderText("¿Confirmas el cambio de estado?");
        if (alert.showAndWait().orElse(ButtonType.NO) != ButtonType.YES) return;
        runChangeState(selected, state);
    }

    private void openForm(UsuarioModel original) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/views/usuario-form.fxml"));
            Parent content = loader.load();
            UsuarioFormController form = loader.getController();
            form.setCatalogs(List.copyOf(roles.values()), List.copyOf(members.values()));
            if (original != null) form.setUsuario(original);
            ButtonType saveType = new ButtonType(original == null ? "Crear usuario" : "Guardar cambios", ButtonBar.ButtonData.OK_DONE);
            Dialog<Void> dialog = new Dialog<>();
            dialog.setTitle(original == null ? "Nuevo usuario" : "Editar usuario");
            dialog.initOwner(tablaUsuarios.getScene().getWindow());
            dialog.getDialogPane().setContent(content);
            dialog.getDialogPane().getButtonTypes().addAll(saveType, ButtonType.CANCEL);
            ResponsiveWindowService.fitDialog(dialog, tablaUsuarios.getScene().getWindow(), 560);
            Button save = (Button) dialog.getDialogPane().lookupButton(saveType);
            save.addEventFilter(ActionEvent.ACTION, event -> {
                event.consume();
                UsuarioRequest request = form.validatedRequest();
                if (request != null) {
                    save.setDisable(true);
                    save(original, request, form, dialog, save);
                }
            });
            dialog.show();
        } catch (Exception exception) {
            showError("No fue posible abrir el formulario: " + exception.getMessage());
        }
    }

    private void save(UsuarioModel original, UsuarioRequest request, UsuarioFormController form, Dialog<Void> dialog, Button button) {
        lblEstadoModulo.setText("Guardando usuario...");
        Task<UsuarioModel> task = new Task<>() {
            @Override protected UsuarioModel call() throws Exception {
                UsuarioApiClient api = new UsuarioApiClient();
                return original == null ? api.create(request) : api.update(original.getIdUsuario(), request);
            }
        };
        task.setOnSucceeded(event -> {
            replace(original, task.getValue());
            lblEstadoModulo.setText("Usuario guardado");
            dialog.close();
        });
        task.setOnFailed(event -> {
            button.setDisable(false);
            lblEstadoModulo.setText("No fue posible guardar");
            form.showError(message(task.getException()));
        });
        start(task, "guardar-usuario-api");
    }

    private void runChangeState(UsuarioModel original, String state) {
        lblEstadoModulo.setText("Actualizando estado...");
        Task<UsuarioModel> task = new Task<>() {
            @Override protected UsuarioModel call() throws Exception {
                return new UsuarioApiClient().changeState(original.getIdUsuario(), state);
            }
        };
        task.setOnSucceeded(event -> {
            replace(original, task.getValue());
            lblEstadoModulo.setText("Estado actualizado");
        });
        task.setOnFailed(event -> {
            lblEstadoModulo.setText("No fue posible cambiar el estado");
            showError(message(task.getException()));
        });
        start(task, "estado-usuario-api");
    }

    private void loadData() {
        lblEstadoModulo.setText("Conectando al servidor...");
        Task<Data> task = new Task<>() {
            @Override protected Data call() throws Exception {
                return new Data(new UsuarioApiClient().findAll(), new RolApiClient().findAll(), new MiembroApiClient().findAll());
            }
        };
        task.setOnSucceeded(event -> {
            roles.clear();
            task.getValue().roles().forEach(role -> roles.put(role.idRol(), role));
            members.clear();
            task.getValue().members().forEach(member -> members.put(member.getIdMiembro(), member));
            users.setAll(task.getValue().users());
            tablaUsuarios.refresh();
            lblEstadoModulo.setText("Conectado al servidor");
            updateTotal();
        });
        task.setOnFailed(event -> {
            lblEstadoModulo.setText("Sin conexión al servidor");
            tablaUsuarios.setPlaceholder(new Label("No fue posible obtener los usuarios. " + message(task.getException())));
        });
        start(task, "cargar-usuarios-api");
    }

    private void replace(UsuarioModel original, UsuarioModel updated) {
        int index = original == null ? -1 : users.indexOf(original);
        if (index >= 0) users.set(index, updated); else users.add(updated);
        tablaUsuarios.getSelectionModel().select(updated);
        tablaUsuarios.refresh();
        updateTotal();
    }

    private void filter(String text) {
        String query = normalize(text);
        filtered.setPredicate(user -> query.isBlank() || normalize(user.getNombreUsuario()).contains(query)
            || normalize(roleName(user.getIdRol())).contains(query) || normalize(memberName(user.getIdMiembro())).contains(query)
            || normalize(user.getEstado()).contains(query));
        updateTotal();
    }

    private String roleName(Integer id) { return id == null || !roles.containsKey(id) ? "Rol " + id : roles.get(id).nombre(); }
    private String memberName(Integer id) {
        if (id == null) return "Sin miembro asociado";
        MiembroModel member = members.get(id);
        return member == null ? "Miembro " + id : (member.getNombres() + " " + member.getApellidos()).trim();
    }
    private void updateTotal() { lblTotalUsuarios.setText(filtered.size() + " usuarios"); }
    private String normalize(String value) {
        if (value == null) return "";
        return Normalizer.normalize(value, Normalizer.Form.NFD).replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT).trim();
    }
    private String message(Throwable error) { return error == null || error.getMessage() == null ? "Error desconocido" : error.getMessage(); }
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
    private record Data(List<UsuarioModel> users, List<RolModel> roles, List<MiembroModel> members) { }
}
