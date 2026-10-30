package fr.maxlego08.sarah.conditions;

import fr.maxlego08.sarah.DatabaseConfiguration;
import fr.maxlego08.sarah.dialect.Dialect;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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
    private ColumnOperation operation = ColumnOperation.SET;
    private String expression;
    private List<Object> expressionParameters = Collections.emptyList();

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
        Dialect dialect = databaseConfiguration.getDialect();
        String columnType = dialect.mapType(type);

        String sizedType;
        if (enumValues != null && !enumValues.isEmpty()) {
            String fallbackType = dialect.enumFallbackType();
            if (fallbackType != null) {
                sizedType = fallbackType;
            } else {
                String values = enumValues.stream()
                        .map(v -> "'" + v.replace("'", "''") + "'")
                        .collect(Collectors.joining(", "));
                sizedType = "ENUM(" + values + ")";
            }
        } else if (length != 0 && decimal != 0) {
            sizedType = columnType + "(" + length + "," + decimal + ")";
        } else if (length != 0) {
            sizedType = columnType + "(" + length + ")";
        } else {
            sizedType = columnType;
        }

        StringBuilder columnSQL = new StringBuilder("`" + name + "` ");
        if (isAutoIncrement && isPrimaryKey) {
            columnSQL.append(dialect.autoIncrementColumn(sizedType));
            if (dialect.inlineAutoIncrementPrimaryKey()) {
                if (unique) {
                    columnSQL.append(" UNIQUE");
                }
                return columnSQL.toString();
            }
        } else {
            columnSQL.append(sizedType);
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

    public ColumnOperation getOperation() {
        return operation;
    }

    public ColumnDefinition setOperation(ColumnOperation operation) {
        this.operation = operation;
        return this;
    }

    public String getExpression() {
        return expression;
    }

    public List<Object> getExpressionParameters() {
        return expressionParameters;
    }

    public ColumnDefinition setExpression(String expression, Object... parameters) {
        this.operation = ColumnOperation.EXPRESSION;
        this.expression = expression;
        this.expressionParameters = new ArrayList<>(Arrays.asList(parameters));
        return this;
    }

    /**
     * Whether an upsert puts this column in the row it inserts.
     */
    public boolean isInsertedByUpsert() {
        return !this.isAutoIncrement && this.operation != ColumnOperation.EXPRESSION;
    }

    /**
     * The value placeholder of this column in an {@code INSERT}: {@code ?} for a value, or the raw
     * expression.
     *
     * @param parameters receives the parameters bound to the placeholder, in order
     */
    public String insertPlaceholder(List<Object> parameters) {
        if (this.operation == ColumnOperation.EXPRESSION) {
            parameters.addAll(this.expressionParameters);
            return this.expression;
        }
        parameters.add(this.object);
        return "?";
    }

    /**
     * The {@code SET} assignment of this column in an {@code UPDATE}.
     *
     * @param parameters receives the parameters bound to the assignment, in order
     */
    public String updateAssignment(List<Object> parameters) {
        String safeName = this.getSafeName();
        switch (this.operation) {
            case INCREMENT:
                parameters.add(this.object);
                return safeName + " = " + safeName + " + ?";
            case EXPRESSION:
                parameters.addAll(this.expressionParameters);
                return safeName + " = " + this.expression;
            default:
                parameters.add(this.object);
                return safeName + " = ?";
        }
    }

    /**
     * The assignment applied to this column when an upsert finds an existing row.
     *
     * @param dialect    the dialect of the statement
     * @param tableName  the table, used to reference the existing row
     * @param parameters receives the parameters bound to the assignment, in order
     * @param batch      whether the statement inserts several rows, where MySQL must reference the
     *                   inserted values through {@code VALUES(col)} instead of re-binding them
     * @return the assignment, or {@code null} when the existing value is kept
     */
    public String conflictAssignment(Dialect dialect, String tableName, List<Object> parameters, boolean batch) {
        String safeName = this.getSafeName();
        boolean onConflict = dialect.usesOnConflict();
        switch (this.operation) {
            case INSERT_ONLY:
                return null;
            case EXPRESSION:
                parameters.addAll(this.expressionParameters);
                return safeName + " = " + this.expression;
            case INCREMENT:
                if (onConflict) {
                    return safeName + " = " + tableName + "." + safeName + " + excluded." + safeName;
                }
                if (batch) {
                    return safeName + " = " + safeName + " + VALUES(" + safeName + ")";
                }
                parameters.add(this.object);
                return safeName + " = " + safeName + " + ?";
            default:
                if (onConflict) {
                    return safeName + " = excluded." + safeName;
                }
                if (batch) {
                    return safeName + " = VALUES(" + safeName + ")";
                }
                parameters.add(this.object);
                return safeName + " = ?";
        }
    }
}
