package fr.maxlego08.sarah;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Turns the current row of a {@link ResultSet} into an object, for
 * {@link RequestHelper#query(String, RowMapper, Object...)}.
 *
 * @param <T> the mapped type
 */
@FunctionalInterface
public interface RowMapper<T> {

    /**
     * @param resultSet positioned on the row to map; implementations must not move it
     * @return the mapped object
     */
    T map(ResultSet resultSet) throws SQLException;
}
