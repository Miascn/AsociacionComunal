package controller;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.util.Duration;
import models.*;
import org.kordamp.ikonli.javafx.FontIcon;
import service.DashboardService;
import service.MiembroApiClient;
import service.ResponsiveWindowService;
import service.ViviendaApiClient;

public class DashboardController {

    // KPIs
    @FXML private Label lblMiembros;
    @FXML private Label lblMiembrosTotal;
    @FXML private Label lblFamilias;
    @FXML private Label lblHabitantesSub;
    @FXML private Label lblIngresos;
    @FXML private Label lblIngresosMesSub;
    @FXML private Label lblProyectos;
    @FXML private Label lblProyectosSub;

    // Encabezado
    @FXML private Label lblEstadoConexion;
    @FXML private Label lblFechaActual;
    @FXML private Button btnActualizar;

    // Secciones intermedias
    @FXML private HBox chartBarsContainer;
    @FXML private VBox boxActividadReciente;
    @FXML private VBox boxProximasActividades;

    // Mapa comunitario
    @FXML private StackPane mapContainer;
    @FXML private Label lblMapStats;

    // Ficha rápida de vivienda seleccionada
    @FXML private HBox panelDetalleViviendaMapa;
    @FXML private Label lblDetalleCodigo;
    @FXML private Label lblDetalleDireccion;
    @FXML private Label lblDetalleRepresentante;
    @FXML private Label lblDetalleHabitantes;
    @FXML private Label lblDetalleEstado;
    @FXML private Button btnAbrirExpediente;

    private final DashboardService dashboardService = new DashboardService();
    private CommunityMapPane communityMapPane;
    private ViviendaModel viviendaSeleccionadaActual;

    @FXML
    private void initialize() {
        // Inicializar fecha actual en español
        LocalDate hoy = LocalDate.now();
        String fechaFormateada = hoy.format(DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM 'de' yyyy", new Locale("es", "ES")));
        fechaFormateada = fechaFormateada.substring(0, 1).toUpperCase(Locale.ROOT) + fechaFormateada.substring(1);
        if (lblFechaActual != null) {
            lblFechaActual.setText(fechaFormateada);
        }

        // Montar el mapa interactivo nativo
        communityMapPane = new CommunityMapPane();
        communityMapPane.setOnViviendaSelected(this::mostrarFichaVivienda);
        communityMapPane.setOnNuevaViviendaAction(() -> abrirDialogoNuevaVivienda(null, null, null));
        communityMapPane.setOnColocarViviendaListener((x, y, sector) -> abrirDialogoNuevaVivienda(x, y, sector));
        if (mapContainer != null) {
            mapContainer.getChildren().setAll(communityMapPane);
        }

        // Cargar datos reales asíncronamente
        cargarDatos();
    }

    @FXML
    private void onActualizar() {
        if (btnActualizar != null) {
            btnActualizar.setDisable(true);
            btnActualizar.setText("Actualizando...");
        }
        if (lblEstadoConexion != null) {
            lblEstadoConexion.setText("● Conectando...");
        }

        cargarDatos();

        PauseTransition pause = new PauseTransition(Duration.seconds(1));
        pause.setOnFinished(e -> {
            if (btnActualizar != null) {
                btnActualizar.setDisable(false);
                btnActualizar.setText("Actualizar");
            }
        });
        pause.play();
    }

    private void cargarDatos() {
        dashboardService.loadDashboardAsync().thenAccept(data -> Platform.runLater(() -> {
            actualizarMetricas(data);
            actualizarGraficaMensual(data.getAportacionesPorMes());
            actualizarActividadReciente(data.getActividadReciente());
            actualizarProximasReuniones(data.getProximasReuniones());
            actualizarMapa(data.getViviendas());

            if (lblEstadoConexion != null) {
                lblEstadoConexion.setText("● Conectado a la API");
            }
        })).exceptionally(ex -> {
            Platform.runLater(() -> {
                if (lblEstadoConexion != null) {
                    lblEstadoConexion.setText("● Sin conexión remota");
                }
            });
            return null;
        });
    }

    private void actualizarMetricas(DashboardData data) {
        if (lblMiembros != null) {
            lblMiembros.setText(String.valueOf(data.getMiembrosActivos()));
        }
        if (lblMiembrosTotal != null) {
            lblMiembrosTotal.setText(String.format("↑ %d de %d registrados", data.getMiembrosActivos(), data.getMiembros()));
        }

        if (lblFamilias != null) {
            lblFamilias.setText(String.valueOf(data.getFamilias()));
        }
        if (lblHabitantesSub != null) {
            lblHabitantesSub.setText(String.format("Censo: %d hab. (%d ad., %d men.)",
                    data.getTotalHabitantes(), data.getTotalAdultos(), data.getTotalMenores()));
        }

        if (lblIngresos != null) {
            lblIngresos.setText(String.format("$%,.2f", data.getIngresos()));
        }
        if (lblIngresosMesSub != null) {
            lblIngresosMesSub.setText(String.format("↑ $%,.2f este mes", data.getIngresosMesActual()));
        }

        if (lblProyectos != null) {
            lblProyectos.setText(String.valueOf(data.getProyectos()));
        }
        if (lblProyectosSub != null) {
            lblProyectosSub.setText(String.format("%d en ejecución / %d total", data.getProyectos(), data.getTotalProyectos()));
        }
    }

    private void actualizarGraficaMensual(Map<String, Double> porMes) {
        if (chartBarsContainer == null || porMes == null || porMes.isEmpty()) return;
        chartBarsContainer.getChildren().clear();

        double maxMonto = porMes.values().stream().mapToDouble(Double::doubleValue).max().orElse(1.0);
        if (maxMonto <= 0) maxMonto = 1.0;
        double maxBarHeight = 110.0;

        int index = 0;
        int total = porMes.size();

        for (Map.Entry<String, Double> entry : porMes.entrySet()) {
            String mes = entry.getKey();
            Double monto = entry.getValue();

            double ratio = monto / maxMonto;
            double barHeight = Math.max(16.0, ratio * maxBarHeight);

            VBox column = new VBox(6);
            column.setAlignment(Pos.BOTTOM_CENTER);
            HBox.setHgrow(column, Priority.ALWAYS);

            // Monto arriba de la barra
            Label lblMonto = new Label(String.format("$%.0f", monto));
            lblMonto.getStyleClass().add("chart-bar-value-label");

            // Barra con gradiente
            Region bar = new Region();
            bar.setPrefHeight(barHeight);
            bar.setMinHeight(barHeight);
            bar.setMaxHeight(barHeight);
            bar.getStyleClass().add("chart-bar");

            // Resaltar el mes actual (último)
            if (index == total - 1) {
                bar.getStyleClass().add("chart-bar-highlight");
            }

            Tooltip tooltip = new Tooltip(String.format("%s: $%,.2f recaudados", mes, monto));
            Tooltip.install(bar, tooltip);

            // Etiqueta de mes
            Label lblMes = new Label(mes);
            lblMes.getStyleClass().add("chart-bar-month-label");

            column.getChildren().addAll(lblMonto, bar, lblMes);
            chartBarsContainer.getChildren().add(column);
            index++;
        }
    }

    private void actualizarActividadReciente(List<BitacoraModel> bitacoras) {
        if (boxActividadReciente == null) return;
        boxActividadReciente.getChildren().clear();

        if (bitacoras == null || bitacoras.isEmpty()) {
            HBox itemVacio = new HBox(8);
            itemVacio.setAlignment(Pos.CENTER_LEFT);
            FontIcon icon = new FontIcon("fth-info");
            icon.getStyleClass().add("muted-label");
            Label lbl = new Label("No se registran transacciones recientes en la bitácora.");
            lbl.getStyleClass().add("muted-label");
            itemVacio.getChildren().addAll(icon, lbl);
            boxActividadReciente.getChildren().add(itemVacio);
            return;
        }

        DateTimeFormatter timeFmt = DateTimeFormatter.ofPattern("d MMM • HH:mm", new Locale("es", "ES"));

        for (BitacoraModel b : bitacoras) {
            HBox item = new HBox(12);
            item.setAlignment(Pos.CENTER_LEFT);
            item.getStyleClass().add("dashboard-activity-item");

            FontIcon icon = new FontIcon(obtenerIconoAccion(b.accion()));
            icon.getStyleClass().add("activity-icon");

            VBox desc = new VBox(2);
            String tituloAccion = (b.accion() != null ? b.accion() : "Acción") + " en " +
                    (b.entidad() != null ? b.entidad() : "sistema");
            Label lblAccion = new Label(tituloAccion);
            lblAccion.getStyleClass().add("activity-text-title");

            String usuario = b.getUsuarioDisplay();
            Label lblUsuario = new Label("Por " + usuario);
            lblUsuario.getStyleClass().add("muted-label");
            desc.getChildren().addAll(lblAccion, lblUsuario);

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            String fechaStr = "Reciente";
            if (b.fechaHora() != null) {
                try {
                    java.time.LocalDateTime ldt = java.time.LocalDateTime.parse(b.fechaHora());
                    fechaStr = ldt.format(timeFmt);
                } catch (Exception ignored) {
                    fechaStr = b.fechaHora();
                }
            }
            Label lblFecha = new Label(fechaStr);
            lblFecha.getStyleClass().add("muted-label");

            item.getChildren().addAll(icon, desc, spacer, lblFecha);
            boxActividadReciente.getChildren().add(item);
        }
    }

    private void actualizarProximasReuniones(List<ReunionModel> reuniones) {
        if (boxProximasActividades == null) return;
        boxProximasActividades.getChildren().clear();

        if (reuniones == null || reuniones.isEmpty()) {
            VBox card = new VBox(4);
            card.getStyleClass().add("activity-card");
            Label lblVacio = new Label("No hay reuniones programadas");
            lblVacio.getStyleClass().add("feature-title");
            Label lblSub = new Label("Utiliza 'Programar reunión' para convocar una asamblea.");
            lblSub.getStyleClass().add("muted-label");
            card.getChildren().addAll(lblVacio, lblSub);
            boxProximasActividades.getChildren().add(card);
            return;
        }

        DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern("d 'de' MMMM • hh:mm a", new Locale("es", "ES"));

        for (ReunionModel r : reuniones) {
            VBox card = new VBox(3);
            card.getStyleClass().add("activity-card");

            HBox header = new HBox(6);
            header.setAlignment(Pos.CENTER_LEFT);
            Label lblTitulo = new Label(r.getTitulo());
            lblTitulo.getStyleClass().add("feature-title");
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            Label badgeTipo = new Label(r.getTipo() != null ? r.getTipo() : "ASAMBLEA");
            badgeTipo.getStyleClass().add("panel-chip");
            header.getChildren().addAll(lblTitulo, spacer, badgeTipo);

            String fechaStr = "Fecha por definir";
            if (r.getFechaHora() != null && !r.getFechaHora().isBlank()) {
                try {
                    java.time.LocalDateTime ldt = java.time.LocalDateTime.parse(r.getFechaHora());
                    fechaStr = ldt.format(dateFmt);
                } catch (Exception ignored) {
                    fechaStr = r.getFechaHora();
                }
            }
            String lugarStr = r.getLugar() != null && !r.getLugar().isBlank() ? " • " + r.getLugar() : "";
            Label lblFechaLugar = new Label(fechaStr + lugarStr);
            lblFechaLugar.getStyleClass().add("muted-label");

            card.getChildren().addAll(header, lblFechaLugar);
            boxProximasActividades.getChildren().add(card);
        }
    }

    private void actualizarMapa(List<ViviendaModel> viviendas) {
        if (communityMapPane != null) {
            communityMapPane.setViviendas(viviendas);
        }
        if (lblMapStats != null) {
            int censadas = communityMapPane != null ? communityMapPane.getCantidadViviendas() : (viviendas != null ? viviendas.size() : 0);
            int pendientes = communityMapPane != null ? communityMapPane.getCantidadPendientes() : 0;
            if (pendientes > 0) {
                lblMapStats.setText(String.format("%d colocadas (%d pendientes por colocar) • 4 sectores", censadas, pendientes));
            } else {
                String texto = censadas == 1 ? "1 vivienda en el plano • 4 sectores" : String.format("%d viviendas en el plano • 4 sectores", censadas);
                lblMapStats.setText(texto);
            }
        }
    }

    private void mostrarFichaVivienda(ViviendaModel v) {
        this.viviendaSeleccionadaActual = v;
        if (panelDetalleViviendaMapa == null) return;

        if (v == null) {
            panelDetalleViviendaMapa.setVisible(false);
            panelDetalleViviendaMapa.setManaged(false);
            return;
        }

        panelDetalleViviendaMapa.setVisible(true);
        panelDetalleViviendaMapa.setManaged(true);

        if (lblDetalleCodigo != null) {
            lblDetalleCodigo.setText("Casa " + v.getCodigo());
        }
        if (lblDetalleDireccion != null) {
            lblDetalleDireccion.setText((v.getSector() != null ? v.getSector() : "Sector") + " • " + (v.getDireccion() != null ? v.getDireccion() : "Sin dirección"));
        }
        if (lblDetalleRepresentante != null) {
            String rep = v.getRepresentante() != null && !v.getRepresentante().isBlank()
                    ? v.getRepresentante()
                    : "Sin representante registrado";
            lblDetalleRepresentante.setText("Representante: " + rep);
        }
        if (lblDetalleHabitantes != null) {
            lblDetalleHabitantes.setText(String.format("%d habitantes (%d adultos, %d menores)",
                    v.getTotalResidentes(), v.getAdultos(), v.getMenores()));
        }
        if (lblDetalleEstado != null) {
            lblDetalleEstado.setText(v.getEstado() != null ? v.getEstado() : "ACTIVA");
        }
    }

    @FXML
    private void onAbrirExpedienteSeleccionado() {
        if (viviendaSeleccionadaActual == null || viviendaSeleccionadaActual.getIdVivienda() == null) return;
        ViviendaApiClient api = new ViviendaApiClient();
        try {
            var detail = api.find(viviendaSeleccionadaActual.getIdVivienda());
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/views/detalle-vivienda.fxml"));
            Parent content = loader.load();
            loader.<DetalleViviendaController>getController().setDetail(detail);

            Dialog<Void> dialog = new Dialog<>();
            dialog.initOwner(mapContainer.getScene().getWindow());
            dialog.setTitle("Expediente de Vivienda " + viviendaSeleccionadaActual.getCodigo());
            dialog.getDialogPane().setContent(content);
            dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
            ResponsiveWindowService.fitDialog(dialog, mapContainer.getScene().getWindow(), 660);
            dialog.showAndWait();
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION, "Expediente de la casa " + viviendaSeleccionadaActual.getCodigo());
            alert.setHeaderText("Sector: " + viviendaSeleccionadaActual.getSector());
            alert.showAndWait();
        }
    }

    // --- Acciones Rápidas ---
    @FXML
    private void onRegistrarMiembro() {
        navegarHacia("#btnNavMiembros");
    }

    @FXML
    private void onNuevaVivienda() {
        abrirDialogoNuevaVivienda(null, null, null);
    }

    public void abrirDialogoNuevaVivienda(Double clickX, Double clickY, String sectorSugerido) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/views/vivienda-form.fxml"));
            Parent content = loader.load();
            ViviendaFormController form = loader.getController();

            int proximoNumero = (communityMapPane != null ? communityMapPane.getCantidadViviendas() : 0) + 1;
            String codigoSugerido = String.format("VIV-%04d", proximoNumero);
            String sector = sectorSugerido != null && !sectorSugerido.isBlank() ? sectorSugerido : "Sector A";
            form.setSugerencias(codigoSugerido, sector);

            ButtonType saveBtn = new ButtonType("Registrar y Colocar Vivienda", ButtonBar.ButtonData.OK_DONE);
            Dialog<Void> dialog = new Dialog<>();
            if (mapContainer != null && mapContainer.getScene() != null) {
                dialog.initOwner(mapContainer.getScene().getWindow());
            }
            dialog.setTitle("Nueva Vivienda para el Plano");
            dialog.getDialogPane().setContent(content);
            dialog.getDialogPane().getButtonTypes().addAll(saveBtn, ButtonType.CANCEL);
            estilarDialogo(dialog);
            ResponsiveWindowService.fitDialog(dialog, mapContainer != null && mapContainer.getScene() != null ? mapContainer.getScene().getWindow() : null, 680);

            Button button = (Button) dialog.getDialogPane().lookupButton(saveBtn);
            button.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
                event.consume();
                var request = form.request();
                if (request == null) return;
                button.setDisable(true);

                ViviendaApiClient api = new ViviendaApiClient();
                new Thread(() -> {
                    try {
                        var saved = api.create(request);
                        Platform.runLater(() -> {
                            dialog.close();
                            if (communityMapPane != null && clickX != null && clickY != null) {
                                communityMapPane.registrarPosicionVivienda(saved.getCodigo(), clickX, clickY, 0.0);
                            }
                            cargarDatos();
                            PauseTransition pt = new PauseTransition(Duration.millis(600));
                            pt.setOnFinished(ev -> {
                                if (communityMapPane != null) {
                                    String msg = clickX != null ?
                                            "¡Vivienda " + saved.getCodigo() + " colocada! Gírala con la rueda del ratón o clic derecho." :
                                            "¡Vivienda " + saved.getCodigo() + " agregada! Arrástrala a su posición en el plano.";
                                    communityMapPane.enfocarYEditarVivienda(saved.getCodigo(), msg);
                                }
                            });
                            pt.play();
                        });
                    } catch (Exception ex) {
                        Platform.runLater(() -> {
                            button.setDisable(false);
                            form.showError("Error al guardar: " + ex.getMessage());
                        });
                    }
                }).start();
            });

            dialog.showAndWait();
        } catch (Exception ex) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "No fue posible abrir el formulario de vivienda: " + ex.getMessage());
            alert.showAndWait();
        }
    }

    private void estilarDialogo(Dialog<?> dialog) {
        dialog.getDialogPane().getStyleClass().add("member-dialog");
        var css = getClass().getResource("/styles/member-dialog.css");
        if (css != null) {
            dialog.getDialogPane().getStylesheets().add(css.toExternalForm());
        }
    }

    @FXML
    private void onRegistrarAportacion() {
        navegarHacia("#btnNavAportaciones");
    }

    @FXML
    private void onProgramarReunion() {
        navegarHacia("#btnNavReuniones");
    }

    private void navegarHacia(String selectorBotonNav) {
        try {
            if (mapContainer != null && mapContainer.getScene() != null) {
                Node navBtn = mapContainer.getScene().lookup(selectorBotonNav);
                if (navBtn instanceof Button button) {
                    button.fire();
                }
            }
        } catch (Exception ignored) {}
    }

    private String obtenerIconoAccion(String accion) {
        if (accion == null) return "fth-activity";
        String a = accion.toUpperCase(Locale.ROOT);
        if (a.contains("CREAR") || a.contains("REGISTRAR") || a.contains("INSERT")) return "fth-plus-circle";
        if (a.contains("EDITAR") || a.contains("ACTUALIZAR") || a.contains("MODIFICAR")) return "fth-edit-2";
        if (a.contains("ELIMINAR") || a.contains("ANULAR") || a.contains("REVOCAR")) return "fth-trash-2";
        if (a.contains("PAGO") || a.contains("APORTACION")) return "fth-dollar-sign";
        if (a.contains("REUNION") || a.contains("ASAMBLEA")) return "fth-calendar";
        if (a.contains("VOTO") || a.contains("VOTACION")) return "fth-check-circle";
        return "fth-check";
    }
}

