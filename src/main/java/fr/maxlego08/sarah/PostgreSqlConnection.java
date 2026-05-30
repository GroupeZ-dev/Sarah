package fr.maxlego08.sarah;

import fr.maxlego08.sarah.dialect.SqlDialects;
import fr.maxlego08.sarah.logger.Logger;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Properties;

public class PostgreSqlConnection extends DatabaseConnection {

    public PostgreSqlConnection(DatabaseConfiguration databaseConfiguration, Logger logger) {
        super(databaseConfiguration, logger);
    }

    @Override
    public Connection connectToDatabase() throws Exception {
        Properties properties = new Properties();
        properties.setProperty("user", databaseConfiguration.getUser());
        properties.setProperty("password", databaseConfiguration.getPassword());
        String url = SqlDialects.from(databaseConfiguration.getDatabaseType()).jdbcUrl(databaseConfiguration);
        return DriverManager.getConnection(url, properties);
    }
}
