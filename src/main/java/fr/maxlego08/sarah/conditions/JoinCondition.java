package fr.maxlego08.sarah.conditions;

import fr.maxlego08.sarah.dialect.Dialect;
import fr.maxlego08.sarah.security.SqlIdentifiers;

public class JoinCondition {
    private final String primaryTable;
    private final String primaryTableAlias;
    private final String primaryColumn;
    private final String foreignTable;
    private final String foreignColumn;
    private final JoinType joinType;
    private final JoinCondition additionalCondition;

    public JoinCondition(JoinType joinType, String primaryTable, String primaryTableAlias, String primaryColumn, String foreignTable, String foreignColumn, JoinCondition additionalCondition) {
        this.primaryTable = primaryTable;
        this.primaryTableAlias = primaryTableAlias;
        this.primaryColumn = primaryColumn;
        this.foreignTable = foreignTable;
        this.foreignColumn = foreignColumn;
        this.joinType = joinType;
        this.additionalCondition = additionalCondition;
    }

    public static JoinCondition and(String primaryTableAlias, String primaryColumn, String foreignColumn) {
        return new JoinCondition(null, null, primaryTableAlias, primaryColumn, null, foreignColumn, null);
    }

    /**
     * Constructs the SQL JOIN clause for this JoinCondition.
     *
     * @return a string representing the SQL JOIN clause, including the join type,
     * primary table, primary column, foreign table, and foreign column.
     * If there is an additional condition, it is appended with an AND clause.
     */
    public String getJoinClause() {
        return getJoinClause(null);
    }

    /**
     * @param dialect the engine the clause is written for, which decides how the value of an
     *                {@link #and(String, String, String)} condition is escaped, or {@code null} to
     *                escape it in a way that is safe on every engine
     */
    public String getJoinClause(Dialect dialect) {
        StringBuilder joinClause = new StringBuilder();
        joinClause.append(this.joinType.getSql()).append(" ")
                .append(SqlIdentifiers.reference(this.primaryTable)).append(" AS ").append(SqlIdentifiers.reference(this.primaryTableAlias))
                .append(" ON ").append(this.primaryTableAlias).append(".").append(SqlIdentifiers.reference(this.primaryColumn))
                .append(" = ").append(SqlIdentifiers.reference(this.foreignTable)).append(".").append(SqlIdentifiers.reference(this.foreignColumn));

        if (this.additionalCondition != null) {
            joinClause.append(" AND ").append(this.additionalCondition.getCondition(dialect));
        }
        return joinClause.toString();
    }

    private String getCondition(Dialect dialect) {
        // foreignColumn holds the value compared to here, written as a string literal
        String value = dialect == null ? SqlIdentifiers.stringLiteral(this.foreignColumn) : dialect.stringLiteral(this.foreignColumn);
        return SqlIdentifiers.reference(this.primaryTableAlias) + "." + SqlIdentifiers.reference(this.primaryColumn) + " = " + value;
    }

    public enum JoinType {
        INNER("INNER JOIN"),
        LEFT("LEFT JOIN"),
        RIGHT("RIGHT JOIN"),
        FULL("FULL OUTER JOIN");

        private final String sql;

        JoinType(String sql) {
            this.sql = sql;
        }

        public String getSql() {
            return this.sql;
        }
    }
}
