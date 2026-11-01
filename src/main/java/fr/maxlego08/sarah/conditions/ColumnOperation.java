package fr.maxlego08.sarah.conditions;

/**
 * How a value column is written by {@code INSERT}, {@code UPDATE} and {@code UPSERT} requests.
 */
public enum ColumnOperation {

    /**
     * The column receives the value. The default.
     */
    SET,

    /**
     * The value is added to the current one: {@code col = col + ?} on update, and on an upsert
     * conflict. A new row receives the value as is.
     */
    INCREMENT,

    /**
     * The column receives a raw SQL expression with its own parameters. On an upsert it is only
     * applied when the row already exists and is left out of the inserted row.
     */
    EXPRESSION,

    /**
     * The column is written when an upsert inserts a row and left untouched when the row already
     * exists, for example a creation date.
     */
    INSERT_ONLY
}
