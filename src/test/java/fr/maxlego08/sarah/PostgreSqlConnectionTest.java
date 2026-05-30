package fr.maxlego08.sarah;

import fr.maxlego08.sarah.database.DatabaseType;
import fr.maxlego08.sarah.dialect.SqlDialects;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class PostgreSqlConnectionTest {

    @Test
    public void testPostgreSqlConfigurationFactory() {
        DatabaseConfiguration configuration = DatabaseConfiguration.createPostgreSql("sarah", "secret", "localhost", "sarah_db");

        assertEquals(DatabaseType.POSTGRESQL, configuration.getDatabaseType());
        assertEquals(5432, configuration.getPort());
        assertEquals("sarah", configuration.getUser());
        assertEquals("secret", configuration.getPassword());
        assertEquals("localhost", configuration.getHost());
        assertEquals("sarah_db", configuration.getDatabase());
    }

    @Test
    public void testPostgreSqlJdbcUrlAndDriver() {
        DatabaseConfiguration configuration = DatabaseConfiguration.createPostgreSql("sarah", "secret", 15432, "db.local", "sarah_db");

        assertEquals("org.postgresql.Driver", SqlDialects.from(DatabaseType.POSTGRESQL).driverClassName());
        assertEquals("jdbc:postgresql://db.local:15432/sarah_db", SqlDialects.from(DatabaseType.POSTGRESQL).jdbcUrl(configuration));
    }

    @Test
    public void testExistingJdbcUrlsStayStable() {
        DatabaseConfiguration mysql = DatabaseConfiguration.create("u", "p", 3307, "host", "db");
        DatabaseConfiguration maria = DatabaseConfiguration.createMariaDb("u", "p", 3308, "host", "db");

        assertEquals("jdbc:mysql://host:3307/db?allowMultiQueries=true", SqlDialects.from(DatabaseType.MYSQL).jdbcUrl(mysql));
        assertEquals("jdbc:mariadb://host:3308/db?allowMultiQueries=true", SqlDialects.from(DatabaseType.MARIADB).jdbcUrl(maria));
    }
}
