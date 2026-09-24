package controller;

import java.time.format.DateTimeFormatter;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import service.ViviendaApiClient.HouseDetail;
import service.ViviendaApiClient.Resident;

public final class DetalleViviendaController {
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    @FXML private Label lblCodigo,lblEstado,lblSector,lblDireccion,lblReferencia,lblFecha,lblResumen;
    @FXML private ListView<String> listaAdultos,listaMenores;

    public void setDetail(HouseDetail detail) {
        var house=detail.vivienda();
        lblCodigo.setText(house.getCodigo());lblEstado.setText(house.getEstado());lblSector.setText(house.getSector());
        lblDireccion.setText(house.getDireccion());lblReferencia.setText(value(house.getReferencia(),"Sin referencia"));
        lblFecha.setText(house.getFechaRegistro()==null?"No registrada":house.getFechaRegistro().format(DATE));
        for(Resident resident:detail.residentes()){
            String name=resident.name()+(resident.representative()?" (Representante)":"");
            if("MENOR".equals(resident.type()))listaMenores.getItems().add(name);else listaAdultos.getItems().add(name);
        }
        listaAdultos.setPlaceholder(new Label("No hay adultos registrados"));listaMenores.setPlaceholder(new Label("No hay menores registrados"));
        lblResumen.setText(listaAdultos.getItems().size()+" adultos · "+listaMenores.getItems().size()+" menores");
    }
    private String value(String value,String fallback){return value==null||value.isBlank()?fallback:value;}
}
