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
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import models.MiembroModel;
import models.RolModel;
import models.UsuarioModel;
import security.SessionManager;
import service.HeroIcon;
import service.ResponsiveWindowService;
import service.TablePaginator;
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
    @FXML private TableColumn<UsuarioModel, Void> columnaAcciones;
    @FXML private TextField campoBusqueda;
    @FXML private Label lblTotalUsuarios;
    @FXML private Label lblEstadoModulo;
    @FXML private HBox barraPie;
    @FXML private Button btnEditar;         // opcional, puede no estar en FXML
    @FXML private Button btnCambiarEstado;  // opcional
    @FXML private Button btnBloquear;       // opcional
    @FXML private Button btnRestablecerClave; // opcional

    private final ObservableList<UsuarioModel> users = FXCollections.observableArrayList();
    private final Map<Integer, RolModel> roles = new HashMap<>();
    private final Map<Integer, MiembroModel> members = new HashMap<>();
    private FilteredList<UsuarioModel> filtered;
    private TablePaginator<UsuarioModel> paginator;

    @FXML private void initialize() {
        columnaUsuario.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getNombreUsuario()));
        columnaRol.setCellValueFactory(cell -> new SimpleStringProperty(roleName(cell.getValue().getIdRol())));
        columnaMiembro.setCellValueFactory(cell -> new SimpleStringProperty(memberName(cell.getValue().getIdMiembro())));
        
        columnaEstado.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getEstado()));
        columnaEstado.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String estado, boolean empty) {
                super.updateItem(estado, empty);
                if (empty || estado == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    Label badge = new Label(estado);
                    if ("ACTIVO".equalsIgnoreCase(estado)) {
                        badge.setStyle("-fx-background-color: #dcfce7; -fx-text-fill: #15803d; -fx-font-weight: bold; -fx-padding: 2 8 2 8; -fx-background-radius: 6; -fx-font-size: 11px;");
                    } else if ("BLOQUEADO".equalsIgnoreCase(estado)) {
                        badge.setStyle("-fx-background-color: #fee2e2; -fx-text-fill: #b91c1c; -fx-font-weight: bold; -fx-padding: 2 8 2 8; -fx-background-radius: 6; -fx-font-size: 11px;");
                    } else {
                        badge.setStyle("-fx-background-color: #f1f5f9; -fx-text-fill: #64748b; -fx-font-weight: bold; -fx-padding: 2 8 2 8; -fx-background-radius: 6; -fx-font-size: 11px;");
                    }
                    setGraphic(badge);
                    setText(null);
                }
            }
        });

        columnaUltimoAcceso.setCellValueFactory(cell -> new SimpleStringProperty(
            cell.getValue().getUltimoAcceso() == null ? "Nunca" : cell.getValue().getUltimoAcceso()));
        
        filtered = new FilteredList<>(users, value -> true);
        paginator = new TablePaginator<>(tablaUsuarios, filtered, "usuarios", 5);
        if (barraPie != null) {
            paginator.attachTo(barraPie);
        }
        campoBusqueda.textProperty().addListener((obs, previous, value) -> filter(value));

        // Botones del hero son opcionales (pueden no estar en el FXML si se usan inline)
        if (btnEditar != null) btnEditar.disableProperty().bind(tablaUsuarios.getSelectionModel().selectedItemProperty().isNull());
        if (btnCambiarEstado != null) btnCambiarEstado.disableProperty().bind(tablaUsuarios.getSelectionModel().selectedItemProperty().isNull());
        if (btnBloquear != null) btnBloquear.disableProperty().bind(tablaUsuarios.getSelectionModel().selectedItemProperty().isNull());
        if (btnRestablecerClave != null) btnRestablecerClave.disableProperty().bind(tablaUsuarios.getSelectionModel().selectedItemProperty().isNull());

        tablaUsuarios.getSelectionModel().selectedItemProperty().addListener((obs, previous, selected) -> {
            if (btnCambiarEstado == null && btnBloquear == null) return;
            if (selected == null) {
                if (btnCambiarEstado != null) btnCambiarEstado.setText("Desactivar");
                if (btnBloquear != null) btnBloquear.setText("Bloquear");
            } else {
                if (btnCambiarEstado != null) btnCambiarEstado.setText("INACTIVO".equalsIgnoreCase(selected.getEstado()) ? "Reactivar" : "Desactivar");
                if (btnBloquear != null) btnBloquear.setText("BLOQUEADO".equalsIgnoreCase(selected.getEstado()) ? "Desbloquear" : "Bloquear");
            }
        });

        // Columna ACCIONES inline: Edit (amber pencil), ChangeState (refresh/archive), Block (trash/refresh)
        if (columnaAcciones != null) {
            columnaAcciones.setCellFactory(col -> new TableCell<>() {
                private final Button btnEdit   = new Button();
                private final Button btnKey    = new Button();
                private final Button btnToggle = new Button();
                private final Button btnLock   = new Button();
                private final HBox box = new HBox(8, btnEdit, btnKey, btnToggle, btnLock);
                {
                    box.getStyleClass().add("row-actions-box");
                    box.setAlignment(Pos.CENTER);

                    btnEdit.getStyleClass().addAll("btn-row-action", "btn-action-edit");
                    btnEdit.setGraphic(HeroIcon.create(HeroIcon.PENCIL, HeroIcon.AMBER_600, 18));
                    btnEdit.setTooltip(new Tooltip("Editar usuario"));
                    btnEdit.setOnAction(e -> {
                        UsuarioModel item = getTableView().getItems().get(getIndex());
                        if (item != null) openForm(item);
                    });

                    btnKey.getStyleClass().addAll("btn-row-action", "btn-action-view");
                    btnKey.setGraphic(HeroIcon.create(HeroIcon.KEY, HeroIcon.BLUE_600, 18));
                    btnKey.setTooltip(new Tooltip("Restablecer contraseña"));
                    btnKey.setOnAction(e -> {
                        UsuarioModel item = getTableView().getItems().get(getIndex());
                        if (item != null) { tablaUsuarios.getSelectionModel().select(item); restablecerClave(); }
                    });

                    btnToggle.getStyleClass().addAll("btn-row-action", "btn-action-view");
                    btnToggle.setTooltip(new Tooltip("Cambiar estado"));
                    btnToggle.setOnAction(e -> {
                        UsuarioModel item = getTableView().getItems().get(getIndex());
                        if (item != null) { tablaUsuarios.getSelectionModel().select(item); changeState(); }
                    });

                    btnLock.getStyleClass().addAll("btn-row-action", "btn-action-delete");
                    btnLock.setTooltip(new Tooltip("Bloquear / Desbloquear"));
                    btnLock.setOnAction(e -> {
                        UsuarioModel item = getTableView().getItems().get(getIndex());
                        if (item != null) { tablaUsuarios.getSelectionModel().select(item); bloquearUsuario(); }
                    });
                }

                @Override
                protected void updateItem(Void item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || getIndex() < 0 || getIndex() >= getTableView().getItems().size()) {
                        setGraphic(null);
                    } else {
                        UsuarioModel u = getTableView().getItems().get(getIndex());
                        boolean inactivo = "INACTIVO".equalsIgnoreCase(u.getEstado());
                        boolean bloqueado = "BLOQUEADO".equalsIgnoreCase(u.getEstado());

                        btnToggle.setGraphic(inactivo
                            ? HeroIcon.create(HeroIcon.REFRESH, HeroIcon.GREEN_600, 18)
                            : HeroIcon.create(HeroIcon.ARCHIVE, HeroIcon.SLATE_700, 18));
                        btnToggle.setTooltip(new Tooltip(inactivo ? "Reactivar usuario" : "Desactivar usuario"));

                        btnLock.setGraphic(bloqueado
                            ? HeroIcon.create(HeroIcon.REFRESH, HeroIcon.GREEN_600, 18)
                            : HeroIcon.create(HeroIcon.TRASH, HeroIcon.RED_600, 18));
                        btnLock.setTooltip(new Tooltip(bloqueado ? "Desbloquear usuario" : "Bloquear usuario"));

                        setGraphic(box);
                    }
                }
            });
        }

        loadData();
    }

    @FXML private void create() { openForm(null); }

    @FXML private void edit() {
        UsuarioModel selected = tablaUsuarios.getSelectionModel().getSelectedItem();
        if (selected != null) openForm(selected);
    }

    private boolean esMismoUsuario(UsuarioModel selected) {
        if (selected == null || selected.getNombreUsuario() == null) return false;
        return SessionManager.getInstance().getCurrentUser()
            .map(u -> selected.getNombreUsuario().equalsIgnoreCase(u.getUsername()))
            .orElse(false);
    }

    @FXML private void changeState() {
        UsuarioModel selected = tablaUsuarios.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        if (esMismoUsuario(selected)) {
            showError("No puedes desactivar tu propia cuenta de usuario en sesión.");
            return;
        }

        boolean inactivo = "INACTIVO".equalsIgnoreCase(selected.getEstado());
        String state = inactivo ? "ACTIVO" : "INACTIVO";
        String accion = inactivo ? "reactivar" : "desactivar";

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
            "¿Confirmas que deseas " + accion + " al usuario '" + selected.getNombreUsuario() +
            "'?\nLa cuenta cambiará a estado " + state + ". El registro y su historial se conservarán.",
            ButtonType.YES, ButtonType.NO);
        alert.setHeaderText("Confirmar cambio de estado");
        if (alert.showAndWait().orElse(ButtonType.NO) != ButtonType.YES) return;
        runChangeState(selected, state);
    }

    @FXML private void bloquearUsuario() {
        UsuarioModel selected = tablaUsuarios.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        if (esMismoUsuario(selected)) {
            showError("No puedes bloquear tu propia cuenta de usuario en sesión.");
            return;
        }

        boolean bloqueado = "BLOQUEADO".equalsIgnoreCase(selected.getEstado());
        String state = bloqueado ? "ACTIVO" : "BLOQUEADO";
        String accion = bloqueado ? "desbloquear y reactivar" : "bloquear";

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
            "¿Confirmas que deseas " + accion + " al usuario '" + selected.getNombreUsuario() +
            "'?\nEl estado pasará a " + state + ".",
            ButtonType.YES, ButtonType.NO);
        alert.setHeaderText("Confirmar " + (bloqueado ? "desbloqueo" : "bloqueo") + " de usuario");
        if (alert.showAndWait().orElse(ButtonType.NO) != ButtonType.YES) return;
        runChangeState(selected, state);
    }

    @FXML private void restablecerClave() {
        UsuarioModel selected = tablaUsuarios.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
            "Se generará una contraseña temporal de un solo uso para el usuario '" + selected.getNombreUsuario() +
            "'. El usuario deberá cambiarla obligatoriamente en su próximo inicio de sesión.\n\n¿Deseas continuar?",
            ButtonType.YES, ButtonType.NO);
        alert.setTitle("Restablecer Contraseña");
        alert.setHeaderText("¿Confirmas el restablecimiento de contraseña?");

        if (alert.showAndWait().orElse(ButtonType.NO) != ButtonType.YES) return;

        lblEstadoModulo.setText("Restableciendo contraseña...");
        if (btnRestablecerClave != null) btnRestablecerClave.setDisable(true);

        Task<UsuarioApiClient.ResetPasswordResult> task = new Task<>() {
            @Override protected UsuarioApiClient.ResetPasswordResult call() throws Exception {
                return new UsuarioApiClient().resetPassword(selected.getIdUsuario());
            }
        };

        task.setOnSucceeded(event -> {
            if (btnRestablecerClave != null) btnRestablecerClave.setDisable(false);
            lblEstadoModulo.setText("Contraseña restablecida");
            mostrarCredencialesRestablecidas(task.getValue());
        });

        task.setOnFailed(event -> {
            if (btnRestablecerClave != null) btnRestablecerClave.setDisable(false);
            lblEstadoModulo.setText("No fue posible restablecer");
            showError("Error al restablecer la contraseña: " + message(task.getException()));
        });

        start(task, "restablecer-clave-api");
    }

    private void mostrarCredencialesRestablecidas(UsuarioApiClient.ResetPasswordResult result) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Contraseña Restablecida Exitosamente");
        alert.setHeaderText("Entregue esta clave temporal al usuario");

        TextArea credentials = new TextArea("Usuario: " + result.nombreUsuario()
            + "\nContraseña temporal: " + result.temporaryPassword()
            + "\n\n(El usuario deberá cambiar su clave al iniciar sesión)");
        credentials.setEditable(false);
        credentials.setWrapText(true);
        credentials.setPrefRowCount(4);
        credentials.setStyle("-fx-font-family: monospace; -fx-font-size: 13px;");

        alert.getDialogPane().setContent(credentials);
        alert.setContentText(null);
        alert.showAndWait();
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
            paginator.updatePagination();
            lblEstadoModulo.setText("Conectado al servidor");
        });
        task.setOnFailed(event -> {
            users.clear();
            paginator.updatePagination();
            lblEstadoModulo.setText("Sin conexión al servidor");
            tablaUsuarios.setPlaceholder(new Label("No fue posible obtener los usuarios. " + message(task.getException())));
        });
        start(task, "cargar-usuarios-api");
    }

    private void replace(UsuarioModel original, UsuarioModel updated) {
        int index = original == null ? -1 : users.indexOf(original);
        if (index >= 0) users.set(index, updated); else users.add(updated);
        paginator.updatePagination();
        tablaUsuarios.getSelectionModel().select(updated);
    }

    private void filter(String text) {
        String query = normalize(text);
        filtered.setPredicate(user -> query.isBlank() || normalize(user.getNombreUsuario()).contains(query)
            || normalize(roleName(user.getIdRol())).contains(query) || normalize(memberName(user.getIdMiembro())).contains(query)
            || normalize(user.getEstado()).contains(query));
        paginator.updatePagination();
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
