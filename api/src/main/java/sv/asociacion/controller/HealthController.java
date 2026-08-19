package sv.asociacion.controller;

import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import sv.asociacion.config.DBConnection;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;

public class HealthController {

    public void health(Context context) {
        try (Connection connection = DBConnection.getInstance().getConnection()) {
            boolean valid = connection.isValid(3);
            context.status(valid ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE)
                .json(Map.of("service", "asociacion-api", "database", valid ? "available" : "unavailable"));
        } catch (SQLException | IllegalStateException exception) {
            context.status(HttpStatus.SERVICE_UNAVAILABLE)
                .json(Map.of("service", "asociacion-api", "database", "unavailable"));
        }
    }
}