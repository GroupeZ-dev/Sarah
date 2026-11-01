package fr.maxlego08.sarah;

import fr.maxlego08.sarah.exceptions.DatabaseException;
import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Raw statements, paging, transactions, parameter conversion and error reporting.
 */
public class RawQueryAndTransactionTest extends DatabaseTestBase {

    @Override
    protected void afterConnectionSetup() throws Exception {
        SchemaBuilder.create(null, "test_pages", schema -> {
            schema.integer("position").primary();
            schema.string("name", 64);
            schema.timestamp("seen_at").nullable();
        }).execute(connection, testLogger);
    }

    private void insertRows(int count) {
        for (int index = 0; index < count; index++) {
            int position = index;
            requestHelper.insert("test_pages", schema -> {
                schema.object("position", position);
                schema.string("name", "row-" + position);
            });
        }
    }

    @Test
    public void testLimitAndOffset() {
        insertRows(10);

        List<Map<String, Object>> page = requestHelper.select("test_pages", schema -> {
            schema.orderBy("position");
            schema.limit(3).offset(4);
        });

        assertEquals(3, page.size());
        assertEquals("row-4", page.get(0).get("name"));
        assertEquals("row-6", page.get(2).get("name"));
    }

    @Test
    public void testOffsetWithoutLimit() {
        insertRows(5);

        List<Map<String, Object>> rest = requestHelper.select("test_pages", schema -> {
            schema.orderBy("position");
            schema.offset(3);
        });

        assertEquals(2, rest.size());
        assertEquals("row-3", rest.get(0).get("name"));
    }

    @Test
    public void testExecuteAndQuery() {
        insertRows(3);

        int changed = requestHelper.execute("UPDATE test_pages SET `name` = ? WHERE `position` >= ?", "renamed", 1);
        List<String> names = requestHelper.query("SELECT `name` FROM test_pages ORDER BY `position`", resultSet -> resultSet.getString(1));

        assertEquals(2, changed);
        assertEquals("row-0", names.get(0));
        assertEquals("renamed", names.get(2));
    }

    @Test
    public void testDateAndUuidParametersAreConverted() {
        Date now = new Date();
        UUID id = UUID.randomUUID();

        requestHelper.insert("test_pages", schema -> {
            schema.object("position", 1);
            schema.object("name", id);
            schema.object("seen_at", now);
        });

        List<String> names = requestHelper.query("SELECT `name` FROM test_pages WHERE `seen_at` IS NOT NULL", resultSet -> resultSet.getString(1));
        assertEquals(id.toString(), names.get(0));
    }

    @Test
    public void testTransactionCommits() {
        int result = requestHelper.inTransaction(tx -> {
            tx.insert("test_pages", schema -> {
                schema.object("position", 1);
                schema.string("name", "one");
            });
            tx.insert("test_pages", schema -> {
                schema.object("position", 2);
                schema.string("name", "two");
            });
            return tx.count("test_pages", schema -> {
            }) > 0 ? 2 : 0;
        });

        assertEquals(2, result);
        assertEquals(2, requestHelper.count("test_pages", schema -> {
        }));
    }

    @Test
    public void testTransactionRollsBackOnException() {
        assertThrows(IllegalStateException.class, () -> requestHelper.inTransaction(tx -> {
            tx.insert("test_pages", schema -> {
                schema.object("position", 1);
                schema.string("name", "one");
            });
            throw new IllegalStateException("abort");
        }));

        assertEquals(0, requestHelper.count("test_pages", schema -> {
        }));
    }

    @Test
    public void testTransactionRollsBackOnFailedStatement() {
        insertRows(1);

        assertThrows(DatabaseException.class, () -> requestHelper.inTransaction(tx -> {
            tx.insert("test_pages", schema -> {
                schema.object("position", 5);
                schema.string("name", "five");
            });
            tx.insert("test_pages", schema -> {
                schema.object("position", 0);
                schema.string("name", "duplicate");
            });
            return null;
        }));

        assertEquals(1, requestHelper.count("test_pages", schema -> {
        }));
    }

    @Test
    public void testSelectOnMissingTableReturnsEmptyByDefault() {
        assertTrue(requestHelper.select("test_missing_table", schema -> {
        }).isEmpty());
        assertEquals(0, requestHelper.count("test_missing_table", schema -> {
        }));
    }

    @Test
    public void testSelectOnMissingTableThrowsWithStrictReads() {
        requestHelper.setStrictReads(true);
        assertThrows(DatabaseException.class, () -> requestHelper.select("test_missing_table", schema -> {
        }));
        assertThrows(DatabaseException.class, () -> requestHelper.selectAll("test_missing_table", Object.class));
        assertThrows(DatabaseException.class, () -> requestHelper.count("test_missing_table", schema -> {
        }));
    }

    @Test
    public void testConnectionIsReleasedAfterTransactions() {
        for (int index = 0; index < 50; index++) {
            int position = index;
            requestHelper.inTransaction(tx -> tx.insertAndGetKey("test_pages", schema -> {
                schema.object("position", position);
                schema.string("name", "row");
            }));
        }
        assertTrue(requestHelper.count("test_pages", schema -> {
        }) == 50);
    }
}
