package sv.asociacion.dao.archivo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.Serializable;
import java.lang.reflect.Modifier;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import sv.asociacion.dao.DAO;

/**
 * Verifica que {@link AbstractArchivoDAO} concentre comportamiento de
 * persistencia realmente reutilizable.
 *
 * <p>La prueba usa una entidad propia, {@code Nota}, que no tiene ninguna
 * relacion con el dominio de la asociacion. Si estas pruebas pasan con una
 * entidad inventada, queda demostrado que la abstraccion es generica y no
 * arrastra logica de {@code Miembro} ni de ninguna otra entidad concreta.</p>
 */
class AbstractArchivoDAOTest {

    /** Entidad de prueba, deliberadamente ajena al dominio del sistema. */
    static class Nota implements Serializable {
        private static final long serialVersionUID = 1L;
        private Integer id;
        private String texto;

        Nota(Integer id, String texto) { this.id = id; this.texto = texto; }
        Integer getId() { return id; }
        void setId(Integer id) { this.id = id; }
        String getTexto() { return texto; }
        void setTexto(String texto) { this.texto = texto; }
    }

    /**
     * Implementacion minima: solo resuelve los dos puntos de extension.
     * Todo lo demas lo hereda.
     */
    static class NotaArchivoDAO extends AbstractArchivoDAO<Nota, Integer> {
        NotaArchivoDAO(Path archivo) { super(archivo); }

        @Override
        protected Integer idDe(Nota nota) { return nota.getId(); }

        @Override
        protected void asignarIdentidad(Nota nota, List<Nota> existentes) {
            int siguiente = existentes.stream()
                .map(Nota::getId)
                .filter(java.util.Objects::nonNull)
                .mapToInt(Integer::intValue)
                .max().orElse(0) + 1;
            nota.setId(siguiente);
        }
    }

    @TempDir Path carpeta;
    private NotaArchivoDAO dao;

    @BeforeEach
    void preparar() {
        dao = new NotaArchivoDAO(carpeta.resolve("notas.dat"));
    }

    @Test
    @DisplayName("La clase es abstracta y cumple el contrato DAO<T,ID>")
    void esAbstractaYCumpleElContrato() {
        assertTrue(Modifier.isAbstract(AbstractArchivoDAO.class.getModifiers()),
            "no debe poder instanciarse por si sola: no sabe que entidad guarda");
        assertTrue(DAO.class.isAssignableFrom(AbstractArchivoDAO.class),
            "debe compartir el mismo contrato que los DAO JDBC");
        assertInstanceOf(DAO.class, dao);
    }

    @Test
    @DisplayName("Una implementacion minima solo resuelve los dos puntos de extension")
    void implementacionMinimaReutilizaElComportamientoComun() {
        // NotaArchivoDAO solo declara idDe() y asignarIdentidad().
        long propios = java.util.Arrays.stream(NotaArchivoDAO.class.getDeclaredMethods())
            .filter(m -> !m.isSynthetic())
            .count();
        assertEquals(2, propios,
            "la implementacion concreta no deberia necesitar mas que los dos ganchos");
    }

    @Test
    @DisplayName("Un archivo inexistente se comporta como una lista vacia")
    void archivoInexistenteEsListaVacia() {
        assertFalse(dao.existeArchivo());
        assertEquals(List.of(), dao.findAll());
        assertEquals(0, dao.count());
        assertEquals(Optional.empty(), dao.findById(1));
    }

    @Test
    @DisplayName("save asigna identidad y persiste en el archivo")
    void guardaYAsignaIdentidad() {
        Nota guardada = dao.save(new Nota(null, "Acta de asamblea"));

        assertEquals(1, guardada.getId(), "la implementacion genera el primer id");
        assertTrue(dao.existeArchivo(), "el archivo .dat debe quedar creado");
        assertEquals(1, dao.count());

        dao.save(new Nota(null, "Convocatoria"));
        assertEquals(2, dao.count());
    }

    @Test
    @DisplayName("Lo escrito se recupera con otra instancia del DAO")
    void loEscritoSobreviveAlaInstancia() {
        dao.save(new Nota(null, "Pase de lista"));
        dao.save(new Nota(null, "Quorum alcanzado"));

        NotaArchivoDAO otra = new NotaArchivoDAO(carpeta.resolve("notas.dat"));
        List<Nota> recuperadas = otra.findAll();

        assertEquals(2, recuperadas.size());
        assertEquals("Pase de lista", recuperadas.get(0).getTexto());
        assertEquals("Quorum alcanzado", recuperadas.get(1).getTexto());
    }

    @Test
    @DisplayName("findById localiza por identificador")
    void buscaPorIdentificador() {
        dao.save(new Nota(null, "Primera"));
        Nota segunda = dao.save(new Nota(null, "Segunda"));

        Optional<Nota> encontrada = dao.findById(segunda.getId());
        assertTrue(encontrada.isPresent());
        assertEquals("Segunda", encontrada.get().getTexto());

        assertEquals(Optional.empty(), dao.findById(99));
        assertEquals(Optional.empty(), dao.findById(null));
    }

    @Test
    @DisplayName("update reemplaza el registro existente")
    void actualizaRegistroExistente() {
        Nota nota = dao.save(new Nota(null, "Borrador"));
        nota.setTexto("Version final");

        dao.update(nota);

        assertEquals(1, dao.count(), "actualizar no debe duplicar registros");
        assertEquals("Version final", dao.findById(nota.getId()).orElseThrow().getTexto());
    }

    @Test
    @DisplayName("delete elimina e informa si habia algo que eliminar")
    void eliminaRegistro() {
        Nota nota = dao.save(new Nota(null, "Temporal"));

        assertTrue(dao.delete(nota.getId()));
        assertEquals(0, dao.count());
        assertFalse(dao.delete(nota.getId()), "borrar dos veces devuelve false");
        assertFalse(dao.delete(null));
    }

    @Test
    @DisplayName("Las operaciones invalidas fallan con un mensaje de dominio")
    void operacionesInvalidas() {
        Nota sinId = new Nota(null, "Sin identificador");
        assertThrows(IllegalArgumentException.class, () -> dao.update(sinId),
            "no se puede actualizar algo que nunca se guardo");

        Nota inexistente = new Nota(77, "Fantasma");
        assertThrows(IllegalStateException.class, () -> dao.update(inexistente));

        Nota guardada = dao.save(new Nota(null, "Original"));
        Nota duplicada = new Nota(guardada.getId(), "Duplicada");
        assertThrows(IllegalStateException.class, () -> dao.save(duplicada),
            "no debe admitir dos registros con el mismo id");

        assertThrows(NullPointerException.class, () -> dao.save(null));
    }

    @Test
    @DisplayName("La abstraccion no depende de ninguna entidad del dominio")
    void noDependeDelDominio() {
        // Ni la clase ni su firma mencionan Miembro, Residente ni Persona.
        String firma = AbstractArchivoDAO.class.toGenericString();
        assertFalse(firma.contains("Miembro"));

        boolean mencionaDominio = java.util.Arrays.stream(AbstractArchivoDAO.class.getDeclaredMethods())
            .anyMatch(m -> m.toGenericString().contains("sv.asociacion.domain"));
        assertFalse(mencionaDominio,
            "la abstraccion debe servir para cualquier entidad serializable");

        // La prueba completa funciona con Nota, que no pertenece al dominio.
        assertEquals("Nota", Nota.class.getSimpleName());
    }
}
