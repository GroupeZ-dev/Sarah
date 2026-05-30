package fr.maxlego08.sarah.conditions;

import fr.maxlego08.sarah.DatabaseConfiguration;
import fr.maxlego08.sarah.database.DatabaseType;
import fr.maxlego08.sarah.dialect.SqlDialect;
import fr.maxlego08.sarah.dialect.SqlDialects;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

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
    private List<String> enumValues;

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
        return build(databaseConfiguration, SqlDialects.from(databaseConfiguration.getDatabaseType()));
    }

    public String build(DatabaseConfiguration databaseConfiguration, SqlDialect dialect) {
        if (isAutoIncrement && isPrimaryKey && databaseConfiguration.getDatabaseType() == DatabaseType.SQLITE) {
            StringBuilder sqliteAutoIncrement = new StringBuilder(dialect.quoteIdentifier(name)).append(" INTEGER PRIMARY KEY AUTOINCREMENT");
            if (unique) {
                sqliteAutoIncrement.append(" UNIQUE");
            }
            return sqliteAutoIncrement.toString();
        }

        StringBuilder columnSQL = new StringBuilder(dialect.quoteIdentifier(name)).append(" ");

        boolean isEnumColumn = enumValues != null && !enumValues.isEmpty();
        if (isEnumColumn) {
            columnSQL.append(dialect.enumColumnType(this));
        } else {
            columnSQL.append(dialect.columnType(this));
        }

        String autoIncrementKeyword = dialect.autoIncrementKeyword(this);
        if (!autoIncrementKeyword.isEmpty()) {
            columnSQL.append(" ").append(autoIncrementKeyword);
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

    public int getDecimal() {
        return decimal;
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

    public ColumnDefinition setPrimaryKey(boolean primaryKey) {
        isPrimaryKey = primaryKey;
        return this;
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

    public List<String> getEnumValues() {
        return enumValues;
    }

    public ColumnDefinition setEnumValues(List<String> enumValues) {
        this.enumValues = enumValues;
        return this;
    }

    public ColumnDefinition setEnumValues(String... enumValues) {
        this.enumValues = Arrays.asList(enumValues);
        return this;
    }

    public <E extends Enum<E>> ColumnDefinition setEnumValues(Class<E> enumClass) {
        this.enumValues = Arrays.stream(enumClass.getEnumConstants())
                .map(Enum::name)
                .collect(Collectors.toList());
        return this;
    }
}
