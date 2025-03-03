package fr.maxlego08.sarah.requests;

import fr.maxlego08.sarah.DatabaseConfiguration;
import fr.maxlego08.sarah.DatabaseConnection;
import fr.maxlego08.sarah.conditions.ColumnDefinition;
import fr.maxlego08.sarah.conditions.JoinCondition;
import fr.maxlego08.sarah.database.Executor;
import fr.maxlego08.sarah.database.Schema;
import fr.maxlego08.sarah.logger.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class UpdateBatchRequest implements Executor {

    private final List<Schema> schemas;

    public UpdateBatchRequest(List<Schema> schemas) {
        this.schemas = schemas;
    }

    @Override
    public int execute(DatabaseConnection databaseConnection, DatabaseConfiguration databaseConfiguration, Logger logger) {
        if (schemas.isEmpty()) {
            return 0;
        }

        StringBuilder updateQuery = new StringBuilder("UPDATE " + schemas.get(0).getTableName() + " SET ");
        List<Object> values = new ArrayList<>();
        List<String> columnAssignments = new ArrayList<>();
        List<String> whereConditions = new ArrayList<>();

        for (ColumnDefinition column : schemas.get(0).getColumns()) {
            columnAssignments.add(column.getSafeName() + " = ?");
        }
        updateQuery.append(String.join(", ", columnAssignments));

        for (Schema schema : schemas) {
            StringBuilder whereClause = new StringBuilder();
            schema.whereConditions(whereClause);
            whereConditions.add("(" + whereClause + ")");
            for (ColumnDefinition column : schema.getColumns()) {
                values.add(column.getObject());
            }
        }

        updateQuery.append(" WHERE ").append(String.join(" OR ", whereConditions));
        String finalQuery = databaseConfiguration.replacePrefix(updateQuery.toString());

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
            exception.printStackTrace();
            return -1;
        }
    }
}