package controller;

import java.util.List;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import models.MiembroModel;
import models.RolModel;
import models.UsuarioModel;
import service.UsuarioApiClient.UsuarioRequest;

public class UsuarioFormController {
    @FXML private Label lblTitulo;
    @FXML private Label lblError;
    @FXML private TextField campoUsuario;
    @FXML private PasswordField campoClave;
    @FXML private ComboBox<RolModel> selectorRol;
    @FXML private ComboBox<MiembroModel> selectorMiembro;
    @FXML private ComboBox<String> selectorEstado;
    private boolean editing;

    @FXML private void initialize() {
        selectorEstado.getItems().addAll("ACTIVO", "BLOQUEADO", "INACTIVO");
        selectorEstado.setValue("ACTIVO");
        selectorEstado.setDisable(true);
        selectorMiembro.setCellFactory(list -> memberCell());
        selectorMiembro.setButtonCell(memberCell());
    }

    public void setCatalogs(List<RolModel> roles, List<MiembroModel> members) {
        selectorRol.getItems().setAll(roles);
        MiembroModel none = new MiembroModel(0, "", "DUI", null, null, "Sin miembro", "asociado", null, null, null, null, "ACTIVO");
        selectorMiembro.getItems().setAll(none);
        selectorMiembro.getItems().addAll(members);
        selectorMiembro.setValue(none);
        if (roles.size() == 1) selectorRol.setValue(roles.get(0));
    }

    public void setUsuario(UsuarioModel user) {
        editing = true;
        selectorEstado.setDisable(false);
        lblTitulo.setText("Editar usuario");
        campoUsuario.setText(user.getNombreUsuario());
        campoClave.setPromptText("Dejar vacío para conservar la contraseña");
        selectorRol.getItems().stream().filter(role -> role.idRol().equals(user.getIdRol())).findFirst().ifPresent(selectorRol::setValue);
        selectorMiembro.getItems().stream().filter(member -> java.util.Objects.equals(member.getIdMiembro(), user.getIdMiembro()))
            .findFirst().ifPresent(selectorMiembro::setValue);
        selectorEstado.setValue(user.getEstado());
    }

    public UsuarioRequest validatedRequest() {
        String username = campoUsuario.getText() == null ? "" : campoUsuario.getText().trim();
        String password = campoClave.getText();
        RolModel role = selectorRol.getValue();
        MiembroModel member = selectorMiembro.getValue();
        if (!username.matches("[A-Za-z0-9._@-]{3,50}")) return invalid("El usuario debe tener de 3 a 50 caracteres válidos.");
        if ((!editing || !password.isBlank()) && password.length() < 8) return invalid("La contraseña debe contener al menos 8 caracteres.");
        if (role == null) return invalid("Selecciona un rol.");
        lblError.setText("");
        return new UsuarioRequest(username, password.isBlank() ? null : password, role.idRol(),
            member == null || member.getIdMiembro() == 0 ? 0 : member.getIdMiembro(), selectorEstado.getValue());
    }

    public void showError(String message) { lblError.setText(message); }

    private UsuarioRequest invalid(String message) { showError(message); return null; }

    private javafx.scene.control.ListCell<MiembroModel> memberCell() {
        return new javafx.scene.control.ListCell<>() {
            @Override protected void updateItem(MiembroModel value, boolean empty) {
                super.updateItem(value, empty);
                setText(empty || value == null ? null : (value.getNombres() + " " + value.getApellidos()).trim());
            }
        };
    }
}
