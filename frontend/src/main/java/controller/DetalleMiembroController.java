package controller;

import java.time.format.DateTimeFormatter;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import sv.asociacion.backend.entity.Miembro;

public class DetalleMiembroController {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @FXML private Label lblIniciales;
    @FXML private Label lblNombreCompleto;
    @FXML private Label lblDocumento;
    @FXML private Label lblTipoDocumento;
    @FXML private Label lblPais;
    @FXML private Label lblTelefono;
    @FXML private Label lblCorreo;
    @FXML private Label lblDireccion;
    @FXML private Label lblFechaIngreso;
    @FXML private Label lblEstado;

    public void setMiembro(Miembro miembro) {
        String nombres = value(miembro.getNombres());
        String apellidos = value(miembro.getApellidos());
        lblNombreCompleto.setText((nombres + " " + apellidos).trim());
        lblIniciales.setText(inicial(nombres) + inicial(apellidos));
        lblDocumento.setText(formatearDocumento(miembro));
        lblTipoDocumento.setText(nombreTipo(miembro.getTipoDocumento()));
        lblPais.setText(valueOrDefault(miembro.getPaisOrigen(), "El Salvador"));
        lblTelefono.setText(valueOrDefault(miembro.getTelefono(), "No registrado"));
        lblCorreo.setText(valueOrDefault(miembro.getCorreo(), "No registrado"));
        lblDireccion.setText(valueOrDefault(miembro.getDireccion(), "No registrada"));
        lblFechaIngreso.setText(miembro.getFechaIngreso() == null
            ? "No registrada" : miembro.getFechaIngreso().format(DATE_FORMAT));
        lblEstado.setText(miembro.getEstado() == null ? "Sin estado" : miembro.getEstado().name());
    }

    private String formatearDocumento(Miembro miembro) {
        String value = value(miembro.getDui());
        if ((miembro.getTipoDocumento() == null || "DUI".equals(miembro.getTipoDocumento()))
            && value.matches("\\d{9}")) {
            return value.substring(0, 8) + "-" + value.substring(8);
        }
        return value;
    }

    private String nombreTipo(String tipo) {
        if ("PASAPORTE".equals(tipo)) return "Pasaporte";
        if ("CARNET_RESIDENTE".equals(tipo)) return "Carnet de residente";
        return "DUI";
    }

    private String inicial(String value) { return value.isBlank() ? "" : value.substring(0, 1).toUpperCase(); }
    private String value(String value) { return value == null ? "" : value.trim(); }
    private String valueOrDefault(String value, String fallback) { return value(value).isBlank() ? fallback : value.trim(); }
}
