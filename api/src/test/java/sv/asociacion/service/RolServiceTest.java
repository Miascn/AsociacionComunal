package sv.asociacion.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import sv.asociacion.dao.RolDAO;
import sv.asociacion.dao.UsuarioDAO;
import sv.asociacion.domain.dto.RolRequest;
import sv.asociacion.domain.dto.RolResponse;
import sv.asociacion.domain.entity.Rol;

class RolServiceTest {
    private MemoryRolDAO rolDAO;
    private MemoryUsuarioDAO usuarioDAO;
    private RolService service;

    @BeforeEach
    void setUp() {
        rolDAO = new MemoryRolDAO();
        usuarioDAO = new MemoryUsuarioDAO();
        service = new RolService(rolDAO, usuarioDAO);

        // Seed base roles
        rolDAO.save(new Rol(1, "ADMINISTRADOR", "Administrador del sistema"));
        rolDAO.save(new Rol(2, "MIEMBRO", "Miembro general"));
        rolDAO.save(new Rol(3, "TESORERO", "Encargado de finanzas"));
    }

    @Test
    void createsAndRetrievesRolesWithUserCount() {
        usuarioDAO.setUserCount(1, 3);
        usuarioDAO.setUserCount(2, 10);

        var response = service.create(new RolRequest("COORDINADOR", "Coordina comisiones"));
        assertNotNull(response.idRol());
        assertEquals("COORDINADOR", response.nombre());
        assertEquals(0, response.usuariosAsociados());

        List<RolResponse> all = service.findAll();
        assertEquals(4, all.size());

        RolResponse admin = service.findById(1);
        assertNotNull(admin);
        assertEquals(3, admin.usuariosAsociados());
    }

    @Test
    void rejectsDuplicateRoleName() {
        assertThrows(IllegalStateException.class,
            () -> service.create(new RolRequest("ADMINISTRADOR", "Duplicado")));
        assertThrows(IllegalStateException.class,
            () -> service.create(new RolRequest("administrador", "Mismo nombre en minusculas")));
    }

    @Test
    void rejectsInvalidRoleName() {
        assertThrows(IllegalArgumentException.class,
            () -> service.create(new RolRequest("", "Vacio")));
        assertThrows(IllegalArgumentException.class,
            () -> service.create(new RolRequest("A", "Demasiado corto")));
    }

    @Test
    void updatesRoleSuccessfully() {
        var created = service.create(new RolRequest("AUDITOR", "Auditor de cuentas"));
        var updated = service.update(created.idRol(), new RolRequest("AUDITOR_GENERAL", "Auditor senior"));

        assertEquals("AUDITOR_GENERAL", updated.nombre());
        assertEquals("Auditor senior", updated.descripcion());
    }

    @Test
    void preventsRenamingBaseRole() {
        assertThrows(IllegalStateException.class,
            () -> service.update(1, new RolRequest("SUPER_ADMIN", "Intento de cambiar nombre")));
    }

    @Test
    void preventsDeletingBaseRole() {
        assertThrows(IllegalStateException.class,
            () -> service.delete(1));
        assertThrows(IllegalStateException.class,
            () -> service.delete(2));
    }

    @Test
    void preventsDeletingRoleWithUsers() {
        var created = service.create(new RolRequest("VOCAL", "Vocal de comite"));
        usuarioDAO.setUserCount(created.idRol(), 2);

        IllegalStateException ex = assertThrows(IllegalStateException.class,
            () -> service.delete(created.idRol()));
        assertTrue(ex.getMessage().contains("usuarios asociados"));
    }

    @Test
    void deletesUnassignedCustomRole() {
        var created = service.create(new RolRequest("TEMPORAL", "Rol que se borrara"));
        assertTrue(service.delete(created.idRol()));
        assertThrows(NoSuchElementException.class,
            () -> service.delete(created.idRol()));
    }

    // ------------------------------------------------------------------
    // SCRUM-329 — proteger DIRECTIVO y alinear los roles sembrados
    // ------------------------------------------------------------------

    @Test
    void protegeLosTresRolesSembrados() {
        assertTrue(RolService.isBaseRole("ADMIN"), "ADMIN se siembra y debe estar protegido.");
        assertTrue(RolService.isBaseRole("DIRECTIVO"), "DIRECTIVO se siembra y debe estar protegido.");
        assertTrue(RolService.isBaseRole("MIEMBRO"), "MIEMBRO se siembra y debe estar protegido.");
    }

    @Test
    void todoRolSembradoEnSchemaEstaProtegido() throws IOException {
        // Lee la semilla real de schema.sql en lugar de repetirla aquí: si el catálogo
        // inicial crece sin añadir el nombre a ROLES_BASE, esta prueba falla.
        Set<String> sembrados = rolesSembradosEnSchemaSql();

        // Comprobación del propio extractor: sin esto la prueba pasaría aunque el
        // parseo devolviera un subconjunto y no verificara casi nada.
        assertTrue(sembrados.containsAll(List.of("ADMIN", "DIRECTIVO", "MIEMBRO")),
            "El extractor no recuperó la semilla conocida; leyó: " + sembrados);

        for (String nombre : sembrados) {
            assertTrue(RolService.isBaseRole(nombre),
                "El rol '" + nombre + "' se siembra en schema.sql pero isBaseRole() no lo protege.");
        }
    }

    @Test
    void preventsRenamingDirectivo() {
        rolDAO.save(new Rol(4, "DIRECTIVO", "Miembro de la junta directiva"));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
            () -> service.update(4, new RolRequest("JUNTA_DIRECTIVA", "Intento de renombrar")));
        assertTrue(ex.getMessage().contains("rol base"),
            "El mensaje debe señalar que es un rol base: " + ex.getMessage());
    }

    @Test
    void preventsDeletingDirectivo() {
        rolDAO.save(new Rol(4, "DIRECTIVO", "Miembro de la junta directiva"));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
            () -> service.delete(4));
        assertTrue(ex.getMessage().contains("rol base"),
            "El mensaje debe señalar que es un rol base: " + ex.getMessage());
        assertTrue(rolDAO.findById(4).isPresent(), "El rol no debe haberse eliminado.");
    }

    @Test
    void conservaLosDemasNombresProtegidos() {
        // Criterio 8: este ticket no retira ningún nombre de ROLES_BASE. Varios
        // participan en comprobaciones de permisos vigentes —por ejemplo SECRETARIO
        // en ReunionesController del frontend—, de modo que retirarlos permitiría
        // renombrarlos y perder permisos en silencio.
        for (String nombre : List.of("ADMINISTRADOR", "PRESIDENTE", "SECRETARIO", "TESORERO", "SINDICO")) {
            assertTrue(RolService.isBaseRole(nombre),
                nombre + " no debe perder la protección en SCRUM-329.");
        }
    }

    @Test
    void nombreNoBaseSigueSiendoEditable() {
        // Contraparte: la protección no debe extenderse a roles propios.
        assertFalse(RolService.isBaseRole("COORDINADOR"));
        var created = service.create(new RolRequest("COORDINADOR", "Coordina comisiones"));
        assertEquals("COORDINADOR_GENERAL",
            service.update(created.idRol(), new RolRequest("COORDINADOR_GENERAL", "Renombrado")).nombre());
    }

    /** Extrae los nombres del {@code INSERT INTO rol} de {@code schema.sql}. */
    private static Set<String> rolesSembradosEnSchemaSql() throws IOException {
        String sql = Files.readString(localizarSchemaSql(), StandardCharsets.UTF_8);

        Matcher bloque = Pattern
            .compile("INSERT\\s+INTO\\s+rol\\s*\\([^)]*\\)\\s*VALUES(.*?);",
                Pattern.CASE_INSENSITIVE | Pattern.DOTALL)
            .matcher(sql);
        assertTrue(bloque.find(), "No se encontró la sentencia INSERT INTO rol en schema.sql.");

        Matcher nombres = Pattern.compile("\\(\\s*'([^']+)'").matcher(bloque.group(1));
        Set<String> resultado = new LinkedHashSet<>();
        while (nombres.find()) {
            resultado.add(nombres.group(1).trim().toUpperCase(Locale.ROOT));
        }
        return resultado;
    }

    /** {@code schema.sql} vive en la raíz del repositorio, por encima del módulo api. */
    private static Path localizarSchemaSql() {
        Path dir = Path.of("").toAbsolutePath();
        for (int i = 0; i < 4 && dir != null; i++, dir = dir.getParent()) {
            Path candidato = dir.resolve("schema.sql");
            if (Files.isRegularFile(candidato)) {
                return candidato;
            }
        }
        throw new IllegalStateException(
            "No se encontró schema.sql subiendo desde " + Path.of("").toAbsolutePath());
    }

    private static final class MemoryRolDAO extends RolDAO {
        private final List<Rol> roles = new ArrayList<>();

        @Override
        public List<Rol> findAll() {
            return new ArrayList<>(roles);
        }

        @Override
        public Optional<Rol> findById(Integer id) {
            return roles.stream().filter(r -> r.getIdRol().equals(id)).findFirst();
        }

        @Override
        public Optional<Rol> findByNombre(String nombre) {
            if (nombre == null) return Optional.empty();
            String trimmed = nombre.trim();
            return roles.stream()
                .filter(r -> r.getNombre() != null && r.getNombre().equalsIgnoreCase(trimmed))
                .findFirst();
        }

        @Override
        public Rol save(Rol entity) {
            if (entity.getIdRol() == null) {
                entity.setIdRol(roles.size() + 1);
            }
            roles.add(entity);
            return entity;
        }

        @Override
        public Rol update(Rol entity) {
            for (int i = 0; i < roles.size(); i++) {
                if (roles.get(i).getIdRol().equals(entity.getIdRol())) {
                    roles.set(i, entity);
                    return entity;
                }
            }
            return null;
        }

        @Override
        public boolean delete(Integer id) {
            return roles.removeIf(r -> r.getIdRol().equals(id));
        }
    }

    private static final class MemoryUsuarioDAO extends UsuarioDAO {
        private final Map<Integer, Integer> userCounts = new HashMap<>();

        void setUserCount(Integer idRol, int count) {
            userCounts.put(idRol, count);
        }

        @Override
        public int countByRol(Integer idRol) {
            return userCounts.getOrDefault(idRol, 0);
        }
    }
}
