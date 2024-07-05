package fr.maxlego08.sarah.requests;

import fr.maxlego08.sarah.DatabaseConfiguration;
import fr.maxlego08.sarah.conditions.ColumnDefinition;
import fr.maxlego08.sarah.conditions.JoinCondition;
import fr.maxlego08.sarah.database.Executor;
import fr.maxlego08.sarah.database.Schema;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class UpdateRequest implements Executor {

    private final Schema schema;

    public UpdateRequest(Schema schema) {
        this.schema = schema;
    }

    @Override
    public int execute(Connection connection, DatabaseConfiguration databaseConfiguration, Logger logger) throws SQLException {

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
        String upsertQuery = databaseConfiguration.replacePrefix(updateQuery.toString());

        if (databaseConfiguration.isDebug()) {
            logger.info("Executing SQL: " + upsertQuery);
        }

        try (PreparedStatement preparedStatement = connection.prepareStatement(upsertQuery)) {
            for (int i = 0; i < values.size(); i++) {
                preparedStatement.setObject(i + 1, values.get(i));
            }
            this.schema.applyWhereConditions(preparedStatement, values.size() + 1);
            preparedStatement.executeUpdate();
        } catch (SQLException exception) {
            exception.printStackTrace();
            throw new SQLException("Failed to execute upsert: " + exception.getMessage(), exception);
        }

        return -1;
    }
}
