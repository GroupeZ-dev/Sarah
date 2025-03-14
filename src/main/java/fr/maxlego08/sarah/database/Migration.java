package fr.maxlego08.sarah.database;

import fr.maxlego08.sarah.SchemaBuilder;

import java.util.function.Consumer;

/**
 * Represents a database migration for creating or modifying tables.
 */
public abstract class Migration {

    private boolean alter = false;

    /**
     * Modifies the database according to the instructions in this migration.
     * Implementations should call the various methods provided by this class
     * to create, modify, or drop tables in the database.
     */
    public abstract void up();

    /**
     * Creates a new table in the database.
     *
     * @param table    The name of the table to create.
     * @param consumer A consumer that will be called with a {@link Schema} object.
     *                 The consumer can then use the methods of the schema to define
     *                 the table.
     */
    protected void create(String table, Consumer<Schema> consumer) {
        SchemaBuilder.create(this, table, consumer);
    }

    /**
     * Creates a new table in the database using a template class.
     *
     * @param table    The name of the table to create.
     * @param template The class template used to define the table structure.
     */
    protected void create(String table, Class<?> template) {
        SchemaBuilder.create(this, table, template);
    }

    /**
     * Drops a table from the database.
     *
     * @param table The name of the table to drop.
     */
    protected void drop(String table) {
        SchemaBuilder.drop(this, table);
    }

    /**
     * Modifies an existing table in the database.
     *
     * @param table    The name of the table to modify.
     * @param consumer A consumer that will be called with a {@link Schema} object.
     *                 The consumer can then use the methods of the schema to modify
     *                 the table.
     */
    protected void modify(String table, Consumer<Schema> consumer) {
        SchemaBuilder.modify(this, table, consumer);
    }

    /**
     * Creates a new table or alters an existing one in the database.
     *
     * @param table    The name of the table to create or alter.
     * @param consumer A consumer that will be called with a {@link Schema} object.
     *                 The consumer can then use the methods of the schema to define
     *                 the table or apply modifications.
     *                 This method sets the alter flag to true after attempting to create the table.
     */
    protected void createOrAlter(String table, Consumer<Schema> consumer) {
        this.create(table, consumer);
        this.alter = true;
    }

    /**
     * Creates a new table or alters an existing one in the database using a template class.
     *
     * @param table    The name of the table to create or alter.
     * @param template The class template used to define the table structure.
     *                 This method sets the alter flag to true after attempting to create the table.
     */
    protected void createOrAlter(String table, Class<?> template) {
        this.create(table, template);
        this.alter = true;
    }

    /**
     * Checks if this migration is an alter migration.
     * This method is useful for other classes to check if this migration
     * is an alter migration.
     *
     * @return True if this migration is an alter migration, false otherwise.
     */
    public boolean isAlter() {
        return alter;
    }
}

