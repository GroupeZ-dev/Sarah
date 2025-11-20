package fr.maxlego08.sarah;

import fr.maxlego08.sarah.database.Schema;
import org.junit.jupiter.api.Test;

import java.sql.ResultSet;
import java.sql.Statement;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test suite for INSERT operations
 */
public class InsertRequestTest extends DatabaseTestBase {

    @Override
    protected void afterConnectionSetup() throws Exception {
        // Create test table using Schema.create with consumer
        SchemaBuilder.create(null, "test_users", schema -> {
            schema.autoIncrementBigInt("id");
            schema.string("username", 50);
            schema.string("email", 100).nullable();
            schema.integer("age").nullable(); // Allow null for testing
            schema.bool("active").defaultValue(true);
            schema.timestamps();
        }).execute(connection, testLogger);
    }

    @Test
    public void testSimpleInsert() throws Exception {
        // Use RequestHelper.insert with consumer
        AtomicInteger generatedId = new AtomicInteger();
        requestHelper.insert("test_users", schema -> {
            schema.string("username", "john_doe");
            schema.string("email", "john@example.com");
            schema.bigInt("age", 25);
        }, generatedId::set);

        // Verify
        assertTrue(generatedId.get() > 0, "Generated ID should be positive");
        assertEquals(1, countRows("test_users"));

        // Verify inserted data
        try (Statement stmt = connection.getConnection().createStatement()) {
            ResultSet rs = stmt.executeQuery("SELECT * FROM test_users WHERE id = " + generatedId.get());
            assertTrue(rs.next());
            assertEquals("john_doe", rs.getString("username"));
            assertEquals("john@example.com", rs.getString("email"));
            assertEquals(25, rs.getInt("age"));
        }
    }

    @Test
    public void testInsertWithNullValues() throws Exception {
        AtomicInteger generatedId = new AtomicInteger();
        requestHelper.insert("test_users", schema -> {
            schema.string("username", "jane_doe");
            schema.string("email", null);
            schema.object("age", null);
        }, generatedId::set);

        assertTrue(generatedId.get() > 0);

        // Verify null values
        try (Statement stmt = connection.getConnection().createStatement()) {
            ResultSet rs = stmt.executeQuery("SELECT * FROM test_users WHERE id = " + generatedId.get());
            assertTrue(rs.next());
            assertEquals("jane_doe", rs.getString("username"));
            assertNull(rs.getString("email"));
        }
    }

    @Test
    public void testInsertWithTimestamps() throws Exception {
        AtomicInteger generatedId = new AtomicInteger();
        requestHelper.insert("test_users", schema -> {
            schema.string("username", "time_user");
            schema.string("email", "time@example.com");
            schema.bigInt("age", 30);
        }, generatedId::set);

        // Verify timestamps are auto-generated
        try (Statement stmt = connection.getConnection().createStatement()) {
            ResultSet rs = stmt.executeQuery("SELECT created_at, updated_at FROM test_users WHERE id = " + generatedId.get());
            assertTrue(rs.next());
            assertNotNull(rs.getTimestamp("created_at"));
            assertNotNull(rs.getTimestamp("updated_at"));
        }
    }

    @Test
    public void testInsertMultipleRows() throws Exception {
        // Insert first row
        AtomicInteger id1 = new AtomicInteger();
        requestHelper.insert("test_users", schema -> {
            schema.string("username", "user1");
            schema.string("email", "user1@example.com");
            schema.bigInt("age", 20);
        }, id1::set);

        // Insert second row
        AtomicInteger id2 = new AtomicInteger();
        requestHelper.insert("test_users", schema -> {
            schema.string("username", "user2");
            schema.string("email", "user2@example.com");
            schema.bigInt("age", 30);
        }, id2::set);

        // Insert third row
        AtomicInteger id3 = new AtomicInteger();
        requestHelper.insert("test_users", schema -> {
            schema.string("username", "user3");
            schema.string("email", "user3@example.com");
            schema.bigInt("age", 40);
        }, id3::set);

        // Verify all inserted
        assertEquals(3, countRows("test_users"));
        assertTrue(id1.get() > 0 && id2.get() > 0 && id3.get() > 0);
        assertTrue(id2.get() > id1.get() && id3.get() > id2.get(), "IDs should be sequential");
    }

    @Test
    public void testInsertWithBoolean() throws Exception {
        // Insert active user
        AtomicInteger id1 = new AtomicInteger();
        requestHelper.insert("test_users", schema -> {
            schema.string("username", "active_user");
            schema.string("email", "active@example.com");
            schema.bigInt("age", 25);
            schema.bool("active", true);
        }, id1::set);

        // Insert inactive user
        AtomicInteger id2 = new AtomicInteger();
        requestHelper.insert("test_users", schema -> {
            schema.string("username", "inactive_user");
            schema.string("email", "inactive@example.com");
            schema.bigInt("age", 25);
            schema.bool("active", false);
        }, id2::set);

        // Verify boolean values
        try (Statement stmt = connection.getConnection().createStatement()) {
            ResultSet rs = stmt.executeQuery("SELECT active FROM test_users WHERE id = " + id1.get());
            assertTrue(rs.next());
            assertTrue(rs.getBoolean("active"));

            rs = stmt.executeQuery("SELECT active FROM test_users WHERE id = " + id2.get());
            assertTrue(rs.next());
            assertFalse(rs.getBoolean("active"));
        }
    }

    @Test
    public void testInsertAutoIncrementSkipped() throws Exception {
        // The autoincrement column should be skipped in INSERT
        AtomicInteger generatedId = new AtomicInteger();
        requestHelper.insert("test_users", schema -> {
            // Note: We don't set the 'id' column
            schema.string("username", "auto_user");
            schema.string("email", "auto@example.com");
            schema.bigInt("age", 35);
        }, generatedId::set);

        // Verify ID was auto-generated
        assertTrue(generatedId.get() > 0);

        // Verify the record exists with the auto-generated ID
        try (Statement stmt = connection.getConnection().createStatement()) {
            ResultSet rs = stmt.executeQuery("SELECT id FROM test_users WHERE username = 'auto_user'");
            assertTrue(rs.next());
            assertEquals(generatedId.get(), rs.getInt("id"));
        }
    }

    @Test
    public void testInsertWithDifferentDataTypes() throws Exception {
        // Create a table with various data types
        SchemaBuilder.create(null, "test_products", schema -> {
            schema.autoIncrementBigInt("id");
            schema.string("name", 100);
            schema.decimal("price", 10, 2);
            schema.bigInt("stock");
            schema.text("description");
            schema.bool("available");
        }).execute(connection, testLogger);

        AtomicInteger generatedId = new AtomicInteger();
        requestHelper.insert("test_products", schema -> {
            schema.string("name", "Test Product");
            schema.decimal("price", 19.99);
            schema.bigInt("stock", 100);
            schema.string("description", "This is a test product with a longer description");
            schema.bool("available", true);
        }, generatedId::set);

        assertTrue(generatedId.get() > 0);

        // Verify inserted data
        try (Statement stmt = connection.getConnection().createStatement()) {
            ResultSet rs = stmt.executeQuery("SELECT * FROM test_products WHERE id = " + generatedId.get());
            assertTrue(rs.next());
            assertEquals("Test Product", rs.getString("name"));
            assertEquals(19.99, rs.getDouble("price"), 0.01);
            assertEquals(100, rs.getLong("stock"));
            assertEquals("This is a test product with a longer description", rs.getString("description"));
            assertTrue(rs.getBoolean("available"));
        }
    }
}