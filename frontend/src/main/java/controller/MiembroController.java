package controller;

import java.io.IOException;
import java.text.Normalizer;
import java.util.List;
import java.util.Locale;

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
import javafx.scene.control.TextArea;
import javafx.scene.control.TableRow;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.input.MouseButton;
import javafx.stage.Stage;
import javafx.stage.Modality;
import javafx.scene.Scene;
import service.MiembroApiClient;
import service.ResponsiveWindowService;
import service.MiembroApiClient.CreateMemberRequest;
import service.MiembroApiClient.CreateMemberResult;
import service.ViviendaApiClient;
import models.MiembroModel;
import security.SessionManager;

public class MiembroController {
    @FXML private TableView<MiembroModel> tablaMiembros;
    @FXML private TableColumn<MiembroModel, String> columnaDui;
    @FXML private TableColumn<MiembroModel, String> columnaNombres;
    @FXML private TableColumn<MiembroModel, String> columnaApellidos;
    @FXML private TableColumn<MiembroModel, String> columnaTelefono;
    @FXML private TableColumn<MiembroModel, String> columnaCorreo;
    @FXML private TableColumn<MiembroModel, String> columnaEstado;
    @FXML private Label lblTotalMiembros;
    @FXML private Label lblEstadoModulo;
    @FXML private TextField campoBusqueda;
    @FXML private Button btnVerDetalle;
    @FXML private Button btnNuevoMiembro;
    @FXML private Button btnEditar;
    @FXML private Button btnCambiarEstado;
    @FXML private Button btnVerAsistencia;

    private final ObservableList<MiembroModel> miembros = FXCollections.observableArrayList();
    private FilteredList<MiembroModel> miembrosFiltrados;

    @FXML
    private void initialize() {
        tablaMiembros.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        columnaDui.setCellValueFactory(cell -> new SimpleStringProperty(formatearDocumento(cell.getValue())));
        columnaNombres.setCellValueFactory(new PropertyValueFactory<>("nombres"));
        columnaApellidos.setCellValueFactory(new PropertyValueFactory<>("apellidos"));
        columnaTelefono.setCellValueFactory(new PropertyValueFactory<>("telefono"));
        columnaCorreo.setCellValueFactory(new PropertyValueFactory<>("correo"));
        columnaEstado.setCellValueFactory(new PropertyValueFactory<>("estado"));

        miembrosFiltrados = new FilteredList<>(miembros, miembro -> true);
        tablaMiembros.setItems(miembrosFiltrados);
        campoBusqueda.textProperty().addListener((observable, anterior, actual) -> filtrar(actual));
        btnVerDetalle.disableProperty().bind(tablaMiembros.getSelectionModel().selectedItemProperty().isNull());
        if (btnVerAsistencia != null) {
            btnVerAsistencia.disableProperty().bind(tablaMiembros.getSelectionModel().selectedItemProperty().isNull());
        }
        boolean puedeGestionar = puedeGestionar(SessionManager.getInstance().requireCurrentUser().getRole());
        btnNuevoMiembro.setDisable(!puedeGestionar);
        if (puedeGestionar) {
            btnEditar.disableProperty().bind(tablaMiembros.getSelectionModel().selectedItemProperty().isNull());
            btnCambiarEstado.disableProperty().bind(tablaMiembros.getSelectionModel().selectedItemProperty().isNull());
        } else {
            btnEditar.setDisable(true);
            btnCambiarEstado.setDisable(true);
        }
        tablaMiembros.getSelectionModel().selectedItemProperty().addListener((observable, previous, selected) ->
            btnCambiarEstado.setText(selected != null && "INACTIVO".equals(selected.getEstado())
                ? "Reactivar" : "Desactivar")
        );
        tablaMiembros.setRowFactory(table -> {
            TableRow<MiembroModel> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (!row.isEmpty() && event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2) {
                    mostrarDetalle(row.getItem());
                }
            });
            return row;
        });
        actualizarTotal();
        cargarMiembros();
    }

    static boolean puedeGestionar(String role) {
        return role != null && ("ADMIN".equalsIgnoreCase(role) || "ADMINISTRADOR".equalsIgnoreCase(role));
    }

    public ObservableList<MiembroModel> getMiembros() {
        return miembros;
    }

    @FXML
    private void abrirRegistro() {
        try {
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/fxml/views/registrar-miembro.fxml")
            );
            Parent content = loader.load();
            RegistrarMiembroController controller = loader.getController();
            var viviendas = new ViviendaApiClient().findAll().stream()
                .filter(vivienda -> "ACTIVA".equals(vivienda.getEstado()))
                .toList();
            if (viviendas.isEmpty()) {
                mostrarError("Primero registra una vivienda activa para poder agregar miembros.");
                return;
            }
            controller.setViviendas(viviendas);
            ButtonType guardarType = new ButtonType("Guardar miembro", ButtonBar.ButtonData.OK_DONE);
            Dialog<Void> dialog = new Dialog<>();
            dialog.setTitle("Registrar miembro");
            dialog.initOwner(tablaMiembros.getScene().getWindow());
            dialog.getDialogPane().setContent(content);
            dialog.getDialogPane().getButtonTypes().addAll(guardarType, ButtonType.CANCEL);
            Button guardar = (Button) dialog.getDialogPane().lookupButton(guardarType);
            guardar.addEventFilter(ActionEvent.ACTION, event -> {
                event.consume();
                CreateMemberRequest request = controller.validatedRequest();
                if (request != null) {
                    guardar.setDisable(true);
                    registrarMiembro(request, controller, dialog, guardar);
                }
            });
            dialog.getDialogPane().getStyleClass().add("member-dialog");
            java.net.URL memberDialogCss = MiembroController.class.getResource("/styles/member-dialog.css");
            if (memberDialogCss != null) {
                dialog.getDialogPane().getStylesheets().add(memberDialogCss.toExternalForm());
            }
            ResponsiveWindowService.fitDialog(dialog, tablaMiembros.getScene().getWindow(), 720);
            dialog.show();
        } catch (Exception exception) {
            mostrarError("No fue posible abrir el formulario de registro: " + exception.getMessage());
        }
    }

    @FXML
    private void verDetalle() {
        MiembroModel seleccionado = tablaMiembros.getSelectionModel().getSelectedItem();
        if (seleccionado != null) mostrarDetalle(seleccionado);
    }

    @FXML
    private void verAsistencia() {
        MiembroModel seleccionado = tablaMiembros.getSelectionModel().getSelectedItem();
        if (seleccionado == null) return;
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/views/historial-asistencia-modal.fxml"));
            Parent content = loader.load();
            HistorialAsistenciaModalController ctrl = loader.getController();
            ctrl.setMiembro(seleccionado);

            Stage stage = new Stage();
            stage.setTitle("Historial de asistencia - " + seleccionado.getNombreCompleto());
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.initOwner(tablaMiembros.getScene().getWindow());
            stage.setScene(new Scene(content));
            stage.setResizable(true);
            ResponsiveWindowService.fitModalStage(stage, tablaMiembros.getScene().getWindow());
            stage.showAndWait();
        } catch (Exception e) {
            e.printStackTrace();
            mostrarError("No fue posible abrir el historial de asistencia: " + e.getMessage());
        }
    }

    @FXML
    private void editar() {
        MiembroModel seleccionado = tablaMiembros.getSelectionModel().getSelectedItem();
        if (seleccionado == null) return;
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/views/registrar-miembro.fxml"));
            Parent content = loader.load();
            RegistrarMiembroController controller = loader.getController();
            var viviendas = new ViviendaApiClient().findAll().stream()
                .filter(vivienda -> "ACTIVA".equals(vivienda.getEstado())
                    || vivienda.getIdVivienda().equals(seleccionado.getIdVivienda()))
                .toList();
            controller.setViviendas(viviendas);
            controller.setMiembro(seleccionado);

            ButtonType guardarType = new ButtonType("Guardar cambios", ButtonBar.ButtonData.OK_DONE);
            Dialog<Void> dialog = memberFormDialog("Editar miembro", content, guardarType);
            Button guardar = (Button) dialog.getDialogPane().lookupButton(guardarType);
            guardar.addEventFilter(ActionEvent.ACTION, event -> {
                event.consume();
                CreateMemberRequest request = controller.validatedRequest();
                if (request != null) {
                    guardar.setDisable(true);
                    actualizarMiembro(seleccionado, request, controller, dialog, guardar);
                }
            });
            dialog.show();
        } catch (Exception exception) {
            mostrarError("No fue posible abrir la edición: " + exception.getMessage());
        }
    }

    @FXML
    private void cambiarEstado() {
        MiembroModel seleccionado = tablaMiembros.getSelectionModel().getSelectedItem();
        if (seleccionado == null) return;
        String nuevoEstado = "ACTIVO".equals(seleccionado.getEstado()) ? "INACTIVO" : "ACTIVO";
        String accion = "ACTIVO".equals(nuevoEstado) ? "reactivar" : "desactivar";
        Alert confirmation = new Alert(
            Alert.AlertType.CONFIRMATION,
            "El miembro y su cuenta cambiarán a " + nuevoEstado + ". Se conservarán sus relaciones y su historial.",
            ButtonType.YES, ButtonType.NO
        );
        confirmation.setHeaderText("¿Deseas " + accion + " a " + seleccionado.getNombres() + "?");
        confirmation.initOwner(tablaMiembros.getScene().getWindow());
        if (confirmation.showAndWait().orElse(ButtonType.NO) != ButtonType.YES) return;

        lblEstadoModulo.setText("ACTIVO".equals(nuevoEstado) ? "Reactivando miembro..." : "Desactivando miembro...");
        Task<MiembroModel> task = new Task<>() {
            @Override protected MiembroModel call() throws Exception {
                return new MiembroApiClient().changeState(seleccionado.getIdMiembro(), nuevoEstado);
            }
        };
        task.setOnSucceeded(event -> {
            reemplazarMiembro(seleccionado, task.getValue());
            lblEstadoModulo.setText("ACTIVO".equals(nuevoEstado) ? "Miembro reactivado" : "Miembro desactivado");
        });
        task.setOnFailed(event -> {
            lblEstadoModulo.setText("No fue posible cambiar el estado");
            mostrarError(task.getException() == null ? "No fue posible cambiar el estado." : task.getException().getMessage());
        });
        Thread thread = new Thread(task, "cambiar-estado-miembro-api");
        thread.setDaemon(true);
        thread.start();
    }

    private Dialog<Void> memberFormDialog(String title, Parent content, ButtonType actionType) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle(title);
        dialog.initOwner(tablaMiembros.getScene().getWindow());
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(actionType, ButtonType.CANCEL);
        dialog.getDialogPane().getStyleClass().add("member-dialog");
        java.net.URL css = MiembroController.class.getResource("/styles/member-dialog.css");
        if (css != null) dialog.getDialogPane().getStylesheets().add(css.toExternalForm());
        ResponsiveWindowService.fitDialog(dialog, tablaMiembros.getScene().getWindow(), 720);
        return dialog;
    }

    private void actualizarMiembro(
        MiembroModel original,
        CreateMemberRequest request,
        RegistrarMiembroController form,
        Dialog<Void> dialog,
        Button guardar
    ) {
        lblEstadoModulo.setText("Actualizando miembro...");
        Task<MiembroModel> task = new Task<>() {
            @Override protected MiembroModel call() throws Exception {
                return new MiembroApiClient().update(original.getIdMiembro(), request);
            }
        };
        task.setOnSucceeded(event -> {
            reemplazarMiembro(original, task.getValue());
            lblEstadoModulo.setText("Miembro actualizado");
            dialog.close();
        });
        task.setOnFailed(event -> {
            guardar.setDisable(false);
            lblEstadoModulo.setText("No fue posible actualizar");
            form.showError(task.getException() == null ? "No fue posible actualizar el miembro." : task.getException().getMessage());
        });
        Thread thread = new Thread(task, "actualizar-miembro-api");
        thread.setDaemon(true);
        thread.start();
    }

    private void reemplazarMiembro(MiembroModel original, MiembroModel updated) {
        int index = miembros.indexOf(original);
        if (index >= 0) miembros.set(index, updated);
        else miembros.add(updated);
        tablaMiembros.getSelectionModel().select(updated);
        tablaMiembros.refresh();
        actualizarTotal();
    }

    private void mostrarDetalle(MiembroModel miembro) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/views/detalle-miembro.fxml"));
            Parent content = loader.load();
            DetalleMiembroController controller = loader.getController();
            controller.setMiembro(miembro);

            Dialog<Void> dialog = new Dialog<>();
            dialog.setTitle("Detalle del miembro");
            dialog.initOwner(tablaMiembros.getScene().getWindow());
            dialog.getDialogPane().setContent(content);
            dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
            dialog.getDialogPane().getStyleClass().addAll("member-dialog", "member-detail-dialog");
            java.net.URL css = MiembroController.class.getResource("/styles/member-dialog.css");
            if (css != null) dialog.getDialogPane().getStylesheets().add(css.toExternalForm());
            ResponsiveWindowService.fitDialog(dialog, tablaMiembros.getScene().getWindow(), 660);
            dialog.showAndWait();
        } catch (IOException exception) {
            mostrarError("No fue posible abrir el detalle del miembro.");
        }
    }

    private void registrarMiembro(
        CreateMemberRequest request,
        RegistrarMiembroController form,
        Dialog<Void> dialog,
        Button guardar
    ) {
        lblEstadoModulo.setText("Guardando miembro...");
        Task<CreateMemberResult> task = new Task<>() {
            @Override
            protected CreateMemberResult call() throws Exception {
                return new MiembroApiClient().create(request);
            }
        };
        task.setOnSucceeded(event -> {
            miembros.add(task.getValue().member());
            lblEstadoModulo.setText("Miembro registrado");
            actualizarTotal();
            dialog.close();
            mostrarCredenciales(task.getValue());
        });
        task.setOnFailed(event -> {
            guardar.setDisable(false);
            lblEstadoModulo.setText("No fue posible registrar");
            String message = task.getException() == null
                ? "No fue posible registrar el miembro."
                : task.getException().getMessage();
            form.showError(message);
        });
        Thread thread = new Thread(task, "registrar-miembro-api");
        thread.setDaemon(true);
        thread.start();
    }

    private void mostrarCredenciales(CreateMemberResult result) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Cuenta del miembro creada");
        alert.setHeaderText("Entregue estas credenciales una sola vez al miembro");
        TextArea credentials = new TextArea("Usuario (documento): " + result.username()
            + "\nContraseña temporal: " + result.temporaryPassword());
        credentials.setEditable(false);
        credentials.setWrapText(true);
        credentials.setPrefRowCount(3);
        credentials.getStyleClass().add("credentials-box");
        alert.getDialogPane().setContent(credentials);
        alert.setContentText(null);
        alert.getDialogPane().getStyleClass().addAll("member-dialog", "credentials-dialog");
        java.net.URL memberDialogCss = MiembroController.class.getResource("/styles/member-dialog.css");
        if (memberDialogCss != null) {
            alert.getDialogPane().getStylesheets().add(memberDialogCss.toExternalForm());
        }
        alert.showAndWait();
    }

    private void mostrarError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message, ButtonType.OK);
        alert.setHeaderText("Ocurrió un problema");
        alert.showAndWait();
    }

    private void cargarMiembros() {
        lblEstadoModulo.setText("Conectando al servidor...");
        Task<List<MiembroModel>> task = new Task<>() {
            @Override
            protected List<MiembroModel> call() throws Exception {
                return new MiembroApiClient().findAll();
            }
        };
        task.setOnSucceeded(event -> {
            miembros.setAll(task.getValue());
            lblEstadoModulo.setText("Conectado al servidor");
            actualizarTotal();
        });
        task.setOnFailed(event -> {
            Throwable error = task.getException();
            String detail = error == null || error.getMessage() == null
                ? "Error desconocido"
                : error.getMessage();
            lblEstadoModulo.setText("Sin conexión al servidor");
            tablaMiembros.setPlaceholder(new Label(
                "No fue posible obtener los miembros. " + detail
            ));
            actualizarTotal();
        });
        Thread thread = new Thread(task, "cargar-miembros-api");
        thread.setDaemon(true);
        thread.start();
    }

    private void actualizarTotal() {
        int visibles = miembrosFiltrados == null ? miembros.size() : miembrosFiltrados.size();
        lblTotalMiembros.setText(visibles == miembros.size()
            ? miembros.size() + " miembros registrados"
            : visibles + " de " + miembros.size() + " miembros");
    }

    private void filtrar(String texto) {
        String criterio = normalizar(texto);
        miembrosFiltrados.setPredicate(miembro -> criterio.isBlank()
            || contiene(miembro.getDui(), criterio)
            || contiene(miembro.getNombres(), criterio)
            || contiene(miembro.getApellidos(), criterio)
            || contiene(miembro.getCorreo(), criterio));
        actualizarTotal();
    }

    private boolean contiene(String valor, String criterio) {
        return valor != null && normalizar(valor).contains(criterio);
    }

    private String formatearDocumento(MiembroModel miembro) {
        String documento = miembro.getDui();
        if (documento == null) return "";
        if ((miembro.getTipoDocumento() == null || "DUI".equals(miembro.getTipoDocumento()))
            && documento.matches("\\d{9}")) {
            return documento.substring(0, 8) + "-" + documento.substring(8);
        }
        return documento;
    }

    private String normalizar(String valor) {
        if (valor == null) {
            return "";
        }
        String sinTildes = Normalizer.normalize(valor, Normalizer.Form.NFD)
            .replaceAll("\\p{M}", "");
        return sinTildes.toLowerCase(Locale.ROOT).trim();
    }
}
