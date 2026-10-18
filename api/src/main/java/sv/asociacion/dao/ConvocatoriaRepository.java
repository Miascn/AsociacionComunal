package sv.asociacion.dao;

import java.util.List;

/**
 * Persistencia que necesita la convocatoria concurrente a reuniones.
 *
 * <p>El servicio concurrente depende de esta interfaz y no de una clase JDBC
 * concreta: en produccion se usa {@link JdbcConvocatoriaRepository} sobre el
 * DataSource con pool; en las pruebas, un repositorio en memoria. Asi la logica de
 * hilos, lotes y errores se prueba sin MySQL.</p>
 *
 * <p>Las implementaciones deben ser seguras para usarse desde varios hilos a la
 * vez: cada llamada a {@link #convocarLote} se ejecuta en un hilo distinto.</p>
 */
public interface ConvocatoriaRepository {

    /** Identificadores de todos los miembros en estado ACTIVO. */
    List<Integer> idsMiembrosActivos();

    /**
     * Inscribe en la reunion a los miembros del lote que esten activos y que aun no
     * esten convocados. Debe ser idempotente: repetir la llamada no duplica
     * registros, porque la unicidad (reunion, miembro) la garantiza la base de datos.
     *
     * @return cuantos registros nuevos se crearon
     */
    int convocarLote(int idReunion, List<Integer> idsMiembros);
}
