package fr.maxlego08.sarah;

import fr.maxlego08.sarah.database.Schema;
import org.junit.jupiter.api.Test;

import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test suite for INSERT BATCH operations
 */
public class InsertBatchRequestTest extends DatabaseTestBase {

    @Override
    protected void afterConnectionSetup() throws Exception {
        // Create test table
        SchemaBuilder.create(null, "test_users", schema -> {
            schema.autoIncrementBigInt("id");
            schema.string("username", 50);
            schema.string("email", 100).nullable();
            schema.integer("age");
            schema.timestamps();
        }).execute(connection, testLogger);
    }

    @Test
    public void testBatchInsertMultipleRows() throws Exception {
        List<Schema> schemas = new ArrayList<>();

        // Create multiple schemas for batch insert
        schemas.add(SchemaBuilder.insert("test_users", schema -> {
            schema.string("username", "user1");
            schema.string("email", "user1@example.com");
            schema.bigInt("age", 20);
        }));

        schemas.add(SchemaBuilder.insert("test_users", schema -> {
            schema.string("username", "user2");
            schema.string("email", "user2@example.com");
            schema.bigInt("age", 30);
        }));

        schemas.add(SchemaBuilder.insert("test_users", schema -> {
            schema.string("username", "user3");
            schema.string("email", "user3@example.com");
            schema.bigInt("age", 40);
        }));

        // Execute batch insert
        requestHelper.insertMultiple(schemas);

        // Verify all rows were inserted
        assertEquals(3, countRows("test_users"));

        // Verify data integrity
        try (Statement stmt = connection.getConnection().createStatement()) {
            ResultSet rs = stmt.executeQuery("SELECT * FROM test_users ORDER BY id");

            assertTrue(rs.next());
            assertEquals("user1", rs.getString("username"));
            assertEquals(20, rs.getInt("age"));

            assertTrue(rs.next());
            assertEquals("user2", rs.getString("username"));
            assertEquals(30, rs.getInt("age"));

            assertTrue(rs.next());
            assertEquals("user3", rs.getString("username"));
            assertEquals(40, rs.getInt("age"));

            assertFalse(rs.next());
        }
    }

    @Test
    public void testBatchInsertWithNullValues() throws Exception {
        List<Schema> schemas = new ArrayList<>();

        schemas.add(SchemaBuilder.insert("test_users", schema -> {
            schema.string("username", "user1");
            schema.string("email", null);
            schema.bigInt("age", 25);
        }));

        schemas.add(SchemaBuilder.insert("test_users", schema -> {
            schema.string("username", "user2");
            schema.string("email", "user2@example.com");
            schema.bigInt("age", 35);
        }));

        requestHelper.insertMultiple(schemas);

        assertEquals(2, countRows("test_users"));

        try (Statement stmt = connection.getConnection().createStatement()) {
            ResultSet rs = stmt.executeQuery("SELECT * FROM test_users WHERE username = 'user1'");
            assertTrue(rs.next());
            assertNull(rs.getString("email"));
        }
    }

    @Test
    public void testBatchInsertLargeDataset() throws Exception {
        List<Schema> schemas = new ArrayList<>();

        // Create 100 users
        for (int i = 0; i < 100; i++) {
            final int index = i;
            schemas.add(SchemaBuilder.insert("test_users", schema -> {
                schema.string("username", "user" + index);
                schema.string("email", "user" + index + "@example.com");
                schema.bigInt("age", 20 + (index % 50));
            }));
        }

        requestHelper.insertMultiple(schemas);

        assertEquals(100, countRows("test_users"));
    }

    @Test
    public void testBatchInsertEmptyList() throws Exception {
        List<Schema> schemas = new ArrayList<>();

        // Should not throw an exception
        requestHelper.insertMultiple(schemas);

        assertEquals(0, countRows("test_users"));
    }

    @Test
    public void testBatchInsertSingleRow() throws Exception {
        List<Schema> schemas = new ArrayList<>();

        schemas.add(SchemaBuilder.insert("test_users", schema -> {
            schema.string("username", "single_user");
            schema.string("email", "single@example.com");
            schema.bigInt("age", 42);
        }));

        requestHelper.insertMultiple(schemas);

        assertEquals(1, countRows("test_users"));

        try (Statement stmt = connection.getConnection().createStatement()) {
            ResultSet rs = stmt.executeQuery("SELECT * FROM test_users");
            assertTrue(rs.next());
            assertEquals("single_user", rs.getString("username"));
            assertEquals(42, rs.getInt("age"));
        }
    }
}