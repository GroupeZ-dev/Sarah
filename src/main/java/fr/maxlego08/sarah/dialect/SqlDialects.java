package fr.maxlego08.sarah.dialect;

import fr.maxlego08.sarah.database.DatabaseType;

public final class SqlDialects {

    private static final SqlDialect MYSQL = new MySqlDialect();
    private static final SqlDialect MARIADB = new MariaDbDialect();
    private static final SqlDialect SQLITE = new SqliteDialect();
    private static final SqlDialect POSTGRESQL = new PostgreSqlDialect();

    private SqlDialects() {
    }

    public static SqlDialect from(DatabaseType databaseType) {
        if (databaseType == null) {
            throw new IllegalArgumentException("Database type cannot be null");
        }

        switch (databaseType) {
            case MYSQL:
                return MYSQL;
            case MARIADB:
                return MARIADB;
            case SQLITE:
                return SQLITE;
            case POSTGRESQL:
                return POSTGRESQL;
            default:
                throw new IllegalArgumentException("Unsupported database type: " + databaseType);
        }
    }
}
