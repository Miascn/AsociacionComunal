package sv.asociacion.dao;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Convierte la fila actual de un {@link ResultSet} en un objeto del dominio.
 *
 * <p>Es una interfaz funcional generica: cada DAO aporta solo "como se lee una
 * fila" (normalmente una referencia a metodo como {@code this::mapear}) y
 * {@link AbstractJdbcDAO} se encarga de todo lo demas.</p>
 *
 * @param <T> tipo que se construye a partir de la fila
 */
@FunctionalInterface
public interface RowMapper<T> {
    T mapear(ResultSet rs) throws SQLException;
}
