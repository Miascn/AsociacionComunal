package components;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;


public class DashboardCard extends VBox {


    public DashboardCard(String titulo, String valor){

        Label title = new Label(titulo);
        Label number = new Label(valor);


        title.setStyle(
                "-fx-font-size:16px;"
        );

        number.setStyle(
                "-fx-font-size:28px;" +
                        "-fx-font-weight:bold;"
        );


        setAlignment(Pos.CENTER);
        setSpacing(10);

        setPrefSize(200,120);


        setStyle(
                "-fx-background-color:white;" +
                        "-fx-background-radius:15;" +
                        "-fx-padding:20;"
        );


        getChildren().addAll(title,number);
    }
}