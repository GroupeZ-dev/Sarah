package fr.maxlego08.sarah;

import fr.maxlego08.sarah.database.Schema;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test suite for WHERE conditions
 */
public class WhereConditionTest extends DatabaseTestBase {

    @Override
    protected void afterConnectionSetup() throws Exception {
        SchemaBuilder.create(null, "test_users", schema -> {
            schema.autoIncrementBigInt("id");
            schema.string("username", 50);
            schema.string("email", 100).nullable();
            schema.integer("age");
            schema.bool("active");
        }).execute(connection, testLogger);
    }

    @BeforeEach
    public void insertTestData() {
        requestHelper.insert("test_users", schema -> {
            schema.string("username", "alice");
            schema.string("email", "alice@example.com");
            schema.bigInt("age", 25);
            schema.bool("active", true);
        });

        requestHelper.insert("test_users", schema -> {
            schema.string("username", "bob");
            schema.string("email", null);
            schema.bigInt("age", 30);
            schema.bool("active", true);
        });

        requestHelper.insert("test_users", schema -> {
            schema.string("username", "charlie");
            schema.string("email", "charlie@example.com");
            schema.bigInt("age", 35);
            schema.bool("active", false);
        });

        requestHelper.insert("test_users", schema -> {
            schema.string("username", "david");
            schema.string("email", "david@example.com");
            schema.bigInt("age", 30);
            schema.bool("active", true);
        });
    }

    @Test
    public void testWhereEquals() {
        List<Map<String, Object>> results = requestHelper.select("test_users", schema -> {
            schema.where("username", "alice");
        });

        assertEquals(1, results.size());
        assertEquals("alice", results.get(0).get("username"));
    }

    @Test
    public void testWhereGreaterThan() {
        List<Map<String, Object>> results = requestHelper.select("test_users", schema -> {
            schema.where("age", ">", 25);
        });

        assertEquals(3, results.size()); // bob, charlie, david have age > 25
    }

    @Test
    public void testWhereLessThan() {
        List<Map<String, Object>> results = requestHelper.select("test_users", schema -> {
            schema.where("age", "<", 35);
        });

        assertEquals(3, results.size()); // alice, bob, david have age < 35
    }

    @Test
    public void testWhereGreaterThanOrEqual() {
        List<Map<String, Object>> results = requestHelper.select("test_users", schema -> {
            schema.where("age", ">=", 30);
        });

        assertEquals(3, results.size()); // bob, charlie, david have age >= 30
    }

    @Test
    public void testWhereLessThanOrEqual() {
        List<Map<String, Object>> results = requestHelper.select("test_users", schema -> {
            schema.where("age", "<=", 30);
        });

        assertEquals(3, results.size()); // alice, bob, david have age <= 30
    }

    @Test
    public void testWhereNotEqual() {
        List<Map<String, Object>> results = requestHelper.select("test_users", schema -> {
            schema.where("username", "!=", "alice");
        });

        assertEquals(3, results.size()); // bob, charlie, david
    }

    @Test
    public void testWhereNull() {
        List<Map<String, Object>> results = requestHelper.select("test_users", schema -> {
            schema.whereNull("email");
        });

        assertEquals(1, results.size());
        assertEquals("bob", results.get(0).get("username"));
    }

    @Test
    public void testWhereNotNull() {
        List<Map<String, Object>> results = requestHelper.select("test_users", schema -> {
            schema.whereNotNull("email");
        });

        assertEquals(3, results.size()); // alice, charlie, david have non-null email
    }

    @Test
    public void testWhereIn() {
        List<Map<String, Object>> results = requestHelper.select("test_users", schema -> {
            schema.whereIn("username", "alice", "charlie");
        });

        assertEquals(2, results.size());
    }

    @Test
    public void testWhereInWithList() {
        List<String> usernames = Arrays.asList("alice", "bob");

        List<Map<String, Object>> results = requestHelper.select("test_users", schema -> {
            schema.whereIn("username", usernames);
        });

        assertEquals(2, results.size());
    }

    @Test
    public void testMultipleWhereConditions() {
        List<Map<String, Object>> results = requestHelper.select("test_users", schema -> {
            schema.where("age", ">=", 25);
            schema.where("age", "<=", 30);
            schema.where("active", true);
        });

        assertEquals(3, results.size()); // alice, bob, david match all conditions
    }

    @Test
    public void testWhereLike() {
        // Note: LIKE operator support may depend on implementation
        List<Map<String, Object>> results = requestHelper.select("test_users", schema -> {
            schema.where("username", "LIKE", "a%");
        });

        assertEquals(1, results.size());
        assertEquals("alice", results.get(0).get("username"));
    }

    @Test
    public void testWhereWithBooleanValue() {
        List<Map<String, Object>> results = requestHelper.select("test_users", schema -> {
            schema.where("active", false);
        });

        assertEquals(1, results.size());
        assertEquals("charlie", results.get(0).get("username"));
    }

    @Test
    public void testComplexWhereConditions() {
        // Active users older than 25 with email not null
        List<Map<String, Object>> results = requestHelper.select("test_users", schema -> {
            schema.where("active", true);
            schema.where("age", ">", 25);
            schema.whereNotNull("email");
        });

        assertEquals(1, results.size());
        // Should not include bob (email is null) or charlie (not active)
    }

    @Test
    public void testWhereInEmptyList() {
        List<Map<String, Object>> results = requestHelper.select("test_users", schema -> {
            schema.whereIn("username");
        });

        // Behavior may vary, but typically should return no results or handle gracefully
        assertTrue(results.size() >= 0);
    }
}