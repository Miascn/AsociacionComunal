package controller;

import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import service.MiembroApiClient.CreateMemberRequest;
import sv.asociacion.backend.entity.Vivienda;
import java.util.List;

public class RegistrarMiembroController {
    private static final String DUI = "DUI";
    private static final String PASAPORTE = "Pasaporte";
    private static final String RESIDENTE = "Carnet de residente";

    @FXML private ComboBox<String> selectorTipoDocumento;
    @FXML private ComboBox<String> selectorPais;
    @FXML private ComboBox<String> selectorCodigoTelefono;
    @FXML private VBox contenedorPais;
    @FXML private Label lblNumeroDocumento;
    @FXML private TextField campoDui;
    @FXML private TextField campoNombres;
    @FXML private TextField campoApellidos;
    @FXML private TextField campoTelefono;
    @FXML private TextField campoCorreo;
    @FXML private ComboBox<Vivienda> selectorVivienda;
    @FXML private Label lblErrorRegistro;

    private boolean actualizandoDocumento;
    private boolean actualizandoTelefono;

    public void setViviendas(List<Vivienda> viviendas) {
        selectorVivienda.getItems().setAll(viviendas);
        selectorVivienda.setCellFactory(list -> viviendaCell());
        selectorVivienda.setButtonCell(viviendaCell());
        if (viviendas.size() == 1) selectorVivienda.getSelectionModel().selectFirst();
    }

    private javafx.scene.control.ListCell<Vivienda> viviendaCell() {
        return new javafx.scene.control.ListCell<>() {
            @Override protected void updateItem(Vivienda value, boolean empty) {
                super.updateItem(value, empty);
                setText(empty || value == null ? null : value.getCodigo() + " - " + value.getDireccion());
            }
        };
    }

    @FXML
    private void initialize() {
        selectorTipoDocumento.getItems().addAll(DUI, PASAPORTE, RESIDENTE);
        selectorPais.getItems().addAll(
            "El Salvador", "Guatemala", "Honduras", "Nicaragua", "Costa Rica", "Panamá", "Belice",
            "México", "Estados Unidos", "Canadá", "Colombia", "Venezuela", "Ecuador",
            "Perú", "Bolivia", "Chile", "Argentina", "Brasil", "España", "Otro"
        );
        selectorCodigoTelefono.getItems().addAll(
            "+1", "+34", "+52", "+502", "+503", "+504", "+505", "+506", "+507",
            "+501", "+57", "+58", "+593", "+51", "+591", "+56", "+54", "+55"
        );
        selectorTipoDocumento.getSelectionModel().select(DUI);
        selectorTipoDocumento.valueProperty().addListener((obs, oldValue, value) -> actualizarTipoDocumento());
        campoDui.textProperty().addListener((obs, oldValue, value) -> formatearDocumento(value));
        campoTelefono.textProperty().addListener((obs, oldValue, value) -> formatearTelefono(value));
        actualizarTipoDocumento();
    }

    private void actualizarTipoDocumento() {
        boolean extranjero = !DUI.equals(selectorTipoDocumento.getValue());
        contenedorPais.setVisible(extranjero);
        contenedorPais.setManaged(extranjero);
        selectorCodigoTelefono.setVisible(extranjero);
        selectorCodigoTelefono.setManaged(extranjero);
        lblNumeroDocumento.setText(extranjero
            ? (PASAPORTE.equals(selectorTipoDocumento.getValue()) ? "Número de pasaporte *" : "Número de carnet *")
            : "Número de DUI *");
        campoDui.setPromptText(extranjero ? "Número del documento" : "00000000-0");
        campoDui.clear();
        if (!extranjero) {
            selectorPais.getSelectionModel().clearSelection();
            selectorCodigoTelefono.getSelectionModel().clearSelection();
        }
    }

    private void formatearDocumento(String value) {
        if (actualizandoDocumento || !DUI.equals(selectorTipoDocumento.getValue())) return;
        String digits = value.replaceAll("\\D", "");
        if (digits.length() > 9) digits = digits.substring(0, 9);
        String formatted = digits.length() > 8
            ? digits.substring(0, 8) + "-" + digits.substring(8)
            : digits;
        if (!formatted.equals(value)) {
            actualizandoDocumento = true;
            campoDui.setText(formatted);
            campoDui.positionCaret(formatted.length());
            actualizandoDocumento = false;
        }
    }

    private void formatearTelefono(String value) {
        if (actualizandoTelefono) return;
        String digits = value.replaceAll("\\D", "");
        if (digits.length() > 8) digits = digits.substring(0, 8);
        String formatted = digits.length() > 4
            ? digits.substring(0, 4) + "-" + digits.substring(4)
            : digits;
        if (!formatted.equals(value)) {
            actualizandoTelefono = true;
            campoTelefono.setText(formatted);
            campoTelefono.positionCaret(formatted.length());
            actualizandoTelefono = false;
        }
    }

    public CreateMemberRequest validatedRequest() {
        String tipo = selectorTipoDocumento.getValue();
        boolean extranjero = !DUI.equals(tipo);
        String documentoVisible = campoDui.getText().trim();
        String documento = DUI.equals(tipo) ? documentoVisible.replaceAll("\\D", "") : documentoVisible.toUpperCase();
        String pais = extranjero ? selectorPais.getValue() : null;
        String nombres = campoNombres.getText().trim();
        String apellidos = campoApellidos.getText().trim();
        String telefonoLocal = campoTelefono.getText().trim();
        String correo = campoCorreo.getText().trim();
        Vivienda vivienda = selectorVivienda.getValue();

        if (DUI.equals(tipo) && !documento.matches("\\d{9}")) return invalid("El DUI debe contener 9 dígitos.");
        if (extranjero && !documento.matches("[A-Z0-9]{5,30}")) return invalid("Ingresa un documento válido de 5 a 30 letras o números.");
        if (extranjero && (pais == null || pais.isBlank())) return invalid("Selecciona el país de origen.");
        if (nombres.isBlank() || apellidos.isBlank()) return invalid("Los nombres y apellidos son obligatorios.");
        if (!telefonoLocal.isBlank() && !telefonoLocal.matches("\\d{4}-\\d{4}")) return invalid("El teléfono debe tener el formato 0000-0000.");
        if (extranjero && !telefonoLocal.isBlank() && selectorCodigoTelefono.getValue() == null) return invalid("Selecciona el código internacional del teléfono.");
        if (!correo.isBlank() && !correo.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) return invalid("Ingresa un correo electrónico válido.");
        if (vivienda == null) return invalid("Selecciona la vivienda donde reside el miembro.");

        String telefono = telefonoLocal.isBlank() ? ""
            : extranjero ? selectorCodigoTelefono.getValue() + " " + telefonoLocal : telefonoLocal;
        lblErrorRegistro.setText("");
        return new CreateMemberRequest(documento, tipoApi(tipo), pais, nombres, apellidos, telefono, correo, vivienda.getIdVivienda());
    }

    public void showError(String message) { lblErrorRegistro.setText(message); }

    private String tipoApi(String tipo) {
        if (PASAPORTE.equals(tipo)) return "PASAPORTE";
        if (RESIDENTE.equals(tipo)) return "CARNET_RESIDENTE";
        return "DUI";
    }

    private CreateMemberRequest invalid(String message) {
        showError(message);
        return null;
    }
}
