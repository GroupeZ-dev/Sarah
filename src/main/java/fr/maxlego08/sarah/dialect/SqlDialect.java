package fr.maxlego08.sarah.dialect;

import fr.maxlego08.sarah.DatabaseConfiguration;
import fr.maxlego08.sarah.DatabaseConnection;
import fr.maxlego08.sarah.conditions.ColumnDefinition;
import fr.maxlego08.sarah.database.Schema;
import fr.maxlego08.sarah.logger.Logger;

import java.util.List;

public interface SqlDialect {

    String quoteIdentifier(String name);

    /**
     * Quotes a table reference, supporting schema-qualified names and optional aliases.
     * <p>
     * Examples:
     * <ul>
     *     <li>{@code users} -&gt; {@code "users"}</li>
     *     <li>{@code main.users} -&gt; {@code "main"."users"}</li>
     *     <li>{@code users u} -&gt; {@code "users" u}</li>
     * </ul>
     *
     * @param tableReference The raw table reference as provided by callers
     * @return The quoted table reference
     */
    String quoteTableReference(String tableReference);

    String qualifyIdentifier(String prefix, String column);

    String driverClassName();

    String jdbcUrl(DatabaseConfiguration configuration);

    String createTableSuffix();

    String columnType(ColumnDefinition column);

    String autoIncrementKeyword(ColumnDefinition column);

    String enumColumnType(ColumnDefinition column);

    String updatedAtDefaultValue();

    String upsertConflictClause(Schema schema);

    String upsertUpdateExpression(String quotedColumn, boolean batch);

    boolean usesUpsertUpdateParameters(boolean batch);

    List<ColumnDefinition> missingColumns(DatabaseConnection connection, Logger logger, String tableName, List<ColumnDefinition> expectedColumns);
}
