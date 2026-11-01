package fr.maxlego08.sarah;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import fr.maxlego08.sarah.database.DatabaseType;
import fr.maxlego08.sarah.dialect.Dialect;
import fr.maxlego08.sarah.exceptions.DatabaseException;
import fr.maxlego08.sarah.logger.Logger;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.LinkedHashMap;
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

    public HikariDatabaseConnection(DatabaseConfiguration databaseConfiguration, Logger logger) {
        super(databaseConfiguration, logger);
        this.initializeDataSource();
    }

    private void initializeDataSource() {
        HikariConfig config = new HikariConfig();
        config.setPoolName("sarah-" + POOL_COUNTER.getAndIncrement());

        Dialect dialect = databaseConfiguration.getDialect();
        if (dialect.getType() == DatabaseType.SQLITE) {
            throw new IllegalArgumentException("SQLite is not pooled, use SqliteConnection");
        }

        config.setJdbcUrl(dialect.jdbcUrl(databaseConfiguration));
        config.setDriverClassName(dialect.driverClassName());

        // Auth
        config.setUsername(databaseConfiguration.getUser());
        config.setPassword(databaseConfiguration.getPassword());

        // Pooling
        int configuredMaxPoolSize = MAXIMUM_POOL_SIZE;
        Integer maxPoolSize = databaseConfiguration.getMaximumPoolSize();
        if (maxPoolSize != null && maxPoolSize > 0) {
            configuredMaxPoolSize = maxPoolSize;
        }

        int configuredMinimumIdle = Math.min(configuredMaxPoolSize, MINIMUM_IDLE);
        Integer minIdle = databaseConfiguration.getMinimumIdle();
        if (minIdle != null && minIdle >= 0) {
            configuredMinimumIdle = Math.min(configuredMaxPoolSize, minIdle);
        }

        config.setMaximumPoolSize(configuredMaxPoolSize);
        config.setMinimumIdle(configuredMinimumIdle);

        long configuredMaxLifetime = MAX_LIFETIME;
        Long maxLifetime = databaseConfiguration.getMaxLifetime();
        if (maxLifetime != null && maxLifetime >= 0) {
            configuredMaxLifetime = maxLifetime;
        }
        config.setMaxLifetime(configuredMaxLifetime);

        Long connectionTimeout = databaseConfiguration.getConnectionTimeout();
        config.setConnectionTimeout(connectionTimeout != null && connectionTimeout >= 0 ? connectionTimeout : CONNECTION_TIMEOUT);
        Long leakDetectionThreshold = databaseConfiguration.getLeakDetectionThreshold();
        config.setLeakDetectionThreshold(leakDetectionThreshold != null && leakDetectionThreshold >= 0 ? leakDetectionThreshold : LEAK_DETECTION_THRESHOLD);

        // The configuration's properties come last so they can replace Sarah's defaults, such as useSSL
        Map<String, String> properties = new LinkedHashMap<>(dialect.dataSourceProperties());
        properties.putAll(databaseConfiguration.getProperties());
        for (Map.Entry<String, String> e : properties.entrySet()) {
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
            this.logger.error("Failed to get connection from Hikari pool: " + exception.getMessage());
            throw new DatabaseException("getConnection", exception);
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
