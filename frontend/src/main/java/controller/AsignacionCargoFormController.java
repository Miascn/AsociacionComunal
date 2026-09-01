package controller;

import java.time.LocalDate;
import java.util.List;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import models.CargoModel;
import models.MiembroModel;
import models.PeriodoDirectivaModel;
import service.CargoApiClient;
import service.DirectivaApiClient;
import service.MiembroApiClient;
import service.PeriodoApiClient;

public class AsignacionCargoFormController {
    @FXML private ComboBox<PeriodoOption> comboPeriodo;
    @FXML private ComboBox<CargoOption> comboCargo;
    @FXML private ComboBox<MiembroOption> comboMiembro;
    @FXML private DatePicker pickerFechaAsignacion;
    @FXML private DatePicker pickerFechaFin;
    @FXML private Label lblError;
    @FXML private Button btnGuardar;

    private Integer defaultPeriodoId;
    private Runnable onSuccessCallback;

    public record PeriodoOption(Integer id, String nombre, String fechaInicio, String fechaFin) {
        @Override public String toString() { return nombre; }
    }
    public record CargoOption(Integer id, String nombre, Integer jerarquia) {
        @Override public String toString() { return nombre + " (Nivel " + jerarquia + ")"; }
    }
    public record MiembroOption(Integer id, String nombreCompleto, String dui) {
        @Override public String toString() { return nombreCompleto + " (" + (dui != null ? dui : "Sin DUI") + ")"; }
    }

    @FXML
    private void initialize() {
        pickerFechaAsignacion.setValue(LocalDate.now());

        comboPeriodo.setConverter(new StringConverter<>() {
            @Override public String toString(PeriodoOption p) { return p != null ? p.toString() : ""; }
            @Override public PeriodoOption fromString(String string) { return null; }
        });
        comboCargo.setConverter(new StringConverter<>() {
            @Override public String toString(CargoOption c) { return c != null ? c.toString() : ""; }
            @Override public CargoOption fromString(String string) { return null; }
        });
        comboMiembro.setConverter(new StringConverter<>() {
            @Override public String toString(MiembroOption m) { return m != null ? m.toString() : ""; }
            @Override public MiembroOption fromString(String string) { return null; }
        });

        comboPeriodo.valueProperty().addListener((obs, o, p) -> {
            if (p != null && p.fechaInicio() != null) {
                try {
                    LocalDate inicio = LocalDate.parse(p.fechaInicio());
                    if (LocalDate.now().isBefore(inicio)) {
                        pickerFechaAsignacion.setValue(inicio);
                    }
                } catch (Exception ignored) {}
            }
        });
    }

    public void initData(Integer defaultPeriodoId, Runnable onSuccess) {
        this.defaultPeriodoId = defaultPeriodoId;
        this.onSuccessCallback = onSuccess;
        cargarCatalogos();
    }

    private void cargarCatalogos() {
        Task<CatalogData> task = new Task<>() {
            @Override
            protected CatalogData call() throws Exception {
                List<PeriodoDirectivaModel> periodos = new PeriodoApiClient().findAll();
                List<CargoModel> cargos = new CargoApiClient().findAll();
                List<MiembroModel> miembros = new MiembroApiClient().findAll().stream()
                    .filter(m -> "ACTIVO".equalsIgnoreCase(m.getEstado())).toList();
                return new CatalogData(periodos, cargos, miembros);
            }
        };

        task.setOnSucceeded(e -> {
            CatalogData data = task.getValue();

            // Períodos no finalizados
            List<PeriodoOption> periodos = data.periodos.stream()
                .filter(p -> !"FINALIZADO".equalsIgnoreCase(p.getEstado()))
                .map(p -> new PeriodoOption(p.getId(), p.getNombre() + " (" + p.getEstado() + ")", p.getFechaInicio(), p.getFechaFin()))
                .toList();
            comboPeriodo.getItems().setAll(periodos);

            if (defaultPeriodoId != null) {
                periodos.stream().filter(p -> p.id().equals(defaultPeriodoId)).findFirst().ifPresent(comboPeriodo::setValue);
            } else if (!periodos.isEmpty()) {
                comboPeriodo.setValue(periodos.get(0));
            }

            // Cargos activos
            List<CargoOption> cargos = data.cargos.stream()
                .filter(CargoModel::isActivo)
                .map(c -> new CargoOption(c.getId(), c.getNombre(), c.getNivelJerarquico()))
                .toList();
            comboCargo.getItems().setAll(cargos);
            if (!cargos.isEmpty()) comboCargo.setValue(cargos.get(0));

            // Miembros activos
            List<MiembroOption> miembros = data.miembros.stream()
                .map(m -> new MiembroOption(m.getId(), m.getNombre() + " " + m.getApellido(), m.getDui()))
                .toList();
            comboMiembro.getItems().setAll(miembros);
            if (!miembros.isEmpty()) comboMiembro.setValue(miembros.get(0));
        });

        task.setOnFailed(e -> {
            lblError.setText("No fue posible cargar los catálogos de asignación.");
        });

        Thread thread = new Thread(task, "cargar-catalogos-asignacion");
        thread.setDaemon(true);
        thread.start();
    }

    @FXML
    private void guardar() {
        lblError.setText("");

        PeriodoOption periodo = comboPeriodo.getValue();
        if (periodo == null) {
            lblError.setText("Debe seleccionar un período directivo.");
            return;
        }

        CargoOption cargo = comboCargo.getValue();
        if (cargo == null) {
            lblError.setText("Debe seleccionar un cargo directivo.");
            return;
        }

        MiembroOption miembro = comboMiembro.getValue();
        if (miembro == null) {
            lblError.setText("Debe seleccionar un miembro.");
            return;
        }

        LocalDate asignacion = pickerFechaAsignacion.getValue();
        if (asignacion == null) {
            lblError.setText("La fecha de asignación es obligatoria.");
            return;
        }

        LocalDate fin = pickerFechaFin.getValue();
        if (fin != null && fin.isBefore(asignacion)) {
            lblError.setText("La fecha de fin no puede ser anterior a la de asignación.");
            return;
        }

        btnGuardar.setDisable(true);
        lblError.setStyle("-fx-text-fill: -color-accent-fg;");
        lblError.setText("Guardando asignación directiva...");

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                new DirectivaApiClient().create(
                    miembro.id(), cargo.id(), periodo.id(),
                    asignacion.toString(),
                    fin != null ? fin.toString() : null
                );
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
            lblError.setText(error != null && error.getMessage() != null ? error.getMessage() : "Error al registrar la asignación.");
        });

        Thread thread = new Thread(task, "guardar-asignacion");
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

    private record CatalogData(
        List<PeriodoDirectivaModel> periodos,
        List<CargoModel> cargos,
        List<MiembroModel> miembros
    ) {}
}
