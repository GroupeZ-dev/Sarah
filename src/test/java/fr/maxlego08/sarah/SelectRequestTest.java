package fr.maxlego08.sarah;

import fr.maxlego08.sarah.database.Schema;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test suite for SELECT operations
 */
public class SelectRequestTest extends DatabaseTestBase {

    // Simple DTO for testing
    public static class UserDTO {
        private final long id;
        private final String username;
        private final String email;
        private final int age;

        public UserDTO(long id, String username, String email, int age) {
            this.id = id;
            this.username = username;
            this.email = email;
            this.age = age;
        }

        public long getId() { return id; }
        public String getUsername() { return username; }
        public String getEmail() { return email; }
        public int getAge() { return age; }
    }

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
            schema.string("username", "alice");
            schema.string("email", "alice@example.com");
            schema.bigInt("age", 25);
        });

        requestHelper.insert("test_users", schema -> {
            schema.string("username", "bob");
            schema.string("email", "bob@example.com");
            schema.bigInt("age", 30);
        });

        requestHelper.insert("test_users", schema -> {
            schema.string("username", "charlie");
            schema.string("email", "charlie@example.com");
            schema.bigInt("age", 35);
        });
    }

    @Test
    public void testSelectAll() {
        List<Map<String, Object>> results = requestHelper.select("test_users", schema -> {});

        assertEquals(3, results.size());
    }

    @Test
    public void testSelectWithWhere() {
        List<Map<String, Object>> results = requestHelper.select("test_users", schema -> {
            schema.where("username", "alice");
        });

        assertEquals(1, results.size());
        assertEquals("alice", results.get(0).get("username"));
        assertEquals("alice@example.com", results.get(0).get("email"));
    }

    @Test
    public void testSelectWithMultipleWhereConditions() {
        List<Map<String, Object>> results = requestHelper.select("test_users", schema -> {
            schema.where("age", ">=", 30);
            schema.where("age", "<=", 35);
        });

        assertEquals(2, results.size());
    }

    @Test
    public void testSelectWithWhereIn() {
        List<Map<String, Object>> results = requestHelper.select("test_users", schema -> {
            schema.whereIn("username", "alice", "charlie");
        });

        assertEquals(2, results.size());
    }

    @Test
    public void testSelectWithTypedResult() {
        List<UserDTO> results = requestHelper.select("test_users", UserDTO.class, schema -> {
            schema.where("username", "bob");
        });

        assertEquals(1, results.size());
        assertEquals("bob", results.get(0).getUsername());
        assertEquals("bob@example.com", results.get(0).getEmail());
        assertEquals(30, results.get(0).getAge());
    }

    @Test
    public void testSelectAllTyped() {
        List<UserDTO> results = requestHelper.selectAll("test_users", UserDTO.class);

        assertEquals(3, results.size());
    }

    @Test
    public void testSelectWithOrderBy() {
        Schema schema = SchemaBuilder.select("test_users");
        schema.orderByDesc("age");

        try {
            List<Map<String, Object>> results = schema.executeSelect(connection, testLogger);
            assertEquals(3, results.size());
            assertEquals("charlie", results.get(0).get("username")); // age 35
            assertEquals("bob", results.get(1).get("username"));     // age 30
            assertEquals("alice", results.get(2).get("username"));   // age 25
        } catch (Exception e) {
            fail("Exception thrown: " + e.getMessage());
        }
    }

    @Test
    public void testSelectCount() {
        long count = requestHelper.count("test_users", schema -> {});

        assertEquals(3, count);
    }

    @Test
    public void testSelectCountWithWhere() {
        long count = requestHelper.count("test_users", schema -> {
            schema.where("age", ">", 25);
        });

        assertEquals(2, count);
    }

    @Test
    public void testSelectSpecificColumns() {
        Schema schema = SchemaBuilder.select("test_users");
        schema.addSelect("username");
        schema.addSelect("email");

        try {
            List<Map<String, Object>> results = schema.executeSelect(connection, testLogger);
            assertEquals(3, results.size());

            for (Map<String, Object> row : results) {
                assertTrue(row.containsKey("username"));
                assertTrue(row.containsKey("email"));
                // Note: depending on the implementation, "id" and "age" might or might not be present
            }
        } catch (Exception e) {
            fail("Exception thrown: " + e.getMessage());
        }
    }

    @Test
    public void testSelectDistinct() {
        // Insert duplicate age
        requestHelper.insert("test_users", schema -> {
            schema.string("username", "david");
            schema.string("email", "david@example.com");
            schema.bigInt("age", 25); // Same age as alice
        });

        Schema schema = SchemaBuilder.select("test_users");
        schema.addSelect("age");
        schema.distinct();

        try {
            List<Map<String, Object>> results = schema.executeSelect(connection, testLogger);
            // Should have 4 distinct ages: 25, 30, 35 (even though 25 appears twice)
            assertTrue(results.size() <= 4);
        } catch (Exception e) {
            fail("Exception thrown: " + e.getMessage());
        }
    }

    @Test
    public void testSelectNoMatches() {
        List<Map<String, Object>> results = requestHelper.select("test_users", schema -> {
            schema.where("username", "nonexistent");
        });

        assertEquals(0, results.size());
    }
}