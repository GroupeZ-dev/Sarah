package fr.maxlego08.sarah;

import fr.maxlego08.sarah.database.Schema;
import org.junit.jupiter.api.Test;

import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test suite specifically for autoincrement functionality and related bug fixes
 * Tests fixes from commits: d8f7972, a7c61fb, aeb6962
 */
public class AutoIncrementTest extends DatabaseTestBase {

    @Test
    public void testAutoIncrementInInsert() throws Exception {
        // Create table with autoincrement
        SchemaBuilder.create(null, "test_autoincrement", schema -> {
            schema.autoIncrementBigInt("id");
            schema.string("name", 50);
        }).execute(connection, testLogger);

        // Insert without specifying ID
        AtomicInteger id1 = new AtomicInteger();
        requestHelper.insert("test_autoincrement", schema -> {
            schema.string("name", "first");
        }, id1::set);

        AtomicInteger id2 = new AtomicInteger();
        requestHelper.insert("test_autoincrement", schema -> {
            schema.string("name", "second");
        }, id2::set);

        // Verify IDs are auto-generated and sequential
        assertTrue(id1.get() > 0);
        assertTrue(id2.get() > id1.get());
    }

    @Test
    public void testAutoIncrementNotInUpsert() throws Exception {
        // Test fix from commit aeb6962 and a7c61fb
        // Autoincrement columns should be skipped in UPSERT operations

        SchemaBuilder.create(null, "test_autoincrement", schema -> {
            schema.autoIncrementBigInt("id");
            schema.string("code", 10).unique();
            schema.string("value", 50);
        }).execute(connection, testLogger);

        // Insert initial record
        requestHelper.insert("test_autoincrement", schema -> {
            schema.string("code", "ABC");
            schema.string("value", "initial");
        });

        // Get the generated ID
        int originalId;
        try (Statement stmt = connection.getConnection().createStatement()) {
            ResultSet rs = stmt.executeQuery("SELECT id FROM test_autoincrement WHERE code = 'ABC'");
            assertTrue(rs.next());
            originalId = rs.getInt("id");
        }

        // Upsert with same code
        requestHelper.upsert("test_autoincrement", schema -> {
            schema.string("code", "ABC").unique(); // Mark as unique for conflict detection
            schema.string("value", "updated");
        });

        // Verify ID hasn't changed
        try (Statement stmt = connection.getConnection().createStatement()) {
            ResultSet rs = stmt.executeQuery("SELECT id, value FROM test_autoincrement WHERE code = 'ABC'");
            assertTrue(rs.next());
            assertEquals(originalId, rs.getInt("id"), "Autoincrement ID should not change on upsert");
            assertEquals("updated", rs.getString("value"));
        }
    }

    @Test
    public void testUpsertWithOnlyAutoIncrementPrimaryKey() throws Exception {
        // Test fix from commit d8f7972
        // This should throw an exception for SQLite as there's no non-autoincrement key for ON CONFLICT

        SchemaBuilder.create(null, "test_autoincrement", schema -> {
            schema.autoIncrementBigInt("id");
            schema.string("name", 50);
        }).execute(connection, testLogger);

        if (configuration.getDatabaseType() == fr.maxlego08.sarah.database.DatabaseType.SQLITE) {
            // For SQLite, UPSERT with only autoincrement primary key should fail or fallback
            try {
                requestHelper.upsert("test_autoincrement", schema -> {
                    schema.string("name", "test");
                });
                // If it doesn't throw, it should have fallen back to regular insert
                assertEquals(1, countRows("test_autoincrement"));
            } catch (Exception e) {
                // Expected for SQLite - this is OK
                assertTrue(e.getMessage().contains("UPSERT") || e.getMessage().contains("autoincrement"));
            }
        } else {
            // For MySQL/MariaDB, this should work with ON DUPLICATE KEY UPDATE
            requestHelper.upsert("test_autoincrement", schema -> {
                schema.string("name", "test");
            });
            assertTrue(countRows("test_autoincrement") > 0);
        }
    }

    @Test
    public void testCompositeKeyWithAutoIncrement() throws Exception {
        // Create table with composite key including an autoincrement column
        executeRawSQL("DROP TABLE IF EXISTS test_composite");

        if (configuration.getDatabaseType() == fr.maxlego08.sarah.database.DatabaseType.SQLITE) {
            executeRawSQL("CREATE TABLE test_composite (id INTEGER PRIMARY KEY AUTOINCREMENT, category VARCHAR(50), name VARCHAR(50), UNIQUE(category, name))");
        } else {
            executeRawSQL("CREATE TABLE test_composite (id BIGINT AUTO_INCREMENT, category VARCHAR(50), name VARCHAR(50), PRIMARY KEY (id), UNIQUE(category, name))");
        }

        // Insert initial record
        requestHelper.insert("test_composite", schema -> {
            schema.string("category", "food");
            schema.string("name", "apple");
        });

        // Upsert with same category+name
        requestHelper.upsert("test_composite", schema -> {
            schema.string("category", "food").unique(); // Mark as part of unique constraint
            schema.string("name", "apple").unique(); // Mark as part of unique constraint
        });

        // Should still have only 1 record
        assertEquals(1, countRows("test_composite"));
    }

    @Test
    public void testBatchInsertWithAutoIncrement() throws Exception {
        SchemaBuilder.create(null, "test_autoincrement", schema -> {
            schema.autoIncrementBigInt("id");
            schema.string("name", 50);
        }).execute(connection, testLogger);

        List<Schema> schemas = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            final int index = i;
            schemas.add(SchemaBuilder.insert("test_autoincrement", schema -> {
                schema.string("name", "batch" + index);
            }));
        }

        requestHelper.insertMultiple(schemas);

        assertEquals(5, countRows("test_autoincrement"));

        // Verify IDs are sequential
        try (Statement stmt = connection.getConnection().createStatement()) {
            ResultSet rs = stmt.executeQuery("SELECT id FROM test_autoincrement ORDER BY id");
            int previousId = 0;
            while (rs.next()) {
                int currentId = rs.getInt("id");
                assertTrue(currentId > previousId);
                previousId = currentId;
            }
        }
    }

    @Test
    public void testAutoIncrementBigIntVsInteger() throws Exception {
        // Clean up any existing tables
        executeRawSQL("DROP TABLE IF EXISTS test_bigint");
        executeRawSQL("DROP TABLE IF EXISTS test_integer");

        // Test autoIncrementBigInt vs autoIncrement
        SchemaBuilder.create(null, "test_bigint", schema -> {
            schema.autoIncrementBigInt("id");
            schema.string("name", 50);
        }).execute(connection, testLogger);

        SchemaBuilder.create(null, "test_integer", schema -> {
            schema.autoIncrement("id");
            schema.string("name", 50);
        }).execute(connection, testLogger);

        // Both should work
        requestHelper.insert("test_bigint", schema -> {
            schema.string("name", "bigint test");
        });

        requestHelper.insert("test_integer", schema -> {
            schema.string("name", "integer test");
        });

        assertEquals(1, countRows("test_bigint"));
        assertEquals(1, countRows("test_integer"));
    }

    @Test
    public void testUpdateDoesNotAffectAutoIncrement() throws Exception {
        SchemaBuilder.create(null, "test_autoincrement", schema -> {
            schema.autoIncrementBigInt("id");
            schema.string("name", 50);
        }).execute(connection, testLogger);

        AtomicInteger generatedId = new AtomicInteger();
        requestHelper.insert("test_autoincrement", schema -> {
            schema.string("name", "original");
        }, generatedId::set);

        int originalId = generatedId.get();

        // Update the record
        requestHelper.update("test_autoincrement", schema -> {
            schema.string("name", "updated");
            schema.where("id", originalId);
        });

        // Verify ID hasn't changed
        try (Statement stmt = connection.getConnection().createStatement()) {
            ResultSet rs = stmt.executeQuery("SELECT id, name FROM test_autoincrement");
            assertTrue(rs.next());
            assertEquals(originalId, rs.getInt("id"));
            assertEquals("updated", rs.getString("name"));
        }
    }
}