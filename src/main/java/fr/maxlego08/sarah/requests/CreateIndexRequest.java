package fr.maxlego08.sarah.requests;

import fr.maxlego08.sarah.DatabaseConfiguration;
import fr.maxlego08.sarah.DatabaseConnection;
import fr.maxlego08.sarah.conditions.ColumnDefinition;
import fr.maxlego08.sarah.database.Executor;
import fr.maxlego08.sarah.database.Schema;
import fr.maxlego08.sarah.exceptions.DatabaseException;
import fr.maxlego08.sarah.logger.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class CreateIndexRequest implements Executor {

    private final Schema schema;

    public CreateIndexRequest(Schema schema) {
        this.schema = schema;
    }

    @Override
    public int execute(DatabaseConnection databaseConnection, DatabaseConfiguration databaseConfiguration, Logger logger) {

        StringBuilder indexTableSQL = new StringBuilder("CREATE INDEX ");
        String tableName = schema.getTableName();
        ColumnDefinition column = schema.getColumns().get(0);
        String indexName = "idx_" + tableName + "_" + column.getName();

        indexTableSQL.append(indexName);
        indexTableSQL.append(" ON ");
        indexTableSQL.append(String.format("`%s`", tableName));
        indexTableSQL.append(" (");
        indexTableSQL.append(column.getSafeName());
        indexTableSQL.append(" )");

        String finalQuery = databaseConfiguration.replacePrefix(indexTableSQL.toString());
        if (databaseConfiguration.isDebug()) {
            logger.info("Executing SQL: " + finalQuery);
        }

        try (Connection connection = databaseConnection.getConnection(); PreparedStatement preparedStatement = connection.prepareStatement(finalQuery)) {
            preparedStatement.execute();
            return preparedStatement.getUpdateCount();
        } catch (SQLException exception) {
            logger.info("Create index operation failed on table: " + tableName + " - " + exception.getMessage());
            throw new DatabaseException("createIndex", tableName, exception);
        }

    }
}
