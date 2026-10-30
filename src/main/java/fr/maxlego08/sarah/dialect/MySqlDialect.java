package fr.maxlego08.sarah.dialect;

import fr.maxlego08.sarah.DatabaseConfiguration;
import fr.maxlego08.sarah.database.DatabaseType;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class MySqlDialect extends AbstractDialect {

    @Override
    public DatabaseType getType() {
        return DatabaseType.MYSQL;
    }

    @Override
    public String autoIncrementColumn(String columnType) {
        return columnType + " AUTO_INCREMENT";
    }

    @Override
    public String tableOptions() {
        return " ENGINE=InnoDB DEFAULT CHARSET=utf8mb4";
    }

    @Override
    public String updatedAtDefault() {
        return "CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP";
    }

    @Override
    public boolean usesOnConflict() {
        return false;
    }

    /**
     * MySQL and MariaDB refuse {@code OFFSET} without {@code LIMIT}; the documented workaround is
     * the largest unsigned BIGINT.
     */
    @Override
    public String limitClause(Integer limit, Integer offset) {
        if (limit == null && offset != null) {
            return " LIMIT 18446744073709551615 OFFSET " + offset;
        }
        return super.limitClause(limit, offset);
    }

    @Override
    public String jdbcUrl(DatabaseConfiguration configuration) {
        return "jdbc:mysql://" + configuration.getHost() + ":" + configuration.getPort() + "/" + configuration.getDatabase() + "?allowMultiQueries=true";
    }

    @Override
    public String driverClassName() {
        return "com.mysql.cj.jdbc.Driver";
    }

    @Override
    public Map<String, String> dataSourceProperties() {
        Map<String, String> properties = commonProperties();
        properties.put("cachePrepStmts", "true");
        properties.put("prepStmtCacheSize", "250");
        properties.put("prepStmtCacheSqlLimit", "2048");
        properties.put("useServerPrepStmts", "true");
        properties.put("useLocalSessionState", "true");
        properties.put("rewriteBatchedStatements", "true");
        properties.put("cacheResultSetMetadata", "true");
        properties.put("cacheServerConfiguration", "true");
        properties.put("elideSetAutoCommits", "true");
        properties.put("maintainTimeStats", "false");
        properties.put("alwaysSendSetIsolation", "false");
        properties.put("cacheCallableStmts", "true");
        return properties;
    }

    protected Map<String, String> commonProperties() {
        Map<String, String> properties = new HashMap<>();
        properties.put("useSSL", "false");
        properties.put("useUnicode", "true");
        properties.put("characterEncoding", "utf8");
        properties.put("socketTimeout", String.valueOf(TimeUnit.SECONDS.toMillis(30)));
        return properties;
    }
}
