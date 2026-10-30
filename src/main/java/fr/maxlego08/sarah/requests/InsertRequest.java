package fr.maxlego08.sarah.requests;

import fr.maxlego08.sarah.DatabaseConfiguration;
import fr.maxlego08.sarah.DatabaseConnection;
import fr.maxlego08.sarah.conditions.ColumnDefinition;
import fr.maxlego08.sarah.database.Executor;
import fr.maxlego08.sarah.database.Schema;
import fr.maxlego08.sarah.database.StatementBinder;
import fr.maxlego08.sarah.exceptions.DatabaseException;
import fr.maxlego08.sarah.logger.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class InsertRequest implements Executor {

    private final Schema schema;

    public InsertRequest(Schema schema) {
        this.schema = schema;
    }

    /**
     * @return the generated key when the table has an auto-increment column, otherwise 0
     */
    @Override
    public int execute(DatabaseConnection databaseConnection, DatabaseConfiguration databaseConfiguration, Logger logger) {

        List<String> columns = new ArrayList<>();
        List<String> placeholders = new ArrayList<>();
        List<Object> values = new ArrayList<>();

        for (ColumnDefinition columnDefinition : this.schema.getColumns()) {
            if (columnDefinition.isAutoIncrement()) {
                continue;
            }
            columns.add(columnDefinition.getSafeName());
            placeholders.add(columnDefinition.insertPlaceholder(values));
        }

        String insertQuery = databaseConfiguration.prepare("INSERT INTO " + this.schema.getTableName() + " (" + String.join(", ", columns) + ") VALUES (" + String.join(", ", placeholders) + ")");

        if (databaseConfiguration.isDebug()) {
            logger.info("Executing SQL: " + insertQuery);
        }

        try (Connection connection = databaseConnection.getConnection();
             PreparedStatement preparedStatement = databaseConfiguration.getDialect().prepareInsert(connection, insertQuery, this.schema)) {

            for (int i = 0; i < values.size(); i++) {
                StatementBinder.bind(preparedStatement, i + 1, values.get(i));
            }

            preparedStatement.executeUpdate();
            return readGeneratedKey(preparedStatement);
        } catch (SQLException exception) {
            logger.info("Insert operation failed on table: " + this.schema.getTableName() + " - " + exception.getMessage());
            throw new DatabaseException("insert", this.schema.getTableName(), exception);
        }
    }

    /**
     * Reads the first generated key. The row is already written at this point, so a driver that
     * cannot report keys for this table must not turn a successful insert into a failure.
     *
     * <p>PostgreSQL may return the whole row: the key is then the {@code id} column, or the first
     * whole-number column.</p>
     */
    static int readGeneratedKey(PreparedStatement preparedStatement) {
        try (ResultSet generatedKeys = preparedStatement.getGeneratedKeys()) {
            if (generatedKeys == null || !generatedKeys.next()) {
                return 0;
            }
            ResultSetMetaData metaData = generatedKeys.getMetaData();
            if (metaData.getColumnCount() == 1) {
                return generatedKeys.getInt(1);
            }
            for (int column = 1; column <= metaData.getColumnCount(); column++) {
                if (metaData.getColumnName(column).equalsIgnoreCase("id")) {
                    return generatedKeys.getInt(column);
                }
            }
            for (int column = 1; column <= metaData.getColumnCount(); column++) {
                Object value = generatedKeys.getObject(column);
                if (value instanceof Integer || value instanceof Long || value instanceof Short) {
                    return ((Number) value).intValue();
                }
            }
        } catch (SQLException ignored) {
        }
        return 0;
    }
}
