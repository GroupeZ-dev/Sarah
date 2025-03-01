package fr.maxlego08.sarah.requests;

import fr.maxlego08.sarah.DatabaseConfiguration;
import fr.maxlego08.sarah.DatabaseConnection;
import fr.maxlego08.sarah.conditions.ColumnDefinition;
import fr.maxlego08.sarah.database.Executor;
import fr.maxlego08.sarah.database.Schema;
import fr.maxlego08.sarah.logger.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class InsertBatchRequest implements Executor {

    private final List<Schema> schemas;

    public InsertBatchRequest(List<Schema> schemas) {
        this.schemas = schemas;
    }

    @Override
    public int execute(DatabaseConnection databaseConnection, DatabaseConfiguration databaseConfiguration, Logger logger) {
        if (schemas.isEmpty()) {
            return 0;
        }

        Schema firstSchema = schemas.get(0);
        StringBuilder insertQuery = new StringBuilder("INSERT INTO " + firstSchema.getTableName() + " (");
        StringBuilder valuesQuery = new StringBuilder("VALUES ");

        List<Object> values = new ArrayList<>();
        List<String> placeholders = new ArrayList<>();
        List<String> columnNames = new ArrayList<>();

        for (ColumnDefinition column : firstSchema.getColumns()) {
            columnNames.add(column.getSafeName());
        }

        insertQuery.append(String.join(", ", columnNames)).append(") ");

        for (Schema schema : schemas) {
            List<String> rowPlaceholders = new ArrayList<>();
            for (ColumnDefinition column : schema.getColumns()) {
                rowPlaceholders.add("?");
                values.add(column.getObject());
            }
            placeholders.add("(" + String.join(", ", rowPlaceholders) + ")");
        }

        valuesQuery.append(String.join(", ", placeholders));
        insertQuery.append(valuesQuery);

        String finalQuery = databaseConfiguration.replacePrefix(insertQuery.toString());
        if (databaseConfiguration.isDebug()) {
            logger.info("Executing SQL: " + finalQuery);
        }

        try (Connection connection = databaseConnection.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(finalQuery, Statement.RETURN_GENERATED_KEYS)) {

            int index = 1;
            for (Object value : values) {
                preparedStatement.setObject(index++, value);
            }
            
            int updatedRows = preparedStatement.executeUpdate();
            
            try (ResultSet generatedKeys = preparedStatement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return generatedKeys.getInt(1);
                }
            }
            return updatedRows;
        } catch (SQLException exception) {
            exception.printStackTrace();
            return -1;
        }
    }
}
