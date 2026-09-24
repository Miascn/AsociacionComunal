package controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import models.VotacionModel;
import security.SessionManager;
import javafx.collections.ObservableList;
import service.HeroIcon;
import service.ResponsiveWindowService;
import service.TablePaginator;
import service.VotacionApiClient;

public class VotacionesController {
    @FXML private Button btnNuevaVotacion;
    @FXML private Label lblTotalAbiertas;
    @FXML private Label lblAbiertasDetalle;
    @FXML private Label lblTotalBorradores;
    @FXML private Label lblTotalCerradas;
    @FXML private Label lblTotalVotos;
    @FXML private Label lblContadorFiltrados;

    @FXML private TextField txtBuscar;
    @FXML private ComboBox<String> comboEstado;

    @FXML private TableView<VotacionModel> tablaVotaciones;
    @FXML private TableColumn<VotacionModel, String> colTitulo;
    @FXML private TableColumn<VotacionModel, String> colProyecto;
    @FXML private TableColumn<VotacionModel, String> colFechas;
    @FXML private TableColumn<VotacionModel, String> colOpcionesVotos;
    @FXML private TableColumn<VotacionModel, String> colEstado;
    @FXML private TableColumn<VotacionModel, Void> colAcciones;

    @FXML private VBox pnlDetalleOpciones;
    @FXML private Label lblDetalleTitulo;
    @FXML private Label lblDetalleEstado;
    @FXML private FlowPane flowOpciones;
    @FXML private HBox barraPie;

    private final ObservableList<VotacionModel> votacionesList = FXCollections.observableArrayList();
    private TablePaginator<VotacionModel> paginator;

    private final VotacionApiClient apiClient = new VotacionApiClient();

    /**
     * Participación del miembro autenticado por votación abierta, resuelta una sola vez
     * en cada carga. Una clave ausente significa <em>no se pudo determinar</em>, que no
     * es lo mismo que "no ha votado": por eso se guarda {@code Boolean} y no {@code boolean}.
     */
    private final Map<Integer, Boolean> participacionPorVotacion = new HashMap<>();

    @FXML
    public void initialize() {
        boolean canManage = canManage();
        btnNuevaVotacion.setVisible(canManage);
        btnNuevaVotacion.setManaged(canManage);

        paginator = new TablePaginator<>(tablaVotaciones, votacionesList, "procesos", 5);
        if (barraPie != null) {
            paginator.attachTo(barraPie);
        }

        configurarFiltros();
        configurarTabla();
        cargarDatos();
    }

    private void configurarFiltros() {
        comboEstado.setItems(FXCollections.observableArrayList(
            "TODOS", "ABIERTA", "BORRADOR", "PROGRAMADA", "CERRADA", "CANCELADA"));
        comboEstado.setValue("TODOS");
        comboEstado.valueProperty().addListener((obs, oldVal, newVal) -> cargarDatos());

        txtBuscar.textProperty().addListener((obs, oldVal, newVal) -> cargarDatos());
    }

    private void configurarTabla() {
        colTitulo.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getTitulo()));
        colTitulo.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    VotacionModel m = getTableRow().getItem();
                    VBox box = new VBox(2);
                    Label lblTitulo = new Label(m.getTitulo());
                    lblTitulo.setStyle("-fx-font-weight: bold; -fx-text-fill: #1e293b;");
                    String desc = m.getDescripcion();
                    if (desc != null && !desc.isBlank()) {
                        Label lblDesc = new Label(desc);
                        lblDesc.setStyle("-fx-text-fill: #64748b; -fx-font-size: 11px;");
                        box.getChildren().addAll(lblTitulo, lblDesc);
                    } else {
                        box.getChildren().add(lblTitulo);
                    }
                    setGraphic(box);
                    setText(null);
                }
            }
        });

        colProyecto.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getProyectoDisplay()));
        colProyecto.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    Label lbl = new Label(item);
                    if ("Consulta General".equalsIgnoreCase(item)) {
                        lbl.setStyle("-fx-background-color: #f1f5f9; -fx-text-fill: #475569; -fx-padding: 2 8 2 8; -fx-background-radius: 6; -fx-font-size: 11px;");
                    } else {
                        lbl.setStyle("-fx-background-color: #eff6ff; -fx-text-fill: #1d4ed8; -fx-padding: 2 8 2 8; -fx-background-radius: 6; -fx-font-size: 11px; -fx-font-weight: bold;");
                    }
                    setGraphic(lbl);
                    setText(null);
                }
            }
        });

        colFechas.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getRangoFechas()));

        colOpcionesVotos.setCellValueFactory(cellData -> {
            VotacionModel m = cellData.getValue();
            return new SimpleStringProperty(m.getTotalOpciones() + " opciones / " + m.getTotalVotos() + " votos");
        });

        colEstado.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getEstado()));
        colEstado.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    Label badge = new Label(item);
                    String style;
                    switch (item.toUpperCase()) {
                        case "ABIERTA" -> style = "-fx-background-color: #dcfce7; -fx-text-fill: #15803d; -fx-font-weight: bold; -fx-padding: 3 8 3 8; -fx-background-radius: 6;";
                        case "BORRADOR", "PROGRAMADA" -> style = "-fx-background-color: #fef3c7; -fx-text-fill: #b45309; -fx-font-weight: bold; -fx-padding: 3 8 3 8; -fx-background-radius: 6;";
                        case "CERRADA" -> style = "-fx-background-color: #f1f5f9; -fx-text-fill: #475569; -fx-font-weight: bold; -fx-padding: 3 8 3 8; -fx-background-radius: 6;";
                        case "CANCELADA" -> style = "-fx-background-color: #fee2e2; -fx-text-fill: #b91c1c; -fx-font-weight: bold; -fx-padding: 3 8 3 8; -fx-background-radius: 6;";
                        default -> style = "-fx-background-color: #e2e8f0; -fx-text-fill: #334155; -fx-padding: 3 8 3 8; -fx-background-radius: 6;";
                    }
                    badge.setStyle(style);
                    setGraphic(badge);
                    setText(null);
                }
            }
        });

        colAcciones.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null);
                } else {
                    VotacionModel m = getTableRow().getItem();
                    HBox box = new HBox(8);
                    box.getStyleClass().add("row-actions-box");
                    box.setAlignment(Pos.CENTER);

                    if (m.isAbierta()) {
                        box.getChildren().add(construirBotonVoto(m));
                    }

                    Button btnDetalle = new Button();
                    btnDetalle.getStyleClass().addAll("btn-row-action", "btn-action-view");
                    btnDetalle.setGraphic(HeroIcon.create(HeroIcon.EYE, HeroIcon.BLUE_600, 18));
                    btnDetalle.setTooltip(new Tooltip("Ver opciones y resultados"));
                    btnDetalle.setOnAction(e -> mostrarDetalle(m));
                    box.getChildren().add(btnDetalle);

                    if (canManage()) {
                        if (m.isAbrible()) {
                            Button btnAbrir = new Button();
                            btnAbrir.getStyleClass().addAll("btn-row-action", "btn-action-view");
                            btnAbrir.setGraphic(HeroIcon.create(HeroIcon.PLAY, HeroIcon.GREEN_600, 18));
                            btnAbrir.setTooltip(new Tooltip("Abrir votación a la comunidad"));
                            btnAbrir.setOnAction(e -> abrirVotacion(m));
                            box.getChildren().add(btnAbrir);
                        }
                        if (m.isEditable()) {
                            Button btnEditar = new Button();
                            btnEditar.getStyleClass().addAll("btn-row-action", "btn-action-edit");
                            btnEditar.setGraphic(HeroIcon.create(HeroIcon.PENCIL, HeroIcon.AMBER_600, 18));
                            btnEditar.setTooltip(new Tooltip("Editar votación"));
                            btnEditar.setOnAction(e -> abrirFormularioEditar(m));
                            box.getChildren().add(btnEditar);
                        }
                        Button btnOpciones = new Button();
                        btnOpciones.getStyleClass().addAll("btn-row-action", "btn-action-view");
                        btnOpciones.setGraphic(HeroIcon.create(HeroIcon.SLIDERS, HeroIcon.INDIGO_600, 18));
                        btnOpciones.setTooltip(new Tooltip("Configurar opciones de votación"));
                        btnOpciones.setOnAction(e -> abrirModalOpciones(m));
                        box.getChildren().add(btnOpciones);

                        if (m.isCerrable()) {
                            Button btnCerrar = new Button();
                            btnCerrar.getStyleClass().addAll("btn-row-action", "btn-action-delete");
                            btnCerrar.setGraphic(HeroIcon.create(HeroIcon.BAN, HeroIcon.SLATE_700, 18));
                            btnCerrar.setTooltip(new Tooltip("Cerrar votación"));
                            btnCerrar.setOnAction(e -> cerrarVotacion(m));
                            box.getChildren().add(btnCerrar);
                        }
                        if (m.isEliminable()) {
                            Button btnEliminar = new Button();
                            btnEliminar.getStyleClass().addAll("btn-row-action", "btn-action-delete");
                            btnEliminar.setGraphic(HeroIcon.create(HeroIcon.TRASH, HeroIcon.RED_600, 18));
                            btnEliminar.setTooltip(new Tooltip("Eliminar votación"));
                            btnEliminar.setOnAction(e -> eliminarVotacion(m));
                            box.getChildren().add(btnEliminar);
                        }
                    }

                    setGraphic(box);
                }
            }
        });

        tablaVotaciones.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                mostrarDetalle(newVal);
            }
        });
    }

    @FXML
    public void cargarDatos() {
        String estado = comboEstado.getValue();
        String busqueda = txtBuscar.getText();

        Task<CargaVotaciones> task = new Task<>() {
            @Override
            protected CargaVotaciones call() throws Exception {
                List<VotacionModel> lista = apiClient.findFiltered(estado, null, busqueda);

                // La participación se resuelve aquí, en el hilo de fondo y una sola vez
                // por carga, solo para las abiertas: son las únicas donde se puede votar.
                // Si la consulta falla, la votación queda sin entrada en el mapa y la
                // interfaz lo tratará como desconocido, nunca como "no ha votado".
                Map<Integer, Boolean> participacion = new HashMap<>();
                for (VotacionModel v : lista) {
                    if (!v.isAbierta() || v.getId() == null) continue;
                    try {
                        participacion.put(v.getId(), apiClient.verificarParticipacion(v.getId()).yaVoto());
                    } catch (Exception ignored) {
                        // Desconocido: se omite deliberadamente del mapa.
                    }
                }
                return new CargaVotaciones(lista, participacion);
            }
        };

        task.setOnSucceeded(e -> {
            CargaVotaciones carga = task.getValue();
            List<VotacionModel> lista = carga.votaciones();

            participacionPorVotacion.clear();
            participacionPorVotacion.putAll(carga.participacion());

            votacionesList.setAll(lista);
            paginator.updatePagination();

            actualizarResumen(lista);

            if (!lista.isEmpty()) {
                tablaVotaciones.getSelectionModel().selectFirst();
            } else {
                limpiarDetalle();
            }
        });

        task.setOnFailed(e -> {
            // Un fallo al cargar se trata igual que el de cualquier acción: visible.
            participacionPorVotacion.clear();
            votacionesList.clear();
            paginator.updatePagination();
            limpiarDetalle();

            Throwable ex = task.getException();
            mostrarAlerta(Alert.AlertType.ERROR, "Error",
                "No fue posible cargar las votaciones: "
                    + (ex != null && ex.getMessage() != null ? ex.getMessage() : "error desconocido."));
        });

        new Thread(task).start();
    }

    private void actualizarResumen(List<VotacionModel> lista) {
        long abiertas = lista.stream().filter(VotacionModel::isAbierta).count();
        long borradores = lista.stream().filter(VotacionModel::isBorrador).count();
        long programadas = lista.stream().filter(VotacionModel::isProgramada).count();
        long cerradas = lista.stream().filter(VotacionModel::isCerrada).count();
        int totalVotos = lista.stream().mapToInt(VotacionModel::getTotalVotos).sum();

        lblTotalAbiertas.setText(abiertas + " activas");
        // La tarjeta agrupa las no iniciadas. Se detallan por separado para que
        // PROGRAMADA no quede invisible en el resumen ahora que es un estado propio.
        lblTotalBorradores.setText(programadas > 0
            ? (borradores + programadas) + " procesos (" + programadas + " programadas)"
            : borradores + " procesos");
        lblTotalCerradas.setText(cerradas + " concluidas");
        lblTotalVotos.setText(totalVotos + " votos");

        if (abiertas > 0) {
            lblAbiertasDetalle.setText("Recepción de votos en curso");
        } else {
            lblAbiertasDetalle.setText("Sin votaciones abiertas");
        }
    }

    private void mostrarDetalle(VotacionModel m) {
        boolean publicados = m.isResultadosPublicados();

        lblDetalleTitulo.setText((publicados ? "Opciones y resultados: " : "Opciones: ") + m.getTitulo());
        lblDetalleEstado.setText(publicados
            ? "Estado: " + m.getEstado() + " | Total votos: " + m.getTotalVotos()
            : "Estado: " + m.getEstado() + " | Los resultados se publicarán al cerrar la votación.");
        flowOpciones.getChildren().clear();

        if (m.getOpciones().isEmpty()) {
            Label lblVacio = new Label("No hay opciones registradas en esta votación.");
            lblVacio.setStyle("-fx-text-fill: #64748b; -fx-font-style: italic;");
            flowOpciones.getChildren().add(lblVacio);
            return;
        }

        for (VotacionModel.OpcionModel op : m.getOpciones()) {
            VBox card = new VBox(4);
            card.setStyle("-fx-background-color: white; -fx-border-color: #cbd5e1; -fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 10 14 10 14; -fx-min-width: 170;");

            Label lblDesc = new Label(op.getDescripcion());
            lblDesc.setStyle("-fx-font-weight: bold; -fx-text-fill: #1e293b;");
            card.getChildren().add(lblDesc);

            // Mientras no se publica, las opciones se listan sin conteos, porcentajes ni
            // barras. Pintar ceros se leería como "nadie ha votado" cuando en realidad
            // el backend aún no publica los resultados.
            if (publicados) {
                // El porcentaje lo calcula el backend (SCRUM-260); aquí solo se muestra.
                Label lblVotos = new Label(
                    op.getVotos() + " votos (" + String.format("%.1f", op.getPorcentaje()) + "%)");
                lblVotos.setStyle("-fx-text-fill: #2563eb; -fx-font-size: 11px;");

                ProgressBar bar = new ProgressBar(op.getFraccion());
                bar.setPrefWidth(150);

                card.getChildren().addAll(lblVotos, bar);
            }

            flowOpciones.getChildren().add(card);
        }
    }

    private void limpiarDetalle() {
        lblDetalleTitulo.setText("Selecciona una votación para ver sus opciones y resultados");
        lblDetalleEstado.setText("");
        flowOpciones.getChildren().clear();
    }

    @FXML
    public void abrirFormularioCrear() {
        abrirModal(null);
    }

    private void abrirFormularioEditar(VotacionModel model) {
        abrirModal(model);
    }

    private void abrirModal(VotacionModel model) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/views/votacion-form.fxml"));
            Parent root = loader.load();

            VotacionFormController ctrl = loader.getController();
            ctrl.setVotacionEdicion(model);
            ctrl.setOnSavedCallback(this::cargarDatos);

            Stage stage = new Stage();
            stage.setTitle(model == null ? "Nueva votación comunal" : "Editar votación comunal");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(root));
            ResponsiveWindowService.fitModalStage(stage);
            stage.showAndWait();
        } catch (Exception e) {
            e.printStackTrace();
            mostrarAlerta(Alert.AlertType.ERROR, "Error", "No se pudo abrir el formulario: " + e.getMessage());
        }
    }

    public void abrirModalOpciones(VotacionModel v) {
        if (v == null) return;
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/views/opciones-votacion-modal.fxml"));
            Parent root = loader.load();

            OpcionesVotacionModalController ctrl = loader.getController();
            ctrl.setVotacion(v);
            ctrl.setOnCloseCallback(this::cargarDatos);

            Stage stage = new Stage();
            stage.setTitle("Opciones de votación: " + v.getTitulo());
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(root));
            ResponsiveWindowService.fitModalStage(stage);
            stage.showAndWait();
        } catch (Exception e) {
            e.printStackTrace();
            mostrarAlerta(Alert.AlertType.ERROR, "Error", "No se pudo abrir el gestor de opciones: " + e.getMessage());
        }
    }

    public void abrirPapeleta(VotacionModel v) {
        if (v == null) return;
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/views/papeleta-votacion-modal.fxml"));
            Parent root = loader.load();

            PapeletaVotacionModalController ctrl = loader.getController();
            ctrl.setVotacion(v);
            ctrl.setOnVotedCallback(this::cargarDatos);

            Stage stage = new Stage();
            stage.setTitle("Emitir voto: " + v.getTitulo());
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(root));
            ResponsiveWindowService.fitModalStage(stage);
            stage.showAndWait();
        } catch (Exception e) {
            e.printStackTrace();
            mostrarAlerta(Alert.AlertType.ERROR, "Error", "No se pudo abrir la papeleta de votación: " + e.getMessage());
        }
    }

    private void abrirVotacion(VotacionModel m) {
        if (m.getTotalOpciones() < 2) {
            mostrarAlerta(Alert.AlertType.WARNING, "Requisito de opciones",
                "Para abrir este proceso de votación comunal se requieren al menos dos opciones registradas.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Abrir proceso de votación");
        confirm.setHeaderText("¿Deseas abrir la votación \"" + m.getTitulo() + "\"?");
        confirm.setContentText("Al abrirla, quedará disponible para que los miembros activos puedan emitir sus votos.");

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                Task<Void> task = new Task<>() {
                    @Override
                    protected Void call() throws Exception {
                        apiClient.abrir(m.getId());
                        return null;
                    }
                };
                task.setOnSucceeded(e -> cargarDatos());
                task.setOnFailed(e -> mostrarAlerta(Alert.AlertType.ERROR, "Error",
                    task.getException() != null ? task.getException().getMessage() : "Error al abrir la votación."));
                new Thread(task).start();
            }
        });
    }

    private void cerrarVotacion(VotacionModel m) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Cerrar proceso de votación");
        confirm.setHeaderText("¿Deseas cerrar definitivamente la votación \"" + m.getTitulo() + "\"?");
        confirm.setContentText("ATENCIÓN: El cierre es irreversible. No se admitirán nuevos votos tras el cierre.");

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                Task<Void> task = new Task<>() {
                    @Override
                    protected Void call() throws Exception {
                        apiClient.cerrar(m.getId());
                        return null;
                    }
                };
                task.setOnSucceeded(e -> cargarDatos());
                task.setOnFailed(e -> mostrarAlerta(Alert.AlertType.ERROR, "Error",
                    task.getException() != null ? task.getException().getMessage() : "Error al cerrar la votación."));
                new Thread(task).start();
            }
        });
    }

    private void eliminarVotacion(VotacionModel m) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Eliminar votación");
        confirm.setHeaderText("¿Eliminar la votación en borrador \"" + m.getTitulo() + "\"?");
        confirm.setContentText("Esta acción eliminará el proceso y sus opciones asociadas.");

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                Task<Void> task = new Task<>() {
                    @Override
                    protected Void call() throws Exception {
                        apiClient.delete(m.getId());
                        return null;
                    }
                };
                task.setOnSucceeded(e -> cargarDatos());
                task.setOnFailed(e -> mostrarAlerta(Alert.AlertType.ERROR, "Error",
                    task.getException() != null ? task.getException().getMessage() : "No se pudo eliminar la votación."));
                new Thread(task).start();
            }
        });
    }

    private void mostrarAlerta(Alert.AlertType type, String title, String msg) {
        Platform.runLater(() -> {
            Alert alert = new Alert(type);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(msg);
            alert.showAndWait();
        });
    }

    /** Resultado de una carga: las votaciones y la participación resuelta para las abiertas. */
    private record CargaVotaciones(List<VotacionModel> votaciones, Map<Integer, Boolean> participacion) {}

    /**
     * Botón de voto para una votación abierta, según lo que se sepa de la participación:
     * ya votó, no ha votado, o no se pudo determinar. El tercer caso no habilita el voto.
     */
    private Button construirBotonVoto(VotacionModel m) {
        Boolean yaVoto = participacionPorVotacion.get(m.getId());

        if (Boolean.TRUE.equals(yaVoto)) {
            Button btn = new Button("Ya votaste");
            btn.setDisable(true);
            btn.setStyle("-fx-background-color: #e2e8f0; -fx-text-fill: #475569; -fx-font-weight: bold; -fx-padding: 3 8 3 8; -fx-background-radius: 4;");
            btn.setTooltip(new Tooltip("Ya emitiste tu voto en esta votación."));
            return btn;
        }

        Button btn = new Button("Votar");
        btn.setStyle("-fx-background-color: #10b981; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 3 8 3 8; -fx-background-radius: 4; -fx-cursor: hand;");

        if (yaVoto == null) {
            btn.setDisable(true);
            btn.setStyle("-fx-background-color: #e2e8f0; -fx-text-fill: #94a3b8; -fx-font-weight: bold; -fx-padding: 3 8 3 8; -fx-background-radius: 4;");
            btn.setTooltip(new Tooltip(
                "No fue posible verificar si ya votaste. Actualiza la lista para reintentar."));
            return btn;
        }

        btn.setOnAction(e -> abrirPapeleta(m));
        return btn;
    }

    private boolean canManage() {
        return SessionManager.getInstance().getCurrentUser()
            .map(u -> {
                String r = u.getRole();
                return r != null && ("ADMIN".equalsIgnoreCase(r) || "ADMINISTRADOR".equalsIgnoreCase(r) || "PRESIDENTE".equalsIgnoreCase(r));
            })
            .orElse(false);
    }
}
