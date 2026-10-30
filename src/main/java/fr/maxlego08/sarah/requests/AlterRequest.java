package fr.maxlego08.sarah.requests;

import fr.maxlego08.sarah.DatabaseConfiguration;
import fr.maxlego08.sarah.DatabaseConnection;
import fr.maxlego08.sarah.conditions.ColumnDefinition;
import fr.maxlego08.sarah.database.DatabaseType;
import fr.maxlego08.sarah.database.Executor;
import fr.maxlego08.sarah.database.Schema;
import fr.maxlego08.sarah.exceptions.DatabaseException;
import fr.maxlego08.sarah.logger.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class AlterRequest implements Executor {

    private final Schema schema;

    public AlterRequest(Schema schema) {
        this.schema = schema;
    }

    @Override
    public int execute(DatabaseConnection databaseConnection, DatabaseConfiguration databaseConfiguration, Logger logger) {

        // SQLite accepts only ONE column per ALTER TABLE statement: joining the ADD COLUMN
        // clauses with commas fails with 'near ",": syntax error'. One statement per column.
        if (databaseConfiguration.getDatabaseType() == DatabaseType.SQLITE) {
            return executeSqlite(databaseConnection, databaseConfiguration, logger);
        }

        StringBuilder alterTableSQL = new StringBuilder("ALTER TABLE ");
        alterTableSQL.append(this.schema.getTableName()).append(" ");

        List<String> columnSQLs = new ArrayList<>();
        for (ColumnDefinition column : this.schema.getColumns()) {
            columnSQLs.add("ADD COLUMN " + column.build(databaseConfiguration));
        }
        alterTableSQL.append(String.join(", ", columnSQLs));

        if (!this.schema.getPrimaryKeys().isEmpty()) {
            alterTableSQL.append(", PRIMARY KEY (").append(String.join(", ", this.schema.getPrimaryKeys())).append(")");
        }

        for (String fk : this.schema.getForeignKeys()) {
            alterTableSQL.append(", ADD ").append(fk);
        }

        String finalQuery = databaseConfiguration.prepare(alterTableSQL.toString());
        if (databaseConfiguration.isDebug()) {
            logger.info("Executing SQL: " + finalQuery);
        }

        try (Connection connection = databaseConnection.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(finalQuery)) {
            preparedStatement.execute();
            return preparedStatement.getUpdateCount();
        } catch (SQLException exception) {
            logger.error("Alter table operation failed on table: " + this.schema.getTableName() + " - " + exception.getMessage());
            throw new DatabaseException("alter", this.schema.getTableName(), exception);
        }
    }

    /**
     * Alters a SQLite table, one statement per column.
     * <p>
     * SQLite's ALTER TABLE takes a single ADD COLUMN clause, and supports neither adding a
     * PRIMARY KEY nor adding a constraint afterwards, so those clauses are skipped here.
     * The statements are not applied atomically: if one fails, the columns already added stay,
     * which is harmless because the migration manager recomputes the missing columns on every
     * startup and only adds the ones that are still absent.
     */
    private int executeSqlite(DatabaseConnection databaseConnection, DatabaseConfiguration databaseConfiguration, Logger logger) {

        if (!this.schema.getPrimaryKeys().isEmpty() || !this.schema.getForeignKeys().isEmpty()) {
            logger.warn("SQLite cannot add primary or foreign keys with ALTER TABLE, ignoring them on table: " + this.schema.getTableName());
        }

        List<String> queries = new ArrayList<>();
        for (ColumnDefinition column : this.schema.getColumns()) {
            String query = "ALTER TABLE " + this.schema.getTableName() + " ADD COLUMN " + column.build(databaseConfiguration);
            queries.add(databaseConfiguration.prepare(query));
        }

        if (queries.isEmpty()) {
            return 0;
        }

        int updateCount = 0;
        try (Connection connection = databaseConnection.getConnection()) {
            for (String query : queries) {
                if (databaseConfiguration.isDebug()) {
                    logger.info("Executing SQL: " + query);
                }
                try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
                    preparedStatement.execute();
                    updateCount = preparedStatement.getUpdateCount();
                }
            }
            return updateCount;
        } catch (SQLException exception) {
            logger.error("Alter table operation failed on table: " + this.schema.getTableName() + " - " + exception.getMessage());
            throw new DatabaseException("alter", this.schema.getTableName(), exception);
        }
    }
}
