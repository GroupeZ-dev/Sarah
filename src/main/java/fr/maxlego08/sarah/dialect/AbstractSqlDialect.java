package fr.maxlego08.sarah.dialect;

import fr.maxlego08.sarah.DatabaseConfiguration;
import fr.maxlego08.sarah.DatabaseConnection;
import fr.maxlego08.sarah.conditions.ColumnDefinition;
import fr.maxlego08.sarah.database.Schema;
import fr.maxlego08.sarah.exceptions.DatabaseException;
import fr.maxlego08.sarah.logger.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public abstract class AbstractSqlDialect implements SqlDialect {

    private final String quote;

    protected AbstractSqlDialect(String quote) {
        this.quote = quote;
    }

    @Override
    public String quoteIdentifier(String name) {
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("Identifier cannot be null or empty");
        }

        if (name.startsWith(quote) && name.endsWith(quote)) {
            return name;
        }

        return quote + name + quote;
    }

    @Override
    public String quoteTableReference(String tableReference) {
        if (tableReference == null) {
            throw new IllegalArgumentException("Table reference cannot be null");
        }

        String trimmed = tableReference.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("Table reference cannot be empty");
        }

        int firstWhitespace = -1;
        for (int i = 0; i < trimmed.length(); i++) {
            if (Character.isWhitespace(trimmed.charAt(i))) {
                firstWhitespace = i;
                break;
            }
        }

        String base = firstWhitespace == -1 ? trimmed : trimmed.substring(0, firstWhitespace);
        String remainder = firstWhitespace == -1 ? "" : trimmed.substring(firstWhitespace).trim();

        String quotedBase;
        int dotIndex = base.indexOf('.');
        if (dotIndex != -1) {
            String[] parts = base.split("\\.");
            List<String> quotedParts = new ArrayList<String>(parts.length);
            for (String part : parts) {
                if (part != null && !part.isEmpty()) {
                    quotedParts.add(quoteIdentifier(part));
                }
            }
            quotedBase = String.join(".", quotedParts);
        } else {
            quotedBase = quoteIdentifier(base);
        }

        return remainder.isEmpty() ? quotedBase : quotedBase + " " + remainder;
    }

    @Override
    public String qualifyIdentifier(String prefix, String column) {
        if (prefix == null || prefix.trim().isEmpty()) {
            return quoteIdentifier(column);
        }
        return prefix + "." + quoteIdentifier(column);
    }

    @Override
    public String driverClassName() {
        throw new UnsupportedOperationException("Driver class is not configured");
    }

    @Override
    public String jdbcUrl(DatabaseConfiguration configuration) {
        throw new UnsupportedOperationException("JDBC URL is not configured");
    }

    @Override
    public String createTableSuffix() {
        return "";
    }

    @Override
    public String columnType(ColumnDefinition column) {
        Objects.requireNonNull(column, "column");

        if (column.getEnumValues() != null && !column.getEnumValues().isEmpty()) {
            return enumColumnType(column);
        }

        String baseType = column.getType();
        if (baseType == null || baseType.isEmpty()) {
            baseType = "TEXT";
        }

        baseType = mapColumnType(column, baseType);

        if (useIntegerTypeForAutoIncrementPrimaryKey(column) && isIntegerType(baseType)) {
            baseType = "INTEGER";
        }

        Integer length = column.getLength();
        int decimal = getDecimal(column);
        if (length != null && length > 0 && decimal > 0) {
            return baseType + "(" + length + "," + decimal + ")";
        }
        if (length != null && length > 0) {
            return baseType + "(" + length + ")";
        }

        return baseType;
    }

    protected String mapColumnType(ColumnDefinition column, String baseType) {
        return baseType;
    }

    @Override
    public String autoIncrementKeyword(ColumnDefinition column) {
        return "";
    }

    @Override
    public String enumColumnType(ColumnDefinition column) {
        return "TEXT";
    }

    @Override
    public String updatedAtDefaultValue() {
        return "CURRENT_TIMESTAMP";
    }

    @Override
    public String upsertConflictClause(Schema schema) {
        return " ON CONFLICT (" + String.join(", ", conflictColumns(schema)) + ") DO UPDATE SET ";
    }

    @Override
    public String upsertUpdateExpression(String quotedColumn, boolean batch) {
        return quotedColumn + " = excluded." + quotedColumn;
    }

    @Override
    public boolean usesUpsertUpdateParameters(boolean batch) {
        return false;
    }

    @Override
    public List<ColumnDefinition> missingColumns(DatabaseConnection connection, Logger logger, String tableName, List<ColumnDefinition> expectedColumns) {
        Objects.requireNonNull(connection, "connection");
        Objects.requireNonNull(tableName, "tableName");
        Objects.requireNonNull(expectedColumns, "expectedColumns");

        return queryMissingColumns(
                connection,
                logger,
                "SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_NAME = ? AND TABLE_SCHEMA = ? AND COLUMN_NAME = ?",
                tableName,
                connection.getDatabaseConfiguration().getDatabase(),
                expectedColumns
        );
    }

    protected boolean useIntegerTypeForAutoIncrementPrimaryKey(ColumnDefinition column) {
        return false;
    }

    protected List<String> conflictColumns(Schema schema) {
        Objects.requireNonNull(schema, "schema");

        List<String> columns = new ArrayList<String>();
        for (String primaryKey : schema.getPrimaryKeys()) {
            ColumnDefinition column = findColumn(schema, primaryKey);
            if (column != null && !column.isAutoIncrement()) {
                columns.add(quoteIdentifier(column.getName()));
            }
        }

        if (columns.isEmpty()) {
            for (ColumnDefinition column : schema.getColumns()) {
                if (column.isUnique() && !column.isAutoIncrement()) {
                    columns.add(quoteIdentifier(column.getName()));
                }
            }
        }

        if (columns.isEmpty()) {
            throw new IllegalStateException("UPSERT requires at least one non-auto-increment primary key or unique constraint");
        }

        return columns;
    }

    protected ColumnDefinition findColumn(Schema schema, String identifier) {
        if (identifier == null) {
            return null;
        }

        String normalizedIdentifier = normalizeIdentifier(identifier);
        for (ColumnDefinition column : schema.getColumns()) {
            if (column.getName() != null && column.getName().equals(identifier)) {
                return column;
            }
            if (column.getSafeName().equals(identifier)) {
                return column;
            }
            if (column.getName() != null && normalizeIdentifier(column.getName()).equals(normalizedIdentifier)) {
                return column;
            }
        }
        return null;
    }

    protected String normalizeIdentifier(String identifier) {
        return identifier.replace("`", "").replace("\"", "");
    }

    protected boolean isIntegerType(String type) {
        String normalized = type.toUpperCase(Locale.ROOT);
        return "INT".equals(normalized) || "INTEGER".equals(normalized) || "BIGINT".equals(normalized);
    }

    protected int getDecimal(ColumnDefinition column) {
        return column.getDecimal();
    }

    protected String escapeSingleQuotes(String value) {
        return value == null ? null : value.replace("'", "''");
    }

    protected List<ColumnDefinition> queryMissingColumns(
            DatabaseConnection connection,
            Logger logger,
            String query,
            String tableName,
            String schemaName,
            List<ColumnDefinition> expectedColumns
    ) {
        List<ColumnDefinition> missing = new ArrayList<ColumnDefinition>();

        try (Connection sqlConnection = connection.getConnection()) {
            for (ColumnDefinition column : expectedColumns) {
                long count = countExistingColumn(sqlConnection, query, tableName, schemaName, column.getName());
                if (count == 0) {
                    missing.add(column);
                }
            }
        } catch (SQLException exception) {
            if (logger != null) {
                logger.info("Failed to check column metadata for table '" + tableName + "': " + exception.getMessage());
            }
            throw new DatabaseException("missing-columns", tableName, exception);
        }

        return missing;
    }

    protected long countExistingColumn(Connection connection, String query, String tableName, String schemaName, String columnName) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, tableName);
            statement.setString(2, schemaName);
            statement.setString(3, columnName);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getLong(1);
                }
            }
        }
        return 0L;
    }

    protected List<ColumnDefinition> missingColumnsFromExistingNames(List<ColumnDefinition> expectedColumns, Set<String> existingColumns, boolean normalizeToLowerCase) {
        Set<String> normalizedExistingColumns = existingColumns;
        if (normalizeToLowerCase) {
            normalizedExistingColumns = existingColumns.stream()
                    .filter(Objects::nonNull)
                    .map(value -> value.toLowerCase(Locale.ROOT))
                    .collect(Collectors.toCollection(HashSet::new));
        }

        List<ColumnDefinition> missing = new ArrayList<ColumnDefinition>();
        for (ColumnDefinition column : expectedColumns) {
            String name = column.getName();
            if (name == null) {
                missing.add(column);
                continue;
            }
            String lookup = normalizeToLowerCase ? name.toLowerCase(Locale.ROOT) : name;
            if (!normalizedExistingColumns.contains(lookup)) {
                missing.add(column);
            }
        }
        return missing;
    }
}
