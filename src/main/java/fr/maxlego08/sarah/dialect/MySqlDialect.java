package fr.maxlego08.sarah.dialect;

import fr.maxlego08.sarah.DatabaseConfiguration;
import fr.maxlego08.sarah.conditions.ColumnDefinition;
import fr.maxlego08.sarah.database.Schema;

import java.util.List;
import java.util.stream.Collectors;

public class MySqlDialect extends AbstractSqlDialect {

    public MySqlDialect() {
        super("`");
    }

    @Override
    public String driverClassName() {
        return "com.mysql.cj.jdbc.Driver";
    }

    @Override
    public String jdbcUrl(DatabaseConfiguration configuration) {
        return "jdbc:mysql://" + configuration.getHost() + ":" + configuration.getPort() + "/" + configuration.getDatabase() + "?allowMultiQueries=true";
    }

    @Override
    public String createTableSuffix() {
        return " ENGINE=InnoDB DEFAULT CHARSET=utf8mb4";
    }

    @Override
    public String autoIncrementKeyword(ColumnDefinition column) {
        return column != null && column.isAutoIncrement() ? "AUTO_INCREMENT" : "";
    }

    @Override
    public String enumColumnType(ColumnDefinition column) {
        List<String> values = column.getEnumValues();
        if (values == null || values.isEmpty()) {
            return "ENUM('')";
        }

        return "ENUM(" + values.stream()
                .map(value -> "'" + escapeSingleQuotes(value) + "'")
                .collect(Collectors.joining(", ")) + ")";
    }

    @Override
    public String updatedAtDefaultValue() {
        return "CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP";
    }

    @Override
    public String upsertConflictClause(Schema schema) {
        return " ON DUPLICATE KEY UPDATE ";
    }

    @Override
    public String upsertUpdateExpression(String quotedColumn, boolean batch) {
        if (batch) {
            return quotedColumn + " = VALUES(" + quotedColumn + ")";
        }
        return quotedColumn + " = ?";
    }

    @Override
    public boolean usesUpsertUpdateParameters(boolean batch) {
        return !batch;
    }
}
