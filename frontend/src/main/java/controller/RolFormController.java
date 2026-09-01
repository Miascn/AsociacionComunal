package controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import models.RolModel;
import service.RolApiClient.RolRequest;

public class RolFormController {
    @FXML private Label lblTitulo;
    @FXML private Label lblError;
    @FXML private TextField campoNombre;
    @FXML private TextField campoDescripcion;
    private boolean editing;
    private boolean baseRole;

    @FXML
    private void initialize() {
        lblError.setText("");
    }

    public void setRol(RolModel rol) {
        editing = true;
        lblTitulo.setText("Editar rol");
        campoNombre.setText(rol.nombre());
        campoDescripcion.setText(rol.descripcion() == null ? "" : rol.descripcion());
        baseRole = isBaseRole(rol.nombre());
        if (baseRole) {
            campoNombre.setDisable(true);
            showError("Los roles base del sistema no pueden cambiar de nombre.");
        }
    }

    public RolRequest validatedRequest() {
        String nombre = campoNombre.getText() == null ? "" : campoNombre.getText().trim();
        String descripcion = campoDescripcion.getText() == null ? null : campoDescripcion.getText().trim();

        if (nombre.length() < 2 || nombre.length() > 40) {
            return invalid("El nombre del rol debe tener entre 2 y 40 caracteres.");
        }
        lblError.setText("");
        return new RolRequest(nombre, descripcion == null || descripcion.isBlank() ? null : descripcion);
    }

    public void showError(String message) {
        lblError.setText(message);
    }

    private RolRequest invalid(String message) {
        showError(message);
        return null;
    }

    private boolean isBaseRole(String nombre) {
        if (nombre == null) return false;
        String upper = nombre.trim().toUpperCase();
        return upper.equals("ADMIN") || upper.equals("ADMINISTRADOR")
            || upper.equals("PRESIDENTE") || upper.equals("SECRETARIO")
            || upper.equals("TESORERO") || upper.equals("SINDICO")
            || upper.equals("MIEMBRO");
    }
}
