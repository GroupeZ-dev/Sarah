package fr.maxlego08.sarah;

import fr.maxlego08.sarah.conditions.JoinCondition;
import fr.maxlego08.sarah.conditions.OrderByCondition;
import fr.maxlego08.sarah.conditions.SelectCondition;
import fr.maxlego08.sarah.conditions.WhereCondition;
import fr.maxlego08.sarah.database.Schema;
import fr.maxlego08.sarah.database.DatabaseType;
import fr.maxlego08.sarah.dialect.SqlDialect;
import fr.maxlego08.sarah.dialect.SqlDialects;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class PostgreSqlConditionRenderingTest {

    private final SqlDialect postgres = SqlDialects.from(DatabaseType.POSTGRESQL);

    @Test
    public void testWhereConditionUsesDialectQuoting() {
        WhereCondition condition = new WhereCondition("u", "name", "=", "Sarah");
        assertEquals("u.\"name\" = ?", condition.getCondition(postgres));
    }

    @Test
    public void testWhereInUsesDialectQuoting() {
        WhereCondition condition = new WhereCondition("u", "id", Arrays.asList("1", "2", "3"));
        assertEquals("u.\"id\" IN (?,?,?)", condition.getCondition(postgres));
    }

    @Test
    public void testSchemaBuilderLegacyWhereConditionsUsesMigrationDialect() {
        DatabaseConfiguration previous = MigrationManager.getDatabaseConfiguration();
        try {
            MigrationManager.setDatabaseConfiguration(DatabaseConfiguration.createPostgreSql("u", "p", 5432, "localhost", "db"));

            Schema schema = SchemaBuilder.delete("users");
            schema.where("name", "Sarah");

            StringBuilder sql = new StringBuilder();
            schema.whereConditions(sql);

            assertEquals(" WHERE \"name\" = ?", sql.toString());
        } finally {
            MigrationManager.setDatabaseConfiguration(previous);
        }
    }

    @Test
    public void testSelectConditionUsesDialectQuoting() {
        SelectCondition select = new SelectCondition("u", "name", "username", false, null);
        assertEquals("u.\"name\" as username", select.getSelectColumn(postgres));
    }

    @Test
    public void testSelectCoalesceUsesDialectQuoting() {
        SelectCondition select = new SelectCondition("u", "name", "username", true, "'N/A'");
        assertEquals("COALESCE(u.\"name\", 'N/A') as username", select.getSelectColumn(postgres));
    }

    @Test
    public void testJoinConditionUsesDialectQuoting() {
        JoinCondition join = new JoinCondition(
                JoinCondition.JoinType.LEFT,
                "orders",
                "o",
                "user_id",
                "users",
                "id",
                null
        );
        assertEquals("LEFT JOIN \"orders\" AS \"o\" ON \"o\".\"user_id\" = \"users\".\"id\"", join.getJoinClause(postgres));
    }

    @Test
    public void testOrderByConditionUsesDialectQuoting() {
        OrderByCondition orderByCondition = new OrderByCondition(null, "created_at", true);
        assertEquals("ORDER BY \"created_at\" DESC", orderByCondition.getOrderByClause(postgres));
    }
}
