package fr.maxlego08.sarah;

import fr.maxlego08.sarah.database.Migration;
import fr.maxlego08.sarah.database.Schema;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Regression tests for bugs found while reviewing the library
 */
public class RegressionTest extends DatabaseTestBase {

    @Test
    public void testWhereInBindsNumbersAsNumbers() throws Exception {
        SchemaBuilder.create(null, "test_users", schema -> {
            schema.autoIncrementBigInt("id");
            schema.string("username", 50);
            schema.integer("age");
        }).execute(connection, testLogger);
        requestHelper.insert("test_users", s -> s.string("username", "alice").bigInt("age", 25));
        requestHelper.insert("test_users", s -> s.string("username", "bob").bigInt("age", 30));
        requestHelper.insert("test_users", s -> s.string("username", "carol").bigInt("age", 35));

        // PostgreSQL rejects an integer column compared to text parameters
        List<Map<String, Object>> varargs = requestHelper.select("test_users", s -> s.whereIn("age", 25, 35));
        List<Map<String, Object>> list = requestHelper.select("test_users", s -> s.whereIn("age", Arrays.asList(30, 35)));

        assertEquals(2, varargs.size());
        assertEquals(2, list.size());
    }

    @Test
    public void testAddSelectKeepsPrefixWithAlias() throws Exception {
        SchemaBuilder.create(null, "test_join_a", schema -> {
            schema.bigInt("id").primary();
            schema.string("name", 50);
        }).execute(connection, testLogger);
        SchemaBuilder.create(null, "test_join_b", schema -> {
            schema.bigInt("a_id").primary();
            schema.string("name", 50);
        }).execute(connection, testLogger);
        requestHelper.insert("test_join_a", s -> s.bigInt("id", 1).string("name", "left"));
        requestHelper.insert("test_join_b", s -> s.bigInt("a_id", 1).string("name", "right"));

        Schema schema = SchemaBuilder.select("test_join_a");
        schema.addSelect("test_join_a", "name", "a_name");
        schema.addSelect("b", "name", "b_name");
        schema.addSelect("b", "name", "b_name_or_default", "'none'");
        schema.leftJoin("test_join_b", "b", "a_id", "test_join_a", "id");

        // Without the prefix, "name" is ambiguous between both tables
        List<Map<String, Object>> results = schema.executeSelect(connection, testLogger);

        assertEquals(1, results.size());
        assertEquals("left", results.get(0).get("a_name"));
        assertEquals("right", results.get(0).get("b_name"));
        assertEquals("right", results.get(0).get("b_name_or_default"));
    }

    @Test
    public void testSelectIntoClassWithTransientAndStaticFields() throws Exception {
        SchemaBuilder.create(null, "test_users_dto", TransientUser.class).execute(connection, testLogger);
        requestHelper.insert("test_users_dto", TransientUser.class, new TransientUser("alice", 25));

        List<TransientUser> users = requestHelper.selectAll("test_users_dto", TransientUser.class);

        assertEquals(1, users.size());
        assertEquals("alice", users.get(0).name);
        assertEquals(25, users.get(0).age);
        assertNull(users.get(0).cached);
    }

    @Test
    public void testTimestampsWithoutMigrationManagerConfiguration() throws Exception {
        MigrationManager.setDatabaseConfiguration(null);

        SchemaBuilder.create(null, "test_create", s -> {
            s.autoIncrementBigInt("id");
            s.string("name", 50);
            s.timestamps();
        }).execute(connection, testLogger);

        requestHelper.insert("test_create", s -> s.string("name", "test"));
        assertEquals(1, countRows("test_create"));
    }

    @Test
    public void testFailedMigrationIsNotRecorded() throws Exception {
        PartiallyFailingMigration migration = new PartiallyFailingMigration();
        MigrationManager.registerMigration(migration);
        try {
            assertThrows(RuntimeException.class, () -> MigrationManager.execute(connection, testLogger));
        } finally {
            MigrationManager.getMigrations().remove(migration);
        }

        // The first schema ran, but the migration must run again until all of its schemas succeed
        long recorded = requestHelper.count(MigrationManager.getMigrationTableName(),
                s -> s.where("migration", PartiallyFailingMigration.class.getSimpleName()));
        assertEquals(0, recorded);
    }

    public static class TransientUser {
        private static final String IGNORED = "static fields are not columns";

        @Column(value = "name", primary = true)
        private final String name;
        private final int age;
        @TransientField
        private final String cached;

        public TransientUser(String name, int age) {
            this.name = name;
            this.age = age;
            this.cached = null;
        }
    }

    public static class PartiallyFailingMigration extends Migration {
        @Override
        public void up() {
            create("test_migration", s -> {
                s.autoIncrementBigInt("id");
                s.string("name", 50);
            });
            index("test_migration", "missing_column");
        }
    }
}
