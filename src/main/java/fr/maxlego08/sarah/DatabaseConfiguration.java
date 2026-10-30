package fr.maxlego08.sarah;

import fr.maxlego08.sarah.database.DatabaseType;
import fr.maxlego08.sarah.dialect.Dialect;
import fr.maxlego08.sarah.dialect.Dialects;

import java.util.Objects;

/**
 * Represents the configuration for connecting to a database.
 * This record encapsulates the database connection details, including the prefix, username, password, port, host,
 * database name, and debug mode.
 */
public class DatabaseConfiguration {
    private final String tablePrefix;
    private final String user;
    private final String password;
    private final int port;
    private final String host;
    private final String database;
    private final boolean debug;
    private final DatabaseType databaseType;
    private final Integer maximumPoolSize;
    private final Integer minimumIdle;
    /**
     * Hikari max connection lifetime in milliseconds. {@code null} keeps the library default.
     * {@code 0} disables retirement. Hikari rejects values below 30 seconds.
     */
    private final Long maxLifetime;

    public DatabaseConfiguration(String tablePrefix, String user, String password, int port, String host,
                                 String database, boolean debug, DatabaseType databaseType) {
        this(tablePrefix, user, password, port, host, database, debug, databaseType, null, null);
    }

    public DatabaseConfiguration(String tablePrefix, String user, String password, int port, String host,
                                 String database, boolean debug, DatabaseType databaseType,
                                 Integer maximumPoolSize, Integer minimumIdle) {
        this(tablePrefix, user, password, port, host, database, debug, databaseType, maximumPoolSize, minimumIdle, null);
    }

    public DatabaseConfiguration(String tablePrefix, String user, String password, int port, String host,
                                 String database, boolean debug, DatabaseType databaseType,
                                 Integer maximumPoolSize, Integer minimumIdle, Long maxLifetime) {
        this.tablePrefix = tablePrefix;
        this.user = user;
        this.password = password;
        this.port = port;
        this.host = host;
        this.database = database;
        this.debug = debug;
        this.databaseType = databaseType;
        this.maximumPoolSize = maximumPoolSize;
        this.minimumIdle = minimumIdle;
        this.maxLifetime = maxLifetime;
    }

    public static DatabaseConfiguration create(String user, String password, int port, String host, String database, DatabaseType databaseType) {
        return new DatabaseConfiguration("", user, password, port, host, database, false, databaseType);
    }

    public static DatabaseConfiguration create(String user, String password, int port, String host, String database) {
        return new DatabaseConfiguration("", user, password, port, host, database, false, DatabaseType.MYSQL);
    }

    public static DatabaseConfiguration createMariaDb(String user, String password, int port, String host, String database) {
        return new DatabaseConfiguration("", user, password, port, host, database, false, DatabaseType.MARIADB);
    }

    public static DatabaseConfiguration create(String user, String password, int port, String host, String database, boolean debug) {
        return new DatabaseConfiguration("", user, password, port, host, database, debug, DatabaseType.MYSQL);
    }

    public static DatabaseConfiguration createMariaDb(String user, String password, int port, String host, String database, boolean debug) {
        return new DatabaseConfiguration("", user, password, port, host, database, debug, DatabaseType.MARIADB);
    }

    public static DatabaseConfiguration create(String user, String password, String host, String database, DatabaseType databaseType) {
        return new DatabaseConfiguration("", user, password, 3306, host, database, false, databaseType);
    }

    public static DatabaseConfiguration create(String user, String password, int port, String host, String database, boolean debug, DatabaseType databaseType) {
        return new DatabaseConfiguration("", user, password, port, host, database, debug, databaseType);
    }

    public static DatabaseConfiguration createMariaDb(String user, String password, String host, String database) {
        return new DatabaseConfiguration("", user, password, 3306, host, database, false, DatabaseType.MARIADB);
    }

    public static DatabaseConfiguration sqlite(boolean debug) {
        return new DatabaseConfiguration("", null, null, 0, null, null, debug, DatabaseType.SQLITE);
    }

    public static DatabaseConfiguration createPostgreSql(String user, String password, int port, String host, String database, boolean debug) {
        return new DatabaseConfiguration("", user, password, port, host, database, debug, DatabaseType.POSTGRESQL);
    }

    public String replacePrefix(String tableName) {
        return this.tablePrefix == null ? tableName : tableName.replace("%prefix%", this.tablePrefix);
    }

    /**
     * Turns a statement built by Sarah into the one sent to the database: replaces
     * {@code %prefix%} and lets the dialect rewrite identifier quoting.
     *
     * @param sql the statement, with MySQL-style backtick quoting
     * @return the statement for the configured engine
     */
    public String prepare(String sql) {
        return this.getDialect().translate(this.replacePrefix(sql));
    }

    public Dialect getDialect() {
        return Dialects.of(this.databaseType);
    }

    public String getTablePrefix() {
        return tablePrefix;
    }

    public String getUser() {
        return user;
    }

    public String getPassword() {
        return password;
    }

    public int getPort() {
        return port;
    }

    public String getHost() {
        return host;
    }

    public String getDatabase() {
        return database;
    }

    public boolean isDebug() {
        return debug;
    }

    public DatabaseType getDatabaseType() {
        return databaseType;
    }

    public Integer getMaximumPoolSize() {
        return maximumPoolSize;
    }

    public Integer getMinimumIdle() {
        return minimumIdle;
    }

    public Long getMaxLifetime() {
        return maxLifetime;
    }

    public DatabaseConfiguration withPoolSettings(Integer maximumPoolSize, Integer minimumIdle) {
        return new DatabaseConfiguration(this.tablePrefix, this.user, this.password, this.port, this.host,
                this.database, this.debug, this.databaseType, maximumPoolSize, minimumIdle, this.maxLifetime);
    }

    /**
     * @param maxLifetime connection lifetime in milliseconds, {@code 0} to keep connections until the pool closes, or {@code null} for the default
     */
    public DatabaseConfiguration withMaxLifetime(Long maxLifetime) {
        return new DatabaseConfiguration(this.tablePrefix, this.user, this.password, this.port, this.host,
                this.database, this.debug, this.databaseType, this.maximumPoolSize, this.minimumIdle, maxLifetime);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DatabaseConfiguration that = (DatabaseConfiguration) o;
        return port == that.port &&
                debug == that.debug &&
                Objects.equals(tablePrefix, that.tablePrefix) &&
                Objects.equals(user, that.user) &&
                Objects.equals(password, that.password) &&
                Objects.equals(host, that.host) &&
                Objects.equals(database, that.database) &&
                databaseType == that.databaseType &&
                Objects.equals(maximumPoolSize, that.maximumPoolSize) &&
                Objects.equals(minimumIdle, that.minimumIdle) &&
                Objects.equals(maxLifetime, that.maxLifetime);
    }

    @Override
    public int hashCode() {
        return Objects.hash(tablePrefix, user, password, port, host, database, debug, databaseType, maximumPoolSize, minimumIdle, maxLifetime);
    }

    @Override
    public String toString() {
        return "DatabaseConfiguration{" +
                "tablePrefix='" + tablePrefix + '\'' +
                ", user='" + user + '\'' +
                ", password='" + password + '\'' +
                ", port=" + port +
                ", host='" + host + '\'' +
                ", database='" + database + '\'' +
                ", debug=" + debug +
                ", databaseType=" + databaseType +
                ", maximumPoolSize=" + maximumPoolSize +
                ", minimumIdle=" + minimumIdle +
                ", maxLifetime=" + maxLifetime +
                '}';
    }
}
