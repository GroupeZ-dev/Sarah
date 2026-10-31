package fr.maxlego08.sarah;

import fr.maxlego08.sarah.database.DatabaseType;
import fr.maxlego08.sarah.database.Schema;
import fr.maxlego08.sarah.database.StatementBinder;
import fr.maxlego08.sarah.exceptions.DatabaseException;
import fr.maxlego08.sarah.logger.Logger;
import fr.maxlego08.sarah.requests.InsertBatchRequest;
import fr.maxlego08.sarah.requests.UpdateBatchRequest;
import fr.maxlego08.sarah.requests.UpsertBatchRequest;
import fr.maxlego08.sarah.transaction.BoundDatabaseConnection;
import fr.maxlego08.sarah.transaction.Transaction;
import fr.maxlego08.sarah.transaction.TransactionCallback;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Consumer;

public class RequestHelper {

    private final DatabaseConnection connection;
    private final Logger logger;
    private boolean strictReads;

    public RequestHelper(DatabaseConnection connection, Logger logger) {
        this.connection = connection;
        this.logger = logger;
    }

    /**
     * Makes {@link #select}, {@link #selectAll} and {@link #count} throw a
     * {@link DatabaseException} when the query fails. By default they log the error and return
     * an empty list or 0, which a caller cannot tell apart from "no data".
     *
     * @param strictReads true to throw on failed reads
     * @return this helper
     */
    public RequestHelper setStrictReads(boolean strictReads) {
        this.strictReads = strictReads;
        return this;
    }

    /**
     * @return whether failed reads throw instead of returning an empty result
     */
    public boolean isStrictReads() {
        return this.strictReads;
    }

    /**
     * Same as {@link #upsertAndCount(String, Class, Object)}, without the row count.
     */
    public <T> void upsert(String tableName, Class<T> clazz, T data) {
        this.upsertAndCount(tableName, clazz, data);
    }

    /**
     * Same as {@link #upsertAndCount(String, Consumer)}, without the row count.
     */
    public void upsert(String tableName, Consumer<Schema> consumer) {
        this.upsertAndCount(tableName, consumer);
    }

    /**
     * Same as {@link #updateAndCount(String, Consumer)}, without the row count.
     */
    public void update(String tableName, Consumer<Schema> consumer) {
        this.updateAndCount(tableName, consumer);
    }

    /**
     * Same as {@link #insertAndGetKey(String, Class, Object)}, without the generated key.
     */
    public <T> void insert(String tableName, Class<T> clazz, T data) {
        this.insertAndGetKey(tableName, clazz, data);
    }

    /**
     * Same as {@link #insertAndGetKey(String, Consumer)}, without the generated key.
     */
    public void insert(String tableName, Consumer<Schema> consumer) {
        this.insertAndGetKey(tableName, consumer);
    }

    /**
     * Same as {@link #deleteAndCount(String, Consumer)}, without the row count.
     */
    public void delete(String tableName, Consumer<Schema> consumer) {
        this.deleteAndCount(tableName, consumer);
    }

    /**
     * Same as {@link #upsertMultipleAndCount(List)}, without the row count.
     */
    public void upsertMultiple(List<Schema> schemas) {
        this.upsertMultipleAndCount(schemas);
    }

    /**
     * Same as {@link #upsertMultipleAndCount(String, Class, List)}, without the row count.
     */
    public <T> void upsertMultiple(String tableName, Class<T> clazz, List<T> dataList) {
        this.upsertMultipleAndCount(tableName, clazz, dataList);
    }

    /**
     * Same as {@link #insertMultipleAndGetKey(List)}, without the generated key.
     */
    public void insertMultiple(List<Schema> schemas) {
        this.insertMultipleAndGetKey(schemas);
    }

    /**
     * Same as {@link #insertMultipleAndGetKey(String, Class, List)}, without the generated key.
     */
    public <T> void insertMultiple(String tableName, Class<T> clazz, List<T> dataList) {
        this.insertMultipleAndGetKey(tableName, clazz, dataList);
    }

    /**
     * Same as {@link #updateMultipleAndCount(List)}, without the row count.
     */
    public void updateMultiple(List<Schema> schemas) {
        this.updateMultipleAndCount(schemas);
    }

    /**
     * Same as {@link #updateMultipleAndCount(String, Class, List)}, without the row count.
     */
    public <T> void updateMultiple(String tableName, Class<T> clazz, List<T> dataList) {
        this.updateMultipleAndCount(tableName, clazz, dataList);
    }

    /**
     * Inserts or updates a table in the database using the given class template and data.
     * The table name is inferred from the class name.
     * The fields of the class are used to define the columns of the table.
     * The data is used to provide values for the columns.
     *
     * @param tableName the name of the table
     * @param clazz     the class template
     * @param data      the data to be inserted or updated
     * @return the number of rows affected, as reported by the driver
     */
    public <T> int upsertAndCount(String tableName, Class<T> clazz, T data) {
        return this.upsertAndCount(tableName, ConsumerConstructor.createConsumerFromTemplate(clazz, data));
    }

    /**
     * Inserts or updates a table in the database using the given schema.
     * The schema builder should have a consumer that defines the columns and values to be inserted or updated.
     *
     * @param tableName the name of the table
     * @param consumer  the consumer that defines the columns and values to be inserted or updated
     * @return the number of rows affected, as reported by the driver (MySQL reports 2 for an
     * updated row)
     */
    public int upsertAndCount(String tableName, Consumer<Schema> consumer) {
        try {
            return SchemaBuilder.upsert(tableName, consumer).execute(this.connection, this.logger);
        } catch (SQLException exception) {
            this.logger.error("Upsert operation failed on table: " + tableName + " - " + exception.getMessage());
            throw new DatabaseException("upsert", tableName, exception);
        }
    }

    /**
     * Updates a table in the database using the given schema.
     * The schema builder should have a consumer that defines the columns and values to be updated.
     *
     * @param tableName the name of the table
     * @param consumer  the consumer that defines the columns and values to be updated
     * @return the number of rows changed, which lets a conditional update (compare-and-set)
     * know whether it won
     */
    public int updateAndCount(String tableName, Consumer<Schema> consumer) {
        try {
            return SchemaBuilder.update(tableName, consumer).execute(this.connection, this.logger);
        } catch (SQLException exception) {
            this.logger.error("Update operation failed on table: " + tableName + " - " + exception.getMessage());
            throw new DatabaseException("update", tableName, exception);
        }
    }

    /**
     * Inserts a row into the table with the given name using the given class template and data.
     * The table name is inferred from the class name.
     * The fields of the class are used to define the columns of the table.
     * The data is used to provide values for the columns.
     *
     * @param tableName the name of the table
     * @param clazz     the class template
     * @param data      the data to be inserted
     * @return the generated key when the table has an auto-increment column, otherwise 0
     */
    public <T> int insertAndGetKey(String tableName, Class<T> clazz, T data) {
        return this.insertAndGetKey(tableName, ConsumerConstructor.createConsumerFromTemplate(clazz, data));
    }

    /**
     * Inserts a row into the table with the given name using the specified schema.
     * The schema builder should have a consumer that defines the columns and values to be inserted.
     *
     * @param tableName the name of the table
     * @param consumer  the consumer that defines the columns and values to be inserted
     * @return the generated key when the table has an auto-increment column, otherwise 0
     */
    public int insertAndGetKey(String tableName, Consumer<Schema> consumer) {
        try {
            return SchemaBuilder.insert(tableName, consumer).execute(this.connection, this.logger);
        } catch (SQLException exception) {
            this.logger.error("Insert operation failed on table: " + tableName + " - " + exception.getMessage());
            throw new DatabaseException("insert", tableName, exception);
        }
    }

    /**
     * Inserts a row into the specified table using the given schema and captures the result.
     * The schema builder should have a consumer that defines the columns and values to be inserted.
     * The result of the insertion is provided to the consumerResult.
     *
     * @param tableName      the name of the table
     * @param consumer       the consumer that defines the columns and values to be inserted
     * @param consumerResult the consumer that receives the result of the insertion execution
     */
    public void insert(String tableName, Consumer<Schema> consumer, Consumer<Integer> consumerResult) {
        this.insert(tableName, consumer, consumerResult, () -> {
        });
    }

    /**
     * Inserts a row into the specified table using the given schema and captures the result.
     * The schema builder should have a consumer that defines the columns and values to be inserted.
     * The result of the insertion is provided to the consumerResult.
     * If an exception is thrown, the exceptionRunnable is executed.
     *
     * @param tableName         the name of the table
     * @param consumer          the consumer that defines the columns and values to be inserted
     * @param consumerResult    the consumer that receives the result of the insertion execution
     * @param exceptionRunnable the runnable that is executed if an exception is thrown
     */
    public void insert(String tableName, Consumer<Schema> consumer, Consumer<Integer> consumerResult, Runnable exceptionRunnable) {
        try {
            consumerResult.accept(SchemaBuilder.insert(tableName, consumer).execute(this.connection, this.logger));
        } catch (SQLException exception) {
            this.logger.error("Insert operation failed on table: " + tableName + " - " + exception.getMessage());
            exceptionRunnable.run();
            throw new DatabaseException("insert", tableName, exception);
        }
    }

    /**
     * Executes a select count(*) query on the specified table using the given schema.
     * The schema builder should have a consumer that defines the columns and values to be used in the query.
     * The result of the query is provided as a long.
     *
     * @param tableName the name of the table
     * @param consumer  the consumer that defines the columns and values to be used in the query
     * @return the result of the query as a long
     * @throws DatabaseException if the query fails and {@link #setStrictReads(boolean) strict
     *                           reads} are on. Otherwise a failure is logged and 0 is returned.
     */
    public long count(String tableName, Consumer<Schema> consumer) {
        Schema schema = SchemaBuilder.selectCount(tableName);
        consumer.accept(schema);
        try {
            return schema.executeSelectCount(this.connection, this.logger);
        } catch (SQLException exception) {
            this.logger.error("Count operation failed on table: " + tableName + " - " + exception.getMessage());
            if (this.strictReads) {
                throw new DatabaseException("count", tableName, exception);
            }
            return 0L;
        }
    }

    /**
     * Executes a select query on the specified table using the given schema.
     * The schema builder should have a consumer that defines the columns and values to be used in the query.
     * The result of the query is provided as a list of objects of the given class type.
     *
     * @param tableName the name of the table
     * @param clazz     the class type of the objects in the result list
     * @param consumer  the consumer that defines the columns and values to be used in the query
     * @return the result of the query as a list of objects of the given class type
     * @throws DatabaseException if the query or the mapping fails and {@link #setStrictReads(boolean)
     *                           strict reads} are on. Otherwise a failure is logged and an empty
     *                           list is returned.
     */
    public <T> List<T> select(String tableName, Class<T> clazz, Consumer<Schema> consumer) {
        Schema schema = SchemaBuilder.select(tableName);
        consumer.accept(schema);
        try {
            return schema.executeSelect(clazz, this.connection, this.logger);
        } catch (Exception exception) {
            this.logger.error("Select operation failed on table: " + tableName + " - " + exception.getMessage());
            if (this.strictReads) {
                throw new DatabaseException("select", tableName, exception);
            }
            return new ArrayList<>();
        }
    }

    /**
     * Executes a select query on the specified table using the given schema.
     * The schema builder should have a consumer that defines the columns and values to be used in the query.
     * The result of the query is provided as a list of maps, where each map represents a row of the query result.
     * The keys of the map are the column names of the query result, and the values are the values of the columns.
     *
     * @param tableName the name of the table
     * @param consumer  the consumer that defines the columns and values to be used in the query
     * @return the result of the query as a list of maps
     * @throws DatabaseException if the query fails and strict reads are on
     */
    public List<Map<String, Object>> select(String tableName, Consumer<Schema> consumer) {
        Schema schema = SchemaBuilder.select(tableName);
        consumer.accept(schema);
        try {
            return schema.executeSelect(this.connection, this.logger);
        } catch (Exception exception) {
            this.logger.error("Select operation failed on table: " + tableName + " - " + exception.getMessage());
            if (this.strictReads) {
                throw new DatabaseException("select", tableName, exception);
            }
            return new ArrayList<>();
        }
    }

    /**
     * Executes a select query on the specified table without any conditions.
     * The result of the query is provided as a list of objects of the given class type.
     *
     * @param tableName the name of the table
     * @param clazz     the class type of the objects in the result list
     * @return the result of the query as a list of objects of the given class type
     * @throws DatabaseException if the query or the mapping fails and strict reads are on
     */
    public <T> List<T> selectAll(String tableName, Class<T> clazz) {
        Schema schema = SchemaBuilder.select(tableName);
        try {
            return schema.executeSelect(clazz, this.connection, this.logger);
        } catch (Exception exception) {
            this.logger.error("SelectAll operation failed on table: " + tableName + " - " + exception.getMessage());
            if (this.strictReads) {
                throw new DatabaseException("selectAll", tableName, exception);
            }
            return new ArrayList<>();
        }
    }

    /**
     * Deletes rows from the specified table using the given schema.
     * The schema builder should have a consumer that defines the columns and values to be used in the query.
     *
     * @param tableName the name of the table
     * @param consumer  the consumer that defines the columns and values to be used in the query
     * @return the number of rows deleted
     */
    public int deleteAndCount(String tableName, Consumer<Schema> consumer) {
        Schema schema = SchemaBuilder.delete(tableName);
        consumer.accept(schema);
        try {
            return schema.execute(this.connection, this.logger);
        } catch (SQLException exception) {
            this.logger.error("Delete operation failed on table: " + tableName + " - " + exception.getMessage());
            throw new DatabaseException("delete", tableName, exception);
        }
    }

    /**
     * Executes an upsert operation on a batch of schemas.
     * This method utilizes an UpsertBatchRequest to perform the upsert operation
     * on the provided list of schemas. Each schema in the list is upserted into
     * the associated database table, allowing for the insertion of new rows or
     * updating existing rows based on primary key constraints.
     *
     * @param schemas a list of Schema objects representing the data to be upserted
     * @return the number of rows affected, as reported by the driver
     */
    public int upsertMultipleAndCount(List<Schema> schemas) {
        UpsertBatchRequest request = new UpsertBatchRequest(schemas);
        return request.execute(this.connection, this.connection.getDatabaseConfiguration(), this.logger);
    }

    /**
     * Executes an upsert operation on a batch of DTOs.
     * This method converts each DTO to a Schema and then performs batch upsert operation,
     * allowing for the insertion of new rows or updating existing rows based on primary key constraints.
     *
     * @param tableName the name of the table
     * @param clazz     the class type of the DTOs
     * @param dataList  a list of DTO objects to be upserted
     * @param <T>       the type of the DTO
     * @return the number of rows affected, as reported by the driver
     */
    public <T> int upsertMultipleAndCount(String tableName, Class<T> clazz, List<T> dataList) {
        List<Schema> schemas = new ArrayList<>();
        for (T data : dataList) {
            Schema schema = SchemaBuilder.upsert(tableName, ConsumerConstructor.createConsumerFromTemplate(clazz, data));
            schemas.add(schema);
        }
        return this.upsertMultipleAndCount(schemas);
    }

    /**
     * Executes an insert operation on a batch of schemas.
     * This method utilizes an InsertBatchRequest to perform the insert operation
     * on the provided list of schemas. Each schema in the list is inserted into
     * the associated database table, allowing for the insertion of multiple rows
     * at once.
     *
     * @param schemas a list of Schema objects representing the data to be inserted
     * @return the first generated key when the table has an auto-increment column, otherwise the
     * number of inserted rows
     */
    public int insertMultipleAndGetKey(List<Schema> schemas) {
        InsertBatchRequest request = new InsertBatchRequest(schemas);
        return request.execute(this.connection, this.connection.getDatabaseConfiguration(), this.logger);
    }

    /**
     * Executes an insert operation on a batch of DTOs.
     * This method converts each DTO to a Schema and then performs batch insert operation,
     * allowing for the insertion of multiple rows at once.
     *
     * @param tableName the name of the table
     * @param clazz     the class type of the DTOs
     * @param dataList  a list of DTO objects to be inserted
     * @param <T>       the type of the DTO
     * @return the first generated key when the table has an auto-increment column, otherwise the
     * number of inserted rows
     */
    public <T> int insertMultipleAndGetKey(String tableName, Class<T> clazz, List<T> dataList) {
        List<Schema> schemas = new ArrayList<>();
        for (T data : dataList) {
            Schema schema = SchemaBuilder.insert(tableName, ConsumerConstructor.createConsumerFromTemplate(clazz, data));
            schemas.add(schema);
        }
        return this.insertMultipleAndGetKey(schemas);
    }

    /**
     * Executes an update operation on a batch of schemas.
     * This method utilizes an UpdateBatchRequest to perform the update operation
     * on the provided list of schemas.
     * Each schema in the list is updated in
     * the associated database table, allowing for modifications to existing rows
     * based on the specified schema definitions.
     *
     * @param schemas a list of Schema objects representing the data to be updated
     * @return the total number of rows changed
     */
    public int updateMultipleAndCount(List<Schema> schemas) {

        if (this.connection.getDatabaseConfiguration().getDatabaseType() == DatabaseType.SQLITE) {
            int total = 0;
            for (Schema schema : schemas) {
                try {
                    total += schema.execute(this.connection, this.logger);
                } catch (SQLException exception) {
                    this.logger.error("UpdateMultiple operation failed for schema: " + schema.getTableName() + " - " + exception.getMessage());
                    throw new DatabaseException("updateMultiple", schema.getTableName(), exception);
                }
            }
            return total;
        }

        UpdateBatchRequest request = new UpdateBatchRequest(schemas);
        return request.execute(this.connection, this.connection.getDatabaseConfiguration(), this.logger);
    }

    /**
     * Executes an update operation on a batch of DTOs.
     * This method converts each DTO to a Schema and then performs batch update operation,
     * allowing for modifications to existing rows based on the DTO data.
     *
     * <p>Note: For SQLite databases, updates are executed sequentially rather than in a single batch
     * due to driver limitations.</p>
     *
     * @param tableName the name of the table
     * @param clazz     the class type of the DTOs
     * @param dataList  a list of DTO objects to be updated
     * @param <T>       the type of the DTO
     * @return the total number of rows changed
     */
    public <T> int updateMultipleAndCount(String tableName, Class<T> clazz, List<T> dataList) {
        List<Schema> schemas = new ArrayList<>();
        for (T data : dataList) {
            Schema schema = SchemaBuilder.update(tableName, ConsumerConstructor.createConsumerFromTemplate(clazz, data));
            schemas.add(schema);
        }
        return this.updateMultipleAndCount(schemas);
    }

    /**
     * Runs a statement Sarah cannot build, such as {@code UPDATE … SET a = a + ? WHERE …} with
     * several conditions on the same column. {@code %prefix%} is replaced and backtick-quoted
     * identifiers are translated for the configured dialect.
     *
     * @param sql        the statement, with {@code ?} placeholders
     * @param parameters the values bound to the placeholders
     * @return the number of rows affected
     */
    public int execute(String sql, Object... parameters) {
        DatabaseConfiguration configuration = this.connection.getDatabaseConfiguration();
        String finalQuery = configuration.prepare(sql);
        if (configuration.isDebug()) {
            this.logger.info("Executing SQL: " + finalQuery);
        }
        try (Connection sqlConnection = this.connection.getConnection();
             PreparedStatement preparedStatement = sqlConnection.prepareStatement(finalQuery)) {
            bind(preparedStatement, parameters);
            return preparedStatement.executeUpdate();
        } catch (SQLException exception) {
            this.logger.error("Raw statement failed: " + finalQuery + " - " + exception.getMessage());
            throw new DatabaseException("execute", exception);
        }
    }

    /**
     * Runs a raw query and maps every row. {@code %prefix%} is replaced and backtick-quoted
     * identifiers are translated for the configured dialect.
     *
     * @param sql        the query, with {@code ?} placeholders
     * @param mapper     turns the current row into an object
     * @param parameters the values bound to the placeholders
     * @return one mapped object per row
     */
    public <T> List<T> query(String sql, RowMapper<T> mapper, Object... parameters) {
        DatabaseConfiguration configuration = this.connection.getDatabaseConfiguration();
        String finalQuery = configuration.prepare(sql);
        if (configuration.isDebug()) {
            this.logger.info("Executing SQL: " + finalQuery);
        }
        try (Connection sqlConnection = this.connection.getConnection();
             PreparedStatement preparedStatement = sqlConnection.prepareStatement(finalQuery)) {
            bind(preparedStatement, parameters);
            List<T> results = new ArrayList<>();
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    results.add(mapper.map(resultSet));
                }
            }
            return results;
        } catch (SQLException exception) {
            this.logger.error("Raw query failed: " + finalQuery + " - " + exception.getMessage());
            throw new DatabaseException("query", exception);
        }
    }

    /**
     * Runs several operations on one connection, in one transaction.
     *
     * <p>The callback receives a helper bound to that connection: every call made through it
     * is part of the transaction. It is committed when the callback returns and rolled back when
     * it throws. Calls made through this helper, rather than the one given to the callback, run
     * outside the transaction.</p>
     *
     * <pre>
     * int changed = requestHelper.inTransaction(tx -&gt; {
     *     tx.update("accounts", s -&gt; s.increment("balance", -10).where("id", from));
     *     tx.update("accounts", s -&gt; s.increment("balance", 10).where("id", to));
     *     return 2;
     * });
     * </pre>
     *
     * @param callback the operations to run
     * @return what the callback returns
     * @throws DatabaseException if the transaction cannot be started or committed, or wrapping
     *                           what the callback threw
     */
    public <T> T inTransaction(TransactionCallback<T> callback) {
        try (Transaction transaction = this.connection.beginTransaction()) {
            BoundDatabaseConnection boundConnection = new BoundDatabaseConnection(this.connection.getDatabaseConfiguration(), this.logger, transaction.getConnection());
            T result;
            try {
                result = callback.execute(new RequestHelper(boundConnection, this.logger).setStrictReads(this.strictReads));
            } catch (RuntimeException exception) {
                transaction.rollback();
                throw exception;
            } catch (Exception exception) {
                transaction.rollback();
                throw new DatabaseException("transaction", exception);
            }
            transaction.commit();
            return result;
        }
    }

    private void bind(PreparedStatement preparedStatement, Object... parameters) throws SQLException {
        for (int index = 0; index < parameters.length; index++) {
            StatementBinder.bind(preparedStatement, index + 1, parameters[index]);
        }
    }

    /**
     * Retrieves the current database connection.
     *
     * @return the active DatabaseConnection instance
     */
    public DatabaseConnection getConnection() {
        return connection;
    }
}
