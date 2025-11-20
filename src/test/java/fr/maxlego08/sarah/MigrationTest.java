package fr.maxlego08.sarah;

import fr.maxlego08.sarah.database.Migration;
import fr.maxlego08.sarah.database.Schema;
import org.junit.jupiter.api.Test;

import java.sql.ResultSet;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test suite for Schema migrations
 */
public class MigrationTest extends DatabaseTestBase {

    @Test
    public void testCreateTable() throws Exception {
        Schema schema = SchemaBuilder.create(null, "test_create", s -> {
            s.autoIncrementBigInt("id");
            s.string("name", 100);
            s.integer("age");
            s.bool("active").defaultValue(true);
            s.timestamps();
        });

        schema.execute(connection, testLogger);

        // Verify table exists by inserting data
        requestHelper.insert("test_create", s -> {
            s.string("name", "test");
            s.bigInt("age", 25);
        });

        assertEquals(1, countRows("test_create"));
    }

    @Test
    public void testDropTable() throws Exception {
        // Create table
        SchemaBuilder.create(null, "test_drop", s -> {
            s.autoIncrementBigInt("id");
            s.string("name", 50);
        }).execute(connection, testLogger);

        // Verify table exists
        requestHelper.insert("test_drop", s -> s.string("name", "test"));
        assertEquals(1, countRows("test_drop"));

        // Drop table
        SchemaBuilder.drop(null, "test_drop").execute(connection, testLogger);

        // Verify table no longer exists
        try {
            countRows("test_drop");
            fail("Table should have been dropped");
        } catch (Exception e) {
            // Expected - table doesn't exist
            assertTrue(e.getMessage().contains("test_drop") || e.getMessage().contains("no such table"));
        }
    }

    @Test
    public void testAlterTableAddColumn() throws Exception {
        // Create initial table
        SchemaBuilder.create(null, "test_alter", s -> {
            s.autoIncrementBigInt("id");
            s.string("name", 50);
        }).execute(connection, testLogger);

        // Insert data
        requestHelper.insert("test_alter", s -> s.string("name", "test"));

        // Alter table to add new column
        SchemaBuilder.alter(null, "test_alter", s -> {
            s.string("email", 100).nullable();
        }).execute(connection, testLogger);

        // Verify new column exists
        try (Statement stmt = connection.getConnection().createStatement()) {
            ResultSet rs = stmt.executeQuery("SELECT id, name, email FROM test_alter");
            assertTrue(rs.next());
            assertEquals("test", rs.getString("name"));
            assertNull(rs.getString("email"));
        }
    }

    @Test
    public void testMigrationWithMigrationManager() throws Exception {
        // Initialize migration manager
        MigrationManager.setDatabaseConfiguration(configuration);

        // Create a test migration
        Migration testMigration = new Migration() {
            @Override
            public void up() {
                this.create("test_migration", schema -> {
                    schema.autoIncrementBigInt("id");
                    schema.string("username", 50);
                    schema.string("email", 100);
                });
            }
        };

        // Register and execute migration
        MigrationManager.registerMigration(testMigration);
        MigrationManager.execute(connection, testLogger);

        // Verify table was created
        requestHelper.insert("test_migration", s -> {
            s.string("username", "migrated_user");
            s.string("email", "migrated@example.com");
        });

        assertEquals(1, countRows("test_migration"));

        // Verify migrations table exists and has our migration
        long migrationCount = requestHelper.count("migrations", s -> {});
        assertTrue(migrationCount > 0, "Migrations table should have at least one entry");
    }

    @Test
    public void testMigrationIdempotency() throws Exception {
        MigrationManager.setDatabaseConfiguration(configuration);

        Migration testMigration = new Migration() {
            @Override
            public void up() {
                this.create("test_idempotent", schema -> {
                    schema.autoIncrementBigInt("id");
                    schema.string("name", 50);
                });
            }
        };

        // Execute migration twice
        MigrationManager.registerMigration(testMigration);
        MigrationManager.execute(connection, testLogger);
        MigrationManager.execute(connection, testLogger);

        // Should still have only one table (no error on second execution)
        requestHelper.insert("test_idempotent", s -> s.string("name", "test"));
        assertEquals(1, countRows("test_idempotent"));
    }

    @Test
    public void testCreateIndex() throws Exception {
        // Create table
        SchemaBuilder.create(null, "test_index", s -> {
            s.autoIncrementBigInt("id");
            s.string("email", 100);
            s.string("username", 50);
        }).execute(connection, testLogger);

        // Create index on email
        SchemaBuilder.createIndex(null, "test_index", "email").execute(connection, testLogger);

        // Verify table still works (index creation doesn't break anything)
        requestHelper.insert("test_index", s -> {
            s.string("email", "test@example.com");
            s.string("username", "testuser");
        });

        assertEquals(1, countRows("test_index"));
    }

    @Test
    public void testRenameTable() throws Exception {
        // Create table
        SchemaBuilder.create(null, "test_old_name", s -> {
            s.autoIncrementBigInt("id");
            s.string("name", 50);
        }).execute(connection, testLogger);

        // Insert data
        requestHelper.insert("test_old_name", s -> s.string("name", "test"));

        // Rename table
        SchemaBuilder.rename(null, "test_old_name", "test_new_name").execute(connection, testLogger);

        // Verify new name works
        assertEquals(1, countRows("test_new_name"));

        // Verify old name doesn't exist
        try {
            countRows("test_old_name");
            fail("Old table name should no longer exist");
        } catch (Exception e) {
            // Expected
            assertTrue(e.getMessage().contains("test_old_name") || e.getMessage().contains("no such table"));
        }
    }

    @Test
    public void testCreateTableWithForeignKey() throws Exception {
        // Create parent table
        SchemaBuilder.create(null, "test_parent", s -> {
            s.autoIncrementBigInt("id");
            s.string("name", 50);
        }).execute(connection, testLogger);

        // Create child table with foreign key
        SchemaBuilder.create(null, "test_child", s -> {
            s.autoIncrementBigInt("id");
            s.bigInt("parent_id").foreignKey("test_parent", "id", true);
            s.string("description", 100);
        }).execute(connection, testLogger);

        // Insert parent
        requestHelper.insert("test_parent", s -> s.string("name", "parent1"));

        // Insert child
        requestHelper.insert("test_child", s -> {
            s.bigInt("parent_id", 1);
            s.string("description", "child1");
        });

        assertEquals(1, countRows("test_parent"));
        assertEquals(1, countRows("test_child"));
    }

    @Test
    public void testCreateTableWithAllDataTypes() throws Exception {
        SchemaBuilder.create(null, "test_all_types", s -> {
            s.autoIncrementBigInt("id");
            s.string("str_col", 50);
            s.text("text_col");
            s.longText("longtext_col");
            s.integer("int_col");
            s.bigInt("bigint_col");
            s.decimal("decimal_col", 10, 2);
            s.bool("bool_col");
            s.timestamp("timestamp_col");
            s.blob("blob_col");
            s.uuid("uuid_col");
            s.timestamps();
        }).execute(connection, testLogger);

        // Verify table was created
        try (Statement stmt = connection.getConnection().createStatement()) {
            ResultSet rs = stmt.executeQuery("SELECT * FROM test_all_types LIMIT 0");
            // Just checking that the query doesn't throw
            assertNotNull(rs);
        }
    }

    @Test
    public void testCreateTableWithUniqueConstraint() throws Exception {
        SchemaBuilder.create(null, "test_unique", s -> {
            s.autoIncrementBigInt("id");
            s.string("email", 100).unique();
            s.string("username", 50);
        }).execute(connection, testLogger);

        // Insert first user
        requestHelper.insert("test_unique", s -> {
            s.string("email", "unique@example.com");
            s.string("username", "user1");
        });

        // Try to insert duplicate email - should either throw exception or fail silently
        try {
            requestHelper.insert("test_unique", s -> {
                s.string("email", "unique@example.com");
                s.string("username", "user2");
            });
        } catch (Exception e) {
            // Expected - unique constraint violation
            assertTrue(e.getMessage().toLowerCase().contains("unique") ||
                      e.getMessage().toLowerCase().contains("constraint") ||
                      e.getMessage().toLowerCase().contains("duplicate"));
        }

        // Verify that duplicate was NOT inserted (either exception or silent failure)
        assertEquals(1, countRows("test_unique"), "Table should still have only 1 row - duplicate should not have been inserted");
    }

    @Test
    public void testCreateTableWithNullableColumn() throws Exception {
        SchemaBuilder.create(null, "test_nullable", s -> {
            s.autoIncrementBigInt("id");
            s.string("required_col", 50); // NOT NULL by default
            s.string("optional_col", 50).nullable();
        }).execute(connection, testLogger);

        // Insert with null optional column
        requestHelper.insert("test_nullable", s -> {
            s.string("required_col", "required");
            s.string("optional_col", null);
        });

        try (Statement stmt = connection.getConnection().createStatement()) {
            ResultSet rs = stmt.executeQuery("SELECT * FROM test_nullable");
            assertTrue(rs.next());
            assertEquals("required", rs.getString("required_col"));
            assertNull(rs.getString("optional_col"));
        }
    }
}