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
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableRow;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Rectangle2D;
import javafx.stage.Screen;
import javafx.scene.input.MouseButton;
import service.MiembroApiClient;
import service.MiembroApiClient.CreateMemberRequest;
import service.MiembroApiClient.CreateMemberResult;
import service.ViviendaApiClient;
import sv.asociacion.backend.entity.Miembro;

public class MiembroController {
    @FXML private TableView<Miembro> tablaMiembros;
    @FXML private TableColumn<Miembro, String> columnaDui;
    @FXML private TableColumn<Miembro, String> columnaNombres;
    @FXML private TableColumn<Miembro, String> columnaApellidos;
    @FXML private TableColumn<Miembro, String> columnaTelefono;
    @FXML private TableColumn<Miembro, String> columnaCorreo;
    @FXML private TableColumn<Miembro, Miembro.Estado> columnaEstado;
    @FXML private Label lblTotalMiembros;
    @FXML private Label lblEstadoModulo;
    @FXML private TextField campoBusqueda;
    @FXML private Button btnVerDetalle;

    private final ObservableList<Miembro> miembros = FXCollections.observableArrayList();
    private FilteredList<Miembro> miembrosFiltrados;

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
        tablaMiembros.setRowFactory(table -> {
            TableRow<Miembro> row = new TableRow<>();
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

    public ObservableList<Miembro> getMiembros() {
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
            Rectangle2D screen = Screen.getScreensForRectangle(
                tablaMiembros.getScene().getWindow().getX(), tablaMiembros.getScene().getWindow().getY(),
                tablaMiembros.getScene().getWindow().getWidth(), tablaMiembros.getScene().getWindow().getHeight()
            ).stream().findFirst().orElse(Screen.getPrimary()).getVisualBounds();
            ScrollPane formScroll = new ScrollPane(content);
            formScroll.setFitToWidth(true);
            formScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
            formScroll.setMaxHeight(Math.max(360, screen.getHeight() - 180));
            formScroll.getStyleClass().add("member-form-scroll");
            dialog.getDialogPane().setContent(formScroll);
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
            dialog.getDialogPane().setPrefWidth(Math.min(720, screen.getWidth() - 40));
            dialog.show();
        } catch (Exception exception) {
            mostrarError("No fue posible abrir el formulario de registro: " + exception.getMessage());
        }
    }

    @FXML
    private void verDetalle() {
        Miembro seleccionado = tablaMiembros.getSelectionModel().getSelectedItem();
        if (seleccionado != null) mostrarDetalle(seleccionado);
    }

    private void mostrarDetalle(Miembro miembro) {
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
            double available = Screen.getScreensForRectangle(
                tablaMiembros.getScene().getWindow().getX(), tablaMiembros.getScene().getWindow().getY(),
                tablaMiembros.getScene().getWindow().getWidth(), tablaMiembros.getScene().getWindow().getHeight()
            ).stream().findFirst().orElse(Screen.getPrimary()).getVisualBounds().getWidth();
            dialog.getDialogPane().setPrefWidth(Math.min(660, available - 40));
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
        Task<List<Miembro>> task = new Task<>() {
            @Override
            protected List<Miembro> call() throws Exception {
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

    private String formatearDocumento(Miembro miembro) {
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
