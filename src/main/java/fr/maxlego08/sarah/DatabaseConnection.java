package fr.maxlego08.sarah;

import fr.maxlego08.sarah.dialect.Dialect;
import fr.maxlego08.sarah.exceptions.DatabaseException;
import fr.maxlego08.sarah.logger.Logger;
import fr.maxlego08.sarah.transaction.Transaction;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Represents a connection to a database.
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

        try {
            Class.forName(this.databaseConfiguration.getDialect().driverClassName());
        } catch (Exception ignored) {
        }

        if (!isConnected(connection)) {
            try (Connection tempConnection = this.connectToDatabase()) {
                return isConnected(tempConnection);
            } catch (Exception exception) {
                this.logger.error("Failed to validate database connection: " + exception.getMessage());
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
                this.logger.error("Failed to disconnect from database: " + exception.getMessage());
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
                this.logger.error("Failed to connect to database: " + exception.getMessage());
                throw new DatabaseException("connect", exception);
            }
        }
    }

    public abstract Connection connectToDatabase() throws Exception;

    /**
     * Opens a connection with {@link DriverManager}, using the dialect's URL and connection
     * properties, then the properties of the configuration.
     */
    protected Connection openDriverConnection() throws SQLException {
        Dialect dialect = this.databaseConfiguration.getDialect();
        try {
            // Plugin class loaders do not always expose the driver to DriverManager's service loader
            Class.forName(dialect.driverClassName());
        } catch (ClassNotFoundException ignored) {
        }

        Properties properties = new Properties();
        properties.putAll(dialect.connectionProperties());
        properties.putAll(this.databaseConfiguration.getProperties());
        properties.setProperty("user", this.databaseConfiguration.getUser());
        properties.setProperty("password", this.databaseConfiguration.getPassword());
        return DriverManager.getConnection(dialect.jdbcUrl(this.databaseConfiguration), properties);
    }

    /**
     * Opens a new connection to the database. Every request closes the connection it gets, so
     * each call returns its own connection rather than one shared between requests and threads.
     *
     * @return a new database connection, which the caller must close
     * @throws DatabaseException if the connection cannot be opened
     */
    public Connection getConnection() {
        try {
            return this.connectToDatabase();
        } catch (Exception exception) {
            this.logger.error("Failed to connect to database: " + exception.getMessage());
            throw new DatabaseException("connect", exception);
        }
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
            this.logger.error("Failed to begin transaction: " + exception.getMessage());
            throw new DatabaseException("begin-transaction", exception);
        }
    }
}
