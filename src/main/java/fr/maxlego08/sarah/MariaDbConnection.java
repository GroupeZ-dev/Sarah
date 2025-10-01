package fr.maxlego08.sarah;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Properties;

/**
 * Represents a connection to a MariaDB database.
 */
public class MariaDbConnection extends DatabaseConnection {

    public MariaDbConnection(DatabaseConfiguration databaseConfiguration) {
        super(databaseConfiguration);
    }

    @Override
    public Connection connectToDatabase() throws Exception {
        Properties properties = new Properties();
        properties.setProperty("useSSL", "false");
        properties.setProperty("user", databaseConfiguration.getUser());
        properties.setProperty("password", databaseConfiguration.getPassword());
        return DriverManager.getConnection("jdbc:mariadb://" + databaseConfiguration.getHost() + ":" + databaseConfiguration.getPort() + "/" + databaseConfiguration.getDatabase() + "?allowMultiQueries=true", properties);
    }
}
