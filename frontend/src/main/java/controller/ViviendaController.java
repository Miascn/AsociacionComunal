package controller;

import java.text.Normalizer;
import java.util.*;
import javafx.collections.*;
import javafx.collections.transformation.FilteredList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.*;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseButton;
import models.MiembroModel;
import models.ViviendaModel;
import service.ResponsiveWindowService;
import service.MiembroApiClient;
import service.ViviendaApiClient;

public class ViviendaController {
    @FXML private TableView<ViviendaModel> tablaViviendas;
    @FXML private TableColumn<ViviendaModel,String> colCodigo,colSector,colDireccion,colRepresentante,colEstado;
    @FXML private TableColumn<ViviendaModel,Integer> colAdultos,colMenores;
    @FXML private Label lblTotal,lblHabitantes,lblEstadoModulo;
    @FXML private TextField campoBusqueda;
    @FXML private Button btnVerDetalle,btnEditar,btnDesactivar;

    private final ObservableList<ViviendaModel> viviendas=FXCollections.observableArrayList();
    private final List<MiembroModel> miembros=new ArrayList<>();
    private FilteredList<ViviendaModel> filtered;
    private final ViviendaApiClient api=new ViviendaApiClient();

    @FXML private void initialize(){
        tablaViviendas.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        colCodigo.setCellValueFactory(new PropertyValueFactory<>("codigo"));
        colSector.setCellValueFactory(new PropertyValueFactory<>("sector"));
        colDireccion.setCellValueFactory(new PropertyValueFactory<>("direccion"));
        colRepresentante.setCellValueFactory(new PropertyValueFactory<>("representante"));
        colAdultos.setCellValueFactory(new PropertyValueFactory<>("adultos"));
        colMenores.setCellValueFactory(new PropertyValueFactory<>("menores"));
        colEstado.setCellValueFactory(new PropertyValueFactory<>("estado"));
        filtered=new FilteredList<>(viviendas,value->true);
        tablaViviendas.setItems(filtered);
        campoBusqueda.textProperty().addListener((observable,oldValue,value)->filter(value));
        btnVerDetalle.disableProperty().bind(tablaViviendas.getSelectionModel().selectedItemProperty().isNull());
        btnEditar.disableProperty().bind(tablaViviendas.getSelectionModel().selectedItemProperty().isNull());
        btnDesactivar.disableProperty().bind(tablaViviendas.getSelectionModel().selectedItemProperty().isNull());
        tablaViviendas.setRowFactory(table->{
            TableRow<ViviendaModel> row=new TableRow<>();
            row.setOnMouseClicked(event->{
                if(!row.isEmpty()&&event.getButton()==MouseButton.PRIMARY&&event.getClickCount()==2) showDetail(row.getItem());
            });
            return row;
        });
        load();
    }

    private void load(){
        run("Conectando al servidor...",()->new Load(api.findAll(),new MiembroApiClient().findAll()),value->{
            viviendas.setAll(value.houses()); miembros.clear(); miembros.addAll(value.members());
            lblEstadoModulo.setText("Conectado al servidor"); updateTotals();
        });
    }
    @FXML private void nueva(){openForm(null);}
    @FXML private void editar(){openForm(tablaViviendas.getSelectionModel().getSelectedItem());}
    @FXML private void verDetalle(){ViviendaModel selected=tablaViviendas.getSelectionModel().getSelectedItem();if(selected!=null)showDetail(selected);}

    private void showDetail(ViviendaModel house){
        run("Consultando vivienda...",()->api.find(house.getIdVivienda()),detail->{
            try{
                FXMLLoader loader=new FXMLLoader(getClass().getResource("/fxml/views/detalle-vivienda.fxml"));
                Parent content=loader.load();
                loader.<DetalleViviendaController>getController().setDetail(detail);
                Dialog<Void> dialog=new Dialog<>(); dialog.initOwner(tablaViviendas.getScene().getWindow());
                dialog.setTitle("Detalle de vivienda "+house.getCodigo()); dialog.getDialogPane().setContent(content);
                dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE); style(dialog); lblEstadoModulo.setText("Conectado al servidor");
                ResponsiveWindowService.fitDialog(dialog, tablaViviendas.getScene().getWindow(), 660);
                dialog.showAndWait();
            }catch(Exception exception){error("No fue posible mostrar el detalle: "+exception.getMessage());}
        });
    }

    private void openForm(ViviendaModel house){
        if(miembros.isEmpty()){error("No hay miembros disponibles para asignar como representantes.");return;}
        try{
            FXMLLoader loader=new FXMLLoader(getClass().getResource("/fxml/views/vivienda-form.fxml"));
            Parent content=loader.load(); ViviendaFormController form=loader.getController(); form.setMembers(miembros);
            if(house!=null){var detail=api.find(house.getIdVivienda());form.setHouse(detail.vivienda(),detail.residentes());}
            ButtonType save=new ButtonType(house==null?"Registrar vivienda":"Guardar cambios",ButtonBar.ButtonData.OK_DONE);
            Dialog<Void> dialog=new Dialog<>();dialog.initOwner(tablaViviendas.getScene().getWindow());
            dialog.setTitle(house==null?"Nueva vivienda":"Editar vivienda");dialog.getDialogPane().setContent(content);
            dialog.getDialogPane().getButtonTypes().addAll(save,ButtonType.CANCEL);style(dialog);
            ResponsiveWindowService.fitDialog(dialog, tablaViviendas.getScene().getWindow(), 680);
            Button button=(Button)dialog.getDialogPane().lookupButton(save);
            button.addEventFilter(ActionEvent.ACTION,event->{
                event.consume();var request=form.request();if(request==null)return;button.setDisable(true);
                run("Guardando vivienda...",()->house==null?api.create(request):api.update(house.getIdVivienda(),request),saved->{
                    if(house==null)viviendas.add(saved);else viviendas.set(viviendas.indexOf(house),saved);
                    updateTotals();dialog.close();
                });
            });
            dialog.showAndWait();
        }catch(Exception exception){error(exception.getMessage());}
    }

    @FXML private void desactivar(){
        ViviendaModel value=tablaViviendas.getSelectionModel().getSelectedItem();if(value==null)return;
        Alert alert=new Alert(Alert.AlertType.CONFIRMATION,"La vivienda conservará todo su historial.",ButtonType.YES,ButtonType.NO);
        alert.setHeaderText("¿Desactivar "+value.getCodigo()+"?");
        if(alert.showAndWait().orElse(ButtonType.NO)!=ButtonType.YES)return;
        run("Desactivando...",()->{api.deactivate(value.getIdVivienda());return value;},done->{value.setEstado("INACTIVA");tablaViviendas.refresh();updateTotals();});
    }

    private <T> void run(String status,Work<T> work,java.util.function.Consumer<T> success){
        lblEstadoModulo.setText(status);Task<T> task=new Task<>(){protected T call()throws Exception{return work.run();}};
        task.setOnSucceeded(event->success.accept(task.getValue()));
        task.setOnFailed(event->{lblEstadoModulo.setText("Error de conexión");error(task.getException()==null?"Error desconocido":task.getException().getMessage());});
        Thread thread=new Thread(task,"viviendas-api");thread.setDaemon(true);thread.start();
    }
    private void filter(String text){String query=normalize(text);filtered.setPredicate(value->query.isBlank()||contains(value.getCodigo(),query)||contains(value.getSector(),query)||contains(value.getDireccion(),query)||contains(value.getRepresentante(),query));updateTotals();}
    private void updateTotals(){lblTotal.setText(filtered.size()+" viviendas");lblHabitantes.setText(filtered.stream().mapToInt(ViviendaModel::getTotalResidentes).sum()+" residentes registrados");}
    private boolean contains(String value,String query){return value!=null&&normalize(value).contains(query);}
    private String normalize(String value){return value==null?"":Normalizer.normalize(value,Normalizer.Form.NFD).replaceAll("\\p{M}","").toLowerCase().trim();}
    private void style(Dialog<?> dialog){dialog.getDialogPane().getStyleClass().add("member-dialog");var css=getClass().getResource("/styles/member-dialog.css");if(css!=null)dialog.getDialogPane().getStylesheets().add(css.toExternalForm());}
    private void error(String message){new Alert(Alert.AlertType.ERROR,message,ButtonType.OK).showAndWait();}
    private record Load(List<ViviendaModel> houses,List<MiembroModel> members){}
    @FunctionalInterface private interface Work<T>{T run()throws Exception;}
}
