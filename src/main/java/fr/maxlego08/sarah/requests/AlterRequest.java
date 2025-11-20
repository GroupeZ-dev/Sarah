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
import java.util.ArrayList;
import java.util.List;

public class AlterRequest implements Executor {

    private final Schema schema;

    public AlterRequest(Schema schema) {
        this.schema = schema;
    }

    @Override
    public int execute(DatabaseConnection databaseConnection, DatabaseConfiguration databaseConfiguration, Logger logger) {

        StringBuilder alterTableSQL = new StringBuilder("ALTER TABLE ");
        alterTableSQL.append(this.schema.getTableName()).append(" ");

        List<String> columnSQLs = new ArrayList<>();
        for (ColumnDefinition column : this.schema.getColumns()) {
            columnSQLs.add("ADD COLUMN " + column.build(databaseConfiguration));
        }
        alterTableSQL.append(String.join(", ", columnSQLs));

        if (!this.schema.getPrimaryKeys().isEmpty()) {
            alterTableSQL.append(", PRIMARY KEY (").append(String.join(", ", this.schema.getPrimaryKeys())).append(")");
        }

        for (String fk : this.schema.getForeignKeys()) {
            alterTableSQL.append(", ADD ").append(fk);
        }

        String finalQuery = databaseConfiguration.replacePrefix(alterTableSQL.toString());
        if (databaseConfiguration.isDebug()) {
            logger.info("Executing SQL: " + finalQuery);
        }

        try (Connection connection = databaseConnection.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(finalQuery)) {
            preparedStatement.execute();
            return preparedStatement.getUpdateCount();
        } catch (SQLException exception) {
            logger.info("Alter table operation failed on table: " + this.schema.getTableName() + " - " + exception.getMessage());
            throw new DatabaseException("alter", this.schema.getTableName(), exception);
        }
    }
}
