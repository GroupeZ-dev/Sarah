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

public class CreateRequest implements Executor {

    private final Schema schema;

    public CreateRequest(Schema schema) {
        this.schema = schema;
    }

    @Override
    public int execute(DatabaseConnection databaseConnection, DatabaseConfiguration databaseConfiguration, Logger logger) {

        SqlDialect dialect = SqlDialects.from(databaseConfiguration.getDatabaseType());
        StringBuilder createTableSQL = new StringBuilder("CREATE TABLE IF NOT EXISTS ");
        createTableSQL.append(dialect.quoteTableReference(this.schema.getTableName())).append(" (");

        List<String> columnSQLs = new ArrayList<>();
        boolean hasInlinePrimaryKey = false;

        for (ColumnDefinition column : this.schema.getColumns()) {
            columnSQLs.add(column.build(databaseConfiguration, dialect));
            // Check if this column has inline PRIMARY KEY (SQLite autoincrement)
            if (column.isAutoIncrement() && column.isPrimaryKey() &&
                databaseConfiguration.getDatabaseType() == fr.maxlego08.sarah.database.DatabaseType.SQLITE) {
                hasInlinePrimaryKey = true;
            }
        }
        createTableSQL.append(String.join(", ", columnSQLs));

        // Only add separate PRIMARY KEY clause if there's no inline PRIMARY KEY
        if (!this.schema.getPrimaryKeys().isEmpty() && !hasInlinePrimaryKey) {
            List<String> primaryKeys = new ArrayList<>();
            for (String primaryKey : this.schema.getPrimaryKeys()) {
                primaryKeys.add(dialect.quoteIdentifier(normalizeIdentifier(primaryKey)));
            }
            createTableSQL.append(", PRIMARY KEY (").append(String.join(", ", primaryKeys)).append(")");
        }

        for (ForeignKeyDefinition fk : this.schema.getForeignKeys()) {
            createTableSQL.append(", ").append(fk.render(dialect));
        }

        createTableSQL.append(")");

        createTableSQL.append(dialect.createTableSuffix());

        String finalQuery = databaseConfiguration.replacePrefix(createTableSQL.toString());
        if (databaseConfiguration.isDebug()) {
            logger.info("Executing SQL: " + finalQuery);
        }

        try (Connection connection = databaseConnection.getConnection(); PreparedStatement preparedStatement = connection.prepareStatement(finalQuery)) {
            preparedStatement.execute();
            return preparedStatement.getUpdateCount();
        } catch (SQLException exception) {
            logger.info("Create table operation failed on table: " + this.schema.getTableName() + " - " + exception.getMessage());
            throw new DatabaseException("create", this.schema.getTableName(), exception);
        }
    }

    private String normalizeIdentifier(String identifier) {
        if (identifier == null || identifier.length() < 2) {
            return identifier;
        }
        if ((identifier.startsWith("`") && identifier.endsWith("`")) || (identifier.startsWith("\"") && identifier.endsWith("\""))) {
            return identifier.substring(1, identifier.length() - 1);
        }
        return identifier;
    }
}
