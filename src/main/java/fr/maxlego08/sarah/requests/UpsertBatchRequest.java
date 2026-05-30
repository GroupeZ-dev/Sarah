package fr.maxlego08.sarah.requests;

import fr.maxlego08.sarah.DatabaseConfiguration;
import fr.maxlego08.sarah.DatabaseConnection;
import fr.maxlego08.sarah.conditions.ColumnDefinition;
import fr.maxlego08.sarah.database.Executor;
import fr.maxlego08.sarah.database.Schema;
import fr.maxlego08.sarah.dialect.SqlDialect;
import fr.maxlego08.sarah.dialect.SqlDialects;
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

        SqlDialect dialect = SqlDialects.from(databaseConfiguration.getDatabaseType());
        Schema firstSchema = schemas.get(0);
        StringBuilder insertQuery = new StringBuilder("INSERT INTO " + dialect.quoteTableReference(firstSchema.getTableName()) + " (");
        StringBuilder valuesQuery = new StringBuilder("VALUES ");

        List<Object> values = new ArrayList<>();
        List<String> placeholders = new ArrayList<>();
        List<String> insertColumnNames = new ArrayList<String>();

        // Build column list - skip auto-increment columns
        for (ColumnDefinition column : firstSchema.getColumns()) {
            if (!column.isAutoIncrement()) {
                insertColumnNames.add(dialect.quoteIdentifier(column.getName()));
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

        StringBuilder onUpdateQuery = new StringBuilder();
        for (int i = 0; i < insertColumnNames.size(); i++) {
            if (i > 0) {
                onUpdateQuery.append(", ");
            }
            onUpdateQuery.append(dialect.upsertUpdateExpression(insertColumnNames.get(i), true));
        }

        insertQuery.append(valuesQuery)
                .append(dialect.upsertConflictClause(firstSchema))
                .append(onUpdateQuery);

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
