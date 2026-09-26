package controller;

import java.io.IOException;
import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

import service.HeroIcon;
import service.MaterialAlertService;
import service.TablePaginator;
import service.MiembroApiClient;
import service.MiembroApiClient.CreateMemberRequest;
import service.MiembroApiClient.CreateMemberResult;
import service.ResponsiveWindowService;
import service.ViviendaApiClient;
import models.MiembroModel;
import models.ViviendaModel;
import security.SessionManager;

public class MiembroController {
    @FXML private TableView<MiembroModel> tablaMiembros;
    @FXML private TableColumn<MiembroModel, String> columnaDui; // Columna ID
    @FXML private TableColumn<MiembroModel, String> columnaNombres; // Columna NOMBRE
    @FXML private TableColumn<MiembroModel, String> columnaTelefono; // Columna TELÉFONO
    @FXML private TableColumn<MiembroModel, String> columnaCorreo; // Columna EMAIL
    @FXML private TableColumn<MiembroModel, String> columnaEstado; // Columna ESTADO
    @FXML private TableColumn<MiembroModel, Void> columnaAcciones; // Columna ACCIONES

    @FXML private VBox contenedorPrincipal;
    @FXML private VBox tarjetaTabla;
    @FXML private HBox barraPie;
    @FXML private Label lblTotalMiembros;
    @FXML private Label lblEstadoModulo;
    @FXML private Label lblSubtitulo;
    @FXML private Label lblResumenPie;
    @FXML private StackPane iconoBusquedaContainer;
    @FXML private TextField campoBusqueda;
    @FXML private Button btnNuevoMiembro;
    @FXML private Button btnToggleInactivos;

    @FXML private HBox bannerClavesProvisionales;
    @FXML private StackPane iconoBannerContainer;
    @FXML private Label lblBannerTexto;
    @FXML private Button btnFiltrarProvisionales;
    private boolean filtrandoSoloProvisionales = false;

    // Botones opcionales para compatibilidad
    @FXML private Button btnVerDetalle;
    @FXML private Button btnEditar;
    @FXML private Button btnCambiarEstado;
    @FXML private Button btnVerAsistencia;

    private boolean mostrarInactivos = false;
    private final ObservableList<MiembroModel> miembros = FXCollections.observableArrayList();
    private FilteredList<MiembroModel> miembrosFiltrados;
    private TablePaginator<MiembroModel> paginator;

    @FXML
    private void initialize() {
        if (iconoBusquedaContainer != null) {
            iconoBusquedaContainer.getChildren().setAll(HeroIcon.create(HeroIcon.SEARCH, "#94a3b8", 16));
        }
        if (iconoBannerContainer != null) {
            iconoBannerContainer.getChildren().setAll(HeroIcon.create(HeroIcon.SHIELD, "#d97706", 24));
        }

        tablaMiembros.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        tablaMiembros.setFixedCellSize(52.0);

        // Esquinas redondeadas superiores perfectas (radio 20px) para la tabla
        javafx.scene.shape.Rectangle clip = new javafx.scene.shape.Rectangle();
        clip.setArcWidth(40);
        clip.setArcHeight(40);
        clip.widthProperty().bind(tablaMiembros.widthProperty());
        clip.heightProperty().bind(tablaMiembros.heightProperty().add(20));
        tablaMiembros.setClip(clip);

        // 1. Columna ID (muestra ID o documento formateado)
        columnaDui.setCellValueFactory(cell -> {
            MiembroModel m = cell.getValue();
            if (m.getId() != null) {
                return new SimpleStringProperty(String.valueOf(m.getId()));
            }
            return new SimpleStringProperty(formatearDocumento(m));
        });

        // 2. Columna NOMBRE (nombre completo)
        columnaNombres.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getNombreCompleto()));

        // 3. Columna TELÉFONO (teléfono o "00")
        columnaTelefono.setCellValueFactory(cell -> {
            String tel = cell.getValue().getTelefono();
            return new SimpleStringProperty(tel != null && !tel.isBlank() ? tel : "00");
        });

        // 4. Columna EMAIL (correo o "—")
        columnaCorreo.setCellValueFactory(cell -> {
            String email = cell.getValue().getCorreo();
            return new SimpleStringProperty(email != null && !email.isBlank() ? email : "—");
        });

        // 5. Columna ESTADO con pastilla (BadgePill) y alerta si tiene clave provisional
        columnaEstado.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getEstado()));
        columnaEstado.setCellFactory(column -> new TableCell<>() {
            private final Label badge = new Label();
            private final Label badgeClave = new Label("⚠️ Clave inicial");
            private final VBox container = new VBox(3);
            {
                badgeClave.setStyle("-fx-background-color: #fef3c7; -fx-text-fill: #b45309; -fx-border-color: #fde68a; -fx-border-radius: 9999px; -fx-background-radius: 9999px; -fx-font-weight: 700; -fx-padding: 2px 7px; -fx-font-size: 10px; -fx-cursor: hand;");
                badgeClave.setTooltip(new Tooltip("Contraseña provisional no cambiada aún en el celular. Clic para revisar credenciales."));
                badgeClave.setOnMouseClicked(e -> {
                    if (getIndex() >= 0 && getIndex() < getTableView().getItems().size()) {
                        MiembroModel m = getTableView().getItems().get(getIndex());
                        if (m != null) revisarCredenciales(m);
                    }
                });
                container.setAlignment(Pos.CENTER_LEFT);
            }

            @Override
            protected void updateItem(String estado, boolean empty) {
                super.updateItem(estado, empty);
                if (empty || estado == null || getIndex() < 0 || getIndex() >= getTableView().getItems().size()) {
                    setGraphic(null);
                    setText(null);
                } else {
                    MiembroModel m = getTableView().getItems().get(getIndex());
                    String estLower = estado.toLowerCase(Locale.ROOT);
                    badge.setText(estLower);
                    badge.getStyleClass().setAll("badge-pill");
                    if ("activo".equals(estLower)) {
                        badge.getStyleClass().add("badge-pill-active");
                    } else {
                        badge.getStyleClass().add("badge-pill-inactive");
                    }
                    container.getChildren().clear();
                    container.getChildren().add(badge);
                    if (m != null && m.tieneClaveProvisional()) {
                        container.getChildren().add(badgeClave);
                    }
                    setGraphic(container);
                    setText(null);
                    setAlignment(Pos.CENTER_LEFT);
                }
            }
        });

        // 6. Columna ACCIONES: Iconos SVG de Heroicons (outline)
        if (columnaAcciones != null) {
            columnaAcciones.setCellFactory(column -> new TableCell<>() {
                private final Button btnKey = new Button();
                private final Button btnVer = new Button();
                private final Button btnEd = new Button();
                private final Button btnDel = new Button();
                private final HBox actionsBox = new HBox(6);

                {
                    actionsBox.getStyleClass().add("row-actions-box");
                    actionsBox.setAlignment(Pos.CENTER_RIGHT);

                    // 0. Revisar credenciales provisionales
                    btnKey.getStyleClass().addAll("btn-row-action", "btn-action-edit");
                    btnKey.setStyle("-fx-background-color: #fef3c7; -fx-border-color: #fde68a;");
                    btnKey.setGraphic(HeroIcon.create(HeroIcon.SHIELD, "#d97706", 16));
                    btnKey.setTooltip(new Tooltip("Revisar credenciales provisionales (Pendiente de cambio en celular)"));
                    btnKey.setOnAction(e -> {
                        MiembroModel item = getTableView().getItems().get(getIndex());
                        if (item != null) revisarCredenciales(item);
                    });

                    // 1. Ver (.btn-action-view): Icono Ojo (eye) en Azul (text-blue-600)
                    btnVer.getStyleClass().addAll("btn-row-action", "btn-action-view");
                    btnVer.setGraphic(HeroIcon.create(HeroIcon.EYE, HeroIcon.BLUE_600, 18));
                    btnVer.setTooltip(new Tooltip("Ver cliente"));
                    btnVer.setOnAction(e -> {
                        MiembroModel item = getTableView().getItems().get(getIndex());
                        if (item != null) mostrarDetalle(item);
                    });

                    // 2. Editar (.btn-action-edit): Icono Lápiz (pencil) en Ámbar (text-amber-600)
                    btnEd.getStyleClass().addAll("btn-row-action", "btn-action-edit");
                    btnEd.setGraphic(HeroIcon.create(HeroIcon.PENCIL, HeroIcon.AMBER_600, 18));
                    btnEd.setTooltip(new Tooltip("Editar cliente"));
                    btnEd.setOnAction(e -> {
                        MiembroModel item = getTableView().getItems().get(getIndex());
                        if (item != null) editarMiembro(item);
                    });

                    // 3. Desactivar (.btn-action-delete): Icono Papelera (trash) en Rojo (text-red-600)
                    btnDel.getStyleClass().addAll("btn-row-action", "btn-action-delete");
                    btnDel.setGraphic(HeroIcon.create(HeroIcon.TRASH, HeroIcon.RED_600, 18));
                    btnDel.setTooltip(new Tooltip("Desactivar cliente"));
                    btnDel.setOnAction(e -> {
                        MiembroModel item = getTableView().getItems().get(getIndex());
                        if (item != null) cambiarEstadoMiembro(item);
                    });
                }

                @Override
                protected void updateItem(Void item, boolean empty) {
                    super.updateItem(item, empty);
                    setAlignment(Pos.CENTER_RIGHT);
                    if (empty || getIndex() < 0 || getIndex() >= getTableView().getItems().size()) {
                        setGraphic(null);
                    } else {
                        MiembroModel m = getTableView().getItems().get(getIndex());
                        boolean activo = "ACTIVO".equalsIgnoreCase(m.getEstado());

                        if (activo) {
                            btnDel.setGraphic(HeroIcon.create(HeroIcon.TRASH, HeroIcon.RED_600, 18));
                            btnDel.setTooltip(new Tooltip("Desactivar cliente"));
                        } else {
                            btnDel.setGraphic(HeroIcon.create(HeroIcon.REFRESH, HeroIcon.GREEN_600, 18));
                            btnDel.setTooltip(new Tooltip("Reactivar cliente"));
                        }

                        boolean puede = puedeGestionar(SessionManager.getInstance().requireCurrentUser().getRole());
                        btnEd.setDisable(!puede);
                        btnDel.setDisable(!puede);

                        actionsBox.getChildren().clear();
                        if (m.tieneClaveProvisional()) {
                            actionsBox.getChildren().add(btnKey);
                        }
                        actionsBox.getChildren().addAll(btnVer, btnEd, btnDel);
                        setGraphic(actionsBox);
                    }
                }
            });
        }

        // Configuración de paginación y filtrado reactivo
        miembrosFiltrados = new FilteredList<>(miembros, miembro -> true);
        paginator = new TablePaginator<>(tablaMiembros, miembrosFiltrados, "miembros", 5);
        if (barraPie != null) {
            paginator.attachTo(barraPie);
        }
        campoBusqueda.textProperty().addListener((observable, anterior, actual) -> filtrar(actual));

        // Permisos de creaciÃ³n
        boolean puedeGestionar = puedeGestionar(SessionManager.getInstance().requireCurrentUser().getRole());
        if (btnNuevoMiembro != null) {
            btnNuevoMiembro.setDisable(!puedeGestionar);
        }

        // Doble clic en fila para ver detalle
        tablaMiembros.setRowFactory(table -> {
            TableRow<MiembroModel> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (!row.isEmpty() && event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2) {
                    mostrarDetalle(row.getItem());
                }
            });
            return row;
        });

        actualizarVistaInactivos();
        cargarMiembros();
    }

    public static boolean puedeGestionar(String role) {
        return role != null && ("ADMIN".equalsIgnoreCase(role) || "ADMINISTRADOR".equalsIgnoreCase(role));
    }

    public ObservableList<MiembroModel> getMiembros() {
        return miembros;
    }

    @FXML
    private void toggleFiltroInactivos() {
        mostrarInactivos = !mostrarInactivos;
        actualizarVistaInactivos();
    }

    private void actualizarVistaInactivos() {
        if (btnToggleInactivos != null) {
            btnToggleInactivos.setGraphic(HeroIcon.create(HeroIcon.ARCHIVE, HeroIcon.SLATE_700, 16));
            if (mostrarInactivos) {
                btnToggleInactivos.setText("Ver activos");
                btnToggleInactivos.getStyleClass().add("active-toggle");
            } else {
                btnToggleInactivos.setText("Ver inactivos");
                btnToggleInactivos.getStyleClass().remove("active-toggle");
            }
        }
        if (lblSubtitulo != null) {
            lblSubtitulo.setText(mostrarInactivos
                ? "Listado de miembros inactivos."
                : "Listado de miembros activos.");
        }
        filtrar(campoBusqueda != null ? campoBusqueda.getText() : "");
    }

    @FXML
    private void abrirRegistro() {
        try {
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/fxml/views/registrar-miembro.fxml")
            );
            Parent content = loader.load();
            RegistrarMiembroController controller = loader.getController();
            List<ViviendaModel> viviendas = List.of();
            try {
                viviendas = new ViviendaApiClient().findAll().stream()
                    .filter(vivienda -> "ACTIVA".equals(vivienda.getEstado()))
                    .toList();
            } catch (Exception ignored) { }
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
        if (seleccionado != null) verAsistenciaMiembro(seleccionado);
    }

    public void verAsistenciaMiembro(MiembroModel seleccionado) {
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
        if (seleccionado != null) editarMiembro(seleccionado);
    }

    public void editarMiembro(MiembroModel seleccionado) {
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
            Dialog<Void> dialog = memberFormDialog("Editar cliente", content, guardarType);
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
        if (seleccionado != null) cambiarEstadoMiembro(seleccionado);
    }

    public void cambiarEstadoMiembro(MiembroModel seleccionado) {
        if (seleccionado == null) return;
        boolean esActivo = "ACTIVO".equalsIgnoreCase(seleccionado.getEstado());
        String nuevoEstado = esActivo ? "INACTIVO" : "ACTIVO";
        Window owner = tablaMiembros.getScene() != null ? tablaMiembros.getScene().getWindow() : null;

        String titulo = esActivo ? "Confirmar desactivación" : "Confirmar reactivación";
        String mensaje = esActivo
            ? seleccionado.getNombreCompleto() + " se desactivará y dejará de aparecer en el listado activo."
            : seleccionado.getNombreCompleto() + " se reactivará y volverá a aparecer en el listado activo.";
        String accionTexto = esActivo ? "Sí, desactivar" : "Sí, reactivar";

        boolean confirmado = MaterialAlertService.confirmacion(
            owner,
            titulo,
            mensaje,
            accionTexto,
            esActivo,
            null
        );

        if (!confirmado) {
            return;
        }

        if (lblEstadoModulo != null) {
            lblEstadoModulo.setText("ACTIVO".equals(nuevoEstado) ? "Reactivando..." : "Desactivando...");
        }
        Task<MiembroModel> task = new Task<>() {
            @Override protected MiembroModel call() throws Exception {
                return new MiembroApiClient().changeState(seleccionado.getIdMiembro(), nuevoEstado);
            }
        };
        task.setOnSucceeded(event -> {
            reemplazarMiembro(seleccionado, task.getValue());
            if (lblEstadoModulo != null) {
                lblEstadoModulo.setText("ACTIVO".equals(nuevoEstado) ? "Cliente reactivado" : "Cliente desactivado");
            }
            filtrar(campoBusqueda != null ? campoBusqueda.getText() : "");
            MaterialAlertService.exito(
                owner,
                "ACTIVO".equals(nuevoEstado) ? "Cliente reactivado" : "Cliente desactivado",
                "El estado de " + seleccionado.getNombreCompleto() + " fue actualizado a " + nuevoEstado.toLowerCase() + "."
            );
        });
        task.setOnFailed(event -> {
            if (lblEstadoModulo != null) {
                lblEstadoModulo.setText("No fue posible cambiar el estado");
            }
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
        if (lblEstadoModulo != null) lblEstadoModulo.setText("Actualizando...");
        Task<MiembroModel> task = new Task<>() {
            @Override protected MiembroModel call() throws Exception {
                return new MiembroApiClient().update(original.getIdMiembro(), request);
            }
        };
        task.setOnSucceeded(event -> {
            reemplazarMiembro(original, task.getValue());
            if (lblEstadoModulo != null) lblEstadoModulo.setText("Cliente actualizado");
            dialog.close();
            filtrar(campoBusqueda != null ? campoBusqueda.getText() : "");
            Window owner = tablaMiembros.getScene() != null ? tablaMiembros.getScene().getWindow() : null;
            MaterialAlertService.exito(
                owner,
                "Cambios guardados",
                "Los datos de " + task.getValue().getNombreCompleto() + " fueron actualizados con éxito."
            );
        });
        task.setOnFailed(event -> {
            guardar.setDisable(false);
            if (lblEstadoModulo != null) lblEstadoModulo.setText("No fue posible actualizar");
            form.showError(task.getException() == null ? "No fue posible actualizar." : task.getException().getMessage());
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
            ResponsiveWindowService.fitDialog(dialog, tablaMiembros.getScene().getWindow(), 760);
            dialog.showAndWait();
        } catch (IOException exception) {
            mostrarError("No fue posible abrir el detalle.");
        }
    }

    private void registrarMiembro(
        CreateMemberRequest request,
        RegistrarMiembroController form,
        Dialog<Void> dialog,
        Button guardar
    ) {
        if (lblEstadoModulo != null) lblEstadoModulo.setText("Guardando miembro...");
        Task<CreateMemberResult> task = new Task<>() {
            @Override
            protected CreateMemberResult call() throws Exception {
                return new MiembroApiClient().create(request);
            }
        };
        task.setOnSucceeded(event -> {
            CreateMemberResult res = task.getValue();
            MiembroModel created = res.member();
            created.setNombreUsuario(res.username());
            created.setClaveTemporal(res.temporaryPassword());
            created.setRequiereCambioClave(true);
            miembros.add(created);
            if (lblEstadoModulo != null) lblEstadoModulo.setText("Cliente registrado");
            actualizarTotal();
            dialog.close();
            filtrar(campoBusqueda != null ? campoBusqueda.getText() : "");
            mostrarCredenciales(res);
        });
        task.setOnFailed(event -> {
            guardar.setDisable(false);
            if (lblEstadoModulo != null) lblEstadoModulo.setText("No fue posible registrar");
            String message = task.getException() == null
                ? "No fue posible registrar."
                : task.getException().getMessage();
            form.showError(message);
        });
        Thread thread = new Thread(task, "registrar-miembro-api");
        thread.setDaemon(true);
        thread.start();
    }

    private void mostrarCredenciales(CreateMemberResult result) {
        Window owner = tablaMiembros != null && tablaMiembros.getScene() != null ? tablaMiembros.getScene().getWindow() : null;
        MaterialAlertService.credenciales(owner, result.username(), result.temporaryPassword());
    }

    private void mostrarError(String message) {
        Window owner = tablaMiembros != null && tablaMiembros.getScene() != null ? tablaMiembros.getScene().getWindow() : null;
        MaterialAlertService.error(owner, "Ocurrió un problema", message);
    }

    @FXML
    private void filtrarProvisionales() {
        filtrandoSoloProvisionales = !filtrandoSoloProvisionales;
        if (btnFiltrarProvisionales != null) {
            btnFiltrarProvisionales.setText(filtrandoSoloProvisionales ? "Ver todos los miembros" : "Ver miembros pendientes");
        }
        filtrar(campoBusqueda != null ? campoBusqueda.getText() : "");
    }

    public void revisarCredenciales(MiembroModel m) {
        if (m == null) return;
        Window owner = tablaMiembros != null && tablaMiembros.getScene() != null ? tablaMiembros.getScene().getWindow() : null;
        if (m.getClaveTemporal() != null && !m.getClaveTemporal().isBlank()) {
            MaterialAlertService.revisarCredencialesProvisionales(
                owner,
                m.getNombreCompleto(),
                m.getNombreUsuario() != null ? m.getNombreUsuario() : m.getDocumento(),
                m.getClaveTemporal()
            );
        } else {
            Task<MiembroApiClient.CredencialesMiembro> task = new Task<>() {
                @Override
                protected MiembroApiClient.CredencialesMiembro call() throws Exception {
                    return new MiembroApiClient().getCredenciales(m.getId());
                }
            };
            task.setOnSucceeded(e -> {
                var cred = task.getValue();
                if (cred != null) {
                    m.setClaveTemporal(cred.claveTemporal());
                    m.setNombreUsuario(cred.nombreUsuario());
                    m.setRequiereCambioClave(cred.requiereCambioClave());
                    MaterialAlertService.revisarCredencialesProvisionales(
                        owner,
                        m.getNombreCompleto(),
                        cred.nombreUsuario(),
                        cred.claveTemporal()
                    );
                }
            });
            task.setOnFailed(e -> {
                MaterialAlertService.confirmacion(
                    owner,
                    "Miembro sin Usuario Móvil",
                    "El miembro " + m.getNombreCompleto() + " no tiene un usuario vinculado actualmente.\n\n¿Desea generar su acceso a la aplicación móvil y contraseña provisional ahora?",
                    "Generar Acceso",
                    false,
                    () -> {
                        Task<MiembroApiClient.CredencialesMiembro> genTask = new Task<>() {
                            @Override
                            protected MiembroApiClient.CredencialesMiembro call() throws Exception {
                                 return new MiembroApiClient().generarCredenciales(m.getId(), m.getDui());
                            }
                        };
                        genTask.setOnSucceeded(ev -> {
                            var nueva = genTask.getValue();
                            if (nueva != null) {
                                m.setClaveTemporal(nueva.claveTemporal());
                                m.setNombreUsuario(nueva.nombreUsuario());
                                m.setRequiereCambioClave(nueva.requiereCambioClave());
                                MaterialAlertService.credenciales(owner, nueva.nombreUsuario(), nueva.claveTemporal());
                                cargarMiembros();
                            }
                        });
                        genTask.setOnFailed(ev -> {
                            Throwable ex = genTask.getException();
                            String msg = ex != null && ex.getMessage() != null ? ex.getMessage() : "No fue posible generar el acceso.";
                            MaterialAlertService.error(owner, "Error", msg);
                        });
                        Thread t = new Thread(genTask, "gen-cred-table");
                        t.setDaemon(true);
                        t.start();
                    }
                );
            });
            Thread thread = new Thread(task, "fetch-credenciales");
            thread.setDaemon(true);
            thread.start();
        }
    }

    private void cargarMiembros() {
        if (lblEstadoModulo != null) lblEstadoModulo.setText("Conectando...");
        Task<List<MiembroModel>> task = new Task<>() {
            @Override
            protected List<MiembroModel> call() throws Exception {
                List<MiembroModel> list = new MiembroApiClient().findAll();
                try {
                    List<models.UsuarioModel> users = new service.UsuarioApiClient().findAll();
                    java.util.Map<Integer, models.UsuarioModel> userMap = new java.util.HashMap<>();
                    for (models.UsuarioModel u : users) {
                        if (u.getIdMiembro() != null) userMap.put(u.getIdMiembro(), u);
                    }
                    for (MiembroModel m : list) {
                        if (m.getId() != null && userMap.containsKey(m.getId())) {
                            models.UsuarioModel u = userMap.get(m.getId());
                            m.setIdUsuario(u.getIdUsuario());
                            m.setNombreUsuario(u.getNombreUsuario());
                            m.setRequiereCambioClave(u.getRequiereCambioClave());
                            m.setClaveTemporal(u.getClaveTemporal());
                        }
                    }
                } catch (Exception ignored) {}
                return list;
            }
        };
        task.setOnSucceeded(event -> {
            miembros.setAll(task.getValue());
            if (lblEstadoModulo != null) lblEstadoModulo.setText("Conectado");
            filtrar(campoBusqueda != null ? campoBusqueda.getText() : "");
            actualizarTotal();
        });
        task.setOnFailed(event -> {
            Throwable error = task.getException();
            String detail = error == null || error.getMessage() == null
                ? "Error de conexión"
                : error.getMessage();
            if (lblEstadoModulo != null) lblEstadoModulo.setText("Sin conexión");
            tablaMiembros.setPlaceholder(new Label("No fue posible cargar los miembros. " + detail));
            actualizarTotal();
            ajustarAlturaTabla();
        });
        Thread thread = new Thread(task, "cargar-miembros-api");
        thread.setDaemon(true);
        thread.start();
    }

    private void actualizarTotal() {
        int visibles = miembrosFiltrados == null ? miembros.size() : miembrosFiltrados.size();
        long totalActivos = miembros.stream().filter(m -> "ACTIVO".equalsIgnoreCase(m.getEstado())).count();
        long totalInactivos = miembros.size() - totalActivos;
        long provisionales = miembros.stream().filter(MiembroModel::tieneClaveProvisional).count();

        if (lblTotalMiembros != null) {
            lblTotalMiembros.setText(miembros.size() + " miembros en total (" + totalActivos + " activos)");
        }

        if (lblResumenPie != null) {
            String tipo = mostrarInactivos ? "inactivos" : "activos";
            lblResumenPie.setText(visibles + " miembros " + tipo + " mostrados · " + miembros.size() + " en total");
        }

        if (bannerClavesProvisionales != null) {
            bannerClavesProvisionales.setVisible(provisionales > 0);
            bannerClavesProvisionales.setManaged(provisionales > 0);
            if (lblBannerTexto != null) {
                lblBannerTexto.setText("Hay " + provisionales + " miembro(s) con contraseña temporal pendiente de cambio en el celular. Puede revisar sus credenciales hasta que las actualicen.");
            }
        }
    }

    private void filtrar(String texto) {
        String criterio = normalizar(texto);
        if (miembrosFiltrados != null) {
            miembrosFiltrados.setPredicate(miembro -> {
                if (filtrandoSoloProvisionales && !miembro.tieneClaveProvisional()) {
                    return false;
                }

                boolean coincideEstado = mostrarInactivos
                    ? !"ACTIVO".equalsIgnoreCase(miembro.getEstado())
                    : "ACTIVO".equalsIgnoreCase(miembro.getEstado());

                if (!coincideEstado) {
                    return false;
                }

                return criterio.isBlank()
                    || contiene(miembro.getDui(), criterio)
                    || contiene(String.valueOf(miembro.getId()), criterio)
                    || contiene(miembro.getNombres(), criterio)
                    || contiene(miembro.getApellidos(), criterio)
                    || contiene(miembro.getCorreo(), criterio);
            });
        }
        actualizarTotal();
        if (paginator != null) {
            paginator.updatePagination();
        } else {
            ajustarAlturaTabla();
        }
    }

    /**
     * Ajusta la altura de la tabla según la cantidad de filas visibles.
     * Como la vista está dentro de un ScrollPane, la tabla crece libremente
     * y el usuario puede desplazarse verticalmente para ver todos los registros.
     */
    private void ajustarAlturaTabla() {
        if (tablaMiembros == null) {
            return;
        }

        int cantidad = (miembrosFiltrados != null) ? miembrosFiltrados.size() : 0;
        double alturaFila = 50.0;
        double alturaCabecera = 47.0;
        double paddingTabla = 3.0;

        double alturaFinal;
        if (cantidad == 0) {
            alturaFinal = alturaCabecera + 85.0; // Espacio limpio para el placeholder
        } else {
            // La tabla crece exactamente según las filas; el ScrollPane padre gestiona el overflow
            alturaFinal = Math.max(130.0, alturaCabecera + (cantidad * alturaFila) + paddingTabla);
        }

        tablaMiembros.setPrefHeight(alturaFinal);
        tablaMiembros.setMinHeight(alturaFinal);
        tablaMiembros.setMaxHeight(alturaFinal);
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
