package fr.maxlego08.sarah;

import fr.maxlego08.sarah.database.DatabaseType;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DatabaseConfigurationTest {

    @Test
    public void maxLifetimeDefaultsToNull() {
        DatabaseConfiguration configuration = DatabaseConfiguration.create("user", "password", "localhost", "db", DatabaseType.MYSQL);

        assertNull(configuration.getMaxLifetime());
    }

    @Test
    public void withMaxLifetimeKeepsPoolSettings() {
        long lifetime = TimeUnit.MINUTES.toMillis(10);
        DatabaseConfiguration configuration = DatabaseConfiguration.create("user", "password", 3306, "localhost", "db", true, DatabaseType.MARIADB)
                .withPoolSettings(4, 1)
                .withMaxLifetime(lifetime);

        assertEquals(Integer.valueOf(4), configuration.getMaximumPoolSize());
        assertEquals(Integer.valueOf(1), configuration.getMinimumIdle());
        assertEquals(Long.valueOf(lifetime), configuration.getMaxLifetime());
        assertEquals("user", configuration.getUser());
        assertTrue(configuration.isDebug());
        assertEquals(DatabaseType.MARIADB, configuration.getDatabaseType());
    }

    @Test
    public void withPoolSettingsKeepsMaxLifetime() {
        DatabaseConfiguration configuration = DatabaseConfiguration.create("user", "password", "localhost", "db", DatabaseType.MYSQL)
                .withMaxLifetime(0L)
                .withPoolSettings(8, 2);

        assertEquals(Long.valueOf(0L), configuration.getMaxLifetime());
        assertEquals(Integer.valueOf(8), configuration.getMaximumPoolSize());
        assertEquals(Integer.valueOf(2), configuration.getMinimumIdle());
    }
}
