package fr.maxlego08.sarah.conditions;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class WhereCondition {

    private final String column;
    private final Object value;
    private final String operator;
    private final WhereAction whereAction;

    private final List<String> values = new ArrayList<>();

    public WhereCondition(String prefix, String column, String operator, Object value) {
        this.column = (prefix == null ? "" : prefix + ".") + "`" + column + "`";
        this.operator = operator;
        this.value = value;
        this.whereAction = WhereAction.NORMAL;
    }

    public WhereCondition(String prefix, String column, List<String> values) {
        this.column = (prefix == null ? "" : prefix + ".") + "`" + column + "`";
        this.value = null;
        this.operator = null;
        this.values.addAll(values);
        this.whereAction = WhereAction.IN;
    }

    public WhereCondition(String column, WhereAction whereAction) {
        this.column = column;
        this.value = null;
        this.operator = null;
        this.whereAction = whereAction;
    }

    public String getCondition() {
        if (this.whereAction == WhereAction.IS_NOT_NULL) return this.column + " IS NOT NULL";
        if (this.whereAction == WhereAction.IS_NULL) return this.column + " IS NULL";
        if (this.whereAction == WhereAction.IN) {
            return this.column + " IN (" + values.stream().map(id -> "?").collect(Collectors.joining(",")) + ")";
        }
        return this.column + " " + this.operator + " ?";
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

    public WhereAction getWhereAction() {
        return whereAction;
    }

    public List<String> getValues() {
        return values;
    }

    public enum WhereAction {
        IS_NOT_NULL, IS_NULL, NORMAL, IN,
    }
}

