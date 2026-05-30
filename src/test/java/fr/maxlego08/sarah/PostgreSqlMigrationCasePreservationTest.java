package fr.maxlego08.sarah;

import fr.maxlego08.sarah.conditions.ColumnDefinition;
import fr.maxlego08.sarah.database.DatabaseType;
import fr.maxlego08.sarah.dialect.SqlDialect;
import fr.maxlego08.sarah.dialect.SqlDialects;
import fr.maxlego08.sarah.logger.Logger;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class PostgreSqlMigrationCasePreservationTest {

    @Test
    public void testMissingColumnsUsesExactCaseForQuotedIdentifiers() throws Exception {
        DatabaseConfiguration configuration = DatabaseConfiguration.createPostgreSql("u", "p", 5432, "localhost", "db");
        DatabaseConnection databaseConnection = mock(DatabaseConnection.class);
        Connection sqlConnection = mock(Connection.class);
        PreparedStatement preparedStatement = mock(PreparedStatement.class);
        ResultSet resultSet = mock(ResultSet.class);
        Logger logger = message -> {
        };

        when(databaseConnection.getConnection()).thenReturn(sqlConnection);
        when(databaseConnection.getDatabaseConfiguration()).thenReturn(configuration);
        when(sqlConnection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        when(resultSet.getLong(1)).thenReturn(1L);

        SqlDialect dialect = SqlDialects.from(DatabaseType.POSTGRESQL);
        List<ColumnDefinition> missingColumns = dialect.missingColumns(
                databaseConnection,
                logger,
                "UsersTable",
                Arrays.asList(new ColumnDefinition("groupId", "VARCHAR"))
        );

        verify(preparedStatement).setString(1, "UsersTable");
        verify(preparedStatement).setString(2, "public");
        verify(preparedStatement).setString(3, "groupId");
        assertTrue(missingColumns.isEmpty());
    }
}
