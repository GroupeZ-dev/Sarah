package fr.maxlego08.sarah;

import fr.maxlego08.sarah.logger.Logger;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Properties;

/**
 * Unpooled PostgreSQL connection. Prefer {@link HikariDatabaseConnection} with
 * {@code DatabaseType.POSTGRESQL} on a server.
 */
public class PostgreSqlConnection extends DatabaseConnection {

    public PostgreSqlConnection(DatabaseConfiguration databaseConfiguration, Logger logger) {
        super(databaseConfiguration, logger);
    }

    @Override
    public Connection connectToDatabase() throws Exception {
        Properties properties = new Properties();
        properties.setProperty("user", databaseConfiguration.getUser());
        properties.setProperty("password", databaseConfiguration.getPassword());
        return DriverManager.getConnection(databaseConfiguration.getDialect().jdbcUrl(databaseConfiguration), properties);
    }
}
