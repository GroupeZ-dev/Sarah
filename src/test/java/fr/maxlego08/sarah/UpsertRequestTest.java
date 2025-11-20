package fr.maxlego08.sarah;

import fr.maxlego08.sarah.database.Schema;
import org.junit.jupiter.api.Test;

import java.sql.ResultSet;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test suite for UPSERT operations - tests the recent autoincrement fixes
 */
public class UpsertRequestTest extends DatabaseTestBase {

    @Override
    protected void afterConnectionSetup() throws Exception {
        // Create test table with autoincrement
        SchemaBuilder.create(null, "test_users", schema -> {
            schema.autoIncrementBigInt("id");
            schema.string("username", 50).unique();
            schema.string("email", 100).nullable(); // Email can be null for upsert tests
            schema.integer("age");
            schema.timestamps();
        }).execute(connection, testLogger);
    }

    @Test
    public void testUpsertInsertNew() throws Exception {
        // Insert new record
        requestHelper.upsert("test_users", schema -> {
            schema.string("username", "john_doe").unique(); // Mark as unique to match table constraint
            schema.string("email", "john@example.com");
            schema.bigInt("age", 25);
        });

        assertEquals(1, countRows("test_users"));

        try (Statement stmt = connection.getConnection().createStatement()) {
            ResultSet rs = stmt.executeQuery("SELECT * FROM test_users WHERE username = 'john_doe'");
            assertTrue(rs.next());
            assertEquals("john@example.com", rs.getString("email"));
            assertEquals(25, rs.getInt("age"));
        }
    }

    @Test
    public void testUpsertUpdateExisting() throws Exception {
        // Insert initial record
        requestHelper.insert("test_users", schema -> {
            schema.string("username", "jane_doe").unique();
            schema.string("email", "jane@example.com");
            schema.bigInt("age", 30);
        });

        // Upsert with same username (should update)
        requestHelper.upsert("test_users", schema -> {
            schema.string("username", "jane_doe").unique();
            schema.string("email", "jane.updated@example.com");
            schema.bigInt("age", 31);
        });

        // Should still have only 1 row
        assertEquals(1, countRows("test_users"));

        // Verify data was updated
        try (Statement stmt = connection.getConnection().createStatement()) {
            ResultSet rs = stmt.executeQuery("SELECT * FROM test_users WHERE username = 'jane_doe'");
            assertTrue(rs.next());
            assertEquals("jane.updated@example.com", rs.getString("email"));
            assertEquals(31, rs.getInt("age"));
        }
    }

    @Test
    public void testUpsertWithAutoIncrementSkipped() throws Exception {
        // This tests the fix from commit d8f7972 and a7c61fb
        // Autoincrement columns should be skipped in both INSERT and UPDATE parts

        // Insert a record
        requestHelper.insert("test_users", schema -> {
            schema.string("username", "auto_test").unique();
            schema.string("email", "auto@example.com");
            schema.bigInt("age", 25);
        });

        // Get the auto-generated ID
        int originalId;
        try (Statement stmt = connection.getConnection().createStatement()) {
            ResultSet rs = stmt.executeQuery("SELECT id FROM test_users WHERE username = 'auto_test'");
            assertTrue(rs.next());
            originalId = rs.getInt("id");
        }

        // Upsert with same username
        requestHelper.upsert("test_users", schema -> {
            schema.string("username", "auto_test").unique();
            schema.string("email", "auto.updated@example.com");
            schema.bigInt("age", 26);
        });

        // Verify the ID hasn't changed (autoincrement wasn't updated)
        try (Statement stmt = connection.getConnection().createStatement()) {
            ResultSet rs = stmt.executeQuery("SELECT id, email, age FROM test_users WHERE username = 'auto_test'");
            assertTrue(rs.next());
            assertEquals(originalId, rs.getInt("id"), "ID should not change on upsert");
            assertEquals("auto.updated@example.com", rs.getString("email"));
            assertEquals(26, rs.getInt("age"));
        }
    }

    @Test
    public void testUpsertMultipleOperations() throws Exception {
        // Mix of inserts and updates
        requestHelper.upsert("test_users", schema -> {
            schema.string("username", "user1").unique();
            schema.string("email", "user1@example.com");
            schema.bigInt("age", 20);
        });

        requestHelper.upsert("test_users", schema -> {
            schema.string("username", "user2").unique();
            schema.string("email", "user2@example.com");
            schema.bigInt("age", 30);
        });

        // Update user1
        requestHelper.upsert("test_users", schema -> {
            schema.string("username", "user1").unique();
            schema.string("email", "user1.updated@example.com");
            schema.bigInt("age", 21);
        });

        // Should have 2 rows
        assertEquals(2, countRows("test_users"));

        // Verify user1 was updated
        try (Statement stmt = connection.getConnection().createStatement()) {
            ResultSet rs = stmt.executeQuery("SELECT email, age FROM test_users WHERE username = 'user1'");
            assertTrue(rs.next());
            assertEquals("user1.updated@example.com", rs.getString("email"));
            assertEquals(21, rs.getInt("age"));
        }
    }

    @Test
    public void testUpsertWithNullValues() throws Exception {
        requestHelper.insert("test_users", schema -> {
            schema.string("username", "null_test").unique();
            schema.string("email", "null@example.com");
            schema.bigInt("age", 25);
        });

        // Upsert with null email
        requestHelper.upsert("test_users", schema -> {
            schema.string("username", "null_test").unique();
            schema.string("email", null);
            schema.bigInt("age", 26);
        });

        try (Statement stmt = connection.getConnection().createStatement()) {
            ResultSet rs = stmt.executeQuery("SELECT email, age FROM test_users WHERE username = 'null_test'");
            assertTrue(rs.next());
            assertNull(rs.getString("email"));
            assertEquals(26, rs.getInt("age"));
        }
    }

    @Test
    public void testUpsertCompositePrimaryKeyTable() throws Exception {
        // Create table with composite primary key (no autoincrement)
        SchemaBuilder.create(null, "test_composite", schema -> {
            schema.string("key1", 50);
            schema.string("key2", 50);
            schema.string("value", 100);
            schema.integer("count");
        }).execute(connection, testLogger);

        // Add composite primary key manually
        executeRawSQL("DROP TABLE IF EXISTS test_composite");
        if (configuration.getDatabaseType() == fr.maxlego08.sarah.database.DatabaseType.SQLITE) {
            executeRawSQL("CREATE TABLE test_composite (key1 VARCHAR(50), key2 VARCHAR(50), value VARCHAR(100), count INT, PRIMARY KEY (key1, key2))");
        } else {
            executeRawSQL("CREATE TABLE test_composite (key1 VARCHAR(50), key2 VARCHAR(50), value VARCHAR(100), count INT, PRIMARY KEY (key1, key2))");
        }

        // Insert initial record
        requestHelper.insert("test_composite", schema -> {
            schema.string("key1", "A");
            schema.string("key2", "B");
            schema.string("value", "initial");
            schema.bigInt("count", 1);
        });

        // Upsert with same composite key
        requestHelper.upsert("test_composite", schema -> {
            schema.string("key1", "A").primary();  // Mark as primary key
            schema.string("key2", "B").primary();  // Mark as primary key
            schema.string("value", "updated");
            schema.bigInt("count", 2);
        });

        assertEquals(1, countRows("test_composite"));

        try (Statement stmt = connection.getConnection().createStatement()) {
            ResultSet rs = stmt.executeQuery("SELECT value, count FROM test_composite WHERE key1 = 'A' AND key2 = 'B'");
            assertTrue(rs.next());
            assertEquals("updated", rs.getString("value"));
            assertEquals(2, rs.getInt("count"));
        }
    }
}