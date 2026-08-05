package controller;

import java.time.LocalDate;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
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

    private final ObservableList<Miembro> miembros = FXCollections.observableArrayList();

    @FXML
    private void initialize() {
        columnaDui.setCellValueFactory(new PropertyValueFactory<>("dui"));
        columnaNombres.setCellValueFactory(new PropertyValueFactory<>("nombres"));
        columnaApellidos.setCellValueFactory(new PropertyValueFactory<>("apellidos"));
        columnaTelefono.setCellValueFactory(new PropertyValueFactory<>("telefono"));
        columnaCorreo.setCellValueFactory(new PropertyValueFactory<>("correo"));
        columnaEstado.setCellValueFactory(new PropertyValueFactory<>("estado"));

        miembros.setAll(datosDeDemostracion());
        tablaMiembros.setItems(miembros);
        actualizarTotal();
    }

    public ObservableList<Miembro> getMiembros() {
        return miembros;
    }

    private void actualizarTotal() {
        lblTotalMiembros.setText(miembros.size() + " miembros registrados");
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
