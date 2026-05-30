package fr.maxlego08.sarah.conditions;

import fr.maxlego08.sarah.database.DatabaseType;
import fr.maxlego08.sarah.dialect.SqlDialect;
import fr.maxlego08.sarah.dialect.SqlDialects;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class WhereCondition {

    private final String tablePrefix;
    private final String column;
    private final Object value;
    private final String operator;
    private final WhereAction whereAction;

    private final List<String> values = new ArrayList<>();

    public WhereCondition(String prefix, String column, String operator, Object value) {
        this.tablePrefix = prefix;
        this.column = column;
        this.operator = operator;
        this.value = value;
        this.whereAction = WhereAction.NORMAL;
    }

    public WhereCondition(String prefix, String column, List<String> values) {
        this.tablePrefix = prefix;
        this.column = column;
        this.value = null;
        this.operator = null;
        this.values.addAll(values);
        this.whereAction = WhereAction.IN;
    }

    public WhereCondition(String column, WhereAction whereAction) {
        this.tablePrefix = null;
        this.column = column;
        this.value = null;
        this.operator = null;
        this.whereAction = whereAction;
    }

    public String getCondition() {
        if (this.whereAction == WhereAction.IS_NOT_NULL) return this.legacyQualifiedColumn() + " IS NOT NULL";
        if (this.whereAction == WhereAction.IS_NULL) return this.legacyQualifiedColumn() + " IS NULL";
        if (this.whereAction == WhereAction.IN) {
            return this.legacyQualifiedColumn() + " IN (" + values.stream().map(id -> "?").collect(Collectors.joining(",")) + ")";
        }
        return this.legacyQualifiedColumn() + " " + this.operator + " ?";
    }

    public String getCondition(SqlDialect dialect) {
        if (this.whereAction == WhereAction.IS_NOT_NULL) return this.qualifiedColumn(dialect) + " IS NOT NULL";
        if (this.whereAction == WhereAction.IS_NULL) return this.qualifiedColumn(dialect) + " IS NULL";
        if (this.whereAction == WhereAction.IN) {
            return this.qualifiedColumn(dialect) + " IN (" + values.stream().map(id -> "?").collect(Collectors.joining(",")) + ")";
        }
        return this.qualifiedColumn(dialect) + " " + this.operator + " ?";
    }

    public String getOperator() {
        return this.operator;
    }

    public Object getValue() {
        return this.value;
    }

    public String getColumn() {
        return this.column;
    }

    public String getTablePrefix() {
        return tablePrefix;
    }

    public WhereAction getWhereAction() {
        return whereAction;
    }

    public List<String> getValues() {
        return values;
    }

    public enum WhereAction {
        IS_NOT_NULL, IS_NULL, NORMAL, IN,
    }

    private String legacyQualifiedColumn() {
        String quote = SqlDialects.from(DatabaseType.MYSQL).quoteIdentifier(this.column);
        return this.tablePrefix == null ? quote : this.tablePrefix + "." + quote;
    }

    private String qualifiedColumn(SqlDialect dialect) {
        return dialect.qualifyIdentifier(this.tablePrefix, this.column);
    }
}

