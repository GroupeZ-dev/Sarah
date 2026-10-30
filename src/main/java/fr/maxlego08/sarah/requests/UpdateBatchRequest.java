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
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class UpdateBatchRequest implements Executor {

    private final List<Schema> schemas;

    public UpdateBatchRequest(List<Schema> schemas) {
        this.schemas = schemas;
    }

    /**
     * Runs the same update for every schema as one JDBC batch. The statement is built from the
     * first schema, so every schema must declare the same columns and conditions.
     *
     * <p>The batch runs in its own transaction, unless the connection already belongs to one
     * (see {@code RequestHelper#inTransaction}), in which case committing is left to it.</p>
     *
     * @return the total number of rows changed. Drivers that run the batch in one round trip
     * (MariaDB) only report that each statement succeeded; each counts as one row then.
     */
    @Override
    public int execute(DatabaseConnection databaseConnection, DatabaseConfiguration databaseConfiguration, Logger logger) {

        if (schemas.isEmpty()) return 0;

        Schema firstSchema = schemas.get(0);
        String updateSql = databaseConfiguration.prepare(UpdateRequest.buildQuery(firstSchema, new ArrayList<>()));

        if (databaseConfiguration.isDebug()) {
            logger.info("Executing SQL Batch: " + updateSql);
        }

        Connection connection = null;
        PreparedStatement preparedStatement = null;
        boolean ownsTransaction = false;

        try {
            connection = databaseConnection.getConnection();
            ownsTransaction = connection.getAutoCommit();
            if (ownsTransaction) {
                connection.setAutoCommit(false);
            }

            preparedStatement = connection.prepareStatement(updateSql);

            for (Schema schema : schemas) {
                List<Object> values = new ArrayList<>();
                for (ColumnDefinition column : schema.getColumns()) {
                    column.updateAssignment(values);
                }
                for (int i = 0; i < values.size(); i++) {
                    StatementBinder.bind(preparedStatement, i + 1, values.get(i));
                }
                schema.applyWhereConditions(preparedStatement, values.size() + 1);
                preparedStatement.addBatch();
            }

            int[] results = preparedStatement.executeBatch();
            if (ownsTransaction) {
                connection.commit();
            }

            int total = 0;
            for (int count : results) {
                if (count == Statement.SUCCESS_NO_INFO) total += 1;
                else total += Math.max(count, 0);
            }
            return total;

        } catch (SQLException exception) {
            if (connection != null && ownsTransaction) {
                try {
                    connection.rollback();
                } catch (SQLException rollbackException) {
                    logger.info("Rollback failed: " + rollbackException.getMessage());
                }
            }
            logger.info("Update batch operation failed on table: " + firstSchema.getTableName() + " - " + exception.getMessage());
            throw new DatabaseException("updateBatch", firstSchema.getTableName(), exception);
        } finally {
            if (preparedStatement != null) {
                try {
                    preparedStatement.close();
                } catch (SQLException ignored) {
                }
            }
            if (connection != null) {
                try {
                    if (ownsTransaction) {
                        connection.setAutoCommit(true);
                    }
                    connection.close();
                } catch (SQLException ignored) {
                }
            }
        }
    }
}
