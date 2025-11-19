package fr.maxlego08.sarah;

import fr.maxlego08.sarah.database.Schema;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test suite for JOIN operations
 */
public class JoinConditionTest extends DatabaseTestBase {

    @Override
    protected void afterConnectionSetup() throws Exception {
        // Create users table
        SchemaBuilder.create(null, "test_users", schema -> {
            schema.autoIncrementBigInt("id");
            schema.string("username", 50);
            schema.string("email", 100);
        }).execute(connection, testLogger);

        // Create orders table
        SchemaBuilder.create(null, "test_orders", schema -> {
            schema.autoIncrementBigInt("id");
            schema.bigInt("user_id");
            schema.string("product", 100);
            schema.decimal("amount", 10, 2);
        }).execute(connection, testLogger);
    }

    @BeforeEach
    public void insertTestData() {
        // Insert users
        requestHelper.insert("test_users", schema -> {
            schema.string("username", "alice");
            schema.string("email", "alice@example.com");
        });

        requestHelper.insert("test_users", schema -> {
            schema.string("username", "bob");
            schema.string("email", "bob@example.com");
        });

        requestHelper.insert("test_users", schema -> {
            schema.string("username", "charlie");
            schema.string("email", "charlie@example.com");
        });

        // Insert orders
        requestHelper.insert("test_orders", schema -> {
            schema.bigInt("user_id", 1);
            schema.string("product", "Laptop");
            schema.decimal("amount", 999.99);
        });

        requestHelper.insert("test_orders", schema -> {
            schema.bigInt("user_id", 1);
            schema.string("product", "Mouse");
            schema.decimal("amount", 29.99);
        });

        requestHelper.insert("test_orders", schema -> {
            schema.bigInt("user_id", 2);
            schema.string("product", "Keyboard");
            schema.decimal("amount", 79.99);
        });
    }

    @Test
    public void testLeftJoin() {
        Schema schema = SchemaBuilder.select("test_users");
        schema.addSelect("test_users", "username");
        schema.addSelect("test_orders", "product");
        schema.leftJoin("test_users", "u", "id", "test_orders", "user_id");

        try {
            List<Map<String, Object>> results = schema.executeSelect(connection, testLogger);
            // Should return all users, even charlie who has no orders
            assertTrue(results.size() >= 3);

            // Verify alice has orders
            boolean foundAlice = false;
            for (Map<String, Object> row : results) {
                if ("alice".equals(row.get("username"))) {
                    foundAlice = true;
                    assertNotNull(row.get("product"));
                }
            }
            assertTrue(foundAlice);
        } catch (Exception e) {
            fail("Exception thrown: " + e.getMessage());
        }
    }

    @Test
    public void testInnerJoin() {
        Schema schema = SchemaBuilder.select("test_users");
        schema.addSelect("test_users", "username");
        schema.addSelect("test_orders", "product");
        schema.innerJoin("test_users", "u", "id", "test_orders", "user_id");

        try {
            List<Map<String, Object>> results = schema.executeSelect(connection, testLogger);
            // Should return only users with orders (alice and bob)
            assertEquals(3, results.size()); // 2 orders for alice, 1 for bob

            // Verify charlie (no orders) is not included
            for (Map<String, Object> row : results) {
                assertNotEquals("charlie", row.get("username"));
            }
        } catch (Exception e) {
            fail("Exception thrown: " + e.getMessage());
        }
    }

    @Test
    public void testJoinWithWhereCondition() {
        Schema schema = SchemaBuilder.select("test_users");
        schema.addSelect("test_users", "username");
        schema.addSelect("test_orders", "product");
        schema.addSelect("test_orders", "amount");
        schema.innerJoin("test_users", "u", "id", "test_orders", "user_id");
        schema.where("test_orders", "amount", ">", 50);

        try {
            List<Map<String, Object>> results = schema.executeSelect(connection, testLogger);
            // Should return only orders with amount > 50 (Laptop and Keyboard)
            assertEquals(2, results.size());

            for (Map<String, Object> row : results) {
                String product = (String) row.get("product");
                assertTrue(product.equals("Laptop") || product.equals("Keyboard"));
            }
        } catch (Exception e) {
            fail("Exception thrown: " + e.getMessage());
        }
    }

    @Test
    public void testJoinWithOrderBy() {
        Schema schema = SchemaBuilder.select("test_users");
        schema.addSelect("test_users", "username");
        schema.addSelect("test_orders", "product");
        schema.addSelect("test_orders", "amount");
        schema.innerJoin("test_users", "u", "id", "test_orders", "user_id");
        schema.orderByDesc("amount");

        try {
            List<Map<String, Object>> results = schema.executeSelect(connection, testLogger);
            assertEquals(3, results.size());

            // First result should be the most expensive order (Laptop)
            assertEquals("Laptop", results.get(0).get("product"));
        } catch (Exception e) {
            fail("Exception thrown: " + e.getMessage());
        }
    }

    @Test
    public void testMultipleJoins() throws SQLException {
        // Create a third table
        SchemaBuilder.create(null, "test_profiles", schema -> {
            schema.autoIncrementBigInt("id");
            schema.bigInt("user_id");
            schema.string("bio", 255);
        }).execute(connection, testLogger);

        requestHelper.insert("test_profiles", schema -> {
            schema.bigInt("user_id", 1);
            schema.string("bio", "Alice's bio");
        });

        Schema selectSchema = SchemaBuilder.select("test_users");
        selectSchema.addSelect("test_users", "username");
        selectSchema.addSelect("test_orders", "product");
        selectSchema.addSelect("test_profiles", "bio");
        selectSchema.innerJoin("test_users", "u", "id", "test_orders", "user_id");
        selectSchema.innerJoin("test_users", "u", "id", "test_profiles", "user_id");

        try {
            List<Map<String, Object>> results = selectSchema.executeSelect(connection, testLogger);
            // Should return only alice's orders (she has both orders and profile)
            assertTrue(results.size() >= 2);

            for (Map<String, Object> row : results) {
                assertEquals("alice", row.get("username"));
                assertEquals("Alice's bio", row.get("bio"));
            }
        } catch (Exception e) {
            fail("Exception thrown: " + e.getMessage());
        }
    }
}