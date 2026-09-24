package controller;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import service.ViviendaApiClient.HouseRequest;
import service.ViviendaApiClient.Resident;
import models.MiembroModel;
import models.ViviendaModel;
import java.util.*;

public class ViviendaFormController {
    @FXML private TextField campoCodigo,campoSector,campoDireccion,campoReferencia;
    @FXML private ComboBox<String> selectorEstado;
    @FXML private TextArea campoAdultos,campoMenores;
    @FXML private Label lblError;

    @FXML private void initialize(){selectorEstado.getItems().addAll("ACTIVA","DESHABITADA","INACTIVA");selectorEstado.setValue("ACTIVA");}
    public void setMembers(List<MiembroModel> members){}
    public void setSugerencias(String codigoSugerido, String sectorSugerido) {
        if (codigoSugerido != null && (campoCodigo.getText() == null || campoCodigo.getText().isBlank())) {
            campoCodigo.setText(codigoSugerido);
        }
        if (sectorSugerido != null && (campoSector.getText() == null || campoSector.getText().isBlank())) {
            campoSector.setText(sectorSugerido);
        }
    }
    public void setHouse(ViviendaModel house,List<Resident> residents){
        campoCodigo.setText(house.getCodigo());campoSector.setText(house.getSector());campoDireccion.setText(house.getDireccion());campoReferencia.setText(house.getReferencia());selectorEstado.setValue(house.getEstado());
        campoAdultos.setText(residents.stream().filter(r->"ADULTO".equals(r.type())&&!r.representative()).map(Resident::name).reduce((a,b)->a+"\n"+b).orElse(""));
        campoMenores.setText(residents.stream().filter(r->"MENOR".equals(r.type())).map(Resident::name).reduce((a,b)->a+"\n"+b).orElse(""));
    }
    public HouseRequest request(){
        if(campoCodigo.getText().trim().length()<3)return invalid("Ingresa un código de al menos 3 caracteres.");
        if(campoSector.getText().isBlank()||campoDireccion.getText().isBlank())return invalid("Sector y dirección son obligatorios.");
        lblError.setText("");
        return new HouseRequest(campoCodigo.getText().trim().toUpperCase(),campoSector.getText().trim(),campoDireccion.getText().trim(),campoReferencia.getText().trim(),null,selectorEstado.getValue(),lines(campoAdultos),lines(campoMenores));
    }
    public void showError(String value){lblError.setText(value);}
    private HouseRequest invalid(String value){showError(value);return null;}
    private List<String> lines(TextArea area){return area.getText().lines().map(String::trim).filter(v->!v.isBlank()).toList();}
    public record MemberOption(Integer id,String name){@Override public String toString(){return name;}}
}
