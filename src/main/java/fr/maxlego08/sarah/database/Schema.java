package fr.maxlego08.sarah.database;

import fr.maxlego08.sarah.DatabaseConnection;
import fr.maxlego08.sarah.conditions.ColumnDefinition;
import fr.maxlego08.sarah.conditions.JoinCondition;
import fr.maxlego08.sarah.conditions.SelectCondition;
import fr.maxlego08.sarah.conditions.WhereCondition;
import fr.maxlego08.sarah.logger.Logger;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Represents a schema builder for database operations.
 */
public interface Schema {

    Schema uuid(String columnName);

    Schema uuid(String columnName, UUID value);

    Schema string(String columnName, int length);

    Schema text(String columnName);

    Schema longText(String columnName);

    Schema decimal(String columnName);

    Schema decimal(String columnName, int length, int decimal);

    Schema string(String columnName, String value);

    Schema decimal(String columnName, Number value);

    Schema date(String columnName, Date value);

    Schema bigInt(String columnName);

    Schema integer(String columnName);

    Schema bigInt(String columnName, long value);

    Schema object(String columnName, Object object);

    Schema bool(String columnName);

    Schema bool(String columnName, boolean value);

    Schema json(String columnName);

    Schema blob(String columnName);

    Schema blob(String columnName, byte[] value);

    Schema blob(String columnName, Object object);

    // Column attributes
    Schema primary();

    Schema foreignKey(String referenceTable);

    Schema foreignKey(String referenceTable, String columnName, boolean onCascade);

    Schema createdAt();

    Schema updatedAt();

    Schema timestamps();

    Schema timestamp(String columnName);

    Schema autoIncrement(String columnName);

    Schema autoIncrementBigInt(String columnName);

    Schema nullable();

    Schema unique();

    Schema unique(boolean unique);

    Schema defaultValue(Object value);

    Schema where(String columnName, Object value);

    Schema where(String columnName, UUID value);

    Schema where(String columnName, String operator, Object value);

    Schema where(String tablePrefix, String columnName, String operator, Object value);

    Schema whereNotNull(String columnName);

    Schema leftJoin(String primaryTable, String primaryColumnAlias, String primaryColumn, String foreignTable, String foreignColumn);

    Schema leftJoin(String primaryTable, String primaryColumnAlias, String primaryColumn, String foreignTable, String foreignColumn, JoinCondition andCondition);

    Schema rightJoin(String primaryTable, String primaryColumnAlias, String primaryColumn, String foreignTable, String foreignColumn);

    Schema innerJoin(String primaryTable, String primaryColumnAlias, String primaryColumn, String foreignTable, String foreignColumn);

    Schema fullJoin(String primaryTable, String primaryColumnAlias, String primaryColumn, String foreignTable, String foreignColumn);

    // Execution methods
    int execute(DatabaseConnection databaseConnection, Logger logger) throws SQLException;

    List<Map<String, Object>> executeSelect(DatabaseConnection databaseConnection, Logger logger) throws SQLException;

    long executeSelectCount(DatabaseConnection databaseConnection, Logger logger) throws SQLException;

    <T> List<T> executeSelect(Class<T> clazz, DatabaseConnection databaseConnection, Logger logger) throws Exception;

    // Migration method
    Migration getMigration();

    String getTableName();

    void whereConditions(StringBuilder stringBuilder);

    void applyWhereConditions(PreparedStatement preparedStatement, int index) throws SQLException;

    List<ColumnDefinition> getColumns();

    List<String> getPrimaryKeys();

    List<String> getForeignKeys();

    List<JoinCondition> getJoinConditions();

    void orderBy(String columnName);

    void orderByDesc(String columnName);

    String getOrderBy();

    void distinct();

    boolean isDistinct();

    void addSelect(String selectedColumn);

    void addSelect(String prefix, String selectedColumn);

    void addSelect(String prefix, String selectedColumn, String aliases);

    void addSelect(String prefix, String selectedColumn, String aliases, Object defaultValue);

    SchemaType getSchemaType();

    Schema addColumn(ColumnDefinition column);

    void setMigration(Migration migration);

    List<WhereCondition> getWhereConditions();

    List<SelectCondition> getSelectColumns();

    String getNewTableName();
}