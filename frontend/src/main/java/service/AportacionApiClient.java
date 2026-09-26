package service;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import models.AportacionModel;
import security.SessionManager;

public final class AportacionApiClient {
    private final QaApiConfig config;
    private final HttpClient client;
    private final ObjectMapper json;

    public AportacionApiClient() {
        this(QaApiConfig.load());
    }

    AportacionApiClient(QaApiConfig config) {
        this.config = config;
        this.client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
        this.json = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    public AportacionModel.Page findPage(Integer idMiembro, Integer idProyecto, String periodo,
                                         String desde, String hasta, String metodo, String estado,
                                         String busqueda, int page, int size)
        throws IOException, InterruptedException {

        StringBuilder query = new StringBuilder("/api/aportaciones?");
        query.append("page=").append(page);
        query.append("&size=").append(size);

        if (idMiembro != null) query.append("&idMiembro=").append(idMiembro);
        if (idProyecto != null) query.append("&idProyecto=").append(idProyecto);
        if (periodo != null && !periodo.isBlank()) query.append("&periodo=").append(encode(periodo.trim()));
        if (desde != null && !desde.isBlank()) query.append("&desde=").append(encode(desde.trim()));
        if (hasta != null && !hasta.isBlank()) query.append("&hasta=").append(encode(hasta.trim()));
        if (metodo != null && !metodo.isBlank() && !"TODOS".equalsIgnoreCase(metodo)) {
            query.append("&metodo=").append(encode(metodo.trim()));
        }
        if (estado != null && !estado.isBlank() && !"TODOS".equalsIgnoreCase(estado)) {
            query.append("&estado=").append(encode(estado.trim()));
        }
        if (busqueda != null && !busqueda.isBlank()) query.append("&busqueda=").append(encode(busqueda.trim()));

        HttpRequest request = requestBuilder(query.toString()).GET().build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new IOException(error(response));
        }

        return json.readValue(response.body(), AportacionModel.Page.class);
    }

    public AportacionModel findById(Long id) throws IOException, InterruptedException {
        HttpRequest request = requestBuilder("/api/aportaciones/" + id).GET().build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new IOException(error(response));
        }

        return json.readValue(response.body(), AportacionModel.class);
    }

    public AportacionModel create(AportacionModel model) throws IOException, InterruptedException {
        Map<String, Object> payload = new java.util.HashMap<>();
        payload.put("idMiembro", model.getIdMiembro());
        payload.put("idProyecto", model.getIdProyecto());
        payload.put("periodoMes", model.getPeriodoMes());
        payload.put("monto", model.getMonto());
        payload.put("fechaPago", model.getFechaPago());
        payload.put("metodoPago", model.getMetodoPago());
        payload.put("referencia", model.getReferencia());
        String body = json.writeValueAsString(payload);

        HttpRequest request = requestBuilder("/api/aportaciones")
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 201) {
            throw new IOException(error(response));
        }
        return json.readValue(response.body(), AportacionModel.class);
    }

    public AportacionModel update(Long id, AportacionModel model) throws IOException, InterruptedException {
        Map<String, Object> payload = new java.util.HashMap<>();
        payload.put("idMiembro", model.getIdMiembro());
        payload.put("idProyecto", model.getIdProyecto());
        payload.put("periodoMes", model.getPeriodoMes());
        payload.put("monto", model.getMonto());
        payload.put("fechaPago", model.getFechaPago());
        payload.put("metodoPago", model.getMetodoPago());
        payload.put("referencia", model.getReferencia());
        String body = json.writeValueAsString(payload);

        HttpRequest request = requestBuilder("/api/aportaciones/" + id)
            .header("Content-Type", "application/json")
            .PUT(HttpRequest.BodyPublishers.ofString(body))
            .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IOException(error(response));
        }
        return json.readValue(response.body(), AportacionModel.class);
    }

    public AportacionModel ajustarMonto(Long id, java.math.BigDecimal nuevoMonto) throws IOException, InterruptedException {
        String body = json.writeValueAsString(Map.of("monto", nuevoMonto));
        HttpRequest request = requestBuilder("/api/aportaciones/" + id + "/monto")
            .header("Content-Type", "application/json")
            .method("PATCH", HttpRequest.BodyPublishers.ofString(body))
            .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IOException(error(response));
        }
        return json.readValue(response.body(), AportacionModel.class);
    }

    public void anular(Long id) throws IOException, InterruptedException {
        HttpRequest request = requestBuilder("/api/aportaciones/" + id + "/anular")
            .method("PATCH", HttpRequest.BodyPublishers.noBody())
            .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IOException(error(response));
        }
    }

    public record ConfiguracionCuota(String clave, java.math.BigDecimal valor, String descripcion) { }

    public record MantenimientoPeriodoResult(
        String periodoMes,
        java.math.BigDecimal cuotaEsperada,
        int totalMiembros,
        int pagados,
        int pendientes,
        java.math.BigDecimal totalRecaudado,
        java.math.BigDecimal totalEsperado,
        java.util.List<Item> items
    ) {
        public record Item(
            Integer idMiembro,
            String nombreCompleto,
            String dui,
            String telefono,
            String direccion,
            boolean pagado,
            Long idAportacion,
            java.math.BigDecimal montoPagado,
            String fechaPago,
            String metodoPago,
            String referencia,
            String estadoAportacion
        ) { }
    }

    public java.math.BigDecimal getCuotaMantenimiento() throws IOException, InterruptedException {
        HttpRequest request = requestBuilder("/api/aportaciones/configuracion").GET().build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IOException(error(response));
        }
        ConfiguracionCuota cfg = json.readValue(response.body(), ConfiguracionCuota.class);
        return cfg.valor();
    }

    public java.math.BigDecimal updateCuotaMantenimiento(java.math.BigDecimal nuevaCuota) throws IOException, InterruptedException {
        String body = json.writeValueAsString(Map.of("cuota", nuevaCuota));
        HttpRequest request = requestBuilder("/api/aportaciones/configuracion")
            .header("Content-Type", "application/json")
            .PUT(HttpRequest.BodyPublishers.ofString(body))
            .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IOException(error(response));
        }
        ConfiguracionCuota cfg = json.readValue(response.body(), ConfiguracionCuota.class);
        return cfg.valor();
    }

    public MantenimientoPeriodoResult getMantenimientoPeriodo(String periodoMes, String busqueda)
        throws IOException, InterruptedException {
        StringBuilder query = new StringBuilder("/api/aportaciones/mantenimiento?");
        if (periodoMes != null && !periodoMes.isBlank()) {
            query.append("periodo=").append(encode(periodoMes.trim()));
        }
        if (busqueda != null && !busqueda.isBlank()) {
            query.append("&busqueda=").append(encode(busqueda.trim()));
        }
        HttpRequest request = requestBuilder(query.toString()).GET().build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IOException(error(response));
        }
        return json.readValue(response.body(), MantenimientoPeriodoResult.class);
    }

    private HttpRequest.Builder requestBuilder(String path) {
        return HttpRequest.newBuilder(URI.create(config.baseUrl() + path))
            .timeout(Duration.ofSeconds(20))
            .header("Authorization", "Bearer " + SessionManager.getInstance().requireToken())
            .header("Accept", "application/json")
            .header("ngrok-skip-browser-warning", "1");
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private String error(HttpResponse<String> response) {
        try {
            return json.readTree(response.body()).path("error").asText("HTTP " + response.statusCode());
        } catch (Exception ignored) {
            return "HTTP " + response.statusCode();
        }
    }
}
