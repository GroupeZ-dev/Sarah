package fr.maxlego08.sarah.requests;

import fr.maxlego08.sarah.DatabaseConfiguration;
import fr.maxlego08.sarah.DatabaseConnection;
import fr.maxlego08.sarah.database.Executor;
import fr.maxlego08.sarah.database.Schema;
import fr.maxlego08.sarah.exceptions.DatabaseException;
import fr.maxlego08.sarah.logger.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class DeleteRequest implements Executor {

    private final Schema schemaBuilder;

    public DeleteRequest(Schema schemaBuilder) {
        this.schemaBuilder = schemaBuilder;
    }

    @Override
    public int execute(DatabaseConnection databaseConnection, DatabaseConfiguration databaseConfiguration, Logger logger) {
        StringBuilder sql = new StringBuilder("DELETE FROM ").append(schemaBuilder.getTableName());
        schemaBuilder.whereConditions(sql);

        String finalQuery = databaseConfiguration.replacePrefix(sql.toString());
        if (databaseConfiguration.isDebug()) {
            logger.info("Executing SQL: " + finalQuery);
        }

        try (Connection connection = databaseConnection.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(finalQuery)) {
            schemaBuilder.applyWhereConditions(preparedStatement, 1);
            int result = preparedStatement.executeUpdate();
            return result;
        } catch (SQLException exception) {
            logger.info("Delete operation failed on table: " + schemaBuilder.getTableName() + " - " + exception.getMessage());
            throw new DatabaseException("delete", schemaBuilder.getTableName(), exception);
        }
    }
}
