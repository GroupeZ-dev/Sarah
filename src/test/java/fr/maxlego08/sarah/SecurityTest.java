package fr.maxlego08.sarah;

import fr.maxlego08.sarah.conditions.JoinCondition;
import fr.maxlego08.sarah.database.DatabaseType;
import fr.maxlego08.sarah.database.Schema;
import fr.maxlego08.sarah.dialect.Dialects;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the checks on the fragments Sarah writes into statements, and for connection safety
 */
public class SecurityTest extends DatabaseTestBase {

    @Override
    protected void afterConnectionSetup() throws Exception {
        SchemaBuilder.create(null, "test_users", schema -> {
            schema.autoIncrementBigInt("id");
            schema.string("username", 50);
            schema.integer("age");
        }).execute(connection, testLogger);
        requestHelper.insert("test_users", s -> s.string("username", "alice").bigInt("age", 25));
        requestHelper.insert("test_users", s -> s.string("username", "bob").bigInt("age", 30));
    }

    @Test
    public void testOperatorInjectionIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> requestHelper.select("test_users", s -> s.where("age", "= 1 OR 1 = 1 --", 5)));
    }

    @Test
    public void testExistingOperatorsStillWork() {
        List<Map<String, Object>> results = requestHelper.select("test_users", s -> s.where("username", "like", "a%"));
        assertEquals(1, results.size());

        for (String operator : new String[]{"=", "!=", "<>", "<=>", ">=", "LIKE", "NOT LIKE", "ILIKE", "REGEXP", "NOT REGEXP", "IS", "IS NOT", "IS DISTINCT FROM", "GLOB"}) {
            assertDoesNotThrow(() -> SchemaBuilder.select("test_users").where("age", operator, 1), operator);
        }
    }

    @Test
    public void testColumnNameCannotEndItsQuotes() {
        assertThrows(IllegalArgumentException.class, () -> requestHelper.select("test_users", s -> s.where("age` = 1 OR `age", 5)));
        assertThrows(IllegalArgumentException.class, () -> requestHelper.select("test_users", s -> s.whereNull("age\" OR 1 = 1 --")));
    }

    @Test
    public void testTableNameInjectionIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> SchemaBuilder.select("test_users; DROP TABLE test_users"));
        assertDoesNotThrow(() -> SchemaBuilder.select("%prefix%users"));
        assertDoesNotThrow(() -> SchemaBuilder.select("information_schema.columns"));
        assertDoesNotThrow(() -> SchemaBuilder.select("test_users AS u"));
        assertDoesNotThrow(() -> SchemaBuilder.select("`%prefix%users`"));
    }

    @Test
    public void testOrderByIsUnchangedButChecked() {
        Schema schema = SchemaBuilder.select("test_users");
        schema.orderByDesc("o.amount");
        assertEquals("ORDER BY o.amount DESC", schema.getOrderBy());
        schema.orderBy("age DESC, id");
        assertEquals("ORDER BY age DESC, id", schema.getOrderBy());

        assertThrows(IllegalArgumentException.class, () -> SchemaBuilder.select("test_users").orderBy("age; DROP TABLE test_users"));

        List<Map<String, Object>> results = requestHelper.select("test_users", s -> s.orderByDesc("age"));
        assertEquals("bob", results.get(0).get("username"));
    }

    @Test
    public void testCoalesceDefaultMustBeALiteral() throws Exception {
        Schema valid = SchemaBuilder.select("test_users");
        valid.addSelect(null, "username", "name", "'it''s none'");
        valid.addSelect(null, "age", "years", 0);
        List<Map<String, Object>> results = valid.executeSelect(connection, testLogger);
        assertEquals(2, results.size());

        Schema invalid = SchemaBuilder.select("test_users");
        invalid.addSelect(null, "username", "name", "0); DROP TABLE test_users --");
        assertThrows(Exception.class, () -> invalid.executeSelect(connection, testLogger));
    }

    @Test
    public void testJoinConditionValueIsEscaped() {
        JoinCondition join = new JoinCondition(JoinCondition.JoinType.LEFT, "test_orders", "o", "user_id", "test_users", "id",
                JoinCondition.and("o", "status", "it's"));
        assertTrue(join.getJoinClause().endsWith("o.status = 'it''s'"), join.getJoinClause());
    }

    @Test
    public void testJoinConditionBackslashCannotEndTheString() {
        JoinCondition join = new JoinCondition(JoinCondition.JoinType.LEFT, "test_orders", "o", "user_id", "test_users", "id",
                JoinCondition.and("o", "status", "\\' OR 1=1 -- "));
        // MySQL reads a backslash as an escape, so it is escaped too and the quote stays inside the string
        assertTrue(join.getJoinClause(Dialects.of(DatabaseType.MYSQL)).endsWith("o.status = '\\\\'' OR 1=1 -- '"));
        assertTrue(join.getJoinClause(Dialects.of(DatabaseType.MARIADB)).endsWith("o.status = '\\\\'' OR 1=1 -- '"));
        assertTrue(join.getJoinClause().endsWith("o.status = '\\\\'' OR 1=1 -- '"));
        // PostgreSQL and SQLite do not, so the backslash is written once
        assertTrue(join.getJoinClause(Dialects.of(DatabaseType.POSTGRESQL)).endsWith("o.status = '\\'' OR 1=1 -- '"));
        assertTrue(join.getJoinClause(Dialects.of(DatabaseType.SQLITE)).endsWith("o.status = '\\'' OR 1=1 -- '"));
    }

    @Test
    public void testHashCommentIsRejected() {
        // MySQL and MariaDB read # as the start of a comment
        assertThrows(IllegalArgumentException.class, () -> SchemaBuilder.select("test_users#"));
        assertThrows(IllegalArgumentException.class, () -> SchemaBuilder.select("test_users").orderBy("age #"));
        assertThrows(IllegalArgumentException.class, () -> SchemaBuilder.select("test_users").whereNull("age IS NULL OR 1=1 #"));
        assertThrows(IllegalArgumentException.class, () -> SchemaBuilder.select("test_users").where("age", "=#", 1));
        Schema coalesce = SchemaBuilder.select("test_users");
        coalesce.addSelect(null, "age", "years", "0 #");
        assertThrows(IllegalArgumentException.class, () -> coalesce.getSelectColumns().get(0).getSelectColumn());
        // A string left open could be closed by a later part of the statement
        assertThrows(IllegalArgumentException.class, () -> SchemaBuilder.select("test_users").orderBy("age, '"));

        // Inside a quoted string, or a quoted column name, # is just a character
        assertDoesNotThrow(() -> SchemaBuilder.select("test_users").orderBy("FIELD(username, '#1', 'it''s #2')"));
        assertDoesNotThrow(() -> SchemaBuilder.select("test_users").where("tag#", 1));
    }

    @Test
    public void testPasswordIsNotPrinted() {
        DatabaseConfiguration configuration = DatabaseConfiguration.create("user", "s3cret", 3306, "localhost", "db");
        assertFalse(configuration.toString().contains("s3cret"));
    }

    @Test
    public void testConfigurationOptions() {
        DatabaseConfiguration configuration = DatabaseConfiguration.create("user", "pass", 3306, "localhost", "db")
                .withProperty("useSSL", "true")
                .withTimeouts(5000L, 0L);

        assertEquals("true", configuration.getProperties().get("useSSL"));
        assertEquals(5000L, configuration.getConnectionTimeout());
        assertEquals(0L, configuration.getLeakDetectionThreshold());
        // Still on by default, but as a property that withProperty("allowMultiQueries", "false") replaces
        assertFalse(Dialects.of(DatabaseType.MYSQL).jdbcUrl(configuration).contains("allowMultiQueries"));
        assertEquals("true", Dialects.of(DatabaseType.MYSQL).connectionProperties().get("allowMultiQueries"));
        assertEquals("true", Dialects.of(DatabaseType.MYSQL).dataSourceProperties().get("allowMultiQueries"));
        assertEquals("true", Dialects.of(DatabaseType.MARIADB).dataSourceProperties().get("allowMultiQueries"));
    }

    @Test
    public void testEachRequestGetsItsOwnConnection() throws Exception {
        try (Connection first = connection.getConnection(); Connection second = connection.getConnection()) {
            assertNotSame(first, second);
            first.close();
            assertFalse(second.isClosed());
        }
    }
}
