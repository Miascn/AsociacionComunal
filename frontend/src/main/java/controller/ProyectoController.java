package controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import models.ProyectoModel;
import service.ProyectoApiClient;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;

public class ProyectoController {
    @FXML private TableView<ProyectoModel> tablaProyectos;
    @FXML private TableColumn<ProyectoModel, String> columnaNombre;
    @FXML private TableColumn<ProyectoModel, String> columnaDescripcion;
    @FXML private TableColumn<ProyectoModel, java.math.BigDecimal> columnaPresupuesto;
    @FXML private TableColumn<ProyectoModel, String> columnaFecha;
    @FXML private TableColumn<ProyectoModel, String> columnaEstado;
    @FXML private Label lblTotalProyectos;
    @FXML private Label lblEstadoModulo;
    @FXML private TextField campoBusqueda;

    private final ObservableList<ProyectoModel> proyectos = FXCollections.observableArrayList();
    private FilteredList<ProyectoModel> proyectosFiltrados;

    @FXML
    private void initialize() {
        tablaProyectos.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        columnaNombre.setCellValueFactory(new PropertyValueFactory<>("nombre"));
        columnaDescripcion.setCellValueFactory(new PropertyValueFactory<>("descripcion"));
        columnaPresupuesto.setCellValueFactory(new PropertyValueFactory<>("presupuesto"));
        columnaFecha.setCellValueFactory(new PropertyValueFactory<>("fechaCreacion"));
        columnaEstado.setCellValueFactory(new PropertyValueFactory<>("estado"));
        proyectosFiltrados = new FilteredList<>(proyectos, proyecto -> true);
        tablaProyectos.setItems(proyectosFiltrados);
        campoBusqueda.textProperty().addListener((observable, anterior, actual) -> filtrar(actual));
        actualizarTotal();
        cargarProyectos();
    }

    private void cargarProyectos() {
        lblEstadoModulo.setText("Conectando al servidor...");
        Task<List<ProyectoModel>> task = new Task<>() {
            @Override protected List<ProyectoModel> call() throws Exception {
                return new ProyectoApiClient().findAll();
            }
        };
        task.setOnSucceeded(event -> {
            proyectos.setAll(task.getValue());
            lblEstadoModulo.setText("Conectado al servidor");
            actualizarTotal();
        });
        task.setOnFailed(event -> {
            lblEstadoModulo.setText("Sin conexion al servidor");
            tablaProyectos.setPlaceholder(new Label("No fue posible obtener los proyectos."));
            actualizarTotal();
        });
        Thread thread = new Thread(task, "cargar-proyectos-api");
        thread.setDaemon(true);
        thread.start();
    }

    private void filtrar(String texto) {
        String criterio = normalizar(texto);
        proyectosFiltrados.setPredicate(proyecto -> criterio.isBlank()
            || contiene(proyecto.getNombre(), criterio)
            || contiene(proyecto.getDescripcion(), criterio)
            || contiene(proyecto.getEstado(), criterio));
        actualizarTotal();
    }

    private void actualizarTotal() {
        int visibles = proyectosFiltrados == null ? proyectos.size() : proyectosFiltrados.size();
        lblTotalProyectos.setText(visibles == proyectos.size()
            ? proyectos.size() + " proyectos registrados"
            : visibles + " de " + proyectos.size() + " proyectos");
    }

    private boolean contiene(String valor, String criterio) {
        return valor != null && normalizar(valor).contains(criterio);
    }

    private String normalizar(String valor) {
        if (valor == null) return "";
        return Normalizer.normalize(valor, Normalizer.Form.NFD)
            .replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT).trim();
    }
}