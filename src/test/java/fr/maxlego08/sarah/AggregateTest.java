package fr.maxlego08.sarah;

import fr.maxlego08.sarah.exceptions.DatabaseException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test suite for GROUP BY, HAVING, aggregates and exists
 */
public class AggregateTest extends DatabaseTestBase {

    @Override
    protected void afterConnectionSetup() throws Exception {
        SchemaBuilder.create(null, "test_sales", schema -> {
            schema.autoIncrementBigInt("id");
            schema.string("seller", 50);
            schema.integer("amount");
        }).execute(connection, testLogger);
    }

    @BeforeEach
    public void insertTestData() {
        insertSale("alice", 10);
        insertSale("alice", 30);
        insertSale("bob", 5);
        insertSale("charlie", 20);
        insertSale("charlie", 20);
        insertSale("charlie", 50);
    }

    private void insertSale(String seller, int amount) {
        requestHelper.insert("test_sales", schema -> {
            schema.string("seller", seller);
            schema.bigInt("amount", amount);
        });
    }

    @Test
    public void testSumMaxMinAvg() {
        assertEquals(135, requestHelper.sum("test_sales", "amount", schema -> {
        }).intValue());
        assertEquals(50, requestHelper.max("test_sales", "amount", schema -> {
        }).intValue());
        assertEquals(5, requestHelper.min("test_sales", "amount", schema -> {
        }).intValue());
        assertEquals(20.0, requestHelper.avg("test_sales", "amount", schema -> schema.where("seller", "alice")).doubleValue(), 0.001);
    }

    @Test
    public void testAggregateUsesConditions() {
        assertEquals(90, requestHelper.sum("test_sales", "amount", schema -> schema.where("seller", "charlie")).intValue());
        assertEquals(35, requestHelper.sum("test_sales", "amount", schema -> schema.where("seller", "bob").orWhere("seller", "alice").where("amount", ">", 10)).intValue());
    }

    @Test
    public void testAggregateWithoutRowsIsNull() {
        assertNull(requestHelper.sum("test_sales", "amount", schema -> schema.where("seller", "nobody")));
        assertNull(requestHelper.max("test_sales", "amount", schema -> schema.where("seller", "nobody")));
    }

    @Test
    public void testExists() {
        assertTrue(requestHelper.exists("test_sales", schema -> schema.where("seller", "bob")));
        assertFalse(requestHelper.exists("test_sales", schema -> schema.where("seller", "nobody")));
        assertTrue(requestHelper.exists("test_sales", schema -> {
        }));
    }

    @Test
    public void testGroupByAndHaving() {
        List<Map<String, Object>> results = requestHelper.select("test_sales", schema -> {
            schema.addSelect("seller");
            schema.where("amount", ">", 5);
            schema.groupBy("seller");
            schema.having("SUM(`amount`)", ">=", 40);
            schema.orderBy("seller");
        });

        // bob's only sale is filtered out by WHERE, so the group does not exist at all
        assertEquals(2, results.size());
        assertEquals("alice", results.get(0).get("seller"));
        assertEquals("charlie", results.get(1).get("seller"));
    }

    @Test
    public void testHavingCount() {
        List<Map<String, Object>> results = requestHelper.select("test_sales", schema -> {
            schema.addSelect("seller");
            schema.groupBy("seller");
            schema.having("COUNT(*)", ">", 1);
            schema.having("MIN(`amount`)", ">", 15);
        });

        assertEquals(1, results.size());
        assertEquals("charlie", results.get(0).get("seller"));
    }

    @Test
    public void testUnsupportedAggregateAndUnsafeExpressionsAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> SchemaBuilder.select("test_sales")
                .executeSelectAggregate("DROP", "amount", connection, testLogger));
        assertThrows(IllegalArgumentException.class, () -> SchemaBuilder.select("test_sales").groupBy("seller; DROP TABLE test_sales"));
        assertThrows(IllegalArgumentException.class, () -> SchemaBuilder.select("test_sales").having("COUNT(*) -- ", ">", 1));
    }

    @Test
    public void testFailedAggregateFollowsStrictReads() {
        assertNull(requestHelper.sum("test_missing_table", "amount", schema -> {
        }));
        assertFalse(requestHelper.exists("test_missing_table", schema -> {
        }));

        requestHelper.setStrictReads(true);
        assertThrows(DatabaseException.class, () -> requestHelper.sum("test_missing_table", "amount", schema -> {
        }));
        assertThrows(DatabaseException.class, () -> requestHelper.exists("test_missing_table", schema -> {
        }));
    }
}
