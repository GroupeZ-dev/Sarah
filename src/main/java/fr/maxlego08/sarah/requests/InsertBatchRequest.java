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

public class InsertBatchRequest implements Executor {

    private final List<Schema> schemas;

    public InsertBatchRequest(List<Schema> schemas) {
        this.schemas = schemas;
    }

    /**
     * Inserts every row in a single statement.
     *
     * @return the first generated key when the table has an auto-increment column, otherwise the
     * number of inserted rows
     */
    @Override
    public int execute(DatabaseConnection databaseConnection, DatabaseConfiguration databaseConfiguration, Logger logger) {

        if (schemas.isEmpty()) {
            return 0;
        }

        Schema firstSchema = schemas.get(0);

        List<String> columnNames = new ArrayList<>();
        for (ColumnDefinition column : firstSchema.getColumns()) {
            if (!column.isAutoIncrement()) {
                columnNames.add(column.getSafeName());
            }
        }

        List<Object> values = new ArrayList<>();
        List<String> rows = new ArrayList<>();
        for (Schema schema : schemas) {
            List<String> rowPlaceholders = new ArrayList<>();
            for (ColumnDefinition column : schema.getColumns()) {
                if (!column.isAutoIncrement()) {
                    rowPlaceholders.add(column.insertPlaceholder(values));
                }
            }
            rows.add("(" + String.join(", ", rowPlaceholders) + ")");
        }

        String finalQuery = databaseConfiguration.prepare("INSERT INTO " + firstSchema.getTableName() + " (" + String.join(", ", columnNames) + ") VALUES " + String.join(", ", rows));

        if (databaseConfiguration.isDebug()) {
            logger.info("Executing SQL: " + finalQuery);
        }

        try (Connection connection = databaseConnection.getConnection();
             PreparedStatement preparedStatement = databaseConfiguration.getDialect().prepareInsert(connection, finalQuery, firstSchema)) {

            int index = 1;
            for (Object value : values) {
                StatementBinder.bind(preparedStatement, index++, value);
            }

            int updatedRows = preparedStatement.executeUpdate();
            int generatedKey = InsertRequest.readGeneratedKey(preparedStatement);
            return generatedKey != 0 ? generatedKey : updatedRows;
        } catch (SQLException exception) {
            logger.error("Insert batch operation failed on table: " + firstSchema.getTableName() + " - " + exception.getMessage());
            throw new DatabaseException("insertBatch", firstSchema.getTableName(), exception);
        }
    }
}
