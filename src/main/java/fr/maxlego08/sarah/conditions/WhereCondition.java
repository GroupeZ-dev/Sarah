package fr.maxlego08.sarah.conditions;

import fr.maxlego08.sarah.security.SqlIdentifiers;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class WhereCondition {

    private final String column;
    private final Object value;
    private final String operator;
    private final WhereAction whereAction;

    private final List<Object> values = new ArrayList<>();

    public WhereCondition(String prefix, String column, String operator, Object value) {
        this.column = qualify(prefix, column);
        this.operator = SqlIdentifiers.operator(operator);
        this.value = value;
        this.whereAction = WhereAction.NORMAL;
    }

    public WhereCondition(String prefix, String column, List<?> values) {
        this.column = qualify(prefix, column);
        this.value = null;
        this.operator = null;
        this.values.addAll(values);
        this.whereAction = WhereAction.IN;
    }

    public WhereCondition(String column, WhereAction whereAction) {
        // Written as is, as it always was, so a qualified name such as t.col keeps working
        this.column = SqlIdentifiers.reference(column);
        this.value = null;
        this.operator = null;
        this.whereAction = whereAction;
    }

    private static String qualify(String prefix, String column) {
        return (prefix == null ? "" : SqlIdentifiers.reference(prefix) + ".") + SqlIdentifiers.quote(column);
    }

    public String getCondition() {
        if (this.whereAction == WhereAction.IS_NOT_NULL) return this.column + " IS NOT NULL";
        if (this.whereAction == WhereAction.IS_NULL) return this.column + " IS NULL";
        if (this.whereAction == WhereAction.IN) {
            // "IN ()" is only accepted by SQLite; an empty list matches nothing everywhere.
            if (values.isEmpty()) return "1 = 0";
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

    /**
     * @return the values of an {@code IN} condition, as strings
     */
    public List<String> getValues() {
        return values.stream().map(String::valueOf).collect(Collectors.toList());
    }

    /**
     * @return the values of an {@code IN} condition, with their own type, as they are bound
     */
    public List<Object> getParameters() {
        return values;
    }

    public enum WhereAction {
        IS_NOT_NULL, IS_NULL, NORMAL, IN,
    }
}

