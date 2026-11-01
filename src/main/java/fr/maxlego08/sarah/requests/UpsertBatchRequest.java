package fr.maxlego08.sarah.requests;

import fr.maxlego08.sarah.DatabaseConfiguration;
import fr.maxlego08.sarah.DatabaseConnection;
import fr.maxlego08.sarah.conditions.ColumnDefinition;
import fr.maxlego08.sarah.database.Executor;
import fr.maxlego08.sarah.database.Schema;
import fr.maxlego08.sarah.database.StatementBinder;
import fr.maxlego08.sarah.exceptions.DatabaseException;
import fr.maxlego08.sarah.logger.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class UpsertBatchRequest implements Executor {

    private final List<Schema> schemas;

    public UpsertBatchRequest(List<Schema> schemas) {
        this.schemas = schemas;
    }

    /**
     * Upserts every row in a single statement. The column list and the conflict handling come
     * from the first schema, so every schema must declare the same columns.
     *
     * <p>Expression parameters are taken from the first schema only, since the conflict clause
     * appears once for the whole statement.</p>
     *
     * <p>PostgreSQL rejects a statement in which two rows target the same key, so callers must
     * merge such rows beforehand.</p>
     *
     * @return the number of rows affected, as reported by the driver
     */
    @Override
    public int execute(DatabaseConnection databaseConnection, DatabaseConfiguration databaseConfiguration, Logger logger) {

        if (schemas.isEmpty()) {
            return 0;
        }

        Schema firstSchema = schemas.get(0);

        List<String> insertColumnNames = new ArrayList<>();
        for (ColumnDefinition column : firstSchema.getColumns()) {
            if (column.isInsertedByUpsert()) {
                insertColumnNames.add(column.getSafeName());
            }
        }

        List<Object> values = new ArrayList<>();
        List<String> rows = new ArrayList<>();
        for (Schema schema : schemas) {
            List<String> rowPlaceholders = new ArrayList<>();
            for (ColumnDefinition column : schema.getColumns()) {
                if (column.isInsertedByUpsert()) {
                    rowPlaceholders.add(column.insertPlaceholder(values));
                }
            }
            rows.add("(" + String.join(", ", rowPlaceholders) + ")");
        }

        String upsertQuery = "INSERT INTO " + firstSchema.getTableName() + " (" + String.join(", ", insertColumnNames) + ") VALUES " + String.join(", ", rows)
                + UpsertRequest.conflictClause(firstSchema, databaseConfiguration.getDialect(), values, true);

        String finalQuery = databaseConfiguration.prepare(upsertQuery);

        if (databaseConfiguration.isDebug()) {
            logger.info("Executing SQL: " + finalQuery);
        }

        try (Connection connection = databaseConnection.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(finalQuery)) {

            int index = 1;
            for (Object value : values) {
                StatementBinder.bind(preparedStatement, index++, value);
            }

            return preparedStatement.executeUpdate();
        } catch (SQLException exception) {
            logger.error("Upsert batch operation failed on table: " + firstSchema.getTableName() + " - " + exception.getMessage());
            throw new DatabaseException("upsertBatch", firstSchema.getTableName(), exception);
        }
    }
}
