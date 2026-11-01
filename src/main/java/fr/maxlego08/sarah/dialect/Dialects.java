package fr.maxlego08.sarah.dialect;

import fr.maxlego08.sarah.database.DatabaseType;

/**
 * Shared dialect instances, one per {@link DatabaseType}.
 */
public final class Dialects {

    private static final Dialect MYSQL = new MySqlDialect();
    private static final Dialect MARIADB = new MariaDbDialect();
    private static final Dialect SQLITE = new SqliteDialect();
    private static final Dialect POSTGRESQL = new PostgreSqlDialect();

    private Dialects() {
    }

    public static Dialect of(DatabaseType databaseType) {
        if (databaseType == null) {
            return MYSQL;
        }
        switch (databaseType) {
            case MARIADB:
                return MARIADB;
            case SQLITE:
                return SQLITE;
            case POSTGRESQL:
                return POSTGRESQL;
            case MYSQL:
            default:
                return MYSQL;
        }
    }
}
