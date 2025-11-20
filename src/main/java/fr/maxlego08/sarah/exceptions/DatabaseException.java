package fr.maxlego08.sarah.exceptions;

/**
 * Exception thrown when a database operation fails.
 * Contains information about the operation and table involved.
 *
 * @since 1.0.0
 */
public class DatabaseException extends SarahException {

    private final String tableName;
    private final String operation;

    public DatabaseException(String operation, String tableName, Throwable cause) {
        super(String.format("Database operation '%s' failed on table '%s'", operation, tableName), cause);
        this.operation = operation;
        this.tableName = tableName;
    }

    public DatabaseException(String operation, Throwable cause) {
        super(String.format("Database operation '%s' failed", operation), cause);
        this.operation = operation;
        this.tableName = null;
    }

    public String getTableName() {
        return tableName;
    }

    public String getOperation() {
        return operation;
    }
}