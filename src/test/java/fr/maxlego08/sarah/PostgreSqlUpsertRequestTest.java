package fr.maxlego08.sarah;

import fr.maxlego08.sarah.database.DatabaseType;
import fr.maxlego08.sarah.database.Schema;
import fr.maxlego08.sarah.dialect.SqlDialect;
import fr.maxlego08.sarah.dialect.SqlDialects;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

public class PostgreSqlUpsertRequestTest {

    @Test
    public void testPostgreSqlConflictTargetSkipsAutoIncrementPrimaryKey() {
        SqlDialect dialect = SqlDialects.from(DatabaseType.POSTGRESQL);
        Schema schema = SchemaBuilder.upsert("users", table -> {
            table.autoIncrementBigInt("id");
            table.string("username", "sarah").unique();
            table.string("email", "sarah@example.com");
        });

        String conflictClause = dialect.upsertConflictClause(schema);
        assertEquals(" ON CONFLICT (\"username\") DO UPDATE SET ", conflictClause);
    }

    @Test
    public void testPostgreSqlUpsertExpressionUsesExcluded() {
        SqlDialect dialect = SqlDialects.from(DatabaseType.POSTGRESQL);

        assertEquals("\"email\" = excluded.\"email\"",
                dialect.upsertUpdateExpression("\"email\"", false));
        assertEquals("\"email\" = excluded.\"email\"",
                dialect.upsertUpdateExpression("\"email\"", true));
        assertFalse(dialect.usesUpsertUpdateParameters(false));
        assertFalse(dialect.usesUpsertUpdateParameters(true));
    }

    @Test
    public void testMysqlSingleAndBatchUpsertDiffer() {
        SqlDialect dialect = SqlDialects.from(DatabaseType.MYSQL);

        assertEquals("`email` = ?",
                dialect.upsertUpdateExpression("`email`", false));
        assertEquals("`email` = VALUES(`email`)",
                dialect.upsertUpdateExpression("`email`", true));
    }
}
