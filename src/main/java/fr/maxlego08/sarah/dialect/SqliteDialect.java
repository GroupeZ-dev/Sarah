package fr.maxlego08.sarah.dialect;

import fr.maxlego08.sarah.DatabaseConfiguration;
import fr.maxlego08.sarah.database.DatabaseType;

public class SqliteDialect extends AbstractDialect {

    @Override
    public DatabaseType getType() {
        return DatabaseType.SQLITE;
    }

    /**
     * SQLite only auto-increments an {@code INTEGER PRIMARY KEY}, declared inline.
     */
    @Override
    public String autoIncrementColumn(String columnType) {
        return "INTEGER PRIMARY KEY AUTOINCREMENT";
    }

    @Override
    public boolean inlineAutoIncrementPrimaryKey() {
        return true;
    }

    @Override
    public String enumFallbackType() {
        return "TEXT";
    }

    /**
     * SQLite needs {@code LIMIT -1} to express an offset without a limit.
     */
    @Override
    public String limitClause(Integer limit, Integer offset) {
        if (limit == null && offset != null) {
            return " LIMIT -1 OFFSET " + offset;
        }
        return super.limitClause(limit, offset);
    }

    @Override
    public String jdbcUrl(DatabaseConfiguration configuration) {
        throw new UnsupportedOperationException("SQLite URLs are built from the database file by SqliteConnection");
    }

    @Override
    public String driverClassName() {
        return "org.sqlite.JDBC";
    }
}
