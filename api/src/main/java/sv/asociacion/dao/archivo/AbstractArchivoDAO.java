package sv.asociacion.dao.archivo;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import sv.asociacion.dao.DAO;

/**
 * Base para los DAO que guardan sus registros en un archivo {@code .dat}
 * mediante serializacion de objetos.
 *
 * <p>Cumple el mismo contrato {@link DAO} que los DAO JDBC del sistema, de modo
 * que un servicio puede recibir cualquiera de las dos familias sin enterarse de
 * donde vienen los datos. Esa es la razon de existir de esta clase: la
 * asociacion opera en un local sin conexion garantizada, y el cliente de
 * escritorio necesita poder seguir trabajando cuando la API no responde.</p>
 *
 * <p>Aqui se concentra todo lo que es igual para cualquier entidad guardada en
 * archivo: abrir y crear el archivo, leer la lista completa, escribirla de
 * vuelta, tratar el archivo vacio como una lista vacia y traducir los errores de
 * entrada y salida a un error de dominio. Las cinco operaciones del contrato se
 * resuelven sobre esas primitivas.</p>
 *
 * <p>Lo unico que esta clase no puede saber es como cada entidad expone su
 * identificador y como se le asigna uno nuevo al insertarla. Esos dos puntos
 * quedan como metodos abstractos.</p>
 *
 * @param <T>  tipo de entidad; debe ser serializable para poder escribirse
 * @param <ID> tipo del identificador de la entidad
 */
public abstract class AbstractArchivoDAO<T extends Serializable, ID> implements DAO<T, ID> {

    private final Path archivo;

    protected AbstractArchivoDAO(Path archivo) {
        this.archivo = Objects.requireNonNull(archivo, "La ruta del archivo es obligatoria.");
    }

    // ---------------------------------------------------------------
    // Puntos de extension: lo unico que cada implementacion debe aportar
    // ---------------------------------------------------------------

    /** Identificador de la entidad, o {@code null} si todavia no tiene uno. */
    protected abstract ID idDe(T entidad);

    /**
     * Asigna un identificador nuevo a una entidad que aun no lo tiene.
     * Cada implementacion decide como generarlo, porque depende del tipo de ID.
     *
     * @param entidad   entidad que se va a insertar
     * @param existentes registros ya guardados, para poder derivar el siguiente id
     */
    protected abstract void asignarIdentidad(T entidad, List<T> existentes);

    // ---------------------------------------------------------------
    // Contrato DAO<T,ID>, resuelto de forma generica
    // ---------------------------------------------------------------

    @Override
    public List<T> findAll() {
        return leerTodo();
    }

    @Override
    public Optional<T> findById(ID id) {
        if (id == null) return Optional.empty();
        return leerTodo().stream()
            .filter(entidad -> id.equals(idDe(entidad)))
            .findFirst();
    }

    @Override
    public T save(T entidad) {
        Objects.requireNonNull(entidad, "La entidad es obligatoria.");
        List<T> registros = leerTodo();
        if (idDe(entidad) == null) {
            asignarIdentidad(entidad, registros);
        }
        ID id = idDe(entidad);
        if (id == null) {
            throw new IllegalStateException("La entidad no tiene identificador despues de asignarlo.");
        }
        if (indiceDe(registros, id) >= 0) {
            throw new IllegalStateException("Ya existe un registro con el identificador " + id + ".");
        }
        registros.add(entidad);
        escribirTodo(registros);
        return entidad;
    }

    @Override
    public T update(T entidad) {
        Objects.requireNonNull(entidad, "La entidad es obligatoria.");
        ID id = idDe(entidad);
        if (id == null) {
            throw new IllegalArgumentException("No se puede actualizar una entidad sin identificador.");
        }
        List<T> registros = leerTodo();
        int posicion = indiceDe(registros, id);
        if (posicion < 0) {
            throw new IllegalStateException("No existe un registro con el identificador " + id + ".");
        }
        registros.set(posicion, entidad);
        escribirTodo(registros);
        return entidad;
    }

    @Override
    public boolean delete(ID id) {
        if (id == null) return false;
        List<T> registros = leerTodo();
        int posicion = indiceDe(registros, id);
        if (posicion < 0) return false;
        registros.remove(posicion);
        escribirTodo(registros);
        return true;
    }

    // ---------------------------------------------------------------
    // Comportamiento comun que las implementaciones concretas reutilizan
    // ---------------------------------------------------------------

    /**
     * Lee el archivo completo. Un archivo inexistente o vacio se trata como una
     * lista vacia: en la primera ejecucion todavia no hay nada guardado.
     */
    protected final List<T> leerTodo() {
        if (!Files.exists(archivo)) return new ArrayList<>();
        try (ObjectInputStream entrada = new ObjectInputStream(Files.newInputStream(archivo))) {
            Object contenido = entrada.readObject();
            if (!(contenido instanceof List<?> lista)) {
                throw new IllegalStateException("El archivo " + archivo.getFileName() + " no contiene una lista de registros.");
            }
            @SuppressWarnings("unchecked")
            List<T> registros = new ArrayList<>((List<T>) lista);
            return registros;
        } catch (EOFException vacio) {
            return new ArrayList<>();
        } catch (IOException | ClassNotFoundException e) {
            throw new IllegalStateException("No fue posible leer el archivo " + archivo.getFileName() + ".", e);
        }
    }

    /** Escribe la lista completa, creando el directorio si hace falta. */
    protected final void escribirTodo(List<T> registros) {
        Objects.requireNonNull(registros, "La lista de registros es obligatoria.");
        try {
            Path carpeta = archivo.getParent();
            if (carpeta != null) Files.createDirectories(carpeta);
            try (ObjectOutputStream salida = new ObjectOutputStream(Files.newOutputStream(archivo))) {
                salida.writeObject(new ArrayList<>(registros));
            }
        } catch (IOException e) {
            throw new IllegalStateException("No fue posible escribir el archivo " + archivo.getFileName() + ".", e);
        }
    }

    /** Ruta del archivo que respalda este DAO. */
    public final Path getArchivo() { return archivo; }

    /** Indica si el archivo ya fue creado. */
    public final boolean existeArchivo() { return Files.exists(archivo); }

    /** Cantidad de registros guardados. */
    public final long count() { return leerTodo().size(); }

    private int indiceDe(List<T> registros, ID id) {
        for (int i = 0; i < registros.size(); i++) {
            if (id.equals(idDe(registros.get(i)))) return i;
        }
        return -1;
    }
}
