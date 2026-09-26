package controller;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import models.AportacionModel;
import models.MiembroModel;
import service.AportacionApiClient;
import service.MiembroApiClient;

public class AportacionFormController {
    @FXML private Label lblTitulo;
    @FXML private Label lblSubtitulo;
    @FXML private ComboBox<MiembroItem> comboMiembro;
    @FXML private ComboBox<ProyectoItem> comboProyecto;
    @FXML private TextField campoPeriodo;
    @FXML private TextField campoMonto;
    @FXML private DatePicker pickerFechaPago;
    @FXML private ComboBox<String> comboMetodoPago;
    @FXML private TextField campoReferencia;
    @FXML private Label lblError;
    @FXML private Button btnGuardar;

    public record MiembroItem(Integer id, String dui, String nombreCompleto) {
        @Override
        public String toString() {
            return (dui != null ? "[" + dui + "] " : "") + nombreCompleto;
        }
    }

    public record ProyectoItem(Integer id, String nombre) {
        @Override
        public String toString() {
            return nombre;
        }
    }

    private AportacionModel aportacionActual;
    private Runnable onSuccessCallback;

    @FXML
    private void initialize() {
        comboMetodoPago.getItems().setAll("EFECTIVO", "TRANSFERENCIA", "OTRO");
        comboMetodoPago.setValue("EFECTIVO");

        pickerFechaPago.setValue(LocalDate.now());
        campoPeriodo.setText(LocalDate.now().toString().substring(0, 7));

        cargarMiembros();
        cargarProyectos();
        prellenarCuotaMantenimiento();
    }

    private void prellenarCuotaMantenimiento() {
        Task<BigDecimal> task = new Task<>() {
            @Override
            protected BigDecimal call() throws Exception {
                return new AportacionApiClient().getCuotaMantenimiento();
            }
        };
        task.setOnSucceeded(e -> {
            BigDecimal cuota = task.getValue();
            if (cuota != null && (campoMonto.getText() == null || campoMonto.getText().isBlank())) {
                campoMonto.setText(cuota.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString());
            }
        });
        Thread t = new Thread(task, "cuota-default");
        t.setDaemon(true);
        t.start();
    }

    public void initMantenimiento(Integer idMiembro, String periodo, BigDecimal cuota, Runnable onSuccess) {
        this.onSuccessCallback = onSuccess;
        lblTitulo.setText("Registrar Cuota de Mantenimiento");
        lblSubtitulo.setText("Aportación mensual ordinaria de la colonia (mantenimiento y vigilancia)");
        btnGuardar.setText("Registrar Cuota");
        if (periodo != null && !periodo.isBlank()) {
            campoPeriodo.setText(periodo.trim());
        }
        if (cuota != null) {
            campoMonto.setText(cuota.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString());
        }
        Platform.runLater(() -> {
            if (idMiembro != null) {
                for (MiembroItem item : comboMiembro.getItems()) {
                    if (item.id().equals(idMiembro)) {
                        comboMiembro.setValue(item);
                        break;
                    }
                }
            }
            if (!comboProyecto.getItems().isEmpty()) {
                comboProyecto.setValue(comboProyecto.getItems().get(0));
            }
        });
    }

    public void initData(AportacionModel aportacion, Runnable onSuccess) {
        this.aportacionActual = aportacion;
        this.onSuccessCallback = onSuccess;

        if (aportacion != null) {
            lblTitulo.setText("Ajustar pago / Editar aportación #" + aportacion.getIdAportacion());
            lblSubtitulo.setText("Modifica la cantidad del pago ($) o actualiza los detalles de la aportación");
            btnGuardar.setText("Guardar cambios");

            campoPeriodo.setText(aportacion.getPeriodoMes());
            campoMonto.setText(aportacion.getMonto() != null ? aportacion.getMonto().toString() : "");
            Platform.runLater(() -> campoMonto.requestFocus());
            if (aportacion.getFechaPago() != null) {
                try {
                    pickerFechaPago.setValue(LocalDate.parse(aportacion.getFechaPago()));
                } catch (Exception ignored) {}
            }
            if (aportacion.getMetodoPago() != null) {
                comboMetodoPago.setValue(aportacion.getMetodoPago());
            }
            campoReferencia.setText(aportacion.getReferencia());

            // Seleccionar miembro y proyecto una vez cargados
            Platform.runLater(() -> {
                if (aportacion.getIdMiembro() != null) {
                    for (MiembroItem m : comboMiembro.getItems()) {
                        if (m.id().equals(aportacion.getIdMiembro())) {
                            comboMiembro.setValue(m);
                            break;
                        }
                    }
                }
                if (aportacion.getIdProyecto() != null) {
                    for (ProyectoItem p : comboProyecto.getItems()) {
                        if (p.id() != null && p.id().equals(aportacion.getIdProyecto())) {
                            comboProyecto.setValue(p);
                            break;
                        }
                    }
                }
            });
        }
    }

    private void cargarMiembros() {
        Task<List<MiembroModel>> task = new Task<>() {
            @Override
            protected List<MiembroModel> call() throws Exception {
                return new MiembroApiClient().findAll();
            }
        };
        task.setOnSucceeded(e -> {
            List<MiembroModel> list = task.getValue();
            comboMiembro.getItems().clear();
            for (MiembroModel m : list) {
                comboMiembro.getItems().add(new MiembroItem(
                    m.getId(), m.getDui(), m.getNombres() + " " + m.getApellidos()
                ));
            }
            if (aportacionActual != null && aportacionActual.getIdMiembro() != null) {
                for (MiembroItem item : comboMiembro.getItems()) {
                    if (item.id().equals(aportacionActual.getIdMiembro())) {
                        comboMiembro.setValue(item);
                        break;
                    }
                }
            }
        });
        Thread thread = new Thread(task, "cargar-miembros-combo");
        thread.setDaemon(true);
        thread.start();
    }

    private void cargarProyectos() {
        comboProyecto.getItems().clear();
        comboProyecto.getItems().add(new ProyectoItem(null, "Mantenimiento Mensual de la Colonia (Principal)"));
        try {
            List<models.ProyectoModel> lista = new service.ProyectoApiClient().findAll();
            for (models.ProyectoModel p : lista) {
                if (p.getIdProyecto() != null) {
                    comboProyecto.getItems().add(new ProyectoItem(p.getIdProyecto(), p.getNombre()));
                }
            }
        } catch (Exception ignored) {}
        comboProyecto.setValue(comboProyecto.getItems().get(0));
    }

    @FXML
    private void guardar() {
        lblError.setText("");

        MiembroItem miembroSel = comboMiembro.getValue();
        if (miembroSel == null) {
            lblError.setText("Por favor selecciona un miembro aportante.");
            return;
        }

        String periodo = campoPeriodo.getText() != null ? campoPeriodo.getText().trim() : "";
        if (!periodo.matches("^\\d{4}-(0[1-9]|1[0-2])$")) {
            lblError.setText("El período debe tener formato YYYY-MM (ejemplo: 2026-03).");
            return;
        }

        String montoStr = campoMonto.getText() != null ? campoMonto.getText().trim() : "";
        BigDecimal monto;
        try {
            monto = new BigDecimal(montoStr);
            if (monto.compareTo(BigDecimal.ZERO) <= 0) {
                lblError.setText("El monto debe ser un valor mayor a cero.");
                return;
            }
        } catch (Exception e) {
            lblError.setText("Ingresa un monto numérico válido (ej. 10.00).");
            return;
        }

        LocalDate fechaPago = pickerFechaPago.getValue();
        if (fechaPago == null) {
            lblError.setText("Por favor selecciona la fecha de pago.");
            return;
        }
        if (fechaPago.isAfter(LocalDate.now())) {
            lblError.setText("La fecha de pago no puede ser futura.");
            return;
        }

        String metodoPago = comboMetodoPago.getValue();
        if (metodoPago == null || metodoPago.isBlank()) {
            lblError.setText("Por favor selecciona un método de pago.");
            return;
        }

        ProyectoItem proyectoSel = comboProyecto.getValue();
        Integer idProyecto = proyectoSel != null ? proyectoSel.id() : null;
        String referencia = campoReferencia.getText() != null ? campoReferencia.getText().trim() : "";

        AportacionModel model = new AportacionModel();
        model.setIdMiembro(miembroSel.id());
        model.setIdProyecto(idProyecto);
        model.setPeriodoMes(periodo);
        model.setMonto(monto);
        model.setFechaPago(fechaPago.toString());
        model.setMetodoPago(metodoPago);
        model.setReferencia(referencia);

        btnGuardar.setDisable(true);
        lblError.setStyle("-fx-text-fill: -color-accent-fg;");
        lblError.setText("Guardando aportación...");

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                AportacionApiClient client = new AportacionApiClient();
                if (aportacionActual == null) {
                    client.create(model);
                } else {
                    client.update(aportacionActual.getIdAportacion(), model);
                }
                return null;
            }
        };

        task.setOnSucceeded(e -> {
            if (onSuccessCallback != null) {
                onSuccessCallback.run();
            }
            cerrar();
        });

        task.setOnFailed(e -> {
            btnGuardar.setDisable(false);
            lblError.setStyle("-fx-text-fill: -color-danger-fg;");
            Throwable error = task.getException();
            lblError.setText(error != null && error.getMessage() != null ? error.getMessage() : "Error al guardar aportación.");
        });

        Thread thread = new Thread(task, "guardar-aportacion");
        thread.setDaemon(true);
        thread.start();
    }

    @FXML
    private void cancelar() {
        cerrar();
    }

    private void cerrar() {
        Stage stage = (Stage) btnGuardar.getScene().getWindow();
        stage.close();
    }
}
