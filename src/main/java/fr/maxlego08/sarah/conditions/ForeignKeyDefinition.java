package fr.maxlego08.sarah.conditions;

import fr.maxlego08.sarah.dialect.SqlDialect;

public class ForeignKeyDefinition {

    private final String sourceColumn;
    private final String referenceTable;
    private final String referenceColumn;
    private final boolean cascade;

    public ForeignKeyDefinition(String sourceColumn, String referenceTable, String referenceColumn, boolean cascade) {
        this.sourceColumn = sourceColumn;
        this.referenceTable = referenceTable;
        this.referenceColumn = referenceColumn;
        this.cascade = cascade;
    }

    public String getSourceColumn() {
        return sourceColumn;
    }

    public String getReferenceTable() {
        return referenceTable;
    }

    public String getReferenceColumn() {
        return referenceColumn;
    }

    public boolean isCascade() {
        return cascade;
    }

    public String render(SqlDialect dialect) {
        return "FOREIGN KEY (" + dialect.quoteIdentifier(sourceColumn) + ") REFERENCES " +
                dialect.quoteTableReference(referenceTable) + "(" + dialect.quoteIdentifier(referenceColumn) + ")" +
                (cascade ? " ON DELETE CASCADE" : "");
    }
}
