package fr.maxlego08.sarah.conditions;

public class WhereCondition {
    private final String column;
    private final Object value;
    private final String operator;
    private boolean isNotNull;

    public WhereCondition(String prefix, String column, String operator, Object value) {
        this.column = (prefix == null ? "" : prefix + ".") + "`" + column + "`";
        this.operator = operator;
        this.value = value;
    }

    public WhereCondition(String column) {
        this.column = column;
        this.value = null;
        this.operator = null;
        this.isNotNull = true;
    }

    /**
     * Gets the SQL condition for the WHERE clause of the query.
     *
     * <p>If the condition is a NOT NULL condition, the SQL condition will be
     * {@code columnName IS NOT NULL}. Otherwise, it will be a SQL condition in
     * the format of {@code columnName operator ?}, where {@code operator} is
     * the operator configured in the condition, and {@code ?} is a placeholder
     * for the value of the condition.
     *
     * @return the SQL condition
     */
    public String getCondition() {
        if (this.isNotNull) return this.column + " IS NOT NULL";
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

    public boolean isNotNull() {
        return isNotNull;
    }
}

