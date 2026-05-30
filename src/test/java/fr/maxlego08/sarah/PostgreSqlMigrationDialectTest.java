package fr.maxlego08.sarah;

import fr.maxlego08.sarah.conditions.ColumnDefinition;
import fr.maxlego08.sarah.database.DatabaseType;
import fr.maxlego08.sarah.dialect.SqlDialect;
import fr.maxlego08.sarah.dialect.SqlDialects;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class PostgreSqlMigrationDialectTest {

    @Test
    public void testPostgreSqlMissingColumnsRequiresConnection() {
        SqlDialect dialect = SqlDialects.from(DatabaseType.POSTGRESQL);
        assertThrows(NullPointerException.class, () -> dialect.missingColumns(null, null, "users", Arrays.asList(new ColumnDefinition("name", "VARCHAR"))));
    }

    @Test
    public void testPostgreSqlMissingColumnsUsesSchemaFromQualifiedTableName() throws Exception {
        SqlDialect dialect = SqlDialects.from(DatabaseType.POSTGRESQL);
        DatabaseConfiguration configuration = DatabaseConfiguration.createPostgreSql("u", "p", 5432, "localhost", "db");
        DatabaseConnection databaseConnection = mock(DatabaseConnection.class);
        Connection sqlConnection = mock(Connection.class);
        PreparedStatement preparedStatement = mock(PreparedStatement.class);
        ResultSet resultSet = mock(ResultSet.class);

        when(databaseConnection.getDatabaseConfiguration()).thenReturn(configuration);
        when(databaseConnection.getConnection()).thenReturn(sqlConnection);
        when(sqlConnection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        when(resultSet.getLong(1)).thenReturn(1L);

        dialect.missingColumns(databaseConnection, null, "myschema.users", Arrays.asList(new ColumnDefinition("name", "VARCHAR")));

        verify(preparedStatement).setString(1, "users");
        verify(preparedStatement).setString(2, "myschema");
        verify(preparedStatement).setString(3, "name");
    }

    @Test
    public void testPostgreSqlMissingColumnsUsesCurrentSchemaWhenNotQualified() throws Exception {
        SqlDialect dialect = SqlDialects.from(DatabaseType.POSTGRESQL);
        DatabaseConfiguration configuration = DatabaseConfiguration.createPostgreSql("u", "p", 5432, "localhost", "db");
        DatabaseConnection databaseConnection = mock(DatabaseConnection.class);
        Connection sqlConnection = mock(Connection.class);
        PreparedStatement preparedStatement = mock(PreparedStatement.class);
        ResultSet resultSet = mock(ResultSet.class);

        when(databaseConnection.getDatabaseConfiguration()).thenReturn(configuration);
        when(databaseConnection.getConnection()).thenReturn(sqlConnection);
        when(sqlConnection.getSchema()).thenReturn("tenant1");
        when(sqlConnection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        when(resultSet.getLong(1)).thenReturn(1L);

        assertTrue(dialect.missingColumns(databaseConnection, null, "users u", Arrays.asList(new ColumnDefinition("name", "VARCHAR"))).isEmpty());

        verify(preparedStatement).setString(1, "users");
        verify(preparedStatement).setString(2, "tenant1");
        verify(preparedStatement).setString(3, "name");
    }
}
