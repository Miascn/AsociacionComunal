package sv.asociacion.frontend.components;

import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;


public class Sidebar extends VBox {


    public Sidebar(){

        setSpacing(15);
        setPadding(new Insets(20));

        setPrefWidth(220);


        Button dashboard = new Button("Dashboard");
        Button personas = new Button("Personas");
        Button comunidad = new Button("Comunidad");
        Button cuotas = new Button("Cuotas");
        Button inventario = new Button("Inventario");
        Button reportes = new Button("Reportes");


        getChildren().addAll(
                dashboard,
                personas,
                comunidad,
                cuotas,
                inventario,
                reportes
        );


        setStyle(
                "-fx-background-color:#1e293b;"
        );


        for(var node : getChildren()){

            Button btn = (Button) node;

            btn.setPrefWidth(180);

            btn.setStyle(
                    "-fx-background-color:transparent;" +
                            "-fx-text-fill:white;" +
                            "-fx-font-size:14px;"
            );
        }

    }
}