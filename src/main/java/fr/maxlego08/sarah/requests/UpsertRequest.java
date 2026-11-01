package fr.maxlego08.sarah.requests;

import fr.maxlego08.sarah.DatabaseConfiguration;
import fr.maxlego08.sarah.DatabaseConnection;
import fr.maxlego08.sarah.conditions.ColumnDefinition;
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

public class UpsertRequest implements Executor {

    private final Schema schema;

    public UpsertRequest(Schema schema) {
        this.schema = schema;
    }

    /**
     * Builds the conflict part of an upsert: {@code ON CONFLICT (…) DO UPDATE SET …} or
     * {@code ON DUPLICATE KEY UPDATE …}.
     *
     * @param schema     the schema that defines the columns
     * @param dialect    the dialect of the statement
     * @param parameters receives the parameters of the assignments, in order
     * @param batch      whether the statement inserts several rows
     */
    static String conflictClause(Schema schema, Dialect dialect, List<Object> parameters, boolean batch) {
        List<String> assignments = new ArrayList<>();
        String firstInsertedColumn = null;
        for (ColumnDefinition column : schema.getColumns()) {
            if (column.isAutoIncrement()) {
                continue;
            }
            if (firstInsertedColumn == null && column.isInsertedByUpsert()) {
                firstInsertedColumn = column.getSafeName();
            }
            String assignment = column.conflictAssignment(dialect, schema.getTableName(), parameters, batch);
            if (assignment != null) {
                assignments.add(assignment);
            }
        }

        if (dialect.usesOnConflict()) {
            String target = " ON CONFLICT (" + String.join(", ", conflictColumns(schema)) + ")";
            return assignments.isEmpty() ? target + " DO NOTHING" : target + " DO UPDATE SET " + String.join(", ", assignments);
        }

        if (assignments.isEmpty()) {
            return " ON DUPLICATE KEY UPDATE " + firstInsertedColumn + " = " + firstInsertedColumn;
        }
        return " ON DUPLICATE KEY UPDATE " + String.join(", ", assignments);
    }

    /**
     * The columns an {@code ON CONFLICT} clause targets: the primary key without its
     * auto-increment part, or the unique columns when there is no such key.
     */
    static List<String> conflictColumns(Schema schema) {
        List<String> conflictColumns = new ArrayList<>();

        for (String primaryKey : schema.getPrimaryKeys()) {
            boolean isAutoIncrement = false;
            for (ColumnDefinition col : schema.getColumns()) {
                if (col.getSafeName().equals(primaryKey) && col.isAutoIncrement()) {
                    isAutoIncrement = true;
                    break;
                }
            }
            if (!isAutoIncrement) {
                conflictColumns.add(primaryKey);
            }
        }

        if (conflictColumns.isEmpty()) {
            for (ColumnDefinition col : schema.getColumns()) {
                if (col.isUnique() && !col.isAutoIncrement()) {
                    conflictColumns.add(col.getSafeName());
                }
            }
        }

        if (conflictColumns.isEmpty()) {
            throw new IllegalStateException("UPSERT requires at least one non-auto-increment primary key or unique constraint for SQLite and PostgreSQL");
        }

        return conflictColumns;
    }

    /**
     * @return the number of rows affected, as reported by the driver. MySQL reports 1 for an
     * inserted row and 2 for an updated one.
     */
    @Override
    public int execute(DatabaseConnection databaseConnection, DatabaseConfiguration databaseConfiguration, Logger logger) {

        Dialect dialect = databaseConfiguration.getDialect();

        List<String> columns = new ArrayList<>();
        List<String> placeholders = new ArrayList<>();
        List<Object> values = new ArrayList<>();

        for (ColumnDefinition columnDefinition : this.schema.getColumns()) {
            if (columnDefinition.isInsertedByUpsert()) {
                columns.add(columnDefinition.getSafeName());
                placeholders.add(columnDefinition.insertPlaceholder(values));
            }
        }

        String upsertQuery = "INSERT INTO " + this.schema.getTableName() + " (" + String.join(", ", columns) + ") VALUES (" + String.join(", ", placeholders) + ")"
                + conflictClause(this.schema, dialect, values, false);

        String finalQuery = databaseConfiguration.prepare(upsertQuery);

        if (databaseConfiguration.isDebug()) {
            logger.info("Executing SQL: " + finalQuery);
        }

        try (Connection connection = databaseConnection.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(finalQuery)) {

            int index = 1;
            for (Object value : values) {
                StatementBinder.bind(preparedStatement, index++, value);
            }

            // getUpdateCount() n'est PAS fiable apres executeUpdate() : le pilote SQLite
            // (org.xerial:sqlite-jdbc) rend 0 alors que executeUpdate() a bien rendu le nombre
            // de lignes modifiees. Toute ecriture compare-and-set batie sur ce compte rendu
            // croyait donc avoir perdu la course a chaque fois, sur toutes les bases SQLite.
            return preparedStatement.executeUpdate();
        } catch (SQLException exception) {
            logger.error("Upsert operation failed on table: " + this.schema.getTableName() + " - " + exception.getMessage());
            throw new DatabaseException("upsert", this.schema.getTableName(), exception);
        }
    }
}
