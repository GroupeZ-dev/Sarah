package fr.maxlego08.sarah.database;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Date;
import java.util.UUID;

/**
 * Binds parameter values the same way on every driver.
 *
 * <p>MySQL and SQLite accept a {@link java.util.Date}, a {@link UUID} or an enum through
 * {@code setObject}, the PostgreSQL driver refuses them because it cannot infer their SQL type.
 * They are converted here to a {@link Timestamp} and to strings.</p>
 */
public final class StatementBinder {

    private StatementBinder() {
    }

    public static void bind(PreparedStatement preparedStatement, int index, Object value) throws SQLException {
        preparedStatement.setObject(index, convert(value));
    }

    public static Object convert(Object value) {
        if (value instanceof Date && !(value instanceof java.sql.Date) && !(value instanceof java.sql.Time) && !(value instanceof Timestamp)) {
            return new Timestamp(((Date) value).getTime());
        }
        if (value instanceof UUID) {
            return value.toString();
        }
        if (value instanceof Enum<?>) {
            return ((Enum<?>) value).name();
        }
        return value;
    }
}
