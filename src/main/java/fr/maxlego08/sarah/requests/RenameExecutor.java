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

public class RenameExecutor implements Executor {

    private final Schema schema;

    public RenameExecutor(Schema schema) {
        this.schema = schema;
    }

    @Override
    public int execute(DatabaseConnection databaseConnection, DatabaseConfiguration databaseConfiguration, Logger logger) {

        StringBuilder alterTableSQL = new StringBuilder("ALTER TABLE ");
        alterTableSQL.append(this.schema.getTableName());
        alterTableSQL.append(" RENAME TO ");
        alterTableSQL.append(this.schema.getNewTableName());

        String finalQuery = databaseConfiguration.replacePrefix(alterTableSQL.toString());
        if (databaseConfiguration.isDebug()) {
            logger.info("Executing SQL: " + finalQuery);
        }

        try (Connection connection = databaseConnection.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(finalQuery)) {
            preparedStatement.execute();
            return preparedStatement.getUpdateCount();
        } catch (SQLException exception) {
            logger.info("Rename table operation failed: " + this.schema.getTableName() + " to " + this.schema.getNewTableName() + " - " + exception.getMessage());
            throw new DatabaseException("rename", this.schema.getTableName(), exception);
        }
    }
}
