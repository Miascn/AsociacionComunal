package sv.asociacion.config;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.Statement;
import java.util.concurrent.atomic.AtomicInteger;
import javax.sql.DataSource;

/**
 * Fuente JDBC falsa para probar el pool sin MySQL.
 *
 * <p>Cada {@code getConnection()} representa una conexion fisica nueva. Los
 * contadores permiten verificar lo que el pool promete: que reutiliza conexiones
 * fisicas, que no supera su maximo y que deshace transacciones abiertas al
 * recibir una conexion de vuelta.</p>
 */
final class FakeJdbc {

    final AtomicInteger fisicasCreadas = new AtomicInteger();
    final AtomicInteger fisicasCerradas = new AtomicInteger();
    final AtomicInteger rollbacks = new AtomicInteger();

    DataSource dataSource() {
        InvocationHandler handler = (proxy, method, args) -> switch (method.getName()) {
            case "getConnection" -> nuevaConexionFisica();
            case "isWrapperFor" -> false;
            case "getLoginTimeout" -> 0;
            case "hashCode" -> System.identityHashCode(proxy);
            case "equals" -> proxy == args[0];
            case "toString" -> "FakeDataSource";
            default -> null;
        };
        return (DataSource) Proxy.newProxyInstance(
            FakeJdbc.class.getClassLoader(), new Class<?>[]{DataSource.class}, handler);
    }

    private Connection nuevaConexionFisica() {
        fisicasCreadas.incrementAndGet();
        boolean[] cerrada = {false};
        boolean[] autoCommit = {true};

        InvocationHandler handler = (proxy, method, args) -> switch (method.getName()) {
            case "close" -> {
                if (!cerrada[0]) fisicasCerradas.incrementAndGet();
                cerrada[0] = true;
                yield null;
            }
            case "isClosed" -> cerrada[0];
            case "isValid" -> !cerrada[0];
            case "getAutoCommit" -> autoCommit[0];
            case "setAutoCommit" -> {
                autoCommit[0] = (Boolean) args[0];
                yield null;
            }
            case "rollback" -> {
                rollbacks.incrementAndGet();
                yield null;
            }
            case "createStatement", "prepareStatement" -> statement();
            case "getTransactionIsolation" -> Connection.TRANSACTION_REPEATABLE_READ;
            case "isReadOnly", "isWrapperFor" -> false;
            case "getCatalog" -> "asociacion_test";
            case "hashCode" -> System.identityHashCode(proxy);
            case "equals" -> proxy == args[0];
            case "toString" -> "FakeConnection";
            default -> null;
        };
        return (Connection) Proxy.newProxyInstance(
            FakeJdbc.class.getClassLoader(), new Class<?>[]{Connection.class}, handler);
    }

    private static Statement statement() {
        InvocationHandler handler = (proxy, method, args) -> switch (method.getName()) {
            case "execute" -> true;
            case "isClosed", "isWrapperFor" -> false;
            case "hashCode" -> System.identityHashCode(proxy);
            case "equals" -> proxy == args[0];
            default -> null;
        };
        return (Statement) Proxy.newProxyInstance(
            FakeJdbc.class.getClassLoader(), new Class<?>[]{java.sql.PreparedStatement.class}, handler);
    }
}
