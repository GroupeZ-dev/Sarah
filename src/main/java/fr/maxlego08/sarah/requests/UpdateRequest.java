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
import java.util.ArrayList;
import java.util.List;

public class UpdateRequest implements Executor {

    private final Schema schema;

    public UpdateRequest(Schema schema) {
        this.schema = schema;
    }

    @Override
    public int execute(DatabaseConnection databaseConnection, DatabaseConfiguration databaseConfiguration, Logger logger) {

        StringBuilder updateQuery = new StringBuilder("UPDATE " + this.schema.getTableName());

        if (!this.schema.getJoinConditions().isEmpty()) {
            for (JoinCondition join : this.schema.getJoinConditions()) {
                updateQuery.append(" ").append(join.getJoinClause());
            }
        }

        updateQuery.append(" SET ");

        List<Object> values = new ArrayList<>();

        for (int i = 0; i < this.schema.getColumns().size(); i++) {
            ColumnDefinition columnDefinition = this.schema.getColumns().get(i);
            updateQuery.append(i > 0 ? ", " : "").append(columnDefinition.getSafeName()).append(" = ?");
            values.add(columnDefinition.getObject());
        }

        this.schema.whereConditions(updateQuery);
        String updateSql = databaseConfiguration.replacePrefix(updateQuery.toString());

        if (databaseConfiguration.isDebug()) {
            logger.info("Executing SQL: " + updateSql);
        }

        try (Connection connection = databaseConnection.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(updateSql)) {
            for (int i = 0; i < values.size(); i++) {
                preparedStatement.setObject(i + 1, values.get(i));
            }
            this.schema.applyWhereConditions(preparedStatement, values.size() + 1);
            preparedStatement.executeUpdate();
            return preparedStatement.getUpdateCount();
        } catch (SQLException exception) {
            logger.info("Update operation failed on table: " + this.schema.getTableName() + " - " + exception.getMessage());
            throw new DatabaseException("update", this.schema.getTableName(), exception);
        }
    }
}
