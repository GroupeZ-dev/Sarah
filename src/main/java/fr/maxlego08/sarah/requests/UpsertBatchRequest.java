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

public class UpsertBatchRequest implements Executor {

    private final List<Schema> schemas;

    public UpsertBatchRequest(List<Schema> schemas) {
        this.schemas = schemas;
    }

    @Override
    public int execute(DatabaseConnection databaseConnection, DatabaseConfiguration databaseConfiguration, Logger logger) {
        if (schemas.isEmpty()) {
            return 0;
        }

        DatabaseType databaseType = databaseConfiguration.getDatabaseType();
        Schema firstSchema = schemas.get(0);
        StringBuilder insertQuery = new StringBuilder("INSERT INTO " + firstSchema.getTableName() + " (");
        StringBuilder valuesQuery = new StringBuilder("VALUES ");
        StringBuilder onUpdateQuery = new StringBuilder();

        List<Object> values = new ArrayList<>();
        List<String> placeholders = new ArrayList<>();
        List<String> insertColumnNames = new ArrayList<>();

        // Build column list - skip auto-increment columns
        for (ColumnDefinition column : firstSchema.getColumns()) {
            if (!column.isAutoIncrement()) {
                insertColumnNames.add(column.getSafeName());
            }
        }

        insertQuery.append(String.join(", ", insertColumnNames)).append(") ");

        for (Schema schema : schemas) {
            List<String> rowPlaceholders = new ArrayList<>();
            for (ColumnDefinition column : schema.getColumns()) {
                // Skip auto-increment columns
                if (!column.isAutoIncrement()) {
                    rowPlaceholders.add("?");
                    values.add(column.getObject());
                }
            }
            placeholders.add("(" + String.join(", ", rowPlaceholders) + ")");
        }

        valuesQuery.append(String.join(", ", placeholders));

        if (databaseType == DatabaseType.SQLITE) {
            StringBuilder onConflictQuery = new StringBuilder(" ON CONFLICT (");
            List<String> primaryKeys = firstSchema.getPrimaryKeys();
            onConflictQuery.append(String.join(", ", primaryKeys)).append(") DO UPDATE SET ");

            // Skip auto-increment columns in UPDATE as well
            for (int i = 0; i < insertColumnNames.size(); i++) {
                if (i > 0) onUpdateQuery.append(", ");
                onUpdateQuery.append(insertColumnNames.get(i)).append(" = excluded.").append(insertColumnNames.get(i));
            }

            insertQuery.append(valuesQuery).append(onConflictQuery).append(onUpdateQuery);
        } else {
            onUpdateQuery.append(" ON DUPLICATE KEY UPDATE ");
            // Skip auto-increment columns in UPDATE as well
            for (int i = 0; i < insertColumnNames.size(); i++) {
                if (i > 0) onUpdateQuery.append(", ");
                onUpdateQuery.append(insertColumnNames.get(i)).append(" = VALUES(").append(insertColumnNames.get(i)).append(")");
            }

            insertQuery.append(valuesQuery).append(onUpdateQuery);
        }

        String finalQuery = databaseConfiguration.replacePrefix(insertQuery.toString());
        if (databaseConfiguration.isDebug()) {
            logger.info("Executing SQL: " + finalQuery);
        }

        try (Connection connection = databaseConnection.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(finalQuery)) {

            int index = 1;
            for (Object value : values) {
                preparedStatement.setObject(index++, value);
            }

            return preparedStatement.executeUpdate();
        } catch (SQLException exception) {
            logger.info("Upsert batch operation failed on table: " + firstSchema.getTableName() + " - " + exception.getMessage());
            throw new DatabaseException("upsertBatch", firstSchema.getTableName(), exception);
        }
    }
}
