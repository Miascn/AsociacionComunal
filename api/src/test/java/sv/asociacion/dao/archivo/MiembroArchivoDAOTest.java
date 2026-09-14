package sv.asociacion.dao.archivo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import sv.asociacion.dao.DAO;
import sv.asociacion.domain.entity.Miembro;
import sv.asociacion.domain.entity.Persona;

/**
 * Verifica que el archivo {@code .dat} sea un origen de datos real para el censo
 * de miembros: lo que se guarda se recupera despues sin intervenir MySQL.
 *
 * <p>Todas las pruebas escriben en un directorio temporal de JUnit, nunca en la
 * carpeta {@code datos/} del proyecto.</p>
 */
class MiembroArchivoDAOTest {

    @TempDir Path carpeta;
    private Path archivo;
    private MiembroArchivoDAO dao;

    private static Miembro nuevoMiembro(String nombres, String apellidos, String dui) {
        Miembro miembro = new Miembro();
        miembro.setNombres(nombres);
        miembro.setApellidos(apellidos);
        miembro.setDui(dui);
        miembro.setTelefono("7000-0000");
        miembro.setFechaIngreso(LocalDate.of(2026, 3, 1));
        miembro.setEstado(Miembro.Estado.ACTIVO);
        return miembro;
    }

    @BeforeEach
    void preparar() {
        archivo = carpeta.resolve("miembros.dat");
        dao = new MiembroArchivoDAO(archivo);
    }

    @Test
    @DisplayName("Cumple el contrato DAO y reutiliza la abstraccion de archivos")
    void cumpleElContrato() {
        assertInstanceOf(DAO.class, dao);
        assertInstanceOf(AbstractArchivoDAO.class, dao);
        assertEquals(archivo, dao.getArchivo());

        // No duplica el CRUD: solo resuelve los dos puntos de extension.
        long propios = java.util.Arrays.stream(MiembroArchivoDAO.class.getDeclaredMethods())
            .filter(m -> !m.isSynthetic() && !java.lang.reflect.Modifier.isStatic(m.getModifiers()))
            .count();
        assertEquals(2, propios, "idDe y asignarIdentidad; el resto se hereda");
    }

    @Test
    @DisplayName("Archivo inexistente: censo vacio, sin error")
    void archivoInexistente() {
        assertFalse(dao.existeArchivo());
        assertEquals(List.of(), dao.findAll());
        assertEquals(0, dao.count());
        assertEquals(Optional.empty(), dao.findById(1));
    }

    @Test
    @DisplayName("save crea el archivo y persiste el miembro")
    void guardaMiembro() {
        Miembro guardado = dao.save(nuevoMiembro("Ana Maria", "Gonzalez Reyes", "01234567-8"));

        assertTrue(Files.exists(archivo), "el archivo .dat debe quedar creado en disco");
        assertNotNull(guardado.getIdMiembro());
        assertEquals(1, dao.count());
    }

    @Test
    @DisplayName("Genera identificadores correlativos a partir del mayor guardado")
    void generaIdentificadores() {
        Miembro primero = dao.save(nuevoMiembro("Ana", "Gonzalez", "01111111-1"));
        Miembro segundo = dao.save(nuevoMiembro("Luis", "Martinez", "02222222-2"));
        Miembro tercero = dao.save(nuevoMiembro("Rosa", "Flores", "03333333-3"));

        assertEquals(1, primero.getIdMiembro());
        assertEquals(2, segundo.getIdMiembro());
        assertEquals(3, tercero.getIdMiembro());

        // Borrar un registro intermedio no altera el correlativo.
        assertTrue(dao.delete(segundo.getIdMiembro()));
        Miembro cuarto = dao.save(nuevoMiembro("Jose", "Ramos", "04444444-4"));
        assertEquals(4, cuarto.getIdMiembro(),
            "el siguiente id sale del mayor guardado, no de la cantidad de registros");
    }

    @Test
    @DisplayName("Limitacion conocida: borrar el id mas alto lo deja disponible de nuevo")
    void borrarElUltimoLiberaElCorrelativo() {
        dao.save(nuevoMiembro("Ana", "Gonzalez", "01111111-1"));
        Miembro ultimo = dao.save(nuevoMiembro("Luis", "Martinez", "02222222-2"));

        assertTrue(dao.delete(ultimo.getIdMiembro()));
        Miembro siguiente = dao.save(nuevoMiembro("Rosa", "Flores", "03333333-3"));

        // Documentado en MiembroArchivoDAO#asignarIdentidad: el correlativo es
        // max+1, por lo que el 2 vuelve a asignarse. Aceptable para un respaldo
        // local de un solo puesto; se deja registrado explicitamente.
        assertEquals(2, siguiente.getIdMiembro());
    }

    @Test
    @DisplayName("Respeta un identificador ya asignado")
    void respetaIdExistente() {
        Miembro conId = nuevoMiembro("Carmen", "Diaz", "05555555-5");
        conId.setIdMiembro(50);

        dao.save(conId);

        assertEquals(50, dao.findById(50).orElseThrow().getIdMiembro());
        Miembro siguiente = dao.save(nuevoMiembro("Mario", "Lopez", "06666666-6"));
        assertEquals(51, siguiente.getIdMiembro());
    }

    @Test
    @DisplayName("findById recupera el miembro con todos sus datos")
    void buscaPorId() {
        Miembro guardado = dao.save(nuevoMiembro("Ana Maria", "Gonzalez Reyes", "01234567-8"));

        Miembro recuperado = dao.findById(guardado.getIdMiembro()).orElseThrow();
        assertEquals("Ana Maria", recuperado.getNombres());
        assertEquals("Gonzalez Reyes", recuperado.getApellidos());
        assertEquals("01234567-8", recuperado.getDui());
        assertEquals(LocalDate.of(2026, 3, 1), recuperado.getFechaIngreso());
        assertEquals(Miembro.Estado.ACTIVO, recuperado.getEstado());

        assertEquals(Optional.empty(), dao.findById(999));
    }

    @Test
    @DisplayName("findAll devuelve todo el censo guardado")
    void listaTodoElCenso() {
        dao.save(nuevoMiembro("Ana", "Gonzalez", "01111111-1"));
        dao.save(nuevoMiembro("Luis", "Martinez", "02222222-2"));
        dao.save(nuevoMiembro("Rosa", "Flores", "03333333-3"));

        List<Miembro> censo = dao.findAll();
        assertEquals(3, censo.size());
        assertEquals(List.of("Ana Gonzalez", "Luis Martinez", "Rosa Flores"),
            censo.stream().map(Miembro::getNombreCompleto).toList());
    }

    @Test
    @DisplayName("update modifica sin duplicar")
    void actualizaMiembro() {
        Miembro miembro = dao.save(nuevoMiembro("Ana", "Gonzalez", "01111111-1"));
        miembro.setTelefono("7555-1234");
        miembro.setEstado(Miembro.Estado.INACTIVO);

        dao.update(miembro);

        assertEquals(1, dao.count(), "actualizar no debe agregar un registro nuevo");
        Miembro recuperado = dao.findById(miembro.getIdMiembro()).orElseThrow();
        assertEquals("7555-1234", recuperado.getTelefono());
        assertEquals(Miembro.Estado.INACTIVO, recuperado.getEstado());
    }

    @Test
    @DisplayName("delete elimina del archivo")
    void eliminaMiembro() {
        Miembro miembro = dao.save(nuevoMiembro("Ana", "Gonzalez", "01111111-1"));

        assertTrue(dao.delete(miembro.getIdMiembro()));
        assertEquals(0, dao.count());
        assertEquals(Optional.empty(), dao.findById(miembro.getIdMiembro()));
        assertFalse(dao.delete(miembro.getIdMiembro()));
    }

    @Test
    @DisplayName("EL ARCHIVO ES ORIGEN DE DATOS: otra instancia recupera el censo sin MySQL")
    void elArchivoEsOrigenDeDatos() {
        // 1. Una instancia guarda el censo.
        MiembroArchivoDAO primera = new MiembroArchivoDAO(archivo);
        primera.save(nuevoMiembro("Ana Maria", "Gonzalez Reyes", "01234567-8"));
        primera.save(nuevoMiembro("Luis Alberto", "Martinez Cruz", "02222222-2"));

        // 2. Esa instancia se descarta por completo.
        primera = null;
        System.gc();

        // 3. Una instancia nueva apunta al mismo archivo.
        MiembroArchivoDAO segunda = new MiembroArchivoDAO(archivo);

        // 4. El censo se recupera del archivo, sin ninguna conexion a base de datos.
        List<Miembro> censo = segunda.findAll();
        assertEquals(2, censo.size());
        assertEquals("Ana Maria Gonzalez Reyes", censo.get(0).getNombreCompleto());
        assertEquals("01234567-8", censo.get(0).getDui());
        assertEquals("Luis Alberto Martinez Cruz", censo.get(1).getNombreCompleto());

        // Y sigue siendo operable: se puede seguir trabajando sobre lo recuperado.
        Miembro agregado = segunda.save(nuevoMiembro("Rosa", "Flores", "03333333-3"));
        assertEquals(3, agregado.getIdMiembro(), "continua el correlativo leido del archivo");
        assertEquals(3, new MiembroArchivoDAO(archivo).count());
    }

    @Test
    @DisplayName("La herencia de Persona sobrevive a la serializacion")
    void conservaLaJerarquiaAlSerializar() {
        Miembro miembro = nuevoMiembro("Ana", "Gonzalez", "01111111-1");
        miembro.setIdVivienda(12);
        dao.save(miembro);

        Miembro recuperado = new MiembroArchivoDAO(archivo).findAll().get(0);

        assertInstanceOf(Persona.class, recuperado);
        assertEquals(12, recuperado.getIdVivienda(), "el estado heredado de Persona tambien se guarda");
        assertEquals("Ana Gonzalez", recuperado.getNombreCompleto());
        assertTrue(recuperado.esAsociado());
    }

    @Test
    @DisplayName("Operaciones invalidas fallan con mensaje de dominio")
    void operacionesInvalidas() {
        Miembro sinGuardar = nuevoMiembro("Fantasma", "Inexistente", "09999999-9");
        assertThrows(IllegalArgumentException.class, () -> dao.update(sinGuardar));

        sinGuardar.setIdMiembro(404);
        assertThrows(IllegalStateException.class, () -> dao.update(sinGuardar));

        Miembro guardado = dao.save(nuevoMiembro("Ana", "Gonzalez", "01111111-1"));
        Miembro duplicado = nuevoMiembro("Otra", "Persona", "08888888-8");
        duplicado.setIdMiembro(guardado.getIdMiembro());
        assertThrows(IllegalStateException.class, () -> dao.save(duplicado));

        assertThrows(NullPointerException.class, () -> dao.save(null));
    }

    @Test
    @DisplayName("La ruta por defecto es configurable y apunta a datos/")
    void rutaPorDefectoConfigurable() {
        Path porDefecto = MiembroArchivoDAO.rutaPorDefecto();
        assertEquals("miembros.dat", porDefecto.getFileName().toString());

        String previo = System.getProperty(MiembroArchivoDAO.CLAVE_CARPETA);
        try {
            System.setProperty(MiembroArchivoDAO.CLAVE_CARPETA, carpeta.toString());
            Path configurada = MiembroArchivoDAO.rutaPorDefecto();
            assertEquals(carpeta.resolve("miembros.dat"), configurada,
                "DATOS_DIR debe permitir mover la carpeta sin tocar codigo");
        } finally {
            if (previo == null) System.clearProperty(MiembroArchivoDAO.CLAVE_CARPETA);
            else System.setProperty(MiembroArchivoDAO.CLAVE_CARPETA, previo);
        }

        // La prueba no crea ningun archivo en la carpeta del proyecto.
        assertFalse(Files.exists(Path.of(MiembroArchivoDAO.CARPETA_POR_DEFECTO, "miembros.dat")));
    }
}
