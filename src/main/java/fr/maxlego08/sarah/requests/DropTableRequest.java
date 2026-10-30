package fr.maxlego08.sarah.requests;

import fr.maxlego08.sarah.DatabaseConfiguration;
import fr.maxlego08.sarah.DatabaseConnection;
import fr.maxlego08.sarah.database.Executor;
import fr.maxlego08.sarah.database.Schema;
import fr.maxlego08.sarah.logger.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class DropTableRequest implements Executor {

    private final Schema schema;

    public DropTableRequest(Schema schema) {
        this.schema = schema;
    }

    @Override
    public int execute(DatabaseConnection databaseConnection, DatabaseConfiguration databaseConfiguration, Logger logger) {
        String tableName = this.schema.getTableName();
        if (tableName == null || tableName.trim().isEmpty()) {
            logger.warn("Invalid table name.");
            return -1;
        }

        String finalQuery = databaseConfiguration.prepare("DROP TABLE IF EXISTS " + tableName);
        if (databaseConfiguration.isDebug()) {
            logger.info("Executing SQL: " + finalQuery);
        }

        try (Connection connection = databaseConnection.getConnection(); PreparedStatement preparedStatement = connection.prepareStatement(finalQuery)) {

            preparedStatement.execute();
            return 0;
        } catch (SQLException exception) {
            logger.error("Error while executing SQL query: " + exception.getMessage());
            return -1;
        }
    }

}
