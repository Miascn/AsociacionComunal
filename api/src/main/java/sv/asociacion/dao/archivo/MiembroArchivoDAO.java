package sv.asociacion.dao.archivo;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import sv.asociacion.config.AppConfig;
import sv.asociacion.domain.entity.Miembro;

/**
 * Persistencia del censo de miembros en un archivo {@code miembros.dat}.
 *
 * <p>Es una implementacion alternativa de {@link sv.asociacion.dao.DAO}, al mismo
 * nivel que {@link sv.asociacion.dao.MiembroDAO}, que guarda contra MySQL. Ambas
 * cumplen el mismo contrato, de modo que quien las use no necesita saber cual
 * tiene detras.</p>
 *
 * <p>El archivo es un <b>origen de datos por si solo</b>: lo que se guarda aqui
 * se recupera despues sin intervenir la base de datos. Esta clase no conoce
 * MySQL, no sincroniza con el y no sustituye a {@code MiembroDAO}.</p>
 *
 * <p>Todo el CRUD lo hereda de {@link AbstractArchivoDAO}. Aqui solo se resuelve
 * lo propio del miembro: de donde se lee su identificador y como se genera uno
 * nuevo.</p>
 */
public class MiembroArchivoDAO extends AbstractArchivoDAO<Miembro, Integer> {

    /** Nombre del archivo dentro de la carpeta de datos. */
    public static final String NOMBRE_ARCHIVO = "miembros.dat";

    /** Carpeta usada cuando no se configura ninguna otra. */
    public static final String CARPETA_POR_DEFECTO = "datos";

    /** Variable de entorno o propiedad del sistema que permite mover la carpeta. */
    public static final String CLAVE_CARPETA = "DATOS_DIR";

    /**
     * Ruta explicita. Es el constructor que deberian usar las pruebas y
     * cualquier punto que necesite controlar donde queda el archivo.
     */
    public MiembroArchivoDAO(Path archivo) {
        super(archivo);
    }

    /** Ubicacion por defecto del archivo de miembros. */
    public MiembroArchivoDAO() {
        this(rutaPorDefecto());
    }

    /**
     * Resuelve la carpeta de datos siguiendo la misma convencion que el resto del
     * proyecto: {@link AppConfig#setting(String)} consulta primero la variable de
     * entorno y despues la propiedad del sistema. Si no hay ninguna configurada
     * se usa {@code datos/} junto al directorio de trabajo.
     */
    public static Path rutaPorDefecto() {
        String configurada = AppConfig.setting(CLAVE_CARPETA);
        Path carpeta = (configurada == null || configurada.isBlank())
            ? Path.of(CARPETA_POR_DEFECTO)
            : Path.of(configurada.trim());
        return carpeta.resolve(NOMBRE_ARCHIVO);
    }

    @Override
    protected Integer idDe(Miembro miembro) {
        return miembro.getIdMiembro();
    }

    /**
     * El siguiente correlativo es el mayor identificador guardado mas uno.
     *
     * <p>Limitacion conocida y aceptada en esta fase: si se elimina el registro
     * con el identificador mas alto, ese numero vuelve a quedar disponible. Para
     * un respaldo local de un solo puesto de trabajo es suficiente; evitarlo
     * exigiria guardar un contador aparte dentro del archivo, complejidad que
     * este alcance no justifica.</p>
     */
    @Override
    protected void asignarIdentidad(Miembro miembro, List<Miembro> existentes) {
        int siguiente = existentes.stream()
            .map(Miembro::getIdMiembro)
            .filter(Objects::nonNull)
            .mapToInt(Integer::intValue)
            .max()
            .orElse(0) + 1;
        miembro.setIdMiembro(siguiente);
    }
}
