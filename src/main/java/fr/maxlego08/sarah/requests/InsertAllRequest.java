package fr.maxlego08.sarah.requests;

import fr.maxlego08.sarah.DatabaseConfiguration;
import fr.maxlego08.sarah.DatabaseConnection;
import fr.maxlego08.sarah.conditions.ColumnDefinition;
import fr.maxlego08.sarah.database.Executor;
import fr.maxlego08.sarah.database.Schema;
import fr.maxlego08.sarah.logger.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class InsertAllRequest implements Executor {

    private final Schema schema;
    private final String toTableName;

    public InsertAllRequest(Schema schema, String toTableName) {
        this.schema = schema;
        this.toTableName = toTableName;
    }

    @Override
    public int execute(DatabaseConnection databaseConnection, DatabaseConfiguration databaseConfiguration, Logger logger) {

        StringBuilder insertBuilder = new StringBuilder("INSERT INTO " + this.toTableName + " (");
        StringBuilder columns = new StringBuilder();

        int columnIndex = 0;
        for (ColumnDefinition columnDefinition : this.schema.getColumns()) {
            // Skip auto-increment columns
            if (columnDefinition.isAutoIncrement()) {
                continue;
            }

            if (columnIndex > 0) {
                columns.append(",");
            }
            columns.append(columnDefinition.getSafeName());
            columnIndex++;
        }

        insertBuilder.append(columns).append(") ");

        insertBuilder.append("SELECT ").append(columns);
        insertBuilder.append(" FROM ");
        insertBuilder.append(this.schema.getTableName());

        String insertQuery = databaseConfiguration.replacePrefix(insertBuilder.toString());

        if (databaseConfiguration.isDebug()) {
            logger.info("Executing SQL: " + insertQuery);
        }

        try (Connection connection = databaseConnection.getConnection(); PreparedStatement preparedStatement = connection.prepareStatement(insertQuery)) {
            preparedStatement.executeUpdate();
        } catch (SQLException exception) {
            exception.printStackTrace();
            return -1;
        }

        return 0;
    }
}
