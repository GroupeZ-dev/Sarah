package fr.maxlego08.sarah;

import fr.maxlego08.sarah.database.DatabaseType;
import fr.maxlego08.sarah.logger.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import java.io.File;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Base class for database tests providing common setup and teardown functionality
 */
public abstract class DatabaseTestBase {

    protected DatabaseConnection connection;
    protected RequestHelper requestHelper;
    protected DatabaseConfiguration configuration;
    protected Logger testLogger;
    protected File sqliteFile;

    @BeforeEach
    public void setUp() throws Exception {
        testLogger = new TestLogger();
        configuration = createConfiguration();

        // CRITICAL: Set database configuration in MigrationManager
        // This is required for timestamps() and other schema operations
        MigrationManager.setDatabaseConfiguration(configuration);

        connection = createConnection();
        connection.connect();
        requestHelper = new RequestHelper(connection, testLogger);

        // Additional setup hook for subclasses
        afterConnectionSetup();
    }

    @AfterEach
    public void tearDown() throws Exception {
        if (connection != null && connection.isValid()) {
            cleanupDatabase();
            connection.disconnect();
        }

        // Clean up SQLite file if exists
        if (configuration.getDatabaseType() == DatabaseType.SQLITE) {
            this.sqliteFile.delete();
        }
    }

    /**
     * Create database configuration for tests
     */
    protected DatabaseConfiguration createConfiguration() {
        // Default to SQLite for easier testing
        return DatabaseConfiguration.sqlite(true);
    }

    /**
     * Create database connection based on configuration
     */
    protected DatabaseConnection createConnection() {
        DatabaseType type = configuration.getDatabaseType();
        switch (type) {
            case SQLITE:
                // SqliteConnection expects a folder, not a file
                // It will create a file named "database.db" inside that folder
                // So we pass the current directory and set the filename
                SqliteConnection sqliteConnection = new SqliteConnection(configuration, new File("."));
                sqliteConnection.setFileName(getSqlitePath());
                this.sqliteFile = sqliteConnection.getFolder().toPath().resolve(this.getSqlitePath()).toFile();
                return sqliteConnection;
            case MYSQL:
                return new MySqlConnection(configuration);
            case MARIADB:
                return new MariaDbConnection(configuration);
            default:
                throw new IllegalArgumentException("Unsupported database type: " + type);
        }
    }

    /**
     * Hook for additional setup after connection is established
     */
    protected void afterConnectionSetup() throws Exception {
        // Override in subclasses if needed
    }

    /**
     * Clean up all test tables
     */
    protected void cleanupDatabase() throws SQLException {
        Connection conn = connection.getConnection();
        try (Statement stmt = conn.createStatement()) {
            // Drop common test tables
            String[] tables = {"test_users", "test_products", "test_orders", "test_composite",
                             "test_autoincrement", "migrations", "test_join_a", "test_join_b"};
            for (String table : tables) {
                try {
                    if (configuration.getDatabaseType() == DatabaseType.SQLITE) {
                        stmt.execute("DROP TABLE IF EXISTS " + table);
                    } else {
                        stmt.execute("DROP TABLE IF EXISTS " + table);
                    }
                } catch (SQLException e) {
                    // Ignore if table doesn't exist
                }
            }
        }
    }

    /**
     * Get SQLite database file path for tests
     */
    protected String getSqlitePath() {
        return "test_database.db";
    }

    /**
     * Simple test logger implementation
     */
    protected static class TestLogger implements Logger {
        @Override
        public void info(String message) {
            System.out.println("[INFO] " + message);
        }
    }

    /**
     * Helper method to execute raw SQL (useful for setup/verification)
     */
    protected void executeRawSQL(String sql) throws SQLException {
        try (Statement stmt = connection.getConnection().createStatement()) {
            stmt.execute(sql);
        }
    }

    /**
     * Helper method to count rows in a table
     */
    protected int countRows(String tableName) throws SQLException {
        try (Statement stmt = connection.getConnection().createStatement()) {
            ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM " + tableName);
            if (rs.next()) {
                return rs.getInt(1);
            }
            return 0;
        }
    }
}