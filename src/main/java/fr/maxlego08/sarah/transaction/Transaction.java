package fr.maxlego08.sarah.transaction;

import fr.maxlego08.sarah.exceptions.DatabaseException;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Represents a database transaction that groups multiple operations.
 * Provides commit and rollback capabilities.
 *
 */
public class Transaction implements AutoCloseable {

    private final Connection connection;
    private boolean committed = false;
    private boolean rolledBack = false;

    public Transaction(Connection connection) throws SQLException {
        this.connection = connection;
        this.connection.setAutoCommit(false);
    }

    /**
     * Gets the underlying database connection.
     *
     * @return the connection
     */
    public Connection getConnection() {
        return connection;
    }

    /**
     * Commits the transaction, making all changes permanent.
     *
     * @throws DatabaseException if the commit fails
     */
    public void commit() {
        if (committed || rolledBack) {
            throw new IllegalStateException("Transaction already " + (committed ? "committed" : "rolled back"));
        }
        try {
            connection.commit();
            committed = true;
        } catch (SQLException exception) {
            throw new DatabaseException("commit", exception);
        }
    }

    /**
     * Rolls back the transaction, undoing all changes.
     *
     * @throws DatabaseException if the rollback fails
     */
    public void rollback() {
        if (committed || rolledBack) {
            throw new IllegalStateException("Transaction already " + (committed ? "committed" : "rolled back"));
        }
        try {
            connection.rollback();
            rolledBack = true;
        } catch (SQLException exception) {
            throw new DatabaseException("rollback", exception);
        }
    }

    /**
     * Automatically rolls back the transaction if it hasn't been committed.
     * This is called when using try-with-resources.
     */
    @Override
    public void close() {
        try {
            if (!committed && !rolledBack) {
                connection.rollback();
            }
            connection.setAutoCommit(true);
        } catch (SQLException exception) {
            throw new DatabaseException("close-transaction", exception);
        }
    }

    /**
     * Checks if the transaction has been committed.
     *
     * @return true if committed, false otherwise
     */
    public boolean isCommitted() {
        return committed;
    }

    /**
     * Checks if the transaction has been rolled back.
     *
     * @return true if rolled back, false otherwise
     */
    public boolean isRolledBack() {
        return rolledBack;
    }
}