package fr.maxlego08.sarah;

import fr.maxlego08.sarah.logger.Logger;

import java.sql.Connection;

/**
 * Unpooled MySQL connection. Prefer {@link HikariDatabaseConnection} on a server.
 */
public class MySqlConnection extends DatabaseConnection {

    public MySqlConnection(DatabaseConfiguration databaseConfiguration, Logger logger) {
        super(databaseConfiguration, logger);
    }

    @Override
    public Connection connectToDatabase() throws Exception {
        return this.openDriverConnection();
    }
}
