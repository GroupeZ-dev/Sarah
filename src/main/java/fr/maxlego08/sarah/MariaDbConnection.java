package fr.maxlego08.sarah;

import fr.maxlego08.sarah.logger.Logger;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Unpooled MariaDB connection. Prefer {@link HikariDatabaseConnection} on a server.
 */
public class MariaDbConnection extends DatabaseConnection {

    public MariaDbConnection(DatabaseConfiguration databaseConfiguration, Logger logger) {
        super(databaseConfiguration, logger);
    }

    @Override
    public Connection connectToDatabase() throws SQLException {
        return this.openDriverConnection();
    }
}
