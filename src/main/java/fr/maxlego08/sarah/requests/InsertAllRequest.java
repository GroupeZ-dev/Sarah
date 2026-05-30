package fr.maxlego08.sarah.requests;

import fr.maxlego08.sarah.DatabaseConfiguration;
import fr.maxlego08.sarah.DatabaseConnection;
import fr.maxlego08.sarah.conditions.ColumnDefinition;
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

public class InsertAllRequest implements Executor {

    private final Schema schema;
    private final String toTableName;

    public InsertAllRequest(Schema schema, String toTableName) {
        this.schema = schema;
        this.toTableName = toTableName;
    }

    @Override
    public int execute(DatabaseConnection databaseConnection, DatabaseConfiguration databaseConfiguration, Logger logger) {
        SqlDialect dialect = SqlDialects.from(databaseConfiguration.getDatabaseType());

        StringBuilder insertBuilder = new StringBuilder("INSERT INTO ")
                .append(dialect.quoteTableReference(this.toTableName))
                .append(" (");
        List<String> quotedColumns = new ArrayList<String>();

        for (ColumnDefinition columnDefinition : this.schema.getColumns()) {
            if (columnDefinition.isAutoIncrement()) {
                continue;
            }
            quotedColumns.add(dialect.quoteIdentifier(columnDefinition.getName()));
        }

        String columnsSql = String.join(", ", quotedColumns);
        insertBuilder.append(columnsSql).append(") ");
        insertBuilder.append("SELECT ").append(columnsSql);
        insertBuilder.append(" FROM ").append(dialect.quoteTableReference(this.schema.getTableName()));

        String insertQuery = databaseConfiguration.replacePrefix(insertBuilder.toString());

        if (databaseConfiguration.isDebug()) {
            logger.info("Executing SQL: " + insertQuery);
        }

        try (Connection connection = databaseConnection.getConnection(); PreparedStatement preparedStatement = connection.prepareStatement(insertQuery)) {
            preparedStatement.executeUpdate();
        } catch (SQLException exception) {
            logger.info("Insert all operation failed from table: " + this.schema.getTableName() + " to table: " + this.toTableName + " - " + exception.getMessage());
            throw new DatabaseException("insertAll", this.toTableName, exception);
        }

        return 0;
    }
}
