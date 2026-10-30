package fr.maxlego08.sarah.dialect;

import fr.maxlego08.sarah.DatabaseConfiguration;
import fr.maxlego08.sarah.database.DatabaseType;
import fr.maxlego08.sarah.database.Schema;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Map;

/**
 * Everything that differs between the SQL engines Sarah supports.
 *
 * <p>Requests build their SQL with MySQL-style backtick quoting and ask the dialect for the few
 * fragments that genuinely differ (column types, auto-increment, upsert conflict handling, paging).
 * {@link #translate(String)} then rewrites the finished statement for engines that quote
 * identifiers differently.</p>
 */
public interface Dialect {

    DatabaseType getType();

    /**
     * Rewrites a finished statement for this engine. Called after {@code %prefix%} replacement.
     *
     * @param sql the statement, using backticks for identifiers
     * @return the statement this engine understands
     */
    String translate(String sql);

    /**
     * Maps a Sarah column type ({@code LONGTEXT}, {@code BLOB}…) to this engine's type.
     */
    String mapType(String type);

    /**
     * The column definition fragment of an auto-increment primary key.
     *
     * @param columnType the already mapped type
     * @return the type followed by the auto-increment clause
     */
    String autoIncrementColumn(String columnType);

    /**
     * Whether an auto-increment column carries {@code PRIMARY KEY} inline, so the table must not
     * declare a separate primary key clause.
     */
    boolean inlineAutoIncrementPrimaryKey();

    /**
     * The column type used for an {@code ENUM}, or {@code null} when the engine supports
     * {@code ENUM('a', 'b')} natively.
     */
    String enumFallbackType();

    /**
     * Appended after the closing parenthesis of {@code CREATE TABLE}.
     */
    String tableOptions();

    /**
     * Default value of the {@code updated_at} column created by {@code timestamps()}.
     */
    String updatedAtDefault();

    /**
     * Whether upserts use {@code ON CONFLICT (…) DO UPDATE} (SQLite, PostgreSQL) instead of
     * {@code ON DUPLICATE KEY UPDATE} (MySQL, MariaDB).
     */
    boolean usesOnConflict();

    /**
     * The {@code LIMIT}/{@code OFFSET} clause, with a leading space, or an empty string.
     *
     * @param limit  maximum number of rows, or null for no limit
     * @param offset rows to skip, or null
     */
    String limitClause(Integer limit, Integer offset);

    /**
     * Prepares an {@code INSERT} so that generated keys can be read back when the schema has an
     * auto-increment column.
     */
    PreparedStatement prepareInsert(Connection connection, String sql, Schema schema) throws SQLException;

    /**
     * JDBC URL used by pooled and plain connections.
     */
    String jdbcUrl(DatabaseConfiguration configuration);

    /**
     * Fully qualified JDBC driver class.
     */
    String driverClassName();

    /**
     * Data source properties for the Hikari pool.
     */
    Map<String, String> dataSourceProperties();
}
