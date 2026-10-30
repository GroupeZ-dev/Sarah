package fr.maxlego08.sarah;

import fr.maxlego08.sarah.database.Schema;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Atomic increments, raw expressions and insert-only columns in update and upsert requests.
 */
public class IncrementRequestTest extends DatabaseTestBase {

    @Override
    protected void afterConnectionSetup() throws Exception {
        SchemaBuilder.create(null, "test_counters", schema -> {
            schema.string("owner", 36).primary();
            schema.string("name", 64).primary();
            schema.decimal("amount", 38, 10);
            schema.bigInt("version");
            schema.string("label", 64).nullable();
        }).execute(connection, testLogger);
    }

    private void upsertAmount(String owner, String name, Number amount, String label) {
        requestHelper.upsert("test_counters", schema -> {
            schema.string("owner", owner).primary();
            schema.string("name", name).primary();
            schema.increment("amount", amount);
            schema.increment("version", 1);
            schema.string("label", label).insertOnly();
        });
    }

    private BigDecimal amount(String owner, String name) {
        List<BigDecimal> values = requestHelper.query("SELECT `amount` FROM test_counters WHERE `owner` = ? AND `name` = ?",
                resultSet -> resultSet.getBigDecimal(1), owner, name);
        assertEquals(1, values.size());
        return values.get(0);
    }

    private long version(String owner, String name) {
        return requestHelper.query("SELECT `version` FROM test_counters WHERE `owner` = ? AND `name` = ?",
                resultSet -> resultSet.getLong(1), owner, name).get(0);
    }

    private String label(String owner, String name) {
        return requestHelper.query("SELECT `label` FROM test_counters WHERE `owner` = ? AND `name` = ?",
                resultSet -> resultSet.getString(1), owner, name).get(0);
    }

    @Test
    public void testUpsertIncrementInsertsThenAdds() {
        upsertAmount("a", "coins", 5, "first");
        assertEquals(0, new BigDecimal("5").compareTo(amount("a", "coins")));
        assertEquals(1, version("a", "coins"));

        upsertAmount("a", "coins", 2.5, "second");
        assertEquals(0, new BigDecimal("7.5").compareTo(amount("a", "coins")));
        assertEquals(2, version("a", "coins"));

        upsertAmount("a", "coins", -10, "third");
        assertEquals(0, new BigDecimal("-2.5").compareTo(amount("a", "coins")));
        assertEquals(3, version("a", "coins"));
    }

    @Test
    public void testInsertOnlyKeepsExistingValue() {
        upsertAmount("a", "coins", 1, "first");
        upsertAmount("a", "coins", 1, "second");
        assertEquals("first", label("a", "coins"));
    }

    @Test
    public void testUpsertWithOnlyInsertOnlyColumnsDoesNothingOnConflict() {
        upsertAmount("a", "coins", 3, "first");
        requestHelper.upsert("test_counters", schema -> {
            schema.string("owner", "a").primary().insertOnly();
            schema.string("name", "coins").primary().insertOnly();
            schema.decimal("amount", 99).insertOnly();
            schema.bigInt("version", 99).insertOnly();
        });
        assertEquals(0, new BigDecimal("3").compareTo(amount("a", "coins")));
    }

    @Test
    public void testUpdateIncrementAndExpression() {
        upsertAmount("a", "coins", 10, null);

        int changed = requestHelper.update("test_counters", schema -> {
            schema.increment("amount", -4);
            schema.expression("version", "`version` + ?", 10);
            schema.where("owner", "a");
            schema.where("name", "coins");
        });

        assertEquals(1, changed);
        assertEquals(0, new BigDecimal("6").compareTo(amount("a", "coins")));
        assertEquals(11, version("a", "coins"));
    }

    @Test
    public void testUpdateReturnsZeroWhenVersionDoesNotMatch() {
        upsertAmount("a", "coins", 10, null);

        int stale = requestHelper.update("test_counters", schema -> {
            schema.decimal("amount", 50);
            schema.increment("version", 1);
            schema.where("owner", "a");
            schema.where("name", "coins");
            schema.where("version", 42);
        });
        int current = requestHelper.update("test_counters", schema -> {
            schema.decimal("amount", 50);
            schema.increment("version", 1);
            schema.where("owner", "a");
            schema.where("name", "coins");
            schema.where("version", 1);
        });

        assertEquals(0, stale);
        assertEquals(1, current);
        assertEquals(0, new BigDecimal("50").compareTo(amount("a", "coins")));
    }

    @Test
    public void testUpsertExpressionOnlyAppliesOnConflict() {
        requestHelper.upsert("test_counters", schema -> {
            schema.string("owner", "a").primary();
            schema.string("name", "coins").primary();
            schema.decimal("amount", 1);
            schema.bigInt("version", 1);
            schema.expression("label", "?", "updated");
        });
        assertEquals(null, label("a", "coins"));

        requestHelper.upsert("test_counters", schema -> {
            schema.string("owner", "a").primary();
            schema.string("name", "coins").primary();
            schema.decimal("amount", 1);
            schema.bigInt("version", 1);
            schema.expression("label", "?", "updated");
        });
        assertEquals("updated", label("a", "coins"));
    }

    @Test
    public void testUpsertMultipleIncrements() {
        upsertAmount("a", "coins", 1, null);

        List<Schema> schemas = new ArrayList<>();
        for (String owner : Arrays.asList("a", "b", "c")) {
            schemas.add(SchemaBuilder.upsert("test_counters", schema -> {
                schema.string("owner", owner).primary();
                schema.string("name", "coins").primary();
                schema.increment("amount", 2);
                schema.increment("version", 1);
                schema.string("label", "batch").insertOnly();
            }));
        }
        assertTrue(requestHelper.upsertMultiple(schemas) > 0);

        assertEquals(0, new BigDecimal("3").compareTo(amount("a", "coins")));
        assertEquals(0, new BigDecimal("2").compareTo(amount("b", "coins")));
        assertEquals(0, new BigDecimal("2").compareTo(amount("c", "coins")));
        assertEquals(2, version("a", "coins"));
        assertEquals(null, label("a", "coins"));
        assertEquals("batch", label("b", "coins"));
    }

    @Test
    public void testUpdateMultipleIncrements() {
        upsertAmount("a", "coins", 1, null);
        upsertAmount("b", "coins", 1, null);

        List<Schema> schemas = new ArrayList<>();
        for (String owner : Arrays.asList("a", "b")) {
            schemas.add(SchemaBuilder.update("test_counters", schema -> {
                schema.increment("amount", 4);
                schema.where("owner", owner);
                schema.where("name", "coins");
            }));
        }

        assertEquals(2, requestHelper.updateMultiple(schemas));
        assertEquals(0, new BigDecimal("5").compareTo(amount("a", "coins")));
        assertEquals(0, new BigDecimal("5").compareTo(amount("b", "coins")));
    }

    @Test
    public void testDeleteReturnsAffectedRows() {
        upsertAmount("a", "coins", 1, null);
        upsertAmount("a", "gems", 1, null);
        upsertAmount("b", "coins", 1, null);

        assertEquals(2, requestHelper.delete("test_counters", schema -> schema.where("owner", "a")));
        assertEquals(0, requestHelper.delete("test_counters", schema -> schema.where("owner", "a")));
    }
}
