package fr.maxlego08.sarah.dialect;

import fr.maxlego08.sarah.DatabaseConfiguration;
import fr.maxlego08.sarah.DatabaseConnection;
import fr.maxlego08.sarah.conditions.ColumnDefinition;
import fr.maxlego08.sarah.exceptions.DatabaseException;
import fr.maxlego08.sarah.logger.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class SqliteDialect extends AbstractSqlDialect {

    public SqliteDialect() {
        super("`");
    }

    @Override
    public String driverClassName() {
        return "org.sqlite.JDBC";
    }

    @Override
    public String jdbcUrl(DatabaseConfiguration configuration) {
        throw new UnsupportedOperationException("SQLite JDBC URL is file-based and resolved by SqliteConnection");
    }

    @Override
    public String autoIncrementKeyword(ColumnDefinition column) {
        return "";
    }

    @Override
    protected boolean useIntegerTypeForAutoIncrementPrimaryKey(ColumnDefinition column) {
        return column != null && column.isAutoIncrement();
    }

    @Override
    public List<ColumnDefinition> missingColumns(DatabaseConnection connection, Logger logger, String tableName, List<ColumnDefinition> expectedColumns) {
        Objects.requireNonNull(connection, "connection");
        Objects.requireNonNull(tableName, "tableName");
        Objects.requireNonNull(expectedColumns, "expectedColumns");

        Set<String> existingColumns = new HashSet<String>();

        String query = String.format("PRAGMA table_info(%s)", tableName);
        try (Connection sqlConnection = connection.getConnection();
             PreparedStatement statement = sqlConnection.prepareStatement(query);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                existingColumns.add(resultSet.getString("name"));
            }
        } catch (SQLException exception) {
            if (logger != null) {
                logger.info("Failed to check SQLite table info for '" + tableName + "': " + exception.getMessage());
            }
            throw new DatabaseException("missing-columns", tableName, exception);
        }

        return missingColumnsFromExistingNames(expectedColumns, existingColumns, false);
    }
}
