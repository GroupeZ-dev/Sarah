package fr.maxlego08.sarah;

import fr.maxlego08.sarah.database.Schema;
import fr.maxlego08.sarah.logger.Logger;
import fr.maxlego08.sarah.requests.InsertBatchRequest;
import fr.maxlego08.sarah.requests.UpsertBatchRequest;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class RequestHelper {

    private final DatabaseConnection connection;
    private final Logger logger;

    public RequestHelper(DatabaseConnection connection, Logger logger) {
        this.connection = connection;
        this.logger = logger;
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
     */
    public <T> void upsert(String tableName, Class<T> clazz, T data) {
        this.upsert(tableName, ConsumerConstructor.createConsumerFromTemplate(clazz, data));
    }

    /**
     * Inserts or updates a table in the database using the given schema.
     * The schema builder should have a consumer that defines the columns and values to be inserted or updated.
     *
     * @param tableName the name of the table
     * @param consumer  the consumer that defines the columns and values to be inserted or updated
     */
    public void upsert(String tableName, Consumer<Schema> consumer) {
        try {
            SchemaBuilder.upsert(tableName, consumer).execute(this.connection, this.logger);
        } catch (SQLException exception) {
            exception.printStackTrace();
        }
    }

    /**
     * Updates a table in the database using the given class template and data.
     * The table name is inferred from the class name.
     * The fields of the class are used to define the columns of the table.
     * The data is used to provide values for the columns.
     *
     * @param tableName the name of the table
     * @param clazz     the class template
     * @param data      the data to be updated
     */
    public <T> void update(String tableName, Class<T> clazz, T data) {
        this.update(tableName, ConsumerConstructor.createConsumerFromTemplate(clazz, data));
    }

    /**
     * Updates a table in the database using the given schema.
     * The schema builder should have a consumer that defines the columns and values to be updated.
     *
     * @param tableName the name of the table
     * @param consumer  the consumer that defines the columns and values to be updated
     */
    public void update(String tableName, Consumer<Schema> consumer) {
        try {
            SchemaBuilder.update(tableName, consumer).execute(this.connection, this.logger);
        } catch (SQLException exception) {
            exception.printStackTrace();
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
     */
    public <T> void insert(String tableName, Class<T> clazz, T data) {
        this.insert(tableName, ConsumerConstructor.createConsumerFromTemplate(clazz, data));
    }

    /**
     * Inserts a row into the table with the given name using the specified schema.
     * The schema builder should have a consumer that defines the columns and values to be inserted.
     *
     * @param tableName the name of the table
     * @param consumer  the consumer that defines the columns and values to be inserted
     */
    public void insert(String tableName, Consumer<Schema> consumer) {
        insert(tableName, consumer, id -> {
        });
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
        try {
            consumerResult.accept(SchemaBuilder.insert(tableName, consumer).execute(this.connection, this.logger));
        } catch (SQLException exception) {
            exception.printStackTrace();
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
     */
    public long count(String tableName, Consumer<Schema> consumer) {
        Schema schema = SchemaBuilder.selectCount(tableName);
        consumer.accept(schema);
        try {
            return schema.executeSelectCount(this.connection, this.logger);
        } catch (SQLException exception) {
            exception.printStackTrace();
        }
        return 0L;
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
     */
    public <T> List<T> select(String tableName, Class<T> clazz, Consumer<Schema> consumer) {
        Schema schema = SchemaBuilder.select(tableName);
        consumer.accept(schema);
        try {
            return schema.executeSelect(clazz, this.connection, this.logger);
        } catch (Exception exception) {
            exception.printStackTrace();
        }
        return new ArrayList<>();
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
     */
    public List<Map<String, Object>> select(String tableName, Consumer<Schema> consumer) {
        Schema schema = SchemaBuilder.select(tableName);
        consumer.accept(schema);
        try {
            return schema.executeSelect(this.connection, this.logger);
        } catch (Exception exception) {
            exception.printStackTrace();
        }
        return new ArrayList<>();
    }

    /**
     * Executes a select query on the specified table without any conditions.
     * The result of the query is provided as a list of objects of the given class type.
     *
     * @param tableName the name of the table
     * @param clazz     the class type of the objects in the result list
     * @return the result of the query as a list of objects of the given class type
     */
    public <T> List<T> selectAll(String tableName, Class<T> clazz) {
        Schema schema = SchemaBuilder.select(tableName);
        try {
            return schema.executeSelect(clazz, this.connection, this.logger);
        } catch (Exception exception) {
            exception.printStackTrace();
        }
        return new ArrayList<>();
    }

    /**
     * Deletes rows from the specified table using the given schema.
     * The schema builder should have a consumer that defines the columns and values to be used in the query.
     * The result of the query is not returned.
     *
     * @param tableName the name of the table
     * @param consumer  the consumer that defines the columns and values to be used in the query
     */
    public void delete(String tableName, Consumer<Schema> consumer) {
        Schema schema = SchemaBuilder.delete(tableName);
        consumer.accept(schema);
        try {
            schema.execute(this.connection, this.logger);
        } catch (SQLException exception) {
            exception.printStackTrace();
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
     */
    public void upsertMultiple(List<Schema> schemas) {
        UpsertBatchRequest request = new UpsertBatchRequest(schemas);
        request.execute(this.connection, this.connection.getDatabaseConfiguration(), this.logger);
    }

    /**
     * Executes an insert operation on a batch of schemas.
     * This method utilizes an InsertBatchRequest to perform the insert operation
     * on the provided list of schemas. Each schema in the list is inserted into
     * the associated database table, allowing for the insertion of multiple rows
     * at once.
     *
     * @param schemas a list of Schema objects representing the data to be inserted
     */
    public void insertMultiple(List<Schema> schemas) {
        InsertBatchRequest request = new InsertBatchRequest(schemas);
        request.execute(this.connection, this.connection.getDatabaseConfiguration(), this.logger);
    }
}
