package fr.maxlego08.sarah.requests;

import fr.maxlego08.sarah.DatabaseConfiguration;
import fr.maxlego08.sarah.DatabaseConnection;
import fr.maxlego08.sarah.conditions.ColumnDefinition;
import fr.maxlego08.sarah.conditions.JoinCondition;
import fr.maxlego08.sarah.database.Executor;
import fr.maxlego08.sarah.database.Schema;
import fr.maxlego08.sarah.database.StatementBinder;
import fr.maxlego08.sarah.dialect.Dialect;
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

    static String buildQuery(Schema schema, List<Object> values, Dialect dialect) {
        StringBuilder updateQuery = new StringBuilder("UPDATE " + schema.getTableName());

        for (JoinCondition join : schema.getJoinConditions()) {
            updateQuery.append(" ").append(join.getJoinClause(dialect));
        }

        List<String> assignments = new ArrayList<>();
        for (ColumnDefinition columnDefinition : schema.getColumns()) {
            assignments.add(columnDefinition.updateAssignment(values));
        }
        updateQuery.append(" SET ").append(String.join(", ", assignments));

        schema.whereConditions(updateQuery);
        return updateQuery.toString();
    }

    /**
     * @return the number of rows changed
     */
    @Override
    public int execute(DatabaseConnection databaseConnection, DatabaseConfiguration databaseConfiguration, Logger logger) {

        List<Object> values = new ArrayList<>();
        String updateSql = databaseConfiguration.prepare(buildQuery(this.schema, values, databaseConfiguration.getDialect()));

        if (databaseConfiguration.isDebug()) {
            logger.info("Executing SQL: " + updateSql);
        }

        try (Connection connection = databaseConnection.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(updateSql)) {

            for (int i = 0; i < values.size(); i++) {
                StatementBinder.bind(preparedStatement, i + 1, values.get(i));
            }
            this.schema.applyWhereConditions(preparedStatement, values.size() + 1);

            // getUpdateCount() n'est PAS fiable apres executeUpdate() : le pilote SQLite
            // (org.xerial:sqlite-jdbc) rend 0 alors que executeUpdate() a bien rendu le nombre
            // de lignes modifiees. Toute ecriture compare-and-set batie sur ce compte rendu
            // croyait donc avoir perdu la course a chaque fois, sur toutes les bases SQLite.
            return preparedStatement.executeUpdate();
        } catch (SQLException exception) {
            logger.error("Update operation failed on table: " + this.schema.getTableName() + " - " + exception.getMessage());
            throw new DatabaseException("update", this.schema.getTableName(), exception);
        }
    }
}
