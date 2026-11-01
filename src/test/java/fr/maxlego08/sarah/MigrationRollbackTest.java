package fr.maxlego08.sarah;

import fr.maxlego08.sarah.database.Migration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test suite for migration rollback and status
 */
public class MigrationRollbackTest extends DatabaseTestBase {

    public static class CreateRollbackATable extends Migration {
        @Override
        public void up() {
            create("test_rollback_a", schema -> {
                schema.autoIncrementBigInt("id");
                schema.string("name", 50);
            });
        }

        @Override
        public void down() {
            drop("test_rollback_a");
        }
    }

    public static class CreateRollbackBTable extends Migration {
        @Override
        public void up() {
            create("test_rollback_b", schema -> {
                schema.autoIncrementBigInt("id");
                schema.string("name", 50);
            });
        }

        @Override
        public void down() {
            drop("test_rollback_b");
        }
    }

    public static class CreateRollbackCTableWithoutDown extends Migration {
        @Override
        public void up() {
            create("test_rollback_c", schema -> schema.autoIncrementBigInt("id"));
        }
    }

    private final Migration migrationA = new CreateRollbackATable();
    private final Migration migrationB = new CreateRollbackBTable();
    private final Migration migrationC = new CreateRollbackCTableWithoutDown();

    @AfterEach
    public void unregisterMigrations() {
        MigrationManager.getMigrations().removeAll(Arrays.asList(migrationA, migrationB, migrationC));
    }

    private boolean tableExists(String table) {
        try {
            requestHelper.setStrictReads(true).count(table, schema -> {
            });
            return true;
        } catch (Exception exception) {
            return false;
        }
    }

    private List<String> pendingNames() {
        return MigrationManager.getPendingMigrations(connection, testLogger).stream()
                .map(migration -> migration.getClass().getSimpleName())
                .collect(Collectors.toList());
    }

    @Test
    public void testStatusBeforeAndAfterExecute() {
        MigrationManager.registerMigration(migrationA);
        MigrationManager.registerMigration(migrationB);

        assertEquals(Collections.emptyList(), MigrationManager.getAppliedMigrations(connection, testLogger));
        assertEquals(Arrays.asList("CreateRollbackATable", "CreateRollbackBTable"), pendingNames());

        MigrationManager.execute(connection, testLogger);

        assertEquals(Arrays.asList("CreateRollbackATable", "CreateRollbackBTable"), MigrationManager.getAppliedMigrations(connection, testLogger));
        assertEquals(Collections.emptyList(), pendingNames());
    }

    @Test
    public void testRollbackLastMigrationThenReapply() {
        MigrationManager.registerMigration(migrationA);
        MigrationManager.registerMigration(migrationB);
        MigrationManager.execute(connection, testLogger);

        assertEquals(Collections.singletonList("CreateRollbackBTable"), MigrationManager.rollback(connection, testLogger));
        assertTrue(tableExists("test_rollback_a"));
        assertFalse(tableExists("test_rollback_b"));
        assertEquals(Collections.singletonList("CreateRollbackBTable"), pendingNames());

        MigrationManager.execute(connection, testLogger);
        assertTrue(tableExists("test_rollback_b"));
        assertEquals(Collections.emptyList(), pendingNames());
    }

    @Test
    public void testRollbackSeveralStepsMostRecentFirst() {
        MigrationManager.registerMigration(migrationA);
        MigrationManager.registerMigration(migrationB);
        MigrationManager.execute(connection, testLogger);

        assertEquals(Arrays.asList("CreateRollbackBTable", "CreateRollbackATable"), MigrationManager.rollback(connection, testLogger, 5));
        assertFalse(tableExists("test_rollback_a"));
        assertFalse(tableExists("test_rollback_b"));
        assertEquals(Collections.emptyList(), MigrationManager.getAppliedMigrations(connection, testLogger));
        assertEquals(Collections.emptyList(), MigrationManager.rollback(connection, testLogger));
    }

    @Test
    public void testMigrationWithoutDownStopsBeforeAnyChange() {
        MigrationManager.registerMigration(migrationA);
        MigrationManager.registerMigration(migrationC);
        MigrationManager.execute(connection, testLogger);

        assertThrows(IllegalStateException.class, () -> MigrationManager.rollback(connection, testLogger, 2));
        assertTrue(tableExists("test_rollback_a"));
        assertTrue(tableExists("test_rollback_c"));
        assertEquals(2, MigrationManager.getAppliedMigrations(connection, testLogger).size());
    }

    @Test
    public void testStepsMustBePositive() {
        assertThrows(IllegalArgumentException.class, () -> MigrationManager.rollback(connection, testLogger, 0));
    }
}
