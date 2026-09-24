package services;

import models.DashboardData;
import service.DashboardService;

public class MockDashboardService {
    private final DashboardService service = new DashboardService();

    public DashboardData getDashboard() {
        try {
            return service.loadDashboard();
        } catch (Exception e) {
            return new DashboardData(245, 80, 2450, 3);
        }
    }
}