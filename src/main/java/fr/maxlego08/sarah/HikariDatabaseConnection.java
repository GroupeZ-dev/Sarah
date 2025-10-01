package fr.maxlego08.sarah;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import fr.maxlego08.sarah.database.DatabaseType;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

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

        // URL + Driver
        final String jdbcUrl;
        if (databaseType == DatabaseType.MARIADB) {
            jdbcUrl = "jdbc:mariadb://" + databaseConfiguration.getHost() + ":" + databaseConfiguration.getPort() + "/" + databaseConfiguration.getDatabase() + "?allowMultiQueries=true";
            config.setDriverClassName("org.mariadb.jdbc.Driver");
        } else {
            jdbcUrl = "jdbc:mysql://" + databaseConfiguration.getHost() + ":" + databaseConfiguration.getPort() + "/" + databaseConfiguration.getDatabase() + "?allowMultiQueries=true";
            config.setDriverClassName("com.mysql.cj.jdbc.Driver");
        }
        config.setJdbcUrl(jdbcUrl);

        // Auth
        config.setUsername(databaseConfiguration.getUser());
        config.setPassword(databaseConfiguration.getPassword());

        // Pooling
        config.setMaximumPoolSize(MAXIMUM_POOL_SIZE);
        config.setMinimumIdle(MINIMUM_IDLE);
        config.setMaxLifetime(MAX_LIFETIME);
        config.setConnectionTimeout(CONNECTION_TIMEOUT);
        config.setLeakDetectionThreshold(LEAK_DETECTION_THRESHOLD);

        Map<String, String> commonProps = new HashMap<>();
        commonProps.put("useSSL", "false");
        commonProps.put("useUnicode", "true");
        commonProps.put("characterEncoding", "utf8");
        commonProps.put("socketTimeout", String.valueOf(TimeUnit.SECONDS.toMillis(30)));

        if (databaseType == DatabaseType.MYSQL) {
            commonProps.put("cachePrepStmts", "true");
            commonProps.put("prepStmtCacheSize", "250");
            commonProps.put("prepStmtCacheSqlLimit", "2048");
            commonProps.put("useServerPrepStmts", "true");
            commonProps.put("useLocalSessionState", "true");
            commonProps.put("rewriteBatchedStatements", "true");
            commonProps.put("cacheResultSetMetadata", "true");
            commonProps.put("cacheServerConfiguration", "true");
            commonProps.put("elideSetAutoCommits", "true");
            commonProps.put("maintainTimeStats", "false");
            commonProps.put("alwaysSendSetIsolation", "false");
            commonProps.put("cacheCallableStmts", "true");
        }

        for (Map.Entry<String, String> e : commonProps.entrySet()) {
            config.addDataSourceProperty(e.getKey(), e.getValue());
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
