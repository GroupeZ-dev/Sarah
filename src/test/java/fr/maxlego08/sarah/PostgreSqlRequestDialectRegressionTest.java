package fr.maxlego08.sarah;

import fr.maxlego08.sarah.database.Schema;
import fr.maxlego08.sarah.logger.Logger;
import fr.maxlego08.sarah.requests.DeleteRequest;
import fr.maxlego08.sarah.requests.InsertAllRequest;
import fr.maxlego08.sarah.requests.InsertBatchRequest;
import fr.maxlego08.sarah.requests.InsertRequest;
import fr.maxlego08.sarah.requests.UpdateBatchRequest;
import fr.maxlego08.sarah.requests.UpdateRequest;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class PostgreSqlRequestDialectRegressionTest {

    private final Logger logger = message -> {
    };

    @Test
    public void testDeleteRequestUsesPostgreSqlWhereQuoting() throws Exception {
        DatabaseConfiguration configuration = DatabaseConfiguration.createPostgreSql("u", "p", 5432, "localhost", "db");
        DatabaseConnection databaseConnection = mock(DatabaseConnection.class);
        Connection sqlConnection = mock(Connection.class);
        PreparedStatement preparedStatement = mock(PreparedStatement.class);
        AtomicReference<String> capturedSql = new AtomicReference<String>();

        when(databaseConnection.getConnection()).thenReturn(sqlConnection);
        when(sqlConnection.prepareStatement(anyString())).thenAnswer(invocation -> {
            capturedSql.set(invocation.getArgument(0));
            return preparedStatement;
        });
        when(preparedStatement.executeUpdate()).thenReturn(1);

        Schema schema = SchemaBuilder.delete("users");
        schema.where("email", "sarah@example.com");

        new DeleteRequest(schema).execute(databaseConnection, configuration, logger);

        assertEquals("DELETE FROM \"users\" WHERE \"email\" = ?", capturedSql.get());
        verify(preparedStatement).setObject(1, "sarah@example.com");
    }

    @Test
    public void testDeleteRequestQuotesAliasWithoutQuotingWhitespace() throws Exception {
        DatabaseConfiguration configuration = DatabaseConfiguration.createPostgreSql("u", "p", 5432, "localhost", "db");
        DatabaseConnection databaseConnection = mock(DatabaseConnection.class);
        Connection sqlConnection = mock(Connection.class);
        PreparedStatement preparedStatement = mock(PreparedStatement.class);
        AtomicReference<String> capturedSql = new AtomicReference<String>();

        when(databaseConnection.getConnection()).thenReturn(sqlConnection);
        when(sqlConnection.prepareStatement(anyString())).thenAnswer(invocation -> {
            capturedSql.set(invocation.getArgument(0));
            return preparedStatement;
        });
        when(preparedStatement.executeUpdate()).thenReturn(1);

        Schema schema = SchemaBuilder.delete("users u");
        schema.where("email", "sarah@example.com");

        new DeleteRequest(schema).execute(databaseConnection, configuration, logger);

        assertEquals("DELETE FROM \"users\" u WHERE \"email\" = ?", capturedSql.get());
    }

    @Test
    public void testUpdateRequestUsesPostgreSqlWhereQuoting() throws Exception {
        DatabaseConfiguration configuration = DatabaseConfiguration.createPostgreSql("u", "p", 5432, "localhost", "db");
        DatabaseConnection databaseConnection = mock(DatabaseConnection.class);
        Connection sqlConnection = mock(Connection.class);
        PreparedStatement preparedStatement = mock(PreparedStatement.class);
        AtomicReference<String> capturedSql = new AtomicReference<String>();

        when(databaseConnection.getConnection()).thenReturn(sqlConnection);
        when(sqlConnection.prepareStatement(anyString())).thenAnswer(invocation -> {
            capturedSql.set(invocation.getArgument(0));
            return preparedStatement;
        });
        when(preparedStatement.executeUpdate()).thenReturn(1);

        Schema schema = SchemaBuilder.update("users", builder -> {
            builder.string("name", "Sarah");
            builder.where("email", "sarah@example.com");
        });

        new UpdateRequest(schema).execute(databaseConnection, configuration, logger);

        assertEquals("UPDATE \"users\" SET \"name\" = ? WHERE \"email\" = ?", capturedSql.get());
        verify(preparedStatement).setObject(1, "Sarah");
        verify(preparedStatement).setObject(2, "sarah@example.com");
    }

    @Test
    public void testInsertBatchRequestUsesPostgreSqlIdentifierQuoting() throws Exception {
        DatabaseConfiguration configuration = DatabaseConfiguration.createPostgreSql("u", "p", 5432, "localhost", "db");
        DatabaseConnection databaseConnection = mock(DatabaseConnection.class);
        Connection sqlConnection = mock(Connection.class);
        PreparedStatement preparedStatement = mock(PreparedStatement.class);
        ResultSet generatedKeys = mock(ResultSet.class);
        AtomicReference<String> capturedSql = new AtomicReference<String>();

        when(databaseConnection.getConnection()).thenReturn(sqlConnection);
        when(sqlConnection.prepareStatement(anyString(), anyInt())).thenAnswer(invocation -> {
            capturedSql.set(invocation.getArgument(0));
            return preparedStatement;
        });
        when(preparedStatement.executeUpdate()).thenReturn(2);
        when(preparedStatement.getGeneratedKeys()).thenReturn(generatedKeys);
        when(generatedKeys.next()).thenReturn(false);

        Schema first = SchemaBuilder.insert("users", builder -> builder.string("name", "Sarah"));
        Schema second = SchemaBuilder.insert("users", builder -> builder.string("name", "Max"));

        new InsertBatchRequest(Arrays.asList(first, second)).execute(databaseConnection, configuration, logger);

        assertEquals("INSERT INTO \"users\" (\"name\") VALUES (?), (?)", capturedSql.get());
    }

    @Test
    public void testInsertRequestQuotesSchemaQualifiedTableName() throws Exception {
        DatabaseConfiguration configuration = DatabaseConfiguration.createPostgreSql("u", "p", 5432, "localhost", "db");
        DatabaseConnection databaseConnection = mock(DatabaseConnection.class);
        Connection sqlConnection = mock(Connection.class);
        PreparedStatement preparedStatement = mock(PreparedStatement.class);
        ResultSet generatedKeys = mock(ResultSet.class);
        AtomicReference<String> capturedSql = new AtomicReference<String>();

        when(databaseConnection.getConnection()).thenReturn(sqlConnection);
        when(sqlConnection.prepareStatement(anyString(), anyInt())).thenAnswer(invocation -> {
            capturedSql.set(invocation.getArgument(0));
            return preparedStatement;
        });
        when(preparedStatement.executeUpdate()).thenReturn(1);
        when(preparedStatement.getGeneratedKeys()).thenReturn(generatedKeys);
        when(generatedKeys.next()).thenReturn(false);

        Schema schema = SchemaBuilder.insert("main.users", builder -> builder.string("name", "Sarah"));
        new InsertRequest(schema).execute(databaseConnection, configuration, logger);

        assertEquals("INSERT INTO \"main\".\"users\" (\"name\") VALUES (?)", capturedSql.get());
    }

    @Test
    public void testInsertAllRequestUsesPostgreSqlIdentifierQuoting() throws Exception {
        DatabaseConfiguration configuration = DatabaseConfiguration.createPostgreSql("u", "p", 5432, "localhost", "db");
        DatabaseConnection databaseConnection = mock(DatabaseConnection.class);
        Connection sqlConnection = mock(Connection.class);
        PreparedStatement preparedStatement = mock(PreparedStatement.class);
        AtomicReference<String> capturedSql = new AtomicReference<String>();

        when(databaseConnection.getConnection()).thenReturn(sqlConnection);
        when(sqlConnection.prepareStatement(anyString())).thenAnswer(invocation -> {
            capturedSql.set(invocation.getArgument(0));
            return preparedStatement;
        });
        when(preparedStatement.executeUpdate()).thenReturn(1);

        Schema source = SchemaBuilder.create(null, "users", builder -> builder.string("name", 32));
        new InsertAllRequest(source, "users_tmp").execute(databaseConnection, configuration, logger);

        assertEquals("INSERT INTO \"users_tmp\" (\"name\") SELECT \"name\" FROM \"users\"", capturedSql.get());
    }

    @Test
    public void testUpdateBatchRequestUsesPostgreSqlWhereAndColumnQuoting() throws Exception {
        DatabaseConfiguration configuration = DatabaseConfiguration.createPostgreSql("u", "p", 5432, "localhost", "db");
        DatabaseConnection databaseConnection = mock(DatabaseConnection.class);
        Connection sqlConnection = mock(Connection.class);
        PreparedStatement preparedStatement = mock(PreparedStatement.class);
        AtomicReference<String> capturedSql = new AtomicReference<String>();

        when(databaseConnection.getConnection()).thenReturn(sqlConnection);
        when(sqlConnection.prepareStatement(anyString())).thenAnswer(invocation -> {
            capturedSql.set(invocation.getArgument(0));
            return preparedStatement;
        });
        when(sqlConnection.getAutoCommit()).thenReturn(true);
        when(preparedStatement.executeBatch()).thenReturn(new int[]{1, 1});

        Schema first = SchemaBuilder.update("users", builder -> {
            builder.string("name", "Sarah");
            builder.where("email", "sarah@example.com");
        });
        Schema second = SchemaBuilder.update("users", builder -> {
            builder.string("name", "Max");
            builder.where("email", "max@example.com");
        });

        new UpdateBatchRequest(Arrays.asList(first, second)).execute(databaseConnection, configuration, logger);

        assertEquals("UPDATE \"users\" SET \"name\" = ? WHERE \"email\" = ?", capturedSql.get());
    }
}
