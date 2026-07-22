package controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class PlaceholderController {
    @FXML
    private Label lblTitulo;

    public void setTitulo(String titulo) {
        lblTitulo.setText(titulo);
    }
}
