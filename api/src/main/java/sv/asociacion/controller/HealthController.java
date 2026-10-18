package sv.asociacion.controller;

import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import sv.asociacion.config.DBConnection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

/**
 * Salud del servicio. Ya no abre ni maneja una {@code Connection}: delega en
 * {@link DBConnection} la verificacion y solo traduce el resultado a HTTP.
 */
public class HealthController {

    private final BooleanSupplier disponibilidad;
    private final Supplier<DBConnection.EstadoPool> estadoPool;

    public HealthController() {
        this(DBConnection.getInstance()::verificarDisponibilidad, DBConnection.getInstance()::estadoPool);
    }

    /** Constructor para pruebas: permite simular la base disponible o caida. */
    public HealthController(BooleanSupplier disponibilidad, Supplier<DBConnection.EstadoPool> estadoPool) {
        this.disponibilidad = disponibilidad;
        this.estadoPool = estadoPool;
    }

    public void health(Context context) {
        boolean valid = disponibilidad.getAsBoolean();
        DBConnection.EstadoPool pool = estadoPool.get();

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("service", "asociacion-api");
        body.put("database", valid ? "available" : "unavailable");
        body.put("pool", Map.of(
            "activas", pool.activas(),
            "inactivas", pool.inactivas(),
            "maxActivas", pool.maxActivas()
        ));
        context.status(valid ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE).json(body);
    }
}
