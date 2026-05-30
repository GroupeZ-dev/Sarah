package fr.maxlego08.sarah;

import fr.maxlego08.sarah.conditions.ColumnDefinition;
import fr.maxlego08.sarah.database.DatabaseType;
import fr.maxlego08.sarah.dialect.SqlDialect;
import fr.maxlego08.sarah.dialect.SqlDialects;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class PostgreSqlTypeMappingTest {

    private final SqlDialect postgres = SqlDialects.from(DatabaseType.POSTGRESQL);
    private final DatabaseConfiguration config = new DatabaseConfiguration("", "u", "p", 5432, "localhost", "db", false, DatabaseType.POSTGRESQL);

    @Test
    public void testLongTextMappedToText() {
        ColumnDefinition column = new ColumnDefinition("data", "LONGTEXT");
        String result = column.build(config, postgres);
        assertEquals("\"data\" TEXT NOT NULL", result);
    }

    @Test
    public void testMappingDoesNotMutateOriginalType() {
        ColumnDefinition column = new ColumnDefinition("data", "LONGTEXT");
        column.build(config, postgres);
        assertEquals("LONGTEXT", column.getType());
    }

    @Test
    public void testMediumTextMappedToText() {
        ColumnDefinition column = new ColumnDefinition("content", "MEDIUMTEXT");
        String result = column.build(config, postgres);
        assertEquals("\"content\" TEXT NOT NULL", result);
    }

    @Test
    public void testTinyTextMappedToText() {
        ColumnDefinition column = new ColumnDefinition("note", "TINYTEXT");
        String result = column.build(config, postgres);
        assertEquals("\"note\" TEXT NOT NULL", result);
    }

    @Test
    public void testBlobMappedToBytea() {
        ColumnDefinition column = new ColumnDefinition("payload", "BLOB");
        String result = column.build(config, postgres);
        assertEquals("\"payload\" BYTEA NOT NULL", result);
    }

    @Test
    public void testLongBlobMappedToBytea() {
        ColumnDefinition column = new ColumnDefinition("payload", "LONGBLOB");
        String result = column.build(config, postgres);
        assertEquals("\"payload\" BYTEA NOT NULL", result);
    }

    @Test
    public void testTextStaysText() {
        ColumnDefinition column = new ColumnDefinition("body", "TEXT");
        String result = column.build(config, postgres);
        assertEquals("\"body\" TEXT NOT NULL", result);
    }

    @Test
    public void testVarcharUnchanged() {
        ColumnDefinition column = new ColumnDefinition("name", "VARCHAR").setLength(255);
        String result = column.build(config, postgres);
        assertEquals("\"name\" VARCHAR(255) NOT NULL", result);
    }

    @Test
    public void testIntegerUnchanged() {
        ColumnDefinition column = new ColumnDefinition("count", "INTEGER");
        String result = column.build(config, postgres);
        assertEquals("\"count\" INTEGER NOT NULL", result);
    }

    @Test
    public void testMysqlLongTextIsUnchangedOnMysql() {
        SqlDialect mysql = SqlDialects.from(DatabaseType.MYSQL);
        DatabaseConfiguration mysqlConfig = new DatabaseConfiguration("", "u", "p", 3306, "localhost", "db", false, DatabaseType.MYSQL);
        ColumnDefinition column = new ColumnDefinition("data", "LONGTEXT");
        String result = column.build(mysqlConfig, mysql);
        assertEquals("`data` LONGTEXT NOT NULL", result);
    }
}
