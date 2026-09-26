package controller;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
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
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import models.AportacionModel;
import models.AuthUser;
import models.MantenimientoMiembroModel;
import models.MiembroModel;
import security.SessionManager;
import service.AportacionApiClient;
import service.HeroIcon;
import service.MaterialAlertService;
import service.MiembroApiClient;
import service.ResponsiveWindowService;

public class AportacionesController {
    // --- Segmented Tabs ---
    @FXML private Button btnTabMantenimiento;
    @FXML private Button btnTabHistorial;
    @FXML private VBox contenedorMantenimiento;
    @FXML private VBox contenedorHistorial;

    // --- Controles de Mantenimiento Mensual ---
    @FXML private Button btnMantMesAnterior;
    @FXML private Label lblMantPeriodoTexto;
    @FXML private Button btnMantMesSiguiente;
    @FXML private Button btnMantMesActual;
    @FXML private TextField campoBusquedaMant;
    @FXML private ComboBox<String> comboFiltroMantEstado;
    @FXML private Button btnModificarCuota;
    @FXML private Button btnRegistrarPagoMant;

    // --- KPIs de Mantenimiento Mensual ---
    @FXML private Label lblMantCuotaBase;
    @FXML private Label lblMantRecaudado;
    @FXML private Label lblMantRecaudadoSub;
    @FXML private Label lblMantPagados;
    @FXML private Label lblMantPendientes;

    // --- Tabla de Mantenimiento Mensual ---
    @FXML private TableView<MantenimientoMiembroModel> tablaMantenimiento;
    @FXML private TableColumn<MantenimientoMiembroModel, String> colMantMiembro;
    @FXML private TableColumn<MantenimientoMiembroModel, String> colMantDui;
    @FXML private TableColumn<MantenimientoMiembroModel, String> colMantTelefono;
    @FXML private TableColumn<MantenimientoMiembroModel, String> colMantCuota;
    @FXML private TableColumn<MantenimientoMiembroModel, String> colMantEstado;
    @FXML private TableColumn<MantenimientoMiembroModel, String> colMantFechaPago;
    @FXML private TableColumn<MantenimientoMiembroModel, String> colMantMonto;
    @FXML private TableColumn<MantenimientoMiembroModel, String> colMantMetodo;
    @FXML private TableColumn<MantenimientoMiembroModel, String> colMantReferencia;
    @FXML private TableColumn<MantenimientoMiembroModel, Void>   colMantAcciones;
    @FXML private Label lblMantResumenPie;
    @FXML private Label lblMantSolvenciaPie;

    // --- Tabla y Controles de Historial General (Vista 2) ---
    @FXML private TableView<AportacionModel> tablaAportaciones;
    @FXML private TableColumn<AportacionModel, String> columnaFecha;
    @FXML private TableColumn<AportacionModel, String> columnaMiembro;
    @FXML private TableColumn<AportacionModel, String> columnaDui;
    @FXML private TableColumn<AportacionModel, String> columnaPeriodo;
    @FXML private TableColumn<AportacionModel, String> columnaMonto;
    @FXML private TableColumn<AportacionModel, String> columnaMetodo;
    @FXML private TableColumn<AportacionModel, String> columnaProyecto;
    @FXML private TableColumn<AportacionModel, String> columnaReferencia;
    @FXML private TableColumn<AportacionModel, String> columnaEstado;
    @FXML private TableColumn<AportacionModel, Void>   columnaAcciones;

    @FXML private Label lblTotalRecaudado;
    @FXML private Label lblTotalRegistros;
    @FXML private Label lblPeriodoActual;
    @FXML private Label lblPaginacion;

    @FXML private Label lblStatTotalRecaudado;
    @FXML private Label lblStatTotalRegistros;
    @FXML private Label lblStatPromedio;
    @FXML private Label lblStatPeriodo;
    @FXML private Label lblStatSubtextoRecaudado;
    @FXML private Label lblStatFiltroInfo;

    @FXML private TextField campoFiltroPeriodo;
    @FXML private ComboBox<String> comboMetodo;
    @FXML private ComboBox<String> comboEstado;
    @FXML private ComboBox<String> comboPageSize;
    @FXML private TextField campoBusqueda;

    @FXML private Button btnNuevaAportacion;
    @FXML private Button btnVerRecibo;
    @FXML private Button btnEditar;
    @FXML private Button btnAnular;
    @FXML private Button btnRefrescar;
    @FXML private Button btnAnterior;
    @FXML private Button btnSiguiente;

    // --- Estado de Mantenimiento ---
    private String periodoMantenimientoActual = LocalDate.now().toString().substring(0, 7);
    private BigDecimal cuotaMantenimientoBase = new BigDecimal("10.00");
    private final ObservableList<MantenimientoMiembroModel> listaMantenimiento = FXCollections.observableArrayList();
    private FilteredList<MantenimientoMiembroModel> listaMantenimientoFiltrada;

    // --- Estado de Historial General ---
    private final ObservableList<AportacionModel> aportaciones = FXCollections.observableArrayList();
    private int currentPage = 1;
    private int pageSize = 10;
    private int totalPages = 1;
    private int totalRegistros = 0;
    private BigDecimal totalRecaudado = BigDecimal.ZERO;
    private boolean puedeGestionar = false;

    @FXML
    private void initialize() {
        AuthUser user = SessionManager.getInstance().requireCurrentUser();
        puedeGestionar = isAdministrator(user.getRole()) || "TESORERO".equalsIgnoreCase(user.getRole());

        // Inicializar Vista 1: Mantenimiento Mensual
        initMantenimientoView();

        // Inicializar Vista 2: Historial General
        initHistorialView();

        // Cargar datos
        cargarMantenimientoPeriodo();
        loadPage();
    }

    private void initMantenimientoView() {
        if (comboFiltroMantEstado != null) {
            comboFiltroMantEstado.getItems().setAll("TODOS", "SOLO PAGADOS", "SOLO PENDIENTES");
            comboFiltroMantEstado.setValue("TODOS");
            comboFiltroMantEstado.valueProperty().addListener((obs, o, n) -> filtrarMantenimiento());
        }

        if (campoBusquedaMant != null) {
            campoBusquedaMant.textProperty().addListener((obs, o, n) -> filtrarMantenimiento());
        }

        if (btnModificarCuota != null) {
            btnModificarCuota.setVisible(puedeGestionar);
            btnModificarCuota.setManaged(puedeGestionar);
        }
        if (btnRegistrarPagoMant != null) {
            btnRegistrarPagoMant.setVisible(puedeGestionar);
            btnRegistrarPagoMant.setManaged(puedeGestionar);
        }

        colMantMiembro.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getNombreCompleto()));
        colMantDui.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getDui() != null ? cell.getValue().getDui() : "-"));
        colMantTelefono.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getTelefono() != null ? cell.getValue().getTelefono() : "-"));
        colMantCuota.setCellValueFactory(cell -> new SimpleStringProperty(
            cell.getValue().getCuotaEsperada() != null ? String.format("$%.2f", cell.getValue().getCuotaEsperada()) : "$10.00"
        ));

        // Columna Estado con badge moderno
        colMantEstado.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getEstadoTexto()));
        colMantEstado.setCellFactory(col -> new TableCell<>() {
            private final Label badge = new Label();
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    boolean pagado = "PAGADO".equalsIgnoreCase(item);
                    badge.setText(item);
                    if (pagado) {
                        badge.setStyle("-fx-background-color: #dcfce7; -fx-text-fill: #15803d; -fx-border-color: #bbf7d0; -fx-border-radius: 9999px; -fx-background-radius: 9999px; -fx-font-weight: 800; -fx-padding: 3px 10px; -fx-font-size: 11px;");
                    } else {
                        badge.setStyle("-fx-background-color: #fef3c7; -fx-text-fill: #b45309; -fx-border-color: #fde68a; -fx-border-radius: 9999px; -fx-background-radius: 9999px; -fx-font-weight: 800; -fx-padding: 3px 10px; -fx-font-size: 11px;");
                    }
                    setGraphic(badge);
                    setText(null);
                    setAlignment(Pos.CENTER_LEFT);
                }
            }
        });

        colMantFechaPago.setCellValueFactory(cell -> new SimpleStringProperty(
            cell.getValue().getFechaPago() != null ? cell.getValue().getFechaPago().toString() : "—"
        ));
        colMantMonto.setCellValueFactory(cell -> new SimpleStringProperty(
            cell.getValue().getMontoPagado() != null ? String.format("$%.2f", cell.getValue().getMontoPagado()) : "—"
        ));
        colMantMetodo.setCellValueFactory(cell -> new SimpleStringProperty(
            cell.getValue().getMetodoPago() != null ? cell.getValue().getMetodoPago() : "—"
        ));
        colMantReferencia.setCellValueFactory(cell -> new SimpleStringProperty(
            cell.getValue().getReferencia() != null && !cell.getValue().getReferencia().isBlank() ? cell.getValue().getReferencia() : "—"
        ));

        // Acciones en la tabla de mantenimiento: "+ Registrar Pago" directo o Ver/Editar
        if (colMantAcciones != null) {
            colMantAcciones.setCellFactory(col -> new TableCell<>() {
                private final Button btnPagar = new Button("+ Registrar pago");
                private final Button btnRecibo = new Button();
                private final HBox box = new HBox(6);

                {
                    box.setAlignment(Pos.CENTER_RIGHT);
                    btnPagar.setStyle("-fx-background-color: #16a34a; -fx-text-fill: white; -fx-font-weight: 700; -fx-background-radius: 6px; -fx-padding: 4px 10px; -fx-cursor: hand; -fx-font-size: 11px;");
                    btnPagar.setOnAction(e -> {
                        MantenimientoMiembroModel m = getTableView().getItems().get(getIndex());
                        if (m != null) {
                            abrirFormularioCrearParaMiembro(m.getIdMiembro(), periodoMantenimientoActual, m.getCuotaEsperada());
                        }
                    });

                    btnRecibo.getStyleClass().addAll("btn-row-action", "btn-action-view");
                    btnRecibo.setGraphic(HeroIcon.create(HeroIcon.DOCUMENT_TEXT, HeroIcon.BLUE_600, 16));
                    btnRecibo.setTooltip(new Tooltip("Ver comprobante"));
                    btnRecibo.setOnAction(e -> {
                        MantenimientoMiembroModel m = getTableView().getItems().get(getIndex());
                        if (m != null && m.getIdAportacion() != null) {
                            verReciboPorId(m.getIdAportacion());
                        }
                    });
                }

                @Override
                protected void updateItem(Void item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || getIndex() < 0 || getIndex() >= getTableView().getItems().size()) {
                        setGraphic(null);
                    } else {
                        MantenimientoMiembroModel m = getTableView().getItems().get(getIndex());
                        box.getChildren().clear();
                        if (m.isPagado()) {
                            box.getChildren().add(btnRecibo);
                        } else if (puedeGestionar) {
                            box.getChildren().add(btnPagar);
                        }
                        setGraphic(box);
                        setAlignment(Pos.CENTER_RIGHT);
                    }
                }
            });
        }

        listaMantenimientoFiltrada = new FilteredList<>(listaMantenimiento, p -> true);
        tablaMantenimiento.setItems(listaMantenimientoFiltrada);
    }

    private void initHistorialView() {
        if (comboPageSize != null) {
            comboPageSize.getItems().setAll("5", "10", "25", "50");
            comboPageSize.setValue("10");
            comboPageSize.valueProperty().addListener((obs, o, n) -> {
                if (n != null) {
                    try {
                        pageSize = Integer.parseInt(n.trim());
                        currentPage = 1;
                        loadPage();
                    } catch (NumberFormatException ignored) {}
                }
            });
        }

        if (btnNuevaAportacion != null) {
            btnNuevaAportacion.setVisible(puedeGestionar);
            btnNuevaAportacion.setManaged(puedeGestionar);
        }
        if (btnEditar != null) {
            btnEditar.setVisible(puedeGestionar);
            btnEditar.setManaged(puedeGestionar);
        }
        if (btnAnular != null) {
            btnAnular.setVisible(puedeGestionar);
            btnAnular.setManaged(puedeGestionar);
        }

        columnaFecha.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getFechaPago()));
        columnaMiembro.setCellValueFactory(cell -> new SimpleStringProperty(
            cell.getValue().getNombreMiembro() != null ? cell.getValue().getNombreMiembro() : "Miembro #" + cell.getValue().getIdMiembro()
        ));
        columnaDui.setCellValueFactory(cell -> new SimpleStringProperty(
            cell.getValue().getDuiMiembro() != null ? cell.getValue().getDuiMiembro() : "-"
        ));
        columnaPeriodo.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getPeriodoMes()));
        columnaMonto.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getMontoFormateado()));
        columnaMetodo.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getMetodoPago()));
        columnaProyecto.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getProyectoDisplay()));
        columnaReferencia.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getReferenciaDisplay()));
        columnaEstado.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getEstado()));

        // Estilos para la columna de estado
        columnaEstado.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    if ("REGISTRADA".equalsIgnoreCase(item)) {
                        setStyle("-fx-text-fill: -color-success-fg; -fx-font-weight: bold;");
                    } else {
                        setStyle("-fx-text-fill: -color-danger-fg; -fx-font-style: italic;");
                    }
                }
            }
        });

        if (columnaAcciones != null) {
            columnaAcciones.setCellFactory(col -> new TableCell<>() {
                private final Button btnRecibo = new Button();
                private final Button btnEditarFila = new Button();
                private final Button btnAnular = new Button();
                private final HBox box = new HBox(8);
                {
                    box.getStyleClass().add("row-actions-box");
                    box.setAlignment(Pos.CENTER_RIGHT);

                    btnRecibo.getStyleClass().addAll("btn-row-action", "btn-action-view");
                    btnRecibo.setGraphic(HeroIcon.create(HeroIcon.DOCUMENT_TEXT, HeroIcon.BLUE_600, 18));
                    btnRecibo.setTooltip(new Tooltip("Ver comprobante / recibo"));
                    btnRecibo.setOnAction(e -> {
                        AportacionModel a = getTableView().getItems().get(getIndex());
                        if (a != null) {
                            tablaAportaciones.getSelectionModel().select(a);
                            verRecibo();
                        }
                    });

                    btnEditarFila.getStyleClass().addAll("btn-row-action", "btn-action-edit");
                    btnEditarFila.setGraphic(HeroIcon.create(HeroIcon.PENCIL, HeroIcon.AMBER_600, 18));
                    btnEditarFila.setTooltip(new Tooltip("Ajustar cantidad / Editar pago"));
                    btnEditarFila.setOnAction(e -> {
                        AportacionModel a = getTableView().getItems().get(getIndex());
                        if (a != null) {
                            tablaAportaciones.getSelectionModel().select(a);
                            abrirFormularioEditar();
                        }
                    });

                    btnAnular.getStyleClass().addAll("btn-row-action", "btn-action-delete");
                    btnAnular.setGraphic(HeroIcon.create(HeroIcon.BAN, HeroIcon.RED_600, 18));
                    btnAnular.setTooltip(new Tooltip("Anular aportación"));
                    btnAnular.setOnAction(e -> {
                        AportacionModel a = getTableView().getItems().get(getIndex());
                        if (a != null) {
                            tablaAportaciones.getSelectionModel().select(a);
                            anularAportacion();
                        }
                    });
                }

                @Override
                protected void updateItem(Void item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || getIndex() < 0 || getIndex() >= getTableView().getItems().size()) {
                        setGraphic(null);
                    } else {
                        setAlignment(Pos.CENTER_RIGHT);
                        AportacionModel a = getTableView().getItems().get(getIndex());
                        box.getChildren().clear();
                        box.getChildren().add(btnRecibo);
                        if (puedeGestionar && "REGISTRADA".equalsIgnoreCase(a.getEstado())) {
                            box.getChildren().add(btnEditarFila);
                            box.getChildren().add(btnAnular);
                        }
                        setGraphic(box);
                    }
                }
            });
        }

        tablaAportaciones.setItems(aportaciones);

        comboMetodo.getItems().setAll("TODOS", "EFECTIVO", "TRANSFERENCIA", "OTRO");
        comboMetodo.setValue("TODOS");

        comboEstado.getItems().setAll("TODOS", "REGISTRADA", "ANULADA");
        comboEstado.setValue("TODOS");

        comboMetodo.valueProperty().addListener((obs, o, n) -> { currentPage = 1; loadPage(); });
        comboEstado.valueProperty().addListener((obs, o, n) -> { currentPage = 1; loadPage(); });
        campoFiltroPeriodo.textProperty().addListener((obs, o, n) -> { currentPage = 1; loadPage(); });
        campoBusqueda.textProperty().addListener((obs, o, n) -> { currentPage = 1; loadPage(); });

        if (btnVerRecibo != null) {
            btnVerRecibo.disableProperty().bind(tablaAportaciones.getSelectionModel().selectedItemProperty().isNull());
        }
        if (btnEditar != null) {
            btnEditar.disableProperty().bind(tablaAportaciones.getSelectionModel().selectedItemProperty().isNull());
        }
        if (btnAnular != null) {
            btnAnular.disableProperty().bind(tablaAportaciones.getSelectionModel().selectedItemProperty().isNull());
        }

        tablaAportaciones.setRowFactory(tv -> {
            TableRow<AportacionModel> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && (!row.isEmpty())) {
                    verRecibo();
                }
            });
            return row;
        });
    }

    // =========================================================================
    // NAVEGACIÓN ENTRE PESTAÑAS (Mantenimiento vs Historial)
    // =========================================================================
    @FXML
    private void seleccionarTabMantenimiento() {
        if (contenedorMantenimiento != null) {
            contenedorMantenimiento.setVisible(true);
            contenedorMantenimiento.setManaged(true);
        }
        if (contenedorHistorial != null) {
            contenedorHistorial.setVisible(false);
            contenedorHistorial.setManaged(false);
        }
        if (btnTabMantenimiento != null) {
            btnTabMantenimiento.setStyle("-fx-background-color: #2563eb; -fx-text-fill: white; -fx-font-weight: 700; -fx-background-radius: 9999px; -fx-padding: 8px 18px; -fx-cursor: hand; -fx-font-size: 13px;");
        }
        if (btnTabHistorial != null) {
            btnTabHistorial.setStyle("-fx-background-color: #f1f5f9; -fx-text-fill: #475569; -fx-font-weight: 700; -fx-background-radius: 9999px; -fx-padding: 8px 18px; -fx-cursor: hand; -fx-font-size: 13px;");
        }
        cargarMantenimientoPeriodo();
    }

    @FXML
    private void seleccionarTabHistorial() {
        if (contenedorMantenimiento != null) {
            contenedorMantenimiento.setVisible(false);
            contenedorMantenimiento.setManaged(false);
        }
        if (contenedorHistorial != null) {
            contenedorHistorial.setVisible(true);
            contenedorHistorial.setManaged(true);
        }
        if (btnTabMantenimiento != null) {
            btnTabMantenimiento.setStyle("-fx-background-color: #f1f5f9; -fx-text-fill: #475569; -fx-font-weight: 700; -fx-background-radius: 9999px; -fx-padding: 8px 18px; -fx-cursor: hand; -fx-font-size: 13px;");
        }
        if (btnTabHistorial != null) {
            btnTabHistorial.setStyle("-fx-background-color: #2563eb; -fx-text-fill: white; -fx-font-weight: 700; -fx-background-radius: 9999px; -fx-padding: 8px 18px; -fx-cursor: hand; -fx-font-size: 13px;");
        }
        loadPage();
    }

    // =========================================================================
    // LÓGICA DE MANTENIMIENTO MENSUAL DE LA COLONIA
    // =========================================================================
    @FXML
    private void mantenimientoMesAnterior() {
        try {
            LocalDate d = LocalDate.parse(periodoMantenimientoActual + "-01").minusMonths(1);
            periodoMantenimientoActual = d.toString().substring(0, 7);
            cargarMantenimientoPeriodo();
        } catch (Exception ignored) {}
    }

    @FXML
    private void mantenimientoMesSiguiente() {
        try {
            LocalDate d = LocalDate.parse(periodoMantenimientoActual + "-01").plusMonths(1);
            periodoMantenimientoActual = d.toString().substring(0, 7);
            cargarMantenimientoPeriodo();
        } catch (Exception ignored) {}
    }

    @FXML
    private void mantenimientoMesActual() {
        periodoMantenimientoActual = LocalDate.now().toString().substring(0, 7);
        cargarMantenimientoPeriodo();
    }

    @FXML
    private void modificarCuotaBase() {
        Window owner = (tablaMantenimiento != null && tablaMantenimiento.getScene() != null)
            ? tablaMantenimiento.getScene().getWindow() : null;
        BigDecimal nueva = MaterialAlertService.dialogoModificarCuota(owner, cuotaMantenimientoBase);
        if (nueva != null) {
            Task<BigDecimal> task = new Task<>() {
                @Override
                protected BigDecimal call() throws Exception {
                    return new AportacionApiClient().updateCuotaMantenimiento(nueva);
                }
            };
            task.setOnSucceeded(e -> {
                cuotaMantenimientoBase = task.getValue();
                if (lblMantCuotaBase != null) {
                    lblMantCuotaBase.setText(String.format("$%.2f", cuotaMantenimientoBase));
                }
                cargarMantenimientoPeriodo();
                MaterialAlertService.exito(owner, "Cuota de Mantenimiento Actualizada",
                    "La nueva cuota mensual base fijada para la colonia es de $" + cuotaMantenimientoBase);
            });
            task.setOnFailed(e -> {
                MaterialAlertService.error(owner, "Error al actualizar cuota",
                    task.getException() != null ? task.getException().getMessage() : "No se pudo actualizar.");
            });
            Thread t = new Thread(task, "update-cuota");
            t.setDaemon(true);
            t.start();
        }
    }

    private void cargarMantenimientoPeriodo() {
        if (lblMantPeriodoTexto != null) {
            lblMantPeriodoTexto.setText(periodoMantenimientoActual);
        }
        if (tablaMantenimiento != null) {
            tablaMantenimiento.setPlaceholder(new Label("Consultando aportaciones de " + periodoMantenimientoActual + "..."));
        }

        Task<AportacionApiClient.MantenimientoPeriodoResult> task = new Task<>() {
            @Override
            protected AportacionApiClient.MantenimientoPeriodoResult call() throws Exception {
                return new AportacionApiClient().getMantenimientoPeriodo(
                    periodoMantenimientoActual,
                    campoBusquedaMant != null ? campoBusquedaMant.getText() : null
                );
            }
        };

        task.setOnSucceeded(e -> {
            AportacionApiClient.MantenimientoPeriodoResult res = task.getValue();
            if (res != null) {
                cuotaMantenimientoBase = res.cuotaEsperada() != null ? res.cuotaEsperada() : new BigDecimal("10.00");
                if (lblMantCuotaBase != null) {
                    lblMantCuotaBase.setText(String.format("$%.2f", cuotaMantenimientoBase));
                }
                if (lblMantRecaudado != null) {
                    lblMantRecaudado.setText(String.format("$%.2f", res.totalRecaudado()));
                }
                if (lblMantRecaudadoSub != null) {
                    lblMantRecaudadoSub.setText("De $" + String.format("%.2f", res.totalEsperado()) + " previsto");
                }
                if (lblMantPagados != null) {
                    lblMantPagados.setText(String.valueOf(res.pagados()));
                }
                if (lblMantPendientes != null) {
                    lblMantPendientes.setText(String.valueOf(res.pendientes()));
                }

                List<MantenimientoMiembroModel> items = new java.util.ArrayList<>();
                if (res.items() != null) {
                    for (AportacionApiClient.MantenimientoPeriodoResult.Item it : res.items()) {
                        MantenimientoMiembroModel m = new MantenimientoMiembroModel();
                        m.setIdMiembro(it.idMiembro());
                        m.setNombreCompleto(it.nombreCompleto());
                        m.setDui(it.dui());
                        m.setTelefono(it.telefono());
                        m.setDireccion(it.direccion());
                        m.setPeriodoMes(res.periodoMes());
                        m.setCuotaEsperada(res.cuotaEsperada());
                        m.setPagado(it.pagado());
                        m.setIdAportacion(it.idAportacion());
                        m.setMontoPagado(it.montoPagado());
                        if (it.fechaPago() != null) {
                            try {
                                m.setFechaPago(LocalDate.parse(it.fechaPago()));
                            } catch (Exception ignored) {}
                        }
                        m.setMetodoPago(it.metodoPago());
                        m.setReferencia(it.referencia());
                        m.setEstadoAportacion(it.estadoAportacion());
                        items.add(m);
                    }
                }
                listaMantenimiento.setAll(items);
                filtrarMantenimiento();
            }
        });

        task.setOnFailed(e -> {
            cargarMantenimientoFallback();
        });

        Thread t = new Thread(task, "mantenimiento-periodo");
        t.setDaemon(true);
        t.start();
    }

    private void cargarMantenimientoFallback() {
        Task<List<MantenimientoMiembroModel>> task = new Task<>() {
            @Override
            protected List<MantenimientoMiembroModel> call() throws Exception {
                List<MiembroModel> miembros = new MiembroApiClient().findAll();
                AportacionModel.Page page = new AportacionApiClient().findPage(
                    null, null, periodoMantenimientoActual, null, null, null, "REGISTRADA", null, 1, 5000
                );
                java.util.Map<Integer, AportacionModel> aportePorMiembro = new java.util.HashMap<>();
                if (page.getItems() != null) {
                    for (AportacionModel a : page.getItems()) {
                        if (a.getIdProyecto() == null && a.getIdMiembro() != null) {
                            aportePorMiembro.put(a.getIdMiembro(), a);
                        }
                    }
                }

                List<MantenimientoMiembroModel> items = new java.util.ArrayList<>();
                for (MiembroModel m : miembros) {
                    if (m.getEstado() != null && !"ACTIVO".equalsIgnoreCase(m.getEstado())) continue;
                    MantenimientoMiembroModel item = new MantenimientoMiembroModel();
                    item.setIdMiembro(m.getId());
                    item.setNombreCompleto(m.getNombreCompleto());
                    item.setDui(m.getDui());
                    item.setTelefono(m.getTelefono());
                    item.setDireccion(m.getDireccion());
                    item.setPeriodoMes(periodoMantenimientoActual);
                    item.setCuotaEsperada(cuotaMantenimientoBase);

                    AportacionModel ap = aportePorMiembro.get(m.getId());
                    if (ap != null) {
                        item.setPagado(true);
                        item.setIdAportacion(ap.getIdAportacion());
                        item.setMontoPagado(ap.getMonto());
                        if (ap.getFechaPago() != null) {
                            try { item.setFechaPago(LocalDate.parse(ap.getFechaPago())); } catch (Exception ignored) {}
                        }
                        item.setMetodoPago(ap.getMetodoPago());
                        item.setReferencia(ap.getReferencia());
                        item.setEstadoAportacion(ap.getEstado());
                    } else {
                        item.setPagado(false);
                    }
                    items.add(item);
                }
                return items;
            }
        };

        task.setOnSucceeded(e -> {
            listaMantenimiento.setAll(task.getValue());
            filtrarMantenimiento();
        });

        task.setOnFailed(e -> {
            if (tablaMantenimiento != null) {
                tablaMantenimiento.setPlaceholder(new Label("No se pudieron cargar los datos de mantenimiento: " + message(task.getException())));
            }
        });

        Thread t = new Thread(task, "mantenimiento-fallback");
        t.setDaemon(true);
        t.start();
    }

    private void filtrarMantenimiento() {
        String texto = campoBusquedaMant != null ? campoBusquedaMant.getText().trim().toLowerCase() : "";
        String filtroEstado = comboFiltroMantEstado != null && comboFiltroMantEstado.getValue() != null
            ? comboFiltroMantEstado.getValue() : "TODOS";

        if (listaMantenimientoFiltrada != null) {
            listaMantenimientoFiltrada.setPredicate(item -> {
                if ("SOLO PAGADOS".equalsIgnoreCase(filtroEstado) && !item.isPagado()) {
                    return false;
                }
                if ("SOLO PENDIENTES".equalsIgnoreCase(filtroEstado) && item.isPagado()) {
                    return false;
                }
                if (!texto.isBlank()) {
                    String nombre = item.getNombreCompleto() != null ? item.getNombreCompleto().toLowerCase() : "";
                    String dui = item.getDui() != null ? item.getDui().toLowerCase() : "";
                    String ref = item.getReferencia() != null ? item.getReferencia().toLowerCase() : "";
                    if (!nombre.contains(texto) && !dui.contains(texto) && !ref.contains(texto)) {
                        return false;
                    }
                }
                return true;
            });
        }

        actualizarResumenMantenimiento();
    }

    private void actualizarResumenMantenimiento() {
        int visibles = listaMantenimientoFiltrada != null ? listaMantenimientoFiltrada.size() : listaMantenimiento.size();
        long totalPagados = listaMantenimiento.stream().filter(MantenimientoMiembroModel::isPagado).count();
        long totalPendientes = listaMantenimiento.size() - totalPagados;

        BigDecimal recaudado = listaMantenimiento.stream()
            .filter(MantenimientoMiembroModel::isPagado)
            .map(m -> m.getMontoPagado() != null ? m.getMontoPagado() : BigDecimal.ZERO)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (lblMantPagados != null) lblMantPagados.setText(String.valueOf(totalPagados));
        if (lblMantPendientes != null) lblMantPendientes.setText(String.valueOf(totalPendientes));
        if (lblMantRecaudado != null) lblMantRecaudado.setText(String.format("$%.2f", recaudado));

        if (lblMantResumenPie != null) {
            lblMantResumenPie.setText(visibles + " miembros listados · " + totalPagados + " al día, " + totalPendientes + " pendientes");
        }

        if (lblMantSolvenciaPie != null) {
            if (!listaMantenimiento.isEmpty()) {
                double pct = ((double) totalPagados / listaMantenimiento.size()) * 100.0;
                lblMantSolvenciaPie.setText(String.format("Solvencia de la colonia: %.1f%%", pct));
            } else {
                lblMantSolvenciaPie.setText("Solvencia: 0%");
            }
        }
    }

    public void abrirFormularioCrearParaMiembro(Integer idMiembro, String periodo, BigDecimal cuota) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/views/aportacion-form.fxml"));
            Parent content = loader.load();
            AportacionFormController controller = loader.getController();
            controller.initMantenimiento(idMiembro, periodo, cuota, () -> {
                cargarMantenimientoPeriodo();
                loadPage();
            });

            Stage stage = new Stage();
            stage.setTitle("Registrar Cuota de Mantenimiento");
            stage.initModality(Modality.APPLICATION_MODAL);
            if (tablaMantenimiento != null && tablaMantenimiento.getScene() != null) {
                stage.initOwner(tablaMantenimiento.getScene().getWindow());
                ResponsiveWindowService.fitModalStage(stage, tablaMantenimiento.getScene().getWindow());
            }
            stage.setScene(new Scene(content));
            stage.setResizable(true);
            stage.showAndWait();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void verReciboPorId(Long idAportacion) {
        Task<AportacionModel> task = new Task<>() {
            @Override
            protected AportacionModel call() throws Exception {
                return new AportacionApiClient().findById(idAportacion);
            }
        };
        task.setOnSucceeded(e -> {
            AportacionModel ap = task.getValue();
            if (ap != null) mostrarReciboModal(ap);
        });
        new Thread(task).start();
    }

    // =========================================================================
    // LÓGICA DE HISTORIAL GENERAL Y AUDITORÍA
    // =========================================================================
    @FXML
    private void abrirFormularioCrear() {
        abrirFormulario(null);
    }

    @FXML
    private void abrirFormularioEditar() {
        AportacionModel sel = tablaAportaciones.getSelectionModel().getSelectedItem();
        if (sel != null) {
            abrirFormulario(sel);
        }
    }

    private void abrirFormulario(AportacionModel aportacion) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/views/aportacion-form.fxml"));
            Parent content = loader.load();
            AportacionFormController controller = loader.getController();
            controller.initData(aportacion, () -> {
                loadPage();
                cargarMantenimientoPeriodo();
            });

            Stage stage = new Stage();
            stage.setTitle(aportacion == null ? "Registrar Pago / Aportación" : "Ajustar Pago de Aportación");
            stage.initModality(Modality.APPLICATION_MODAL);
            if (tablaAportaciones != null && tablaAportaciones.getScene() != null) {
                stage.initOwner(tablaAportaciones.getScene().getWindow());
                ResponsiveWindowService.fitModalStage(stage, tablaAportaciones.getScene().getWindow());
            }
            stage.setScene(new Scene(content));
            stage.setResizable(true);
            stage.showAndWait();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void verRecibo() {
        AportacionModel sel = tablaAportaciones.getSelectionModel().getSelectedItem();
        if (sel != null) {
            mostrarReciboModal(sel);
        }
    }

    private void mostrarReciboModal(AportacionModel aportacion) {
        if (aportacion == null) return;
        Window owner = tablaAportaciones != null && tablaAportaciones.getScene() != null
            ? tablaAportaciones.getScene().getWindow()
            : (tablaMantenimiento != null && tablaMantenimiento.getScene() != null ? tablaMantenimiento.getScene().getWindow() : null);
        ReciboAportacionModal.mostrar(aportacion, owner instanceof Stage s ? s : null);
    }

    @FXML
    private void anularAportacion() {
        AportacionModel sel = tablaAportaciones.getSelectionModel().getSelectedItem();
        if (sel == null) return;

        if ("ANULADA".equalsIgnoreCase(sel.getEstado())) {
            new Alert(Alert.AlertType.WARNING, "Esta aportación ya se encuentra anulada.", ButtonType.OK).showAndWait();
            return;
        }

        Alert confirm = new Alert(
            Alert.AlertType.CONFIRMATION,
            "¿Estás seguro de anular la aportación #" + sel.getIdAportacion() + " por " + sel.getMontoFormateado() + "?\nEsta acción no se puede deshacer.",
            ButtonType.YES, ButtonType.NO
        );
        confirm.setTitle("Confirmar anulación");
        confirm.setHeaderText("Anular aportación");
        if (tablaAportaciones.getScene() != null) {
            confirm.initOwner(tablaAportaciones.getScene().getWindow());
        }

        confirm.showAndWait().ifPresent(res -> {
            if (res == ButtonType.YES) {
                Task<Void> task = new Task<>() {
                    @Override
                    protected Void call() throws Exception {
                        new AportacionApiClient().anular(sel.getIdAportacion());
                        return null;
                    }
                };
                task.setOnSucceeded(e -> {
                    loadPage();
                    cargarMantenimientoPeriodo();
                });
                task.setOnFailed(e -> {
                    new Alert(Alert.AlertType.ERROR, "No fue posible anular la aportación: " + message(task.getException()), ButtonType.OK).showAndWait();
                });
                Thread t = new Thread(task, "anular-aportacion-api");
                t.setDaemon(true);
                t.start();
            }
        });
    }

    @FXML
    private void paginaAnterior() {
        if (currentPage > 1) {
            currentPage--;
            loadPage();
        }
    }

    @FXML
    private void paginaSiguiente() {
        if (currentPage < totalPages) {
            currentPage++;
            loadPage();
        }
    }

    private void loadPage() {
        tablaAportaciones.setPlaceholder(new Label("Cargando aportaciones..."));

        String periodo = campoFiltroPeriodo.getText();
        String metodo = comboMetodo.getValue();
        String estado = comboEstado.getValue();
        String busqueda = campoBusqueda.getText();

        Task<AportacionModel.Page> task = new Task<>() {
            @Override
            protected AportacionModel.Page call() throws Exception {
                return new AportacionApiClient().findPage(
                    null, null, periodo, null, null, metodo, estado, busqueda, currentPage, pageSize
                );
            }
        };

        task.setOnSucceeded(event -> {
            AportacionModel.Page page = task.getValue();
            aportaciones.setAll(page.getItems());
            totalRegistros = page.getTotal();
            totalPages = Math.max(1, (int) Math.ceil((double) totalRegistros / pageSize));
            totalRecaudado = page.getTotalRecaudado() != null ? page.getTotalRecaudado() : BigDecimal.ZERO;

            lblTotalRegistros.setText(totalRegistros + " aportaciones");
            lblPaginacion.setText("Página " + currentPage + " de " + totalPages);
            lblTotalRecaudado.setText("Total aportado: " + String.format("$%.2f", totalRecaudado));

            if (lblStatTotalRecaudado != null) {
                lblStatTotalRecaudado.setText(String.format("$%.2f", totalRecaudado));
            }
            if (lblStatTotalRegistros != null) {
                lblStatTotalRegistros.setText(String.valueOf(totalRegistros));
            }
            if (lblStatPromedio != null) {
                if (totalRegistros > 0 && totalRecaudado.compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal promedio = totalRecaudado.divide(BigDecimal.valueOf(totalRegistros), 2, java.math.RoundingMode.HALF_UP);
                    lblStatPromedio.setText(String.format("$%.2f", promedio));
                } else {
                    lblStatPromedio.setText("$0.00");
                }
            }
            if (lblStatPeriodo != null) {
                lblStatPeriodo.setText(periodo != null && !periodo.isBlank() ? periodo.trim() : "Todos");
            }
            if (lblStatSubtextoRecaudado != null) {
                if (busqueda != null && !busqueda.isBlank()) {
                    lblStatSubtextoRecaudado.setText("Filtro: " + busqueda.trim());
                } else if (periodo != null && !periodo.isBlank()) {
                    lblStatSubtextoRecaudado.setText("Período: " + periodo.trim());
                } else {
                    lblStatSubtextoRecaudado.setText("Recaudación efectiva de cuotas activas");
                }
            }
            if (lblStatFiltroInfo != null) {
                String descFiltro = (metodo != null && !"TODOS".equalsIgnoreCase(metodo) ? metodo : "Todos los métodos")
                    + " · " + (estado != null && !"TODOS".equalsIgnoreCase(estado) ? estado : "Cualquier estado");
                lblStatFiltroInfo.setText(descFiltro);
            }

            btnAnterior.setDisable(currentPage <= 1);
            btnSiguiente.setDisable(currentPage >= totalPages);
            tablaAportaciones.refresh();
        });

        task.setOnFailed(event -> {
            tablaAportaciones.setPlaceholder(new Label("Error al cargar aportaciones: " + message(task.getException())));
        });

        Thread thread = new Thread(task, "cargar-aportaciones-api");
        thread.setDaemon(true);
        thread.start();
    }

    private static boolean isAdministrator(String role) {
        return role != null && ("ADMIN".equalsIgnoreCase(role) || "ADMINISTRADOR".equalsIgnoreCase(role));
    }

    private String message(Throwable error) {
        return error == null || error.getMessage() == null ? "Error de conexión" : error.getMessage();
    }
}
