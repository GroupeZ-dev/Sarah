package fr.maxlego08.sarah;

import fr.maxlego08.sarah.logger.Logger;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Properties;

public class MySqlConnection extends DatabaseConnection {

    public MySqlConnection(DatabaseConfiguration databaseConfiguration, Logger logger) {
        super(databaseConfiguration, logger);
    }

    @Override
    public Connection connectToDatabase() throws Exception {
        Properties properties = new Properties();
        properties.setProperty("useSSL", "false");
        properties.setProperty("user", databaseConfiguration.getUser());
        properties.setProperty("password", databaseConfiguration.getPassword());
        return DriverManager.getConnection("jdbc:mysql://" + databaseConfiguration.getHost() + ":" + databaseConfiguration.getPort() + "/" + databaseConfiguration.getDatabase() + "?allowMultiQueries=true", properties);
    }
}
