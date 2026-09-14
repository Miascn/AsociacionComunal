package sv.asociacion.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import sv.asociacion.dao.DAO;
import sv.asociacion.dao.MiembroDAO;
import sv.asociacion.dao.archivo.MiembroArchivoDAO;
import sv.asociacion.domain.dto.MiembroResponse;
import sv.asociacion.domain.entity.Miembro;

/**
 * Evidencia del despacho polimorfico del Avance 2.
 *
 * <p>La misma logica de {@link CensoMiembrosService} se ejecuta contra dos
 * implementaciones distintas del contrato {@code DAO<Miembro,Integer>} y debe
 * producir exactamente el mismo resultado observable:</p>
 *
 * <pre>
 *              CensoMiembrosService
 *                      |
 *              DAO&lt;Miembro,Integer&gt;
 *               /                \
 *        MiembroDAO          MiembroArchivoDAO
 *        JDBC/MySQL                .dat
 * </pre>
 *
 * <p>La rama JDBC se representa con una subclase en memoria de {@code MiembroDAO},
 * la misma estrategia que ya usan las pruebas de servicios del proyecto, para no
 * introducir dependencia de una base de datos externa.</p>
 */
class CensoMiembrosServiceTest {

    // ---------------------------------------------------------------
    // Rama JDBC: subclase en memoria de MiembroDAO, sin base externa.
    // Sigue siendo un DAO<Miembro,Integer> porque MiembroDAO lo implementa.
    // ---------------------------------------------------------------
    static final class MemoryMiembroDAO extends MiembroDAO {
        private final List<Miembro> store = new ArrayList<>();

        @Override
        public Miembro save(Miembro entity) {
            store.add(entity);
            return entity;
        }

        @Override
        public List<Miembro> findAll() {
            return store.stream()
                .sorted(Comparator.comparing(Miembro::getIdMiembro))
                .toList();
        }

        @Override
        public Optional<Miembro> findById(Integer id) {
            return store.stream().filter(m -> id.equals(m.getIdMiembro())).findFirst();
        }
    }

    private static Miembro miembro(int id, String nombres, String apellidos, String dui) {
        Miembro m = new Miembro();
        m.setIdMiembro(id);
        m.setNombres(nombres);
        m.setApellidos(apellidos);
        m.setDui(dui);
        m.setFechaIngreso(LocalDate.of(2026, 2, 10));
        m.setEstado(Miembro.Estado.ACTIVO);
        return m;
    }

    /** Los tres miembros de prueba, identicos para ambas implementaciones. */
    private static List<Miembro> padronDePrueba() {
        return List.of(
            miembro(1, "Ana Maria", "Gonzalez Reyes", "01111111-1"),
            miembro(2, "Luis Alberto", "Martinez Cruz", "02222222-2"),
            miembro(3, "Rosa Elena", "Flores Diaz", "03333333-3")
        );
    }

    /**
     * Entrega las dos implementaciones del contrato, ya cargadas con el mismo
     * padron. El metodo declara {@code DAO<Miembro,Integer>}: quien las reciba no
     * sabe cual es cual.
     */
    static Stream<Arguments> implementacionesDelContrato() throws IOException {
        DAO<Miembro, Integer> jdbc = new MemoryMiembroDAO();
        padronDePrueba().forEach(jdbc::save);

        Path carpeta = Files.createTempDirectory("censo-polimorfismo");
        DAO<Miembro, Integer> archivo = new MiembroArchivoDAO(carpeta.resolve("miembros.dat"));
        padronDePrueba().forEach(archivo::save);

        return Stream.of(
            Arguments.of("JDBC / MySQL", jdbc),
            Arguments.of(".dat / archivo", archivo)
        );
    }

    // ===============================================================
    // La misma prueba, las dos implementaciones
    // ===============================================================

    @ParameterizedTest(name = "findAll con {0}")
    @MethodSource("implementacionesDelContrato")
    @DisplayName("findAll entrega el mismo padron con cualquier implementacion")
    void findAllEsIgualConAmbasImplementaciones(String etiqueta, DAO<Miembro, Integer> origen) {
        CensoMiembrosService censo = new CensoMiembrosService(origen);

        List<MiembroResponse> padron = censo.findAll();

        assertEquals(3, padron.size(), "el padron completo, venga de donde venga");
        assertEquals(List.of("Ana Maria", "Luis Alberto", "Rosa Elena"),
            padron.stream().map(MiembroResponse::nombres).toList());
        assertEquals(List.of("01111111-1", "02222222-2", "03333333-3"),
            padron.stream().map(MiembroResponse::dui).toList());
        assertEquals(List.of("ACTIVO", "ACTIVO", "ACTIVO"),
            padron.stream().map(MiembroResponse::estado).toList());
    }

    @ParameterizedTest(name = "findById con {0}")
    @MethodSource("implementacionesDelContrato")
    @DisplayName("findById devuelve la misma ficha con cualquier implementacion")
    void findByIdEsIgualConAmbasImplementaciones(String etiqueta, DAO<Miembro, Integer> origen) {
        CensoMiembrosService censo = new CensoMiembrosService(origen);

        MiembroResponse ficha = censo.findById(2);

        assertNotNull(ficha);
        assertEquals(2, ficha.id());
        assertEquals("Luis Alberto", ficha.nombres());
        assertEquals("Martinez Cruz", ficha.apellidos());
        assertEquals("02222222-2", ficha.dui());
        assertEquals("2026-02-10", ficha.fechaIngreso());
    }

    @ParameterizedTest(name = "miembro inexistente con {0}")
    @MethodSource("implementacionesDelContrato")
    @DisplayName("Un miembro inexistente devuelve null con cualquier implementacion")
    void miembroInexistenteEsIgualConAmbasImplementaciones(String etiqueta, DAO<Miembro, Integer> origen) {
        CensoMiembrosService censo = new CensoMiembrosService(origen);

        assertNull(censo.findById(999), "mismo comportamiento observable en ambas ramas");
    }

    // ===============================================================
    // Comparacion directa: las dos ramas producen resultados identicos
    // ===============================================================

    @Test
    @DisplayName("Las dos implementaciones producen exactamente el mismo resultado")
    void ambasRamasProducenElMismoResultado() throws IOException {
        DAO<Miembro, Integer> jdbc = new MemoryMiembroDAO();
        padronDePrueba().forEach(jdbc::save);

        Path carpeta = Files.createTempDirectory("censo-comparacion");
        DAO<Miembro, Integer> archivo = new MiembroArchivoDAO(carpeta.resolve("miembros.dat"));
        padronDePrueba().forEach(archivo::save);

        CensoMiembrosService desdeMySql = new CensoMiembrosService(jdbc);
        CensoMiembrosService desdeArchivo = new CensoMiembrosService(archivo);

        assertEquals(desdeMySql.findAll(), desdeArchivo.findAll(),
            "el padron debe ser indistinguible entre una fuente y otra");
        assertEquals(desdeMySql.findById(1), desdeArchivo.findById(1));
        assertEquals(desdeMySql.findById(3), desdeArchivo.findById(3));
        assertEquals(desdeMySql.findById(404), desdeArchivo.findById(404));
    }

    // ===============================================================
    // El servicio depende de la abstraccion, no de la implementacion
    // ===============================================================

    @Test
    @DisplayName("El servicio solo conoce DAO<Miembro,Integer>")
    void elServicioDependeSoloDelContrato() throws NoSuchFieldException {
        Field[] campos = CensoMiembrosService.class.getDeclaredFields();
        assertEquals(1, campos.length, "un unico colaborador");
        assertEquals(DAO.class, campos[0].getType(),
            "el campo debe declararse como el contrato, no como MiembroDAO ni MiembroArchivoDAO");

        assertEquals(DAO.class, CensoMiembrosService.class.getConstructors()[0].getParameterTypes()[0],
            "la implementacion concreta se inyecta desde fuera");
    }

    @Test
    @DisplayName("No hay casts ni instanceof en el servicio")
    void sinCastsNiInstanceof() {
        // Ninguna firma del servicio menciona una implementacion concreta.
        for (Method metodo : CensoMiembrosService.class.getDeclaredMethods()) {
            String firma = metodo.toGenericString();
            assertFalse(firma.contains("MiembroDAO"), "firma acoplada a JDBC: " + firma);
            assertFalse(firma.contains("MiembroArchivoDAO"), "firma acoplada a archivo: " + firma);
        }

        // El servicio no expone forma alguna de averiguar la implementacion.
        boolean exponeElOrigen = java.util.Arrays.stream(CensoMiembrosService.class.getDeclaredMethods())
            .anyMatch(m -> m.getReturnType().equals(DAO.class));
        assertFalse(exponeElOrigen, "el origen concreto no debe filtrarse hacia afuera");
    }

    @Test
    @DisplayName("El origen de datos es obligatorio")
    void origenObligatorio() {
        assertThrows(NullPointerException.class, () -> new CensoMiembrosService(null));
    }

    @Test
    @DisplayName("Ambas implementaciones cumplen realmente el contrato")
    void ambasCumplenElContrato() throws IOException {
        Path carpeta = Files.createTempDirectory("censo-contrato");

        assertTrue(DAO.class.isAssignableFrom(MiembroDAO.class),
            "la rama JDBC cumple el contrato");
        assertTrue(DAO.class.isAssignableFrom(MiembroArchivoDAO.class),
            "la rama de archivo cumple el contrato");

        // Y son clases distintas: el polimorfismo no es aparente.
        assertFalse(MiembroArchivoDAO.class.isAssignableFrom(MiembroDAO.class));
        assertFalse(MiembroDAO.class.isAssignableFrom(MiembroArchivoDAO.class));

        assertNotNull(new MiembroArchivoDAO(carpeta.resolve("x.dat")));
    }
}
