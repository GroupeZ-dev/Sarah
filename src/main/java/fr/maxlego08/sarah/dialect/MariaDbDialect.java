package fr.maxlego08.sarah.dialect;

import fr.maxlego08.sarah.DatabaseConfiguration;
import fr.maxlego08.sarah.database.DatabaseType;

import java.util.Map;

public class MariaDbDialect extends MySqlDialect {

    @Override
    public DatabaseType getType() {
        return DatabaseType.MARIADB;
    }

    @Override
    public String jdbcUrl(DatabaseConfiguration configuration) {
        return "jdbc:mariadb://" + configuration.getHost() + ":" + configuration.getPort() + "/" + configuration.getDatabase();
    }

    @Override
    public String driverClassName() {
        return "org.mariadb.jdbc.Driver";
    }

    @Override
    public Map<String, String> dataSourceProperties() {
        return commonProperties();
    }
}
