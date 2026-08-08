package controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import service.MiembroApiClient.CreateMemberRequest;

public class RegistrarMiembroController {
    @FXML private TextField campoDui;
    @FXML private TextField campoNombres;
    @FXML private TextField campoApellidos;
    @FXML private TextField campoTelefono;
    @FXML private TextField campoCorreo;
    @FXML private TextField campoDireccion;
    @FXML private Label lblErrorRegistro;

    public CreateMemberRequest validatedRequest() {
        String dui = campoDui.getText().trim();
        String nombres = campoNombres.getText().trim();
        String apellidos = campoApellidos.getText().trim();
        String telefono = campoTelefono.getText().trim();
        String correo = campoCorreo.getText().trim();
        String direccion = campoDireccion.getText().trim();
        if (!dui.matches("\\d{8}-\\d")) return invalid("El DUI debe tener el formato 00000000-0.");
        if (nombres.isBlank() || apellidos.isBlank()) return invalid("Los nombres y apellidos son obligatorios.");
        if (!telefono.isBlank() && !telefono.matches("\\d{4}-\\d{4}")) return invalid("El teléfono debe tener el formato 0000-0000.");
        if (!correo.isBlank() && !correo.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) return invalid("Ingresa un correo electrónico válido.");
        if (direccion.isBlank()) return invalid("La dirección es obligatoria.");
        lblErrorRegistro.setText("");
        return new CreateMemberRequest(dui, nombres, apellidos, telefono, correo, direccion);
    }

    public void showError(String message) { lblErrorRegistro.setText(message); }

    private CreateMemberRequest invalid(String message) {
        showError(message);
        return null;
    }
}
