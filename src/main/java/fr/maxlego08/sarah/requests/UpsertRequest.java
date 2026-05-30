package fr.maxlego08.sarah.requests;

import fr.maxlego08.sarah.DatabaseConfiguration;
import fr.maxlego08.sarah.DatabaseConnection;
import fr.maxlego08.sarah.conditions.ColumnDefinition;
import fr.maxlego08.sarah.database.DatabaseType;
import fr.maxlego08.sarah.database.Executor;
import fr.maxlego08.sarah.database.Schema;
import fr.maxlego08.sarah.dialect.SqlDialect;
import fr.maxlego08.sarah.dialect.SqlDialects;
import fr.maxlego08.sarah.exceptions.DatabaseException;
import fr.maxlego08.sarah.logger.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class UpsertRequest implements Executor {

    private final Schema schema;

    public UpsertRequest(Schema schema) {
        this.schema = schema;
    }

    @Override
    public int execute(DatabaseConnection databaseConnection, DatabaseConfiguration databaseConfiguration, Logger logger) {
        SqlDialect dialect = SqlDialects.from(databaseConfiguration.getDatabaseType());
        StringBuilder insertQuery = new StringBuilder("INSERT INTO " + dialect.quoteTableReference(this.schema.getTableName()) + " (");
        StringBuilder valuesQuery = new StringBuilder("VALUES (");
        StringBuilder onUpdateQuery = new StringBuilder();

        List<Object> insertValues = new ArrayList<>();
        List<Object> updateValues = new ArrayList<>();

        int insertIndex = 0;
        int updateIndex = 0;
        for (ColumnDefinition columnDefinition : this.schema.getColumns()) {
            // Skip auto-increment columns in INSERT part
            if (!columnDefinition.isAutoIncrement()) {
                String quotedColumn = dialect.quoteIdentifier(columnDefinition.getName());
                insertQuery.append(insertIndex > 0 ? ", " : "").append(quotedColumn);
                valuesQuery.append(insertIndex > 0 ? ", " : "").append("?");
                insertValues.add(columnDefinition.getObject());
                insertIndex++;
                if (updateIndex > 0) {
                    onUpdateQuery.append(", ");
                }
                onUpdateQuery.append(dialect.upsertUpdateExpression(quotedColumn, false));
                if (dialect.usesUpsertUpdateParameters(false)) {
                    updateValues.add(columnDefinition.getObject());
                }
                updateIndex++;
            }
        }

        insertQuery.append(") ");
        valuesQuery.append(")");

        String upsertQuery = insertQuery + valuesQuery.toString() + dialect.upsertConflictClause(schema) + onUpdateQuery;

        String finalQuery = databaseConfiguration.replacePrefix(upsertQuery);
        if (databaseConfiguration.isDebug()) {
            logger.info("Executing SQL: " + finalQuery);
        }

        try (Connection connection = databaseConnection.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(finalQuery)) {

            int index = 1;

            // Setting values for INSERT part
            for (Object value : insertValues) {
                preparedStatement.setObject(index++, value);
            }

            if (dialect.usesUpsertUpdateParameters(false)) {
                for (Object value : updateValues) {
                    preparedStatement.setObject(index++, value);
                }
            }
            preparedStatement.executeUpdate();
            return preparedStatement.getUpdateCount();
        } catch (SQLException exception) {
            logger.info("Upsert operation failed on table: " + this.schema.getTableName() + " - " + exception.getMessage());
            throw new DatabaseException("upsert", this.schema.getTableName(), exception);
        }

    }
}
