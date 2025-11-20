package fr.maxlego08.sarah.conditions;

import fr.maxlego08.sarah.DatabaseConfiguration;
import fr.maxlego08.sarah.database.DatabaseType;

public class ColumnDefinition {

    private String name;
    private String type;
    private int length;
    private int decimal;
    private boolean nullable = false;
    private String defaultValue;
    private boolean isPrimaryKey = false;
    private String referenceTable;
    private Object object;
    private boolean isAutoIncrement;
    private boolean unique = false;

    public ColumnDefinition(String name, String type) {
        this.name = name;
        this.type = type;
    }

    public ColumnDefinition(String name) {
        this.name = name;
    }

    /**
     * Build an SQL string representation of the column.
     *
     * @param databaseConfiguration The database configuration used to generate the SQL
     * @return The SQL string representation of the column
     */
    public String build(DatabaseConfiguration databaseConfiguration) {
        // For SQLite autoincrement, use INTEGER instead of BIGINT/INT
        String columnType = type;
        if (isAutoIncrement && databaseConfiguration.getDatabaseType() == DatabaseType.SQLITE) {
            if (type.equalsIgnoreCase("BIGINT") || type.equalsIgnoreCase("INT") || type.equalsIgnoreCase("INTEGER")) {
                columnType = "INTEGER";
            }
        }

        StringBuilder columnSQL = new StringBuilder("`" + name + "` " + columnType);

        if (length != 0 && decimal != 0) {
            columnSQL.append("(").append(length).append(",").append(decimal).append(")");
        } else if (length != 0) {
            columnSQL.append("(").append(length).append(")");
        }

        // For autoincrement columns with primary key
        if (isAutoIncrement && isPrimaryKey) {
            if (databaseConfiguration.getDatabaseType() == DatabaseType.SQLITE) {
                // SQLite: INTEGER PRIMARY KEY AUTOINCREMENT (inline, no NOT NULL needed)
                columnSQL.append(" PRIMARY KEY AUTOINCREMENT");
                if (unique) {
                    columnSQL.append(" UNIQUE");
                }
                return columnSQL.toString();
            } else {
                // MySQL/MariaDB: column will have AUTO_INCREMENT
                columnSQL.append(" AUTO_INCREMENT");
            }
        }

        if (nullable) {
            columnSQL.append(" NULL");
        } else {
            columnSQL.append(" NOT NULL");
        }

        if (defaultValue != null) {
            columnSQL.append(" DEFAULT ").append(defaultValue);
        }

        if (unique) {
            columnSQL.append(" UNIQUE");
        }

        return columnSQL.toString();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSafeName() {
        return String.format("`%s`", this.name);
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Integer getLength() {
        return length;
    }

    public ColumnDefinition setLength(Integer length) {
        this.length = length;
        return this;
    }

    public ColumnDefinition setLength(int length) {
        this.length = length;
        return this;
    }

    public ColumnDefinition setDecimal(Integer decimal) {
        this.decimal = decimal;
        return this;
    }

    public Boolean getNullable() {
        return nullable;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }

    public boolean isPrimaryKey() {
        return isPrimaryKey;
    }

    public void setPrimaryKey(boolean primaryKey) {
        isPrimaryKey = primaryKey;
    }

    public String getReferenceTable() {
        return referenceTable;
    }

    public void setReferenceTable(String referenceTable) {
        this.referenceTable = referenceTable;
    }

    public boolean isNullable() {
        return nullable;
    }

    public void setNullable(Boolean nullable) {
        this.nullable = nullable;
    }

    public void setNullable(boolean nullable) {
        this.nullable = nullable;
    }

    public void setUnique(boolean unique) {
        this.unique = unique;
    }

    public boolean isUnique() {
        return unique;
    }

    public Object getObject() {
        return object;
    }

    public ColumnDefinition setObject(Object object) {
        this.object = object;
        return this;
    }

    public boolean isAutoIncrement() {
        return isAutoIncrement;
    }

    public ColumnDefinition setAutoIncrement(boolean isAutoIncrement) {
        this.isAutoIncrement = isAutoIncrement;
        return this;
    }
}
