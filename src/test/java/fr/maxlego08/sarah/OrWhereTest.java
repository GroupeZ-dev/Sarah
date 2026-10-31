package fr.maxlego08.sarah;

import fr.maxlego08.sarah.database.Schema;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Test suite for OR conditions and groups of conditions
 */
public class OrWhereTest extends DatabaseTestBase {

    @Override
    protected void afterConnectionSetup() throws Exception {
        SchemaBuilder.create(null, "test_members", schema -> {
            schema.autoIncrementBigInt("id");
            schema.string("username", 50);
            schema.string("role", 20).nullable();
            schema.integer("age");
            schema.bool("active");
        }).execute(connection, testLogger);
    }

    @BeforeEach
    public void insertTestData() {
        insertMember("alice", "admin", 25, true);
        insertMember("bob", "member", 30, true);
        insertMember("charlie", "owner", 35, false);
        insertMember("david", null, 40, true);
    }

    private void insertMember(String username, String role, int age, boolean active) {
        requestHelper.insert("test_members", schema -> {
            schema.string("username", username);
            schema.string("role", role);
            schema.bigInt("age", age);
            schema.bool("active", active);
        });
    }

    private static String whereClause(Consumer<Schema> consumer) {
        Schema schema = SchemaBuilder.select("test_members");
        consumer.accept(schema);
        StringBuilder sql = new StringBuilder();
        schema.whereConditions(sql);
        return sql.toString();
    }

    private List<String> usernames(Consumer<Schema> consumer) {
        return requestHelper.select("test_members", consumer).stream()
                .map(row -> (String) row.get("username"))
                .sorted()
                .collect(Collectors.toList());
    }

    @Test
    public void testExistingConditionsStillJoinWithAnd() {
        assertEquals("", whereClause(schema -> {
        }));
        assertEquals(" WHERE `username` = ? AND `age` > ? AND role IS NULL AND active IS NOT NULL AND `age` IN (?,?)",
                whereClause(schema -> {
                    schema.where("username", "alice");
                    schema.where("age", ">", 20);
                    schema.whereNull("role");
                    schema.whereNotNull("active");
                    schema.whereIn("age", 25, 30);
                }));
    }

    @Test
    public void testOrWhereSql() {
        assertEquals(" WHERE `role` = ? OR `role` = ? OR role IS NULL",
                whereClause(schema -> schema.where("role", "admin").orWhere("role", "owner").orWhereNull("role")));
    }

    @Test
    public void testGroupSql() {
        assertEquals(" WHERE `active` = ? AND (`role` = ? OR `age` >= ?)",
                whereClause(schema -> schema.where("active", true)
                        .where(group -> group.where("role", "admin").orWhere("age", ">=", 40))));
        assertEquals(" WHERE `role` = ? OR (`active` = ? AND `age` BETWEEN ? AND ?)",
                whereClause(schema -> schema.where("role", "owner")
                        .orWhere(group -> group.where("active", true).whereBetween("age", 26, 35))));
    }

    @Test
    public void testEmptyGroupAddsNothing() {
        assertEquals(" WHERE `active` = ?", whereClause(schema -> schema.where("active", true).orWhere(group -> {
        })));
    }

    @Test
    public void testOrWhere() {
        assertEquals(Arrays.asList("alice", "charlie"), usernames(schema -> schema.where("role", "admin").orWhere("role", "owner")));
    }

    @Test
    public void testAndIsAppliedBeforeOr() {
        // (active AND age > 26) OR role IS NULL
        assertEquals(Arrays.asList("bob", "david"), usernames(schema -> schema
                .where("active", true)
                .where("age", ">", 26)
                .orWhereNull("role")));
    }

    @Test
    public void testGroupChangesPrecedence() {
        // active AND (role = admin OR role IS NULL)
        assertEquals(Arrays.asList("alice", "david"), usernames(schema -> schema
                .where("active", true)
                .where(group -> group.where("role", "admin").orWhereNull("role"))));
    }

    @Test
    public void testParametersAreBoundInOrderAcrossGroups() {
        List<Map<String, Object>> results = requestHelper.select("test_members", schema -> schema
                .whereIn("username", "alice", "bob", "charlie")
                .where(group -> group.orWhereIn("role", "owner").orWhereBetween("age", 29, 31))
                .orWhere(group -> group.where("username", "david").whereNotNull("age")));

        assertEquals(Arrays.asList("bob", "charlie", "david"), results.stream()
                .map(row -> (String) row.get("username")).sorted().collect(Collectors.toList()));
    }

    @Test
    public void testOrWhereInUpdateCountAndDelete() {
        assertEquals(2, requestHelper.updateAndCount("test_members", schema -> {
            schema.bool("active", false);
            schema.where("username", "alice").orWhere("username", "bob");
        }));
        assertEquals(3, requestHelper.count("test_members", schema -> schema.where("active", false)));

        assertEquals(2, requestHelper.deleteAndCount("test_members", schema -> schema.where("age", "<", 30).orWhere("age", ">", 35)));
        assertEquals(Arrays.asList("bob", "charlie"), usernames(schema -> {
        }));
    }
}
