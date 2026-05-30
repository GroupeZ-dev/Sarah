package fr.maxlego08.sarah.requests;

import fr.maxlego08.sarah.DatabaseConfiguration;
import fr.maxlego08.sarah.DatabaseConnection;
import fr.maxlego08.sarah.conditions.ColumnDefinition;
import fr.maxlego08.sarah.conditions.ForeignKeyDefinition;
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

public class AlterRequest implements Executor {

    private final Schema schema;

    public AlterRequest(Schema schema) {
        this.schema = schema;
    }

    @Override
    public int execute(DatabaseConnection databaseConnection, DatabaseConfiguration databaseConfiguration, Logger logger) {
        SqlDialect dialect = SqlDialects.from(databaseConfiguration.getDatabaseType());

        StringBuilder alterTableSQL = new StringBuilder("ALTER TABLE ");
        alterTableSQL.append(dialect.quoteTableReference(this.schema.getTableName())).append(" ");

        List<String> columnSQLs = new ArrayList<>();
        for (ColumnDefinition column : this.schema.getColumns()) {
            columnSQLs.add("ADD COLUMN " + column.build(databaseConfiguration, dialect));
        }
        alterTableSQL.append(String.join(", ", columnSQLs));

        if (!this.schema.getPrimaryKeys().isEmpty()) {
            List<String> primaryKeys = new ArrayList<String>();
            for (String primaryKey : this.schema.getPrimaryKeys()) {
                primaryKeys.add(dialect.quoteIdentifier(stripWrappingQuotes(primaryKey)));
            }
            alterTableSQL.append(", PRIMARY KEY (").append(String.join(", ", primaryKeys)).append(")");
        }

        for (ForeignKeyDefinition foreignKey : this.schema.getForeignKeys()) {
            alterTableSQL.append(", ADD ").append(foreignKey.render(dialect));
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

    private boolean isQuoted(String identifier) {
        return (identifier.startsWith("`") && identifier.endsWith("`")) ||
                (identifier.startsWith("\"") && identifier.endsWith("\""));
    }

    private String stripWrappingQuotes(String identifier) {
        if (identifier == null || identifier.length() < 2) {
            return identifier;
        }
        if (isQuoted(identifier)) {
            return identifier.substring(1, identifier.length() - 1);
        }
        return identifier;
    }
}
