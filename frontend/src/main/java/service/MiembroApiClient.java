package service;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import models.MiembroModel;
import models.UsuarioModel;
import security.SessionManager;

public final class MiembroApiClient {
    private final QaApiConfig config;
    private final HttpClient client;
    private final ObjectMapper json;

    public MiembroApiClient() { this(QaApiConfig.load()); }

    MiembroApiClient(QaApiConfig config) {
        this.config = config;
        client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
        json = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    public List<MiembroModel> findAll() throws IOException, InterruptedException {
        HttpResponse<String> response = send(request("/api/miembros").GET().build());
        ensure(response, 200);
        return Arrays.stream(json.readValue(response.body(), MiembroResponse[].class))
            .map(MiembroResponse::toModel).toList();
    }

    public MiembroModel findById(int id) throws IOException, InterruptedException {
        HttpResponse<String> response = send(request("/api/miembros/" + id).GET().build());
        ensure(response, 200);
        return json.readValue(response.body(), MiembroResponse.class).toModel();
    }

    public CreateMemberResult create(CreateMemberRequest value) throws IOException, InterruptedException {
        HttpRequest request = request("/api/miembros").header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(value))).build();
        HttpResponse<String> response = send(request);
        ensure(response, 201);
        CreateMemberResponse created = json.readValue(response.body(), CreateMemberResponse.class);
        return new CreateMemberResult(created.member().toModel(), created.username(), created.temporaryPassword());
    }

    public MiembroModel update(int id, CreateMemberRequest value) throws IOException, InterruptedException {
        HttpRequest request = request("/api/miembros/" + id).header("Content-Type", "application/json")
            .PUT(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(value))).build();
        HttpResponse<String> response = send(request);
        ensure(response, 200);
        return json.readValue(response.body(), MiembroResponse.class).toModel();
    }

    public MiembroModel changeState(int id, String estado) throws IOException, InterruptedException {
        HttpRequest request = request("/api/miembros/" + id + "/estado").header("Content-Type", "application/json")
            .method("PATCH", HttpRequest.BodyPublishers.ofString(json.writeValueAsString(new MemberStateRequest(estado))))
            .build();
        HttpResponse<String> response = send(request);
        ensure(response, 200);
        return json.readValue(response.body(), MiembroResponse.class).toModel();
    }

    public CredencialesMiembro getCredenciales(int idMiembro) throws IOException, InterruptedException {
        try {
            HttpRequest request = request("/api/miembros/" + idMiembro + "/credenciales").GET().build();
            HttpResponse<String> response = send(request);
            if (response.statusCode() == 200) {
                return json.readValue(response.body(), CredencialesMiembro.class);
            }
        } catch (Exception ignored) {}

        // Fallback: verificar si existe usuario asignado a este miembro
        try {
            UsuarioApiClient usuarioApi = new UsuarioApiClient();
            List<UsuarioModel> usuarios = usuarioApi.findAll();
            for (UsuarioModel u : usuarios) {
                if (u.getIdMiembro() != null && u.getIdMiembro().equals(idMiembro)) {
                    return new CredencialesMiembro(
                        idMiembro,
                        u.getIdUsuario(),
                        u.getNombreUsuario(),
                        u.tieneClaveProvisional(),
                        u.getClaveTemporal(),
                        u.getEstado()
                    );
                }
            }
        } catch (Exception ignored) {}

        return null;
    }

    public CredencialesMiembro generarCredenciales(int idMiembro) throws IOException, InterruptedException {
        return generarCredenciales(idMiembro, null);
    }

    public CredencialesMiembro generarCredenciales(int idMiembro, String documentoSugerido) throws IOException, InterruptedException {
        try {
            HttpRequest request = request("/api/miembros/" + idMiembro + "/credenciales")
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();
            HttpResponse<String> response = send(request);
            if (response.statusCode() == 200 || response.statusCode() == 201) {
                return json.readValue(response.body(), CredencialesMiembro.class);
            }
        } catch (Exception ignored) {}

        return generarCredencialesViaUsuarioApi(idMiembro, documentoSugerido);
    }

    private CredencialesMiembro generarCredencialesViaUsuarioApi(int idMiembro, String documentoSugerido) throws IOException, InterruptedException {
        UsuarioApiClient usuarioApi = new UsuarioApiClient();
        List<UsuarioModel> usuarios;
        try {
            usuarios = usuarioApi.findAll();
        } catch (Exception ex) {
            usuarios = List.of();
        }

        UsuarioModel usuarioExistente = null;
        for (UsuarioModel u : usuarios) {
            if (u.getIdMiembro() != null && u.getIdMiembro().equals(idMiembro)) {
                usuarioExistente = u;
                break;
            }
        }

        if (usuarioExistente != null) {
            try {
                UsuarioApiClient.ResetPasswordResult reset = usuarioApi.resetPassword(usuarioExistente.getIdUsuario());
                return new CredencialesMiembro(
                    idMiembro,
                    usuarioExistente.getIdUsuario(),
                    reset.nombreUsuario() != null ? reset.nombreUsuario() : usuarioExistente.getNombreUsuario(),
                    true,
                    reset.temporaryPassword(),
                    usuarioExistente.getEstado()
                );
            } catch (Exception resetEx) {
                String nuevaClave = generarClaveTemporal();
                try {
                    usuarioApi.update(usuarioExistente.getIdUsuario(), new UsuarioApiClient.UsuarioRequest(
                        usuarioExistente.getNombreUsuario(),
                        nuevaClave,
                        usuarioExistente.getIdRol(),
                        idMiembro,
                        "ACTIVO"
                    ));
                    return new CredencialesMiembro(
                        idMiembro,
                        usuarioExistente.getIdUsuario(),
                        usuarioExistente.getNombreUsuario(),
                        true,
                        nuevaClave,
                        usuarioExistente.getEstado()
                    );
                } catch (Exception updateEx) {
                    throw new IOException("No se pudo restablecer la contraseña del usuario: " + resetEx.getMessage(), resetEx);
                }
            }
        }

        String doc = documentoSugerido;
        if (doc == null || doc.isBlank()) {
            try {
                MiembroModel m = findById(idMiembro);
                if (m != null && m.getDui() != null && !m.getDui().isBlank()) {
                    doc = m.getDui().trim();
                }
            } catch (Exception ignored) {
                try {
                    List<MiembroModel> miembros = findAll();
                    for (MiembroModel m : miembros) {
                        if (m.getIdMiembro() != null && m.getIdMiembro().equals(idMiembro)) {
                            if (m.getDui() != null && !m.getDui().isBlank()) {
                                doc = m.getDui().trim();
                            }
                            break;
                        }
                    }
                } catch (Exception ignored2) {}
            }
        }
        if (doc == null || doc.isBlank()) {
            doc = "miembro" + idMiembro;
        }

        String cleanUsername = doc.replaceAll("[^A-Za-z0-9._@-]", "");
        if (cleanUsername.length() < 3) cleanUsername = "miembro" + idMiembro;
        if (cleanUsername.length() > 50) cleanUsername = cleanUsername.substring(0, 50);

        final String finalBase = cleanUsername;
        boolean enUso = usuarios.stream().anyMatch(u -> u.getNombreUsuario() != null && u.getNombreUsuario().equalsIgnoreCase(finalBase));
        String username = enUso ? (cleanUsername + "_" + idMiembro) : cleanUsername;

        Integer idRol = 2;
        try {
            RolApiClient rolApi = new RolApiClient();
            List<models.RolModel> roles = rolApi.findAll();
            boolean encontrado = false;
            for (models.RolModel r : roles) {
                if (r.nombre() != null && r.nombre().trim().equalsIgnoreCase("MIEMBRO")) {
                    idRol = r.idRol();
                    encontrado = true;
                    break;
                }
            }
            if (!encontrado && !roles.isEmpty()) {
                for (models.RolModel r : roles) {
                    if (r.nombre() != null && !r.nombre().trim().toUpperCase().contains("ADMIN")) {
                        idRol = r.idRol();
                        encontrado = true;
                        break;
                    }
                }
                if (!encontrado) idRol = roles.get(0).idRol();
            }
        } catch (Exception ignored) {}

        String passInicial = generarClaveTemporal();

        UsuarioModel nuevo = usuarioApi.create(new UsuarioApiClient.UsuarioRequest(
            username,
            passInicial,
            idRol,
            idMiembro,
            "ACTIVO"
        ));

        String claveTemporalFinal = passInicial;
        try {
            UsuarioApiClient.ResetPasswordResult reset = usuarioApi.resetPassword(nuevo.getIdUsuario());
            if (reset != null && reset.temporaryPassword() != null && !reset.temporaryPassword().isBlank()) {
                claveTemporalFinal = reset.temporaryPassword();
            }
        } catch (Exception ignored) {}

        return new CredencialesMiembro(
            idMiembro,
            nuevo.getIdUsuario(),
            nuevo.getNombreUsuario() != null ? nuevo.getNombreUsuario() : username,
            true,
            claveTemporalFinal,
            "ACTIVO"
        );
    }

    private static String generarClaveTemporal() {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789!@#$%";
        java.security.SecureRandom random = new java.security.SecureRandom();
        StringBuilder sb = new StringBuilder("Tmp!");
        for (int i = 0; i < 12; i++) sb.append(chars.charAt(random.nextInt(chars.length())));
        return sb.toString();
    }

    private HttpResponse<String> send(HttpRequest request) throws IOException, InterruptedException {
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private HttpRequest.Builder request(String path) {
        return HttpRequest.newBuilder(URI.create(config.baseUrl() + path)).timeout(Duration.ofSeconds(20))
            .header("Authorization", "Bearer " + SessionManager.getInstance().requireToken())
            .header("Accept", "application/json").header("ngrok-skip-browser-warning", "1");
    }

    private void ensure(HttpResponse<String> response, int expected) throws IOException {
        if (response.statusCode() == expected) return;
        String message = "HTTP " + response.statusCode();
        try { message = json.readTree(response.body()).path("error").asText(message); } catch (Exception ignored) { }
        throw new IOException(message);
    }

    public record CreateMemberRequest(String documento, String tipoDocumento, String paisOrigen,
        String nombres, String apellidos, String telefono, String correo, Integer idVivienda) { }
    public record CreateMemberResult(MiembroModel member, String username, String temporaryPassword) { }
    public record CredencialesMiembro(Integer idMiembro, Integer idUsuario, String nombreUsuario,
        boolean requiereCambioClave, String claveTemporal, String estadoUsuario) { }
    private record CreateMemberResponse(MiembroResponse member, String username, String temporaryPassword) { }
    private record MemberStateRequest(String estado) { }
    private record MiembroResponse(Integer id, String dui, String tipoDocumento, String paisOrigen,
        Integer idVivienda, String nombres, String apellidos, String telefono, String correo,
        String direccion, String fechaIngreso, String estado) {
        MiembroModel toModel() { return new MiembroModel(id, dui, tipoDocumento, paisOrigen, idVivienda,
            nombres, apellidos, telefono, correo, direccion, fechaIngreso, estado); }
    }
}
