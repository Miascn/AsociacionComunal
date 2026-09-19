package controller;

import java.util.Comparator;
import java.util.List;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.stage.Stage;
import models.AsistenciaModel;
import models.MiembroModel;
import service.AsistenciaApiClient;

public class HistorialAsistenciaModalController {
    @FXML private Label lblNombreMiembro;
    @FXML private Label lblDuiMiembro;
    @FXML private Label lblEstadoMiembro;

    @FXML private Label lblTotalConvocadas;
    @FXML private Label lblAsistenciasConfirmadas;
    @FXML private Label lblInasistencias;
    @FXML private Label lblPorcentajeCumplimiento;

    @FXML private TableView<AsistenciaModel> tablaHistorial;
    @FXML private TableColumn<AsistenciaModel, String> colReunion;
    @FXML private TableColumn<AsistenciaModel, String> colFechaHora;
    @FXML private TableColumn<AsistenciaModel, String> colTipo;
    @FXML private TableColumn<AsistenciaModel, String> colAsistio;
    @FXML private TableColumn<AsistenciaModel, String> colObservacion;

    private final AsistenciaApiClient apiClient = new AsistenciaApiClient();
    private final ObservableList<AsistenciaModel> historialList = FXCollections.observableArrayList();
    private MiembroModel miembro;

    @FXML
    public void initialize() {
        configurarTabla();
    }

    public void setMiembro(MiembroModel m) {
        this.miembro = m;
        if (m == null) return;

        lblNombreMiembro.setText(m.getNombreCompleto());
        lblDuiMiembro.setText("Documento: " + m.getDocumento() + " | Tel: " + (m.getTelefono() != null ? m.getTelefono() : "-"));
        lblEstadoMiembro.setText(m.getEstado());

        if ("INACTIVO".equalsIgnoreCase(m.getEstado())) {
            lblEstadoMiembro.setStyle("-fx-background-color: #fee2e2; -fx-text-fill: #b91c1c; -fx-font-weight: bold; -fx-padding: 4 10 4 10; -fx-background-radius: 6;");
        } else {
            lblEstadoMiembro.setStyle("-fx-background-color: #dcfce7; -fx-text-fill: #15803d; -fx-font-weight: bold; -fx-padding: 4 10 4 10; -fx-background-radius: 6;");
        }

        cargarHistorial();
    }

    private void configurarTabla() {
        colReunion.setCellValueFactory(cellData -> {
            String title = cellData.getValue().getTituloReunion();
            return new javafx.beans.property.SimpleStringProperty(
                (title != null && !title.isBlank()) ? title : "Reunión #" + cellData.getValue().getIdReunion()
            );
        });
        colReunion.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item);
                    setStyle("-fx-font-weight: bold; -fx-text-fill: #1e293b;");
                }
            }
        });

        colFechaHora.setCellValueFactory(cellData -> {
            String fecha = cellData.getValue().getFechaHoraReunion();
            return new javafx.beans.property.SimpleStringProperty(
                (fecha != null && !fecha.isBlank()) ? fecha.replace("T", " ") : "-"
            );
        });

        colTipo.setCellValueFactory(cellData -> {
            String tipo = cellData.getValue().getTipoReunion();
            return new javafx.beans.property.SimpleStringProperty(
                (tipo != null && !tipo.isBlank()) ? tipo : "ORDINARIA"
            );
        });
        colTipo.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    Label badge = new Label(item);
                    badge.setStyle("EXTRAORDINARIA".equalsIgnoreCase(item)
                        ? "-fx-background-color: #fef3c7; -fx-text-fill: #b45309; -fx-font-weight: bold; -fx-padding: 3 8 3 8; -fx-background-radius: 6; -fx-font-size: 11px;"
                        : "-fx-background-color: #e0e7ff; -fx-text-fill: #4338ca; -fx-font-weight: bold; -fx-padding: 3 8 3 8; -fx-background-radius: 6; -fx-font-size: 11px;");
                    setGraphic(badge);
                    setAlignment(Pos.CENTER_LEFT);
                }
            }
        });

        colAsistio.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(
            cellData.getValue().isAsistio() ? "PRESENTE" : "AUSENTE"
        ));
        colAsistio.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    Label badge = new Label();
                    if ("PRESENTE".equalsIgnoreCase(item)) {
                        badge.setText("✓ PRESENTE");
                        badge.setStyle("-fx-background-color: #dcfce7; -fx-text-fill: #15803d; -fx-font-weight: bold; -fx-padding: 3 8 3 8; -fx-background-radius: 6; -fx-font-size: 11px;");
                    } else {
                        badge.setText("✕ AUSENTE");
                        badge.setStyle("-fx-background-color: #fee2e2; -fx-text-fill: #b91c1c; -fx-font-weight: bold; -fx-padding: 3 8 3 8; -fx-background-radius: 6; -fx-font-size: 11px;");
                    }
                    setGraphic(badge);
                    setAlignment(Pos.CENTER_LEFT);
                }
            }
        });

        colObservacion.setCellValueFactory(cellData -> cellData.getValue().observacionProperty());
        colObservacion.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null || item.isBlank()) {
                    setText("-");
                    setStyle("-fx-text-fill: #94a3b8;");
                } else {
                    setText(item);
                    setStyle("-fx-text-fill: #475569;");
                }
            }
        });

        tablaHistorial.setItems(historialList);
    }

    private void cargarHistorial() {
        if (miembro == null) return;

        Task<List<AsistenciaModel>> task = new Task<>() {
            @Override
            protected List<AsistenciaModel> call() throws Exception {
                return apiClient.getByMiembro(miembro.getIdMiembro());
            }
        };

        task.setOnSucceeded(e -> {
            List<AsistenciaModel> list = task.getValue();
            // Sort chronologically descending (most recent first)
            list.sort(Comparator.comparing(
                (AsistenciaModel a) -> a.getFechaHoraReunion() != null ? a.getFechaHoraReunion() : ""
            ).reversed());

            historialList.setAll(list);
            actualizarMetricas(list);
        });

        task.setOnFailed(e -> {
            Throwable ex = task.getException();
            Platform.runLater(() -> {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Error de carga");
                alert.setHeaderText(null);
                alert.setContentText("No se pudo cargar el historial de asistencia: " + (ex != null ? ex.getMessage() : "Desconocido"));
                alert.showAndWait();
            });
        });

        new Thread(task).start();
    }

    private void actualizarMetricas(List<AsistenciaModel> list) {
        int total = list.size();
        long presentes = list.stream().filter(AsistenciaModel::isAsistio).count();
        long ausentes = total - presentes;
        double pct = total > 0 ? (presentes * 100.0) / total : 0.0;

        lblTotalConvocadas.setText(String.valueOf(total));
        lblAsistenciasConfirmadas.setText(String.valueOf(presentes));
        lblInasistencias.setText(String.valueOf(ausentes));
        lblPorcentajeCumplimiento.setText(String.format("%.1f%%", pct));
    }

    @FXML
    public void cerrar() {
        Stage stage = (Stage) lblNombreMiembro.getScene().getWindow();
        if (stage != null) stage.close();
    }
}
