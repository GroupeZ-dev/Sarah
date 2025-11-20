package fr.maxlego08.sarah;

import fr.maxlego08.sarah.database.Schema;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.ResultSet;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test suite for UPDATE operations
 */
public class UpdateRequestTest extends DatabaseTestBase {

    @Override
    protected void afterConnectionSetup() throws Exception {
        SchemaBuilder.create(null, "test_users", schema -> {
            schema.autoIncrementBigInt("id");
            schema.string("username", 50);
            schema.string("email", 100).nullable(); // Allow null for update tests
            schema.integer("age");
            schema.bool("active").defaultValue(true);
        }).execute(connection, testLogger);
    }

    @BeforeEach
    public void insertTestData() {
        // Insert test data before each test
        requestHelper.insert("test_users", schema -> {
            schema.string("username", "user1");
            schema.string("email", "user1@example.com");
            schema.bigInt("age", 25);
            schema.bool("active", true);
        });

        requestHelper.insert("test_users", schema -> {
            schema.string("username", "user2");
            schema.string("email", "user2@example.com");
            schema.bigInt("age", 30);
            schema.bool("active", true);
        });
    }

    @Test
    public void testSimpleUpdate() throws Exception {
        requestHelper.update("test_users", schema -> {
            schema.string("email", "user1.updated@example.com");
            schema.bigInt("age", 26);
            schema.where("username", "user1");
        });

        try (Statement stmt = connection.getConnection().createStatement()) {
            ResultSet rs = stmt.executeQuery("SELECT email, age FROM test_users WHERE username = 'user1'");
            assertTrue(rs.next());
            assertEquals("user1.updated@example.com", rs.getString("email"));
            assertEquals(26, rs.getInt("age"));
        }
    }

    @Test
    public void testUpdateWithMultipleWhereConditions() throws Exception {
        requestHelper.update("test_users", schema -> {
            schema.bool("active", false);
            schema.where("username", "user1");
            schema.where("age", ">", 20);
        });

        try (Statement stmt = connection.getConnection().createStatement()) {
            ResultSet rs = stmt.executeQuery("SELECT active FROM test_users WHERE username = 'user1'");
            assertTrue(rs.next());
            assertFalse(rs.getBoolean("active"));
        }
    }

    @Test
    public void testUpdateMultipleRows() throws Exception {
        requestHelper.update("test_users", schema -> {
            schema.bool("active", false);
            schema.where("age", ">=", 25);
        });

        try (Statement stmt = connection.getConnection().createStatement()) {
            ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM test_users WHERE active = false");
            assertTrue(rs.next());
            assertEquals(2, rs.getInt(1));
        }
    }

    @Test
    public void testUpdateWithNullValue() throws Exception {
        requestHelper.update("test_users", schema -> {
            schema.string("email", null);
            schema.where("username", "user2");
        });

        try (Statement stmt = connection.getConnection().createStatement()) {
            ResultSet rs = stmt.executeQuery("SELECT email FROM test_users WHERE username = 'user2'");
            assertTrue(rs.next());
            assertNull(rs.getString("email"));
        }
    }

    @Test
    public void testUpdateNoMatches() throws Exception {
        // Update with WHERE condition that matches no rows
        requestHelper.update("test_users", schema -> {
            schema.string("email", "nope@example.com");
            schema.where("username", "nonexistent");
        });

        // Verify original data unchanged
        try (Statement stmt = connection.getConnection().createStatement()) {
            ResultSet rs = stmt.executeQuery("SELECT email FROM test_users WHERE username = 'user1'");
            assertTrue(rs.next());
            assertEquals("user1@example.com", rs.getString("email"));
        }
    }
}