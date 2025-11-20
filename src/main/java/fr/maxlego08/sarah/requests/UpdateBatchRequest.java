package fr.maxlego08.sarah.requests;

import fr.maxlego08.sarah.DatabaseConfiguration;
import fr.maxlego08.sarah.DatabaseConnection;
import fr.maxlego08.sarah.conditions.ColumnDefinition;
import fr.maxlego08.sarah.conditions.JoinCondition;
import fr.maxlego08.sarah.database.Executor;
import fr.maxlego08.sarah.database.Schema;
import fr.maxlego08.sarah.exceptions.DatabaseException;
import fr.maxlego08.sarah.logger.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

public class UpdateBatchRequest implements Executor {

    private final List<Schema> schemas;

    public UpdateBatchRequest(List<Schema> schemas) {
        this.schemas = schemas;
    }

    @Override
    public int execute(DatabaseConnection databaseConnection, DatabaseConfiguration databaseConfiguration, Logger logger) {
        if (schemas.isEmpty()) return 0;

        Schema firstSchema = schemas.get(0);
        StringBuilder updateQuery = new StringBuilder("UPDATE " + firstSchema.getTableName());

        if (!firstSchema.getJoinConditions().isEmpty()) {
            for (JoinCondition join : firstSchema.getJoinConditions()) {
                updateQuery.append(" ").append(join.getJoinClause());
            }
        }

        updateQuery.append(" SET ");

        List<ColumnDefinition> columns = firstSchema.getColumns();
        for (int i = 0; i < columns.size(); i++) {
            ColumnDefinition columnDefinition = columns.get(i);
            updateQuery.append(i > 0 ? ", " : "").append(columnDefinition.getSafeName()).append(" = ?");
        }

        firstSchema.whereConditions(updateQuery);
        String updateSql = databaseConfiguration.replacePrefix(updateQuery.toString());

        if (databaseConfiguration.isDebug()) {
            logger.info("Executing SQL Batch: " + updateSql);
        }

        try (Connection connection = databaseConnection.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(updateSql)) {

            for (Schema schema : schemas) {
                List<ColumnDefinition> schemaColumns = schema.getColumns();
                for (int i = 0; i < schemaColumns.size(); i++) {
                    preparedStatement.setObject(i + 1, schemaColumns.get(i).getObject());
                }
                schema.applyWhereConditions(preparedStatement, schemaColumns.size() + 1);
                preparedStatement.addBatch();
            }

            int[] results = preparedStatement.executeBatch();
            int total = 0;
            for (int count : results) {
                total += count;
            }
            return total;

        } catch (SQLException exception) {
            logger.info("Update batch operation failed on table: " + firstSchema.getTableName() + " - " + exception.getMessage());
            throw new DatabaseException("updateBatch", firstSchema.getTableName(), exception);
        }
    }
}
