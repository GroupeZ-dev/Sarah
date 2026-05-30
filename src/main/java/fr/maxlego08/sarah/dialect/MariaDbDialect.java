package fr.maxlego08.sarah.dialect;

import fr.maxlego08.sarah.DatabaseConfiguration;

public class MariaDbDialect extends MySqlDialect {

    @Override
    public String driverClassName() {
        return "org.mariadb.jdbc.Driver";
    }

    @Override
    public String jdbcUrl(DatabaseConfiguration configuration) {
        return "jdbc:mariadb://" + configuration.getHost() + ":" + configuration.getPort() + "/" + configuration.getDatabase() + "?allowMultiQueries=true";
    }
}
