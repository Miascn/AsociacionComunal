package controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import models.DashboardData;
import services.MockDashboardService;

public class DashboardController {
    @FXML
    private Label lblMiembros;

    @FXML
    private Label lblFamilias;

    @FXML
    private Label lblIngresos;

    @FXML
    private Label lblProyectos;

    private final MockDashboardService dashboardService = new MockDashboardService();

    @FXML
    private void initialize() {
        DashboardData data = dashboardService.getDashboard();
        lblMiembros.setText(String.valueOf(data.getMiembros()));
        lblFamilias.setText(String.valueOf(data.getFamilias()));
        lblIngresos.setText(String.format("$%,.2f", data.getIngresos()));
        lblProyectos.setText(String.valueOf(data.getProyectos()));
    }
}
