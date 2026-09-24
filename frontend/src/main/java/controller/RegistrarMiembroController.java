package controller;

import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import service.MiembroApiClient.CreateMemberRequest;
import models.CountryData;
import models.MiembroModel;
import models.ViviendaModel;
import java.util.List;

public class RegistrarMiembroController {
    private static final String DUI = "DUI";
    private static final String PASAPORTE = "Pasaporte";
    private static final String RESIDENTE = "Carnet de residente";

    @FXML private ComboBox<String> selectorTipoDocumento;
    @FXML private ComboBox<CountryData> selectorPais;
    @FXML private ComboBox<CountryData> selectorCodigoTelefono;
    @FXML private VBox contenedorPais;
    @FXML private Label lblNumeroDocumento;
    @FXML private Label lblModoFormulario;
    @FXML private Label lblTituloFormulario;
    @FXML private Label lblDescripcionFormulario;
    @FXML private TextField campoDui;
    @FXML private TextField campoNombres;
    @FXML private TextField campoApellidos;
    @FXML private TextField campoTelefono;
    @FXML private TextField campoCorreo;
    @FXML private ComboBox<ViviendaModel> selectorVivienda;
    @FXML private Label lblErrorRegistro;

    private boolean actualizandoDocumento;
    private boolean actualizandoTelefono;

    public void setViviendas(List<ViviendaModel> viviendas) {
        selectorVivienda.getItems().clear();
        if (viviendas != null) selectorVivienda.getItems().addAll(viviendas);
        selectorVivienda.setCellFactory(list -> viviendaCell());
        selectorVivienda.setButtonCell(viviendaCell());
        if (viviendas == null || viviendas.isEmpty()) {
            selectorVivienda.setPromptText("Sin viviendas registradas (crea una primero)");
        } else {
            selectorVivienda.setPromptText("Selecciona una vivienda");
            if (viviendas.size() == 1) selectorVivienda.getSelectionModel().selectFirst();
        }
    }

    public void setMiembro(MiembroModel miembro) {
        String tipo = switch (miembro.getTipoDocumento() == null ? "DUI" : miembro.getTipoDocumento()) {
            case "PASAPORTE" -> PASAPORTE;
            case "CARNET_RESIDENTE" -> RESIDENTE;
            default -> DUI;
        };
        selectorTipoDocumento.setValue(tipo);
        campoDui.setText(miembro.getDui() == null ? "" : miembro.getDui());
        if (miembro.getPaisOrigen() != null && !miembro.getPaisOrigen().isBlank()) {
            CountryData c = CountryData.findByName(miembro.getPaisOrigen());
            if (c != null) selectorPais.setValue(c);
        }
        campoNombres.setText(miembro.getNombres());
        campoApellidos.setText(miembro.getApellidos());
        campoCorreo.setText(miembro.getCorreo());
        String telefono = miembro.getTelefono() == null ? "" : miembro.getTelefono().trim();
        if (!DUI.equals(tipo) && telefono.startsWith("+") && telefono.contains(" ")) {
            int separator = telefono.indexOf(' ');
            String prefix = telefono.substring(0, separator);
            CountryData c = CountryData.findByDialCode(prefix);
            if (c != null) selectorCodigoTelefono.setValue(c);
            campoTelefono.setText(telefono.substring(separator + 1));
        } else {
            campoTelefono.setText(telefono);
        }
        selectorVivienda.getItems().stream()
            .filter(value -> value.getIdVivienda().equals(miembro.getIdVivienda()))
            .findFirst()
            .ifPresent(selectorVivienda::setValue);
        lblModoFormulario.setText("ACTUALIZAR REGISTRO");
        lblTituloFormulario.setText("Editar miembro");
        lblDescripcionFormulario.setText("Actualiza la información del miembro. El identificador interno y su historial se conservarán.");
    }

    private javafx.scene.control.ListCell<ViviendaModel> viviendaCell() {
        return new javafx.scene.control.ListCell<>() {
            @Override protected void updateItem(ViviendaModel value, boolean empty) {
                super.updateItem(value, empty);
                setText(empty || value == null ? null : value.getCodigo() + " - " + value.getDireccion());
            }
        };
    }

    private ListCell<CountryData> countryCell() {
        return new ListCell<>() {
            @Override protected void updateItem(CountryData item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(item.name());
                    setGraphic(item.createFlagView());
                    setGraphicTextGap(8);
                }
            }
        };
    }

    private ListCell<CountryData> phoneListCell() {
        return new ListCell<>() {
            @Override protected void updateItem(CountryData item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(item.name() + " (" + item.dialCode() + ")");
                    setGraphic(item.createFlagView());
                    setGraphicTextGap(8);
                }
            }
        };
    }

    private ListCell<CountryData> phoneButtonCell() {
        return new ListCell<>() {
            @Override protected void updateItem(CountryData item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(item.dialCode());
                    setGraphic(item.createFlagView());
                    setGraphicTextGap(6);
                }
            }
        };
    }

    @FXML
    private void initialize() {
        selectorTipoDocumento.getItems().addAll(DUI, PASAPORTE, RESIDENTE);
        
        List<CountryData> countries = CountryData.getAll();
        selectorPais.getItems().setAll(countries);
        selectorPais.setCellFactory(lv -> countryCell());
        selectorPais.setButtonCell(countryCell());

        selectorCodigoTelefono.getItems().setAll(countries);
        selectorCodigoTelefono.setCellFactory(lv -> phoneListCell());
        selectorCodigoTelefono.setButtonCell(phoneButtonCell());

        // Al cambiar país, sincronizar automáticamente el código de teléfono
        selectorPais.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && selectorCodigoTelefono.isVisible()) {
                selectorCodigoTelefono.setValue(newVal);
            }
        });

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
        } else {
            if (selectorCodigoTelefono.getValue() == null) {
                CountryData sv = CountryData.findByIso2("sv");
                if (sv != null) selectorCodigoTelefono.setValue(sv);
            }
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
        CountryData paisData = selectorPais.getValue();
        String pais = extranjero && paisData != null ? paisData.name() : null;
        String nombres = campoNombres.getText().trim();
        String apellidos = campoApellidos.getText().trim();
        String telefonoLocal = campoTelefono.getText().trim();
        String correo = campoCorreo.getText().trim();
        ViviendaModel vivienda = selectorVivienda.getValue();

        if (DUI.equals(tipo) && !documento.matches("\\d{9}")) return invalid("El DUI debe contener 9 dígitos.");
        if (extranjero && !documento.matches("[A-Z0-9]{5,30}")) return invalid("Ingresa un documento válido de 5 a 30 letras o números.");
        if (extranjero && (pais == null || pais.isBlank())) return invalid("Selecciona el país de origen.");
        if (nombres.isBlank() || apellidos.isBlank()) return invalid("Los nombres y apellidos son obligatorios.");
        if (!telefonoLocal.isBlank() && !telefonoLocal.matches("\\d{4}-\\d{4}")) return invalid("El teléfono debe tener el formato 0000-0000.");
        CountryData phoneData = selectorCodigoTelefono.getValue();
        String dialCode = phoneData != null ? phoneData.dialCode() : "";
        if (extranjero && !telefonoLocal.isBlank() && dialCode.isBlank()) return invalid("Selecciona el código internacional del teléfono.");
        if (!correo.isBlank() && !correo.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) return invalid("Ingresa un correo electrónico válido.");
        if (vivienda == null) return invalid("Debes seleccionar la vivienda donde reside.");
        Integer idVivienda = vivienda.getIdVivienda();

        String telefono = telefonoLocal.isBlank() ? ""
            : extranjero ? dialCode + " " + telefonoLocal : telefonoLocal;
        lblErrorRegistro.setText("");
        return new CreateMemberRequest(documento, tipoApi(tipo), pais, nombres, apellidos, telefono, correo, idVivienda);
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
