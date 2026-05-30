package fr.maxlego08.sarah.conditions;

import fr.maxlego08.sarah.database.DatabaseType;
import fr.maxlego08.sarah.dialect.SqlDialect;
import fr.maxlego08.sarah.dialect.SqlDialects;

public class OrderByCondition {

    private final String tablePrefix;
    private final String column;
    private final boolean descending;

    public OrderByCondition(String tablePrefix, String column, boolean descending) {
        this.tablePrefix = tablePrefix;
        this.column = column;
        this.descending = descending;
    }

    public String getOrderByClause() {
        return this.buildClause(SqlDialects.from(DatabaseType.MYSQL));
    }

    public String getOrderByClause(SqlDialect dialect) {
        return this.buildClause(dialect);
    }

    public String getTablePrefix() {
        return tablePrefix;
    }

    public String getColumn() {
        return column;
    }

    public boolean isDescending() {
        return descending;
    }

    private String buildClause(SqlDialect dialect) {
        String qualified = dialect.qualifyIdentifier(this.tablePrefix, this.column);
        return this.descending ? "ORDER BY " + qualified + " DESC" : "ORDER BY " + qualified;
    }
}
