package components;


import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;


public class Header extends HBox {


    public Header(){


        Label title =
                new Label(
                        "Asociación Comunal ERP"
                );


        Label user =
                new Label(
                        "Administrador ▼"
                );


        setSpacing(20);
        setAlignment(Pos.CENTER_RIGHT);


        setPrefHeight(60);


        getChildren().addAll(
                title,
                user
        );


        setStyle(
                "-fx-background-color:white;" +
                        "-fx-padding:15;"
        );

    }
}