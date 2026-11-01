package fr.maxlego08.sarah;

import fr.maxlego08.sarah.database.Migration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

/**
 * {@link MigrationManager#execute} can run more than once in the same JVM, for example on two
 * databases. Each run used to register every schema again, so an index was created twice.
 */
public class MigrationReexecuteTest {

    @TempDir
    File folder;

    private static class IndexedMigration extends Migration {
        @Override
        public void up() {
            this.create("test_reexecute", table -> {
                table.string("name", 32).primary();
                table.bigInt("score");
            });
            this.index("test_reexecute", "score");
        }
    }

    private void execute(String fileName) {
        DatabaseConfiguration configuration = DatabaseConfiguration.sqlite(false);
        MigrationManager.setDatabaseConfiguration(configuration);
        SqliteConnection connection = new SqliteConnection(configuration, this.folder, message -> {
        });
        connection.setFileName(fileName);
        MigrationManager.execute(connection, message -> {
        });
    }

    @Test
    public void migrationsRunOnTwoDatabases() {
        IndexedMigration migration = new IndexedMigration();
        MigrationManager.registerMigration(migration);
        try {
            assertDoesNotThrow(() -> this.execute("first.db"));
            assertDoesNotThrow(() -> this.execute("second.db"));
            assertDoesNotThrow(() -> this.execute("first.db"));
        } finally {
            MigrationManager.getMigrations().remove(migration);
        }
    }
}
