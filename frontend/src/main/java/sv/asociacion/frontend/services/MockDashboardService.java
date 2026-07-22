package sv.asociacion.frontend.services;

import sv.asociacion.frontend.models.DashboardData;

public class MockDashboardService {


    public DashboardData getDashboard(){

        // TODO BACKEND:
        // Aquí después irá la llamada HTTP

        return new DashboardData(
                245,
                80,
                2450,
                3
        );
    }
}