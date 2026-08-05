package controller;

import java.time.LocalDate;
import java.text.Normalizer;
import java.util.Locale;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import sv.asociacion.backend.entity.Miembro;

public class MiembroController {
    @FXML
    private TableView<Miembro> tablaMiembros;

    @FXML
    private TableColumn<Miembro, String> columnaDui;

    @FXML
    private TableColumn<Miembro, String> columnaNombres;

    @FXML
    private TableColumn<Miembro, String> columnaApellidos;

    @FXML
    private TableColumn<Miembro, String> columnaTelefono;

    @FXML
    private TableColumn<Miembro, String> columnaCorreo;

    @FXML
    private TableColumn<Miembro, Miembro.Estado> columnaEstado;

    @FXML
    private Label lblTotalMiembros;

    @FXML
    private TextField campoBusqueda;

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

        miembros.setAll(datosDeDemostracion());
        miembrosFiltrados = new FilteredList<>(miembros, miembro -> true);
        tablaMiembros.setItems(miembrosFiltrados);
        campoBusqueda.textProperty().addListener((observable, anterior, actual) -> filtrar(actual));
        actualizarTotal();
    }

    public ObservableList<Miembro> getMiembros() {
        return miembros;
    }

    private void actualizarTotal() {
        int visibles = miembrosFiltrados == null ? miembros.size() : miembrosFiltrados.size();
        if (visibles == miembros.size()) {
            lblTotalMiembros.setText(miembros.size() + " miembros registrados");
        } else {
            lblTotalMiembros.setText(visibles + " de " + miembros.size() + " miembros");
        }
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

    private ObservableList<Miembro> datosDeDemostracion() {
        return FXCollections.observableArrayList(
            crearMiembro(1, "01234567-8", "María Elena", "Hernández", "7000-1001",
                "maria.hernandez@comunidad.test", Miembro.Estado.ACTIVO),
            crearMiembro(2, "02345678-9", "José Antonio", "Martínez", "7000-1002",
                "jose.martinez@comunidad.test", Miembro.Estado.ACTIVO),
            crearMiembro(3, "03456789-0", "Ana Sofía", "López", "7000-1003",
                "ana.lopez@comunidad.test", Miembro.Estado.ACTIVO),
            crearMiembro(4, "04567890-1", "Carlos Roberto", "Gómez", "7000-1004",
                "carlos.gomez@comunidad.test", Miembro.Estado.INACTIVO)
        );
    }

    private Miembro crearMiembro(
        int id,
        String dui,
        String nombres,
        String apellidos,
        String telefono,
        String correo,
        Miembro.Estado estado
    ) {
        return new Miembro(
            id,
            dui,
            nombres,
            apellidos,
            telefono,
            correo,
            "Comunidad Los Olivares",
            LocalDate.now().minusMonths(id),
            estado
        );
    }
}
