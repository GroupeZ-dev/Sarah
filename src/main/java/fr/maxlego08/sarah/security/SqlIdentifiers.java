package fr.maxlego08.sarah.security;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Checks the fragments Sarah writes into a statement instead of binding them: table and column
 * names, operators and literals. Values are always bound as parameters.
 *
 * <p>The checks only reject what no legitimate name or operator contains (a statement separator,
 * a comment, a character ending a quoted name), so everything that worked before still works.</p>
 */
public final class SqlIdentifiers {

    private static final String[] FORBIDDEN = {";", "--", "/*", "*/", "\0"};
    /**
     * An operator made only of comparison symbols, such as {@code =}, {@code <=>} or {@code !~*}.
     */
    private static final Pattern SYMBOL_OPERATOR = Pattern.compile("[=<>!~*&|^@#%?+-]+");
    private static final Set<String> OPERATOR_KEYWORDS = new HashSet<>(Arrays.asList(
            "NOT", "LIKE", "ILIKE", "RLIKE", "REGEXP", "GLOB", "MATCH", "IS", "DISTINCT", "FROM", "SIMILAR", "TO", "IN"));

    private SqlIdentifiers() {
    }

    /**
     * Wraps a column name in backticks.
     *
     * @throws IllegalArgumentException if the name contains a quote character, which would end the
     *                                  quoted name, or a statement separator or comment
     */
    public static String quote(String name) {
        if (name == null || name.indexOf('`') >= 0 || name.indexOf('"') >= 0) {
            throw new IllegalArgumentException("Invalid column name: " + name);
        }
        return "`" + check(name, "column name") + "`";
    }

    /**
     * Checks a fragment written as is, such as a table name, a prefix, an alias or an
     * {@code ORDER BY} column.
     *
     * @return the fragment, unchanged
     * @throws IllegalArgumentException if it contains a statement separator, a comment or a single
     *                                  quote
     */
    public static String reference(String name) {
        if (name != null && name.indexOf('\'') >= 0) {
            throw new IllegalArgumentException("Invalid name: " + name);
        }
        return check(name, "name");
    }

    /**
     * Checks an expression written as is, such as an {@code ORDER BY} clause. Unlike a name, it may
     * contain quoted strings.
     *
     * @return the expression, unchanged
     */
    public static String expression(String expression) {
        return check(expression, "expression");
    }

    /**
     * Checks a comparison operator: comparison symbols ({@code =}, {@code <>}, {@code <=>}…) or
     * keywords such as {@code LIKE}, {@code NOT LIKE}, {@code REGEXP} or {@code IS NOT}.
     *
     * @return the operator, unchanged
     */
    public static String operator(String operator) {
        String trimmed = operator == null ? "" : operator.trim();
        if (SYMBOL_OPERATOR.matcher(trimmed).matches() && !trimmed.contains("--")) {
            return operator;
        }
        String[] words = trimmed.split("\\s+");
        boolean keywords = !trimmed.isEmpty();
        for (String word : words) {
            keywords &= OPERATOR_KEYWORDS.contains(word.toUpperCase(Locale.ROOT));
        }
        if (!keywords) {
            throw new IllegalArgumentException("Unsupported operator: " + operator);
        }
        return operator;
    }

    /**
     * Checks a value written as is in a statement, such as a {@code COALESCE} default.
     *
     * @return the value as text, unchanged
     */
    public static String literal(Object value) {
        return check(String.valueOf(value), "literal");
    }

    /**
     * Writes a string as a single-quoted literal.
     */
    public static String stringLiteral(String value) {
        return "'" + value.replace("'", "''") + "'";
    }

    private static String check(String fragment, String kind) {
        if (fragment == null || fragment.isEmpty()) {
            throw new IllegalArgumentException("Empty " + kind);
        }
        for (String forbidden : FORBIDDEN) {
            if (fragment.contains(forbidden)) {
                throw new IllegalArgumentException("Invalid " + kind + ": " + fragment);
            }
        }
        return fragment;
    }
}
