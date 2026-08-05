package controller;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;

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
import service.MiembroApiClient;
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

    private final ObservableList<Miembro> miembros = FXCollections.observableArrayList();
    private FilteredList<Miembro> miembrosFiltrados;

    @FXML
    private void initialize() {
        tablaMiembros.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        columnaDui.setCellValueFactory(new PropertyValueFactory<>("dui"));
        columnaNombres.setCellValueFactory(new PropertyValueFactory<>("nombres"));
        columnaApellidos.setCellValueFactory(new PropertyValueFactory<>("apellidos"));
        columnaTelefono.setCellValueFactory(new PropertyValueFactory<>("telefono"));
        columnaCorreo.setCellValueFactory(new PropertyValueFactory<>("correo"));
        columnaEstado.setCellValueFactory(new PropertyValueFactory<>("estado"));

        miembrosFiltrados = new FilteredList<>(miembros, miembro -> true);
        tablaMiembros.setItems(miembrosFiltrados);
        campoBusqueda.textProperty().addListener((observable, anterior, actual) -> filtrar(actual));
        actualizarTotal();
        cargarMiembros();
    }

    public ObservableList<Miembro> getMiembros() {
        return miembros;
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
            lblEstadoModulo.setText("Sin conexion al servidor");
            tablaMiembros.setPlaceholder(new Label(
                "No fue posible obtener los miembros. Verifique Internet e intente nuevamente."
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

    private String normalizar(String valor) {
        if (valor == null) {
            return "";
        }
        String sinTildes = Normalizer.normalize(valor, Normalizer.Form.NFD)
            .replaceAll("\\p{M}", "");
        return sinTildes.toLowerCase(Locale.ROOT).trim();
    }
}
