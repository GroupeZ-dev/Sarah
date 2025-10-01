package fr.maxlego08.sarah;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import fr.maxlego08.sarah.database.DatabaseType;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Configuration borrowed from:
 * <a href="https://github.com/lucko/helper/blob/master/helper-sql/src/main/java/me/lucko/helper/sql/plugin/HelperSql.java">...</a>
 */
public class HikariDatabaseConnection extends DatabaseConnection {

    private static final AtomicInteger POOL_COUNTER = new AtomicInteger(0);

    // https://github.com/brettwooldridge/HikariCP/wiki/About-Pool-Sizing
    private static final int MAXIMUM_POOL_SIZE = (Runtime.getRuntime().availableProcessors() * 2) + 1;
    private static final int MINIMUM_IDLE = Math.min(MAXIMUM_POOL_SIZE, 10);

    private static final long MAX_LIFETIME = TimeUnit.MINUTES.toMillis(30);
    private static final long CONNECTION_TIMEOUT = TimeUnit.SECONDS.toMillis(10);
    private static final long LEAK_DETECTION_THRESHOLD = TimeUnit.SECONDS.toMillis(10);

    private HikariDataSource dataSource;

    public HikariDatabaseConnection(DatabaseConfiguration databaseConfiguration) {
        super(databaseConfiguration);
        this.initializeDataSource();
    }

    private void initializeDataSource() {
        HikariConfig config = new HikariConfig();

        config.setPoolName("sarah-" + POOL_COUNTER.getAndIncrement());

        DatabaseType databaseType = databaseConfiguration.getDatabaseType();
        String jdbcPrefix = databaseType == DatabaseType.MARIADB ? "jdbc:mariadb://" : "jdbc:mysql://";
        config.setJdbcUrl(jdbcPrefix + databaseConfiguration.getHost() + ":" + databaseConfiguration.getPort() + "/" + databaseConfiguration.getDatabase() + "?allowMultiQueries=true");
        config.setDriverClassName("com.mysql.jdbc.Driver");

        config.setUsername(databaseConfiguration.getUser());
        config.setPassword(databaseConfiguration.getPassword());

        config.setMaximumPoolSize(MAXIMUM_POOL_SIZE);
        config.setMinimumIdle(MINIMUM_IDLE);

        config.setMaxLifetime(MAX_LIFETIME);
        config.setConnectionTimeout(CONNECTION_TIMEOUT);
        config.setLeakDetectionThreshold(LEAK_DETECTION_THRESHOLD);

        Map<String, String> properties = new HashMap<String, String>() {{
            put("useSSL", "false");

            // Ensure we use utf8 encoding
            put("useUnicode", "true");
            put("characterEncoding", "utf8");

            // https://github.com/brettwooldridge/HikariCP/wiki/MySQL-Configuration
            put("cachePrepStmts", "true");
            put("prepStmtCacheSize", "250");
            put("prepStmtCacheSqlLimit", "2048");
            put("useServerPrepStmts", "true");
            put("useLocalSessionState", "true");
            put("rewriteBatchedStatements", "true");
            put("cacheResultSetMetadata", "true");
            put("cacheServerConfiguration", "true");
            put("elideSetAutoCommits", "true");
            put("maintainTimeStats", "false");
            put("alwaysSendSetIsolation", "false");
            put("cacheCallableStmts", "true");

            // Set the driver level TCP socket timeout
            // See: https://github.com/brettwooldridge/HikariCP/wiki/Rapid-Recovery
            put("socketTimeout", String.valueOf(TimeUnit.SECONDS.toMillis(30)));
        }};

        for (Map.Entry<String, String> property : properties.entrySet()) {
            config.addDataSourceProperty(property.getKey(), property.getValue());
        }

        this.dataSource = new HikariDataSource(config);
    }

    @Override
    public Connection connectToDatabase() throws SQLException {
        return dataSource.getConnection();
    }

    @Override
    public void connect() {
        // Connection is managed by HikariCP, no need to implement this.
    }

    @Override
    public void disconnect() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }

    @Override
    public boolean isValid() {
        return dataSource != null && dataSource.isRunning();
    }

    @Override
    public Connection getConnection() {
        try {
            return dataSource.getConnection();
        } catch (SQLException exception) {
            exception.printStackTrace();
            return null;
        }
    }

    @Override
    protected boolean isConnected(Connection connection) {
        try {
            return connection != null && connection.isValid(1);
        } catch (SQLException exception) {
            return false;
        }
    }
}
