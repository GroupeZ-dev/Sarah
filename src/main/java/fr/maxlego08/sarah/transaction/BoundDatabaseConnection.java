package fr.maxlego08.sarah.transaction;

import fr.maxlego08.sarah.DatabaseConfiguration;
import fr.maxlego08.sarah.DatabaseConnection;
import fr.maxlego08.sarah.logger.Logger;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.sql.Connection;

/**
 * A {@link DatabaseConnection} that always hands out the same JDBC connection, the one a
 * transaction runs on.
 *
 * <p>Every request closes the connection it borrowed when it is done. Here that would end the
 * transaction after its first statement, so the connection is wrapped in a proxy that ignores
 * {@code close()}. The {@link Transaction} releases the real connection.</p>
 */
public class BoundDatabaseConnection extends DatabaseConnection {

    private final Connection boundConnection;

    public BoundDatabaseConnection(DatabaseConfiguration databaseConfiguration, Logger logger, Connection connection) {
        super(databaseConfiguration, logger);
        this.boundConnection = (Connection) Proxy.newProxyInstance(
                Connection.class.getClassLoader(),
                new Class<?>[]{Connection.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("close") && method.getParameterCount() == 0) {
                        return null;
                    }
                    try {
                        return method.invoke(connection, args);
                    } catch (InvocationTargetException exception) {
                        throw exception.getCause();
                    }
                });
    }

    @Override
    public Connection connectToDatabase() {
        return this.boundConnection;
    }

    @Override
    public Connection getConnection() {
        return this.boundConnection;
    }

    @Override
    public void connect() {
    }

    @Override
    public void disconnect() {
    }

    @Override
    public boolean isValid() {
        return true;
    }
}
