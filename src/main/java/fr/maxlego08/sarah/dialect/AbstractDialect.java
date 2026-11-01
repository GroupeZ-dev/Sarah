package fr.maxlego08.sarah.dialect;

import fr.maxlego08.sarah.database.Schema;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

/**
 * Defaults shared by every dialect, matching the historical MySQL behaviour.
 */
public abstract class AbstractDialect implements Dialect {

    @Override
    public String translate(String sql) {
        return sql;
    }

    @Override
    public String mapType(String type) {
        return type;
    }

    @Override
    public boolean inlineAutoIncrementPrimaryKey() {
        return false;
    }

    @Override
    public String enumFallbackType() {
        return null;
    }

    @Override
    public String tableOptions() {
        return "";
    }

    @Override
    public String updatedAtDefault() {
        return "CURRENT_TIMESTAMP";
    }

    @Override
    public boolean usesOnConflict() {
        return true;
    }

    @Override
    public String limitClause(Integer limit, Integer offset) {
        StringBuilder clause = new StringBuilder();
        if (limit != null) {
            clause.append(" LIMIT ").append(limit);
        }
        if (offset != null) {
            clause.append(" OFFSET ").append(offset);
        }
        return clause.toString();
    }

    @Override
    public PreparedStatement prepareInsert(Connection connection, String sql, Schema schema) throws SQLException {
        return connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
    }

    @Override
    public Map<String, String> dataSourceProperties() {
        return new HashMap<>();
    }
}
