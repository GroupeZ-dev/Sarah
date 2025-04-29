package fr.maxlego08.sarah.database;

import fr.maxlego08.sarah.DatabaseConnection;
import fr.maxlego08.sarah.conditions.ColumnDefinition;
import fr.maxlego08.sarah.conditions.JoinCondition;
import fr.maxlego08.sarah.conditions.SelectCondition;
import fr.maxlego08.sarah.conditions.WhereCondition;
import fr.maxlego08.sarah.logger.Logger;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Represents a schema builder for database operations.
 */
public interface Schema {

    /**
     * Creates a column of type UUID, with default values.
     *
     * @param columnName the name of the column
     * @return the current schema builder
     */
    Schema uuid(String columnName);

    /**
     * Creates a column of type UUID, with default values.
     *
     * @param columnName the name of the column
     * @param value      the default value of the column
     * @return the current schema builder
     */
    Schema uuid(String columnName, UUID value);

    /**
     * Creates a column of type VARCHAR, with the given length.
     *
     * @param columnName the name of the column
     * @param length     the length of the column
     * @return the current schema builder
     */
    Schema string(String columnName, int length);

    /**
     * Creates a column of type TEXT.
     *
     * @param columnName the name of the column
     * @return the current schema builder
     */
    Schema text(String columnName);

    /**
     * Creates a column of type LONGTEXT.
     *
     * @param columnName the name of the column
     * @return the current schema builder
     */
    Schema longText(String columnName);

    /**
     * Creates a column of type DECIMAL, with default values.
     *
     * @param columnName the name of the column
     * @return the current schema builder
     */
    Schema decimal(String columnName);

    /**
     * Creates a column of type DECIMAL, with the given length and decimal points.
     *
     * @param columnName the name of the column
     * @param length     the length of the column
     * @param decimal    the number of decimal points
     * @return the current schema builder
     */
    Schema decimal(String columnName, int length, int decimal);

    /**
     * Creates a column of type VARCHAR, with the given value.
     *
     * @param columnName the name of the column
     * @param value      the default value of the column
     * @return the current schema builder
     */
    Schema string(String columnName, String value);

    /**
     * Creates a column of type DECIMAL, with the given default value.
     *
     * @param columnName the name of the column
     * @param value      the default value of the column
     * @return the current schema builder
     */
    Schema decimal(String columnName, Number value);

    /**
     * Creates a column of type DATE with the specified default value.
     *
     * @param columnName the name of the column
     * @param value      the default date value of the column
     * @return the current schema builder
     */
    Schema date(String columnName, Date value);

    /**
     * Creates a column of type BIGINT.
     *
     * @param columnName the name of the column
     * @return the current schema builder
     */
    Schema bigInt(String columnName);

    /**
     * Creates a column of type INTEGER.
     *
     * @param columnName the name of the column
     * @return the current schema builder
     */
    Schema integer(String columnName);

    /**
     * Creates a column of type BIGINT with a specified default value.
     *
     * @param columnName the name of the column
     * @param value      the default value of the column
     * @return the current schema builder
     */
    Schema bigInt(String columnName, long value);

    /**
     * Creates a column of type BLOB with the given default value.
     * The object will be serialized to a byte array using Java's built-in serialization mechanism.
     * The column is then created as a BLOB column with the given default value.
     *
     * @param columnName the name of the column
     * @param object     the default value of the column
     * @return the current schema builder
     */
    Schema object(String columnName, Object object);

    /**
     * Creates a column of type BOOLEAN.
     *
     * @param columnName the name of the column
     * @return the current schema builder
     */
    Schema bool(String columnName);

    /**
     * Creates a column of type BOOLEAN with the given default value.
     *
     * @param columnName the name of the column
     * @param value      the default value of the column
     * @return the current schema builder
     */
    Schema bool(String columnName, boolean value);

    /**
     * Creates a column of type JSON.
     *
     * @param columnName the name of the column
     * @return the current schema builder
     */
    Schema json(String columnName);

    /**
     * Creates a column of type BLOB.
     *
     * @param columnName the name of the column
     * @return the current schema builder
     */
    Schema blob(String columnName);

    /**
     * Creates a column of type BLOB with the given default value.
     * The given default value is a byte array that is directly used as the default value of the column.
     *
     * @param columnName the name of the column
     * @param value      the default value of the column
     * @return the current schema builder
     */
    Schema blob(String columnName, byte[] value);

    /**
     * Creates a column of type BLOB with the given default value.
     * The given default value is an object that is serialized to a byte array using Java's built-in serialization mechanism.
     * The column is then created as a BLOB column with the given default value.
     *
     * @param columnName the name of the column
     * @param object     the default value of the column
     * @return the current schema builder
     */
    Schema blob(String columnName, Object object);

    /**
     * Makes the last added column the primary key of the table.
     *
     * @return the current schema builder
     */
    Schema primary();

    /**
     * Adds a foreign key constraint to the last added column, referencing a column
     * in the specified table. The referenced column is assumed to have the same name
     * as the last added column.
     *
     * @param referenceTable the name of the table that contains the referenced column
     * @return the current schema builder
     * @throws IllegalStateException if no column has been defined to apply the foreign key
     */
    Schema foreignKey(String referenceTable);

    /**
     * Adds a foreign key constraint to the last added column, referencing a column
     * in the specified table.
     *
     * @param referenceTable the name of the table that contains the referenced column
     * @param columnName     the name of the column in the reference table
     * @param onCascade      whether the foreign key should be created with the "ON DELETE CASCADE" option
     * @return the current schema builder
     * @throws IllegalStateException if no column has been defined to apply the foreign key
     */
    Schema foreignKey(String referenceTable, String columnName, boolean onCascade);

    /**
     * Creates a column named "created_at" of type TIMESTAMP with the default value CURRENT_TIMESTAMP.
     * This column is not nullable and is automatically set to the current timestamp when a new row is inserted.
     *
     * @return the current schema builder
     */
    Schema createdAt();

    /**
     * Creates a column named "updated_at" of type TIMESTAMP with the default value CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP.
     * This column is not nullable and is automatically set to the current timestamp when a row is updated.
     *
     * @return the current schema builder
     */
    Schema updatedAt();

    /**
     * Adds both a "created_at" and an "updated_at" column.
     * Equivalent to calling {@link #createdAt()} and {@link #updatedAt()}.
     *
     * @return the current schema builder
     */
    Schema timestamps();

    /**
     * Adds a column of type TIMESTAMP with the specified column name.
     * The column is added to the schema without any default value or additional attributes.
     *
     * @param columnName the name of the TIMESTAMP column to be added
     * @return the current schema builder
     */
    Schema timestamp(String columnName);

    /**
     * Adds a column of type INTEGER with auto-increment enabled.
     * The column is added to the schema with the specified name and the
     * auto-increment attribute set to true. The column is also set as the
     * primary key of the table.
     *
     * @param columnName the name of the INTEGER column to be added
     * @return the current schema builder
     */
    Schema autoIncrement(String columnName);

    /**
     * Adds a column of type BIGINT with auto-increment enabled.
     * The column is added to the schema with the specified name and the
     * auto-increment attribute set to true. The column is also set as the
     * primary key of the table.
     *
     * @param columnName the name of the BIGINT column to be added
     * @return the current schema builder
     */
    Schema autoIncrementBigInt(String columnName);

    /**
     * Makes the last column nullable.
     * {@link ColumnDefinition#setNullable(boolean) setNullable(true)} on the returned
     * column definition.
     *
     * @return the current schema builder
     */
    Schema nullable();

    /**
     * Marks the last added column as unique.
     * This ensures that all values in the column will be distinct,
     * preventing duplicate entries for that column in the table.
     *
     * @return the current schema builder
     */
    Schema unique();

    /**
     * Sets the uniqueness constraint on the last added column.
     * If the unique parameter is true, the column will be enforced to have unique values.
     * If false, the uniqueness constraint is removed.
     *
     * @param unique a boolean indicating whether the column should have unique values
     * @return the current schema builder
     */
    Schema unique(boolean unique);

    /**
     * Sets the default value for the last added column.
     * The default value can be any object, including strings, numbers, dates, etc.
     *
     * @param value the default value to be set
     * @return the current schema builder
     */
    Schema defaultValue(Object value);

    /**
     * Sets the default value for the last added column to CURRENT_TIMESTAMP.
     * The value of the column will be set to the current timestamp when a new row is inserted.
     *
     * @return the current schema builder
     */
    Schema defaultCurrentTimestamp();

    /**
     * Adds a WHERE condition to the query.
     * The WHERE condition will be {@code columnName = value}, where {@code value} is converted to a string.
     * If the value is a UUID, it will be converted to a string using the {@link UUID#toString()} method.
     * If the value is null, the WHERE condition will be {@code columnName IS NULL}.
     *
     * @param columnName the name of the column to be used in the WHERE condition
     * @param value      the value of the WHERE condition
     * @return the current schema builder
     */
    Schema where(String columnName, Object value);

    /**
     * Adds a WHERE condition to the query using a UUID value.
     * The WHERE condition will be {@code columnName = value.toString()},
     * where the UUID value is converted to a string using the {@link UUID#toString()} method.
     *
     * @param columnName the name of the column to be used in the WHERE condition
     * @param value      the UUID value of the WHERE condition
     * @return the current schema builder
     */
    Schema where(String columnName, UUID value);

    /**
     * Adds a WHERE condition to the query.
     * The WHERE condition will be {@code columnName operator value}, where {@code value} is converted to a string.
     * If the value is a UUID, it will be converted to a string using the {@link UUID#toString()} method.
     * If the value is null, the WHERE condition will be {@code columnName IS NULL}.
     *
     * @param columnName the name of the column to be used in the WHERE condition
     * @param operator   the operator to be used in the WHERE condition
     * @param value      the value of the WHERE condition
     * @return the current schema builder
     */
    Schema where(String columnName, String operator, Object value);

    /**
     * Adds a WHERE condition to the query with an optional table prefix.
     * The condition will be in the format: {@code tablePrefix.columnName operator value}.
     * If the tablePrefix is null, it will be omitted.
     *
     * @param tablePrefix the prefix of the table to be used in the WHERE condition, or null if no prefix is needed
     * @param columnName  the name of the column to be used in the WHERE condition
     * @param operator    the operator to be used in the WHERE condition
     * @param value       the value of the WHERE condition
     * @return the current schema builder
     */
    Schema where(String tablePrefix, String columnName, String operator, Object value);

    /**
     * Adds a WHERE condition to the query, which will be {@code columnName IS NOT NULL}.
     *
     * @param columnName the name of the column to be used in the WHERE condition
     * @return the current schema builder
     */
    Schema whereNotNull(String columnName);

    Schema whereNull(String columnName);

    Schema whereIn(String columnName, Object... objects);

    Schema whereIn(String columnName, List<String> strings);

    Schema whereIn(String tablePrefix, String columnName, Object... objects);

    Schema whereIn(String tablePrefix, String columnName, List<String> strings);

    /**
     * Adds a LEFT JOIN to the query.
     * The join is of the format: {@code primaryTable LEFT JOIN foreignTable ON primaryTable.primaryColumn = foreignTable.foreignColumn}.
     * The {@code primaryColumnAlias} is an optional alias for the primary column.
     * If the alias is null, the column name will be used instead.
     *
     * @param primaryTable       the primary table to be used in the join
     * @param primaryColumnAlias the alias of the primary column, or null if no alias is needed
     * @param primaryColumn      the primary column to be used in the join
     * @param foreignTable       the foreign table to be used in the join
     * @param foreignColumn      the foreign column to be used in the join
     * @return the current schema builder
     */
    Schema leftJoin(String primaryTable, String primaryColumnAlias, String primaryColumn, String foreignTable, String foreignColumn);

    /**
     * Adds a LEFT JOIN to the query with an additional AND condition.
     * The join is of the format: {@code primaryTable LEFT JOIN foreignTable ON primaryTable.primaryColumn = foreignTable.foreignColumn}.
     * The {@code primaryColumnAlias} is an optional alias for the primary column.
     * If the alias is null, the column name will be used instead.
     * An additional AND condition can be specified to further refine the join.
     *
     * @param primaryTable       the primary table to be used in the join
     * @param primaryColumnAlias the alias of the primary column, or null if no alias is needed
     * @param primaryColumn      the primary column to be used in the join
     * @param foreignTable       the foreign table to be used in the join
     * @param foreignColumn      the foreign column to be used in the join
     * @param andCondition       an additional condition to be applied to the join, or null if no additional condition is needed
     * @return the current schema builder
     */
    Schema leftJoin(String primaryTable, String primaryColumnAlias, String primaryColumn, String foreignTable, String foreignColumn, JoinCondition andCondition);

    /**
     * Adds a RIGHT JOIN to the query.
     * The join is of the format: {@code primaryTable RIGHT JOIN foreignTable ON primaryTable.primaryColumn = foreignTable.foreignColumn}.
     * The {@code primaryColumnAlias} is an optional alias for the primary column.
     * If the alias is null, the column name will be used instead.
     *
     * @param primaryTable       the primary table to be used in the join
     * @param primaryColumnAlias the alias of the primary column, or null if no alias is needed
     * @param primaryColumn      the primary column to be used in the join
     * @param foreignTable       the foreign table to be used in the join
     * @param foreignColumn      the foreign column to be used in the join
     * @return the current schema builder
     */
    Schema rightJoin(String primaryTable, String primaryColumnAlias, String primaryColumn, String foreignTable, String foreignColumn);

    /**
     * Adds an INNER JOIN to the query.
     * The join is of the format: {@code primaryTable INNER JOIN foreignTable ON primaryTable.primaryColumn = foreignTable.foreignColumn}.
     * The {@code primaryColumnAlias} is an optional alias for the primary column.
     * If the alias is null, the column name will be used instead.
     *
     * @param primaryTable       the primary table to be used in the join
     * @param primaryColumnAlias the alias of the primary column, or null if no alias is needed
     * @param primaryColumn      the primary column to be used in the join
     * @param foreignTable       the foreign table to be used in the join
     * @param foreignColumn      the foreign column to be used in the join
     * @return the current schema builder
     */
    Schema innerJoin(String primaryTable, String primaryColumnAlias, String primaryColumn, String foreignTable, String foreignColumn);

    /**
     * Adds a FULL JOIN to the query.
     * The join is of the format: {@code primaryTable FULL JOIN foreignTable ON primaryTable.primaryColumn = foreignTable.foreignColumn}.
     * The {@code primaryColumnAlias} is an optional alias for the primary column.
     * If the alias is null, the column name will be used instead.
     *
     * @param primaryTable       the primary table to be used in the join
     * @param primaryColumnAlias the alias of the primary column, or null if no alias is needed
     * @param primaryColumn      the primary column to be used in the join
     * @param foreignTable       the foreign table to be used in the join
     * @param foreignColumn      the foreign column to be used in the join
     * @return the current schema builder
     */
    Schema fullJoin(String primaryTable, String primaryColumnAlias, String primaryColumn, String foreignTable, String foreignColumn);

    /**
     * Executes the current schema operation on the provided database connection.
     *
     * @param databaseConnection the database connection to execute the operation on
     * @param logger             the logger to log any relevant information during execution
     * @return the number of affected rows as a result of the execution
     * @throws SQLException if a database access error occurs or the URL is null
     */
    int execute(DatabaseConnection databaseConnection, Logger logger) throws SQLException;

    /**
     * Executes a SELECT query on the specified database connection and retrieves the results.
     * The query is constructed based on the schema configuration and any specified conditions.
     *
     * @param databaseConnection the database connection to execute the SELECT query on
     * @param logger             the logger to log query execution details and potential errors
     * @return a list of maps, where each map represents a row in the result set with column names as keys
     * @throws SQLException if a database access error occurs or the query execution fails
     */
    List<Map<String, Object>> executeSelect(DatabaseConnection databaseConnection, Logger logger) throws SQLException;

    /**
     * Executes a SELECT COUNT query on the specified database connection and retrieves the result.
     * The query is constructed based on the schema configuration and any specified conditions.
     *
     * @param databaseConnection the database connection to execute the query on
     * @param logger             the logger to log query execution details and potential errors
     * @return the count of records in the result set
     * @throws SQLException if a database access error occurs or the query execution fails
     */
    long executeSelectCount(DatabaseConnection databaseConnection, Logger logger) throws SQLException;

    /**
     * Executes a SELECT query on the specified database connection and retrieves the results.
     * The query is constructed based on the schema configuration and any specified conditions.
     * The results are then transformed into a list of objects of the specified class.
     *
     * @param <T>                the type of objects to be retrieved
     * @param clazz              the class of the objects to be retrieved
     * @param databaseConnection the database connection to execute the query on
     * @param logger             the logger to log query execution details and potential errors
     * @return a list of objects of the specified class
     * @throws Exception if the query execution fails or the transformation of the results fails
     */
    <T> List<T> executeSelect(Class<T> clazz, DatabaseConnection databaseConnection, Logger logger) throws Exception;

    /**
     * Gets the migration associated with this schema.
     *
     * @return the migration associated with this schema
     */
    Migration getMigration();

    /**
     * Sets the migration associated with this schema.
     * The migration is associated with an ALTER or CREATE operation
     * on the database, and is used to track which version of the schema
     * is currently associated with the database.
     *
     * @param migration the migration associated with this schema
     */
    void setMigration(Migration migration);

    /**
     * Gets the name of the table associated with this schema.
     *
     * @return the name of the table associated with this schema
     */
    String getTableName();

    /**
     * Appends WHERE conditions to the provided SQL query.
     * This method iterates over all the WHERE conditions
     * stored in the schema and constructs the appropriate SQL
     * syntax, appending it to the provided StringBuilder.
     *
     * @param stringBuilder the StringBuilder to append the WHERE conditions to
     */
    void whereConditions(StringBuilder stringBuilder);

    /**
     * Applies the stored WHERE conditions to the provided PreparedStatement.
     * This method iterates over all the WHERE conditions configured in the schema,
     * and sets the corresponding values in the PreparedStatement starting from
     * the specified index.
     *
     * @param preparedStatement the PreparedStatement to apply the WHERE conditions to
     * @param index             the starting index in the PreparedStatement for setting the WHERE condition values
     * @throws SQLException if an SQL error occurs while setting the values
     */
    void applyWhereConditions(PreparedStatement preparedStatement, int index) throws SQLException;

    /**
     * Gets the list of columns defined in this schema.
     *
     * @return the list of columns defined in this schema
     */
    List<ColumnDefinition> getColumns();

    /**
     * Gets the list of primary keys defined in this schema.
     *
     * @return the list of primary keys defined in this schema
     */
    List<String> getPrimaryKeys();

    /**
     * Gets the list of foreign keys defined in this schema.
     *
     * @return the list of foreign keys defined in this schema
     */
    List<String> getForeignKeys();

    /**
     * Retrieves the list of join conditions configured in this schema.
     * These conditions define the relationships between tables
     * for JOIN operations in SQL queries.
     *
     * @return a list of JoinCondition objects representing the join conditions
     */
    List<JoinCondition> getJoinConditions();

    /**
     * Specifies the column to order the results of a query by.
     * The sort order is ascending.
     *
     * @param columnName the name of the column to order by
     */
    void orderBy(String columnName);

    /**
     * Specifies the column to order the results of a query by in descending order.
     *
     * @param columnName the name of the column to order by
     */
    void orderByDesc(String columnName);

    /**
     * Gets the ORDER BY clause for this schema.
     *
     * @return the ORDER BY clause for this schema
     */
    String getOrderBy();

    /**
     * Makes the query results of this schema distinct.
     * This has the same effect as adding the DISTINCT keyword to the SQL query.
     */
    void distinct();

    /**
     * Indicates whether the query results of this schema are distinct.
     * This is false by default and can be changed by calling the distinct() method.
     *
     * @return true if the query results are distinct, false otherwise
     */
    boolean isDistinct();

    /**
     * Adds a column to the SELECT clause of the SQL query.
     *
     * @param selectedColumn the name of the column to be selected
     */
    void addSelect(String selectedColumn);

    /**
     * Adds a column to the SELECT clause of the SQL query, with the given table prefix.
     * The column alias is the same as the column name.
     *
     * @param prefix         the prefix of the table for the column to select
     * @param selectedColumn the name of the column to be selected
     */
    void addSelect(String prefix, String selectedColumn);

    /**
     * Adds a column to the SELECT clause of the SQL query, with the given table prefix and column alias.
     *
     * @param prefix         the prefix of the table for the column to select
     * @param selectedColumn the name of the column to be selected
     * @param aliases        the alias for the column
     */
    void addSelect(String prefix, String selectedColumn, String aliases);

    /**
     * Adds a column to the SELECT clause of the SQL query, with the given table prefix, column alias, and default value.
     * The default value is used with the COALESCE SQL function to provide a fallback if the column value is null.
     *
     * @param prefix         the prefix of the table for the column to select
     * @param selectedColumn the name of the column to be selected
     * @param aliases        the alias for the column
     * @param defaultValue   the default value to use if the column value is null
     */
    void addSelect(String prefix, String selectedColumn, String aliases, Object defaultValue);

    /**
     * Gets the type of the schema.
     * This is one of SchemaType#INSERT, SchemaType#UPDATE, SchemaType#DELETE, or SchemaType#RENAME.
     *
     * @return the type of the schema
     */
    SchemaType getSchemaType();

    /**
     * Adds a column to the schema.
     *
     * @param column the column definition to add
     * @return this, for method chaining
     */
    Schema addColumn(ColumnDefinition column);

    /**
     * Gets the list of conditions for the WHERE clause of the SQL query.
     *
     * @return the list of WHERE conditions
     */
    List<WhereCondition> getWhereConditions();

    /**
     * Gets the list of SELECT conditions for the SQL query.
     *
     * @return the list of SELECT conditions
     */
    List<SelectCondition> getSelectColumns();

    /**
     * Gets the new table name for the schema.
     * This is only set if the schema is of type SchemaType#RENAME.
     *
     * @return the new table name, or null if not set
     */
    String getNewTableName();
}