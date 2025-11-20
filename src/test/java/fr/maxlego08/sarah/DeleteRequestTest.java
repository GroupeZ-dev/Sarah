package fr.maxlego08.sarah;

import fr.maxlego08.sarah.database.Schema;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.ResultSet;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test suite for DELETE operations
 */
public class DeleteRequestTest extends DatabaseTestBase {

    @Override
    protected void afterConnectionSetup() throws Exception {
        SchemaBuilder.create(null, "test_users", schema -> {
            schema.autoIncrementBigInt("id");
            schema.string("username", 50);
            schema.string("email", 100);
            schema.integer("age");
        }).execute(connection, testLogger);
    }

    @BeforeEach
    public void insertTestData() {
        requestHelper.insert("test_users", schema -> {
            schema.string("username", "user1");
            schema.string("email", "user1@example.com");
            schema.bigInt("age", 25);
        });

        requestHelper.insert("test_users", schema -> {
            schema.string("username", "user2");
            schema.string("email", "user2@example.com");
            schema.bigInt("age", 30);
        });

        requestHelper.insert("test_users", schema -> {
            schema.string("username", "user3");
            schema.string("email", "user3@example.com");
            schema.bigInt("age", 35);
        });
    }

    @Test
    public void testDeleteSingleRow() throws Exception {
        requestHelper.delete("test_users", schema -> {
            schema.where("username", "user1");
        });

        assertEquals(2, countRows("test_users"));

        try (Statement stmt = connection.getConnection().createStatement()) {
            ResultSet rs = stmt.executeQuery("SELECT * FROM test_users WHERE username = 'user1'");
            assertFalse(rs.next());
        }
    }

    @Test
    public void testDeleteMultipleRows() throws Exception {
        requestHelper.delete("test_users", schema -> {
            schema.where("age", ">=", 30);
        });

        assertEquals(1, countRows("test_users"));

        try (Statement stmt = connection.getConnection().createStatement()) {
            ResultSet rs = stmt.executeQuery("SELECT username FROM test_users");
            assertTrue(rs.next());
            assertEquals("user1", rs.getString("username"));
            assertFalse(rs.next());
        }
    }

    @Test
    public void testDeleteWithMultipleConditions() throws Exception {
        requestHelper.delete("test_users", schema -> {
            schema.where("age", ">", 25);
            schema.where("age", "<", 35);
        });

        assertEquals(2, countRows("test_users"));
    }

    @Test
    public void testDeleteNoMatches() throws Exception {
        requestHelper.delete("test_users", schema -> {
            schema.where("username", "nonexistent");
        });

        assertEquals(3, countRows("test_users"));
    }

    @Test
    public void testDeleteAll() throws Exception {
        // Be careful with this in production!
        SchemaBuilder.delete("test_users").execute(connection, testLogger);

        assertEquals(0, countRows("test_users"));
    }
}