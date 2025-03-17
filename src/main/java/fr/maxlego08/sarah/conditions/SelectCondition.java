package fr.maxlego08.sarah.conditions;

import java.util.Objects;

public class SelectCondition {
    private final String tablePrefix;
    private final String column;
    private final String aliases;
    private final boolean isCoalesce;
    private final Object defaultValue;

    public SelectCondition(String tablePrefix, String column, String aliases, boolean isCoalesce, Object defaultValue) {
        this.tablePrefix = tablePrefix;
        this.column = column;
        this.aliases = aliases;
        this.isCoalesce = isCoalesce;
        this.defaultValue = defaultValue;
    }

    public String getTablePrefix() {
        return tablePrefix;
    }

    public String getColumn() {
        return column;
    }

    public boolean isCoalesce() {
        return isCoalesce;
    }

    public Object getDefaultValue() {
        return defaultValue;
    }

    /**
     * Gets the SQL column name for the given select condition.
     *
     * <p>If the select condition is a COALESCE condition, the SQL column name
     * will be the COALESCE expression. Otherwise, it will be the column name
     * with the given table prefix and aliases.
     *
     * @return the SQL column name
     */
    public String getSelectColumn() {
        String result = this.tablePrefix == null ? this.getColumnAndAliases() : this.tablePrefix + "." + this.getColumnAndAliases();
        if (isCoalesce) {
            String tableName = this.tablePrefix == null ? "`" + this.column + "`" : this.tablePrefix + ".`" + this.column + "`";
            return "COALESCE(" + tableName + ", " + defaultValue + ")" + getAliases();
        }
        return result;
    }

    private String getColumnAndAliases() {
        return "`" + this.column + "`" + getAliases();
    }

    private String getAliases() {
        return this.aliases == null ? "" : " as " + this.aliases;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SelectCondition that = (SelectCondition) o;
        return isCoalesce == that.isCoalesce &&
                Objects.equals(tablePrefix, that.tablePrefix) &&
                Objects.equals(column, that.column) &&
                Objects.equals(aliases, that.aliases) &&
                Objects.equals(defaultValue, that.defaultValue);
    }

    @Override
    public int hashCode() {
        return Objects.hash(tablePrefix, column, aliases, isCoalesce, defaultValue);
    }

    @Override
    public String toString() {
        return "SelectCondition{" +
                "tablePrefix='" + tablePrefix + '\'' +
                ", column='" + column + '\'' +
                ", aliases='" + aliases + '\'' +
                ", isCoalesce=" + isCoalesce +
                ", defaultValue=" + defaultValue +
                '}';
    }
}