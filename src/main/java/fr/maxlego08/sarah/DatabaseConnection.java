package fr.maxlego08.sarah;

import fr.maxlego08.sarah.database.DatabaseType;
import fr.maxlego08.sarah.exceptions.DatabaseException;
import fr.maxlego08.sarah.logger.Logger;
import fr.maxlego08.sarah.transaction.Transaction;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Represents a connection to a MySQL database.
 * This class handles establishing and managing the connection to the database.
 */
public abstract class DatabaseConnection {

    protected final DatabaseConfiguration databaseConfiguration;
    protected final Logger logger;
    protected Connection connection;

    public DatabaseConnection(DatabaseConfiguration databaseConfiguration, Logger logger) {
        this.databaseConfiguration = databaseConfiguration;
        this.logger = logger;
    }

    /**
     * Gets the DatabaseConfiguration instance associated with this connection.
     *
     * @return The DatabaseConfiguration instance.
     */
    public DatabaseConfiguration getDatabaseConfiguration() {
        return databaseConfiguration;
    }

    /**
     * Checks if the connection to the database is valid.
     *
     * @return true if the connection is valid, false otherwise.
     */
    public boolean isValid() {

        DatabaseType databaseType = this.databaseConfiguration.getDatabaseType();

        try {
            if (databaseType == DatabaseType.MARIADB) {
                Class.forName("org.mariadb.jdbc.Driver");
            } else {
                Class.forName("com.mysql.cj.jdbc.Driver");
            }
        } catch (Exception ignored) {
        }

        if (!isConnected(connection)) {
            try (Connection tempConnection = this.connectToDatabase()) {
                return isConnected(tempConnection);
            } catch (Exception exception) {
                this.logger.info("Failed to validate database connection: " + exception.getMessage());
                return false;
            }
        }

        return true;
    }

    /**
     * Checks if the given database connection is connected and valid.
     *
     * @param connection The database connection to check.
     * @return true if the connection is valid, false otherwise.
     */
    protected boolean isConnected(Connection connection) {
        if (connection == null) {
            return false;
        }

        try {
            return !connection.isClosed() && connection.isValid(1);
        } catch (Exception exception) {
            return false;
        }
    }

    /**
     * Disconnects from the database.
     */
    public void disconnect() {
        if (isConnected(connection)) {
            try {
                connection.close();
            } catch (SQLException exception) {
                this.logger.info("Failed to disconnect from database: " + exception.getMessage());
            }
        }
    }

    /**
     * Establishes a connection to the database.
     */
    public void connect() {
        if (!isConnected(connection)) {
            try {
                connection = this.connectToDatabase();
            } catch (Exception exception) {
                this.logger.info("Failed to connect to database: " + exception.getMessage());
                throw new DatabaseException("connect", exception);
            }
        }
    }

    public abstract Connection connectToDatabase() throws Exception;

    /**
     * Gets the connection to the database.
     * If the connection is not established, it attempts to connect first.
     *
     * @return The database connection.
     */
    public Connection getConnection() {
        connect();
        return connection;
    }

    /**
     * Begins a new database transaction.
     * Use try-with-resources to ensure proper cleanup:
     * <pre>
     * try (Transaction tx = connection.beginTransaction()) {
     *     // Execute operations
     *     tx.commit();
     * } // Automatically rolls back if not committed
     * </pre>
     *
     * @return a new Transaction instance
     * @throws DatabaseException if the transaction cannot be started
     */
    public Transaction beginTransaction() {
        try {
            return new Transaction(getConnection());
        } catch (SQLException exception) {
            this.logger.info("Failed to begin transaction: " + exception.getMessage());
            throw new DatabaseException("begin-transaction", exception);
        }
    }
}
