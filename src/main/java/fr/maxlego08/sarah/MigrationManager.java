package fr.maxlego08.sarah;

import fr.maxlego08.sarah.conditions.ColumnDefinition;
import fr.maxlego08.sarah.database.DatabaseType;
import fr.maxlego08.sarah.database.Migration;
import fr.maxlego08.sarah.database.Schema;
import fr.maxlego08.sarah.exceptions.DatabaseException;
import fr.maxlego08.sarah.logger.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class MigrationManager {

    private static final List<Schema> schemas = new ArrayList<>();
    private static final List<Migration> migrations = new ArrayList<>();
    private static String migrationTableName = "migrations";
    private static DatabaseConfiguration databaseConfiguration;

    /**
     * Gets the name of the table used for storing migration information.
     * This table keeps track of the applied migrations in the database.
     *
     * @return the name of the migration table
     */
    public static String getMigrationTableName() {
        return migrationTableName;
    }

    /**
     * Sets the name of the table used to store migration information.
     * This table is used to keep track of which migrations have been
     * applied to the database, and in which order.
     *
     * @param migrationTableName the name of the table to use for migration
     *                           information
     */
    public static void setMigrationTableName(String migrationTableName) {
        MigrationManager.migrationTableName = migrationTableName;
    }

    /**
     * Returns the database configuration currently set for the migration manager.
     *
     * @return the currently set database configuration
     */
    public static DatabaseConfiguration getDatabaseConfiguration() {
        return databaseConfiguration;
    }

    /**
     * Sets the database configuration for the migration manager.
     * This configuration includes details such as the database type,
     * connection credentials, and other connection properties.
     *
     * @param databaseConfiguration the configuration to set
     */
    public static void setDatabaseConfiguration(DatabaseConfiguration databaseConfiguration) {
        MigrationManager.databaseConfiguration = databaseConfiguration;
    }

    /**
     * Registers a schema to execute during the migration process.
     *
     * @param schema the schema to register
     */
    public static void registerSchema(Schema schema) {
        schemas.add(schema);
    }

    /**
     * Executes all the registered migrations.
     *
     * @param databaseConnection the connection to the database
     * @param logger             the logger to use
     */
    public static void execute(DatabaseConnection databaseConnection, Logger logger) {

        createMigrationTable(databaseConnection, logger);

        List<String> migrationsFromDatabase = getMigrations(databaseConnection, logger);

        schemas.clear();
        MigrationManager.migrations.forEach(Migration::up);

        // A migration is recorded once, after all of its schemas ran: recording it after the
        // first one would skip the others for good if one of them failed.
        Map<Migration, List<Schema>> schemasByMigration = new LinkedHashMap<>();
        for (Schema schema : schemas) {
            schemasByMigration.computeIfAbsent(schema.getMigration(), migration -> new ArrayList<>()).add(schema);
        }

        schemasByMigration.forEach((migration, migrationSchemas) -> {
            if (!migrationsFromDatabase.contains(migration.getClass().getSimpleName())) {
                boolean applied = false;
                for (Schema schema : migrationSchemas) {
                    try {
                        applied |= schema.execute(databaseConnection, logger) != -1;
                    } catch (SQLException exception) {
                        throw new RuntimeException(exception);
                    }
                }
                if (applied) {
                    insertMigration(databaseConnection, logger, migration);
                }
            } else {
                migrationSchemas.forEach(schema -> addMissingColumns(databaseConnection, logger, schema));
            }
        });
    }

    /**
     * Returns the names of the migrations recorded in the migration table, creating the table
     * when it does not exist yet. A migration is recorded under its class's simple name.
     *
     * @param databaseConnection the connection to the database
     * @param logger             the logger to use
     * @return the names of the applied migrations
     * @throws DatabaseException if the migration table cannot be read
     */
    public static List<String> getAppliedMigrations(DatabaseConnection databaseConnection, Logger logger) {
        createMigrationTable(databaseConnection, logger);
        return getMigrations(databaseConnection, logger);
    }

    /**
     * Returns the registered migrations that are not recorded in the migration table yet, in
     * the order they were registered.
     *
     * @param databaseConnection the connection to the database
     * @param logger             the logger to use
     * @return the migrations {@link #execute} would apply
     * @throws DatabaseException if the migration table cannot be read
     */
    public static List<Migration> getPendingMigrations(DatabaseConnection databaseConnection, Logger logger) {
        List<String> applied = getAppliedMigrations(databaseConnection, logger);
        return migrations.stream()
                .filter(migration -> !applied.contains(migration.getClass().getSimpleName()))
                .collect(Collectors.toList());
    }

    /**
     * Rolls back the last {@code steps} applied migrations, the most recent first: runs their
     * {@link Migration#down()} and removes them from the migration table, so the next
     * {@link #execute} applies them again.
     *
     * <p>"Last" follows the order the migrations were registered, which is the order
     * {@link #execute} applies them. Only registered migrations can be rolled back. Every
     * migration to roll back must have a {@code down()}: when one does not, nothing is changed
     * and an {@link IllegalStateException} is thrown.</p>
     *
     * @param databaseConnection the connection to the database
     * @param logger             the logger to use
     * @param steps              how many migrations to roll back
     * @return the names of the migrations rolled back, the most recent first
     * @throws DatabaseException if a {@code down()} schema or the migration table update fails.
     *                           The migrations rolled back before it stay rolled back.
     */
    public static List<String> rollback(DatabaseConnection databaseConnection, Logger logger, int steps) {
        if (steps < 1) {
            throw new IllegalArgumentException("steps must be at least 1");
        }

        List<String> applied = getAppliedMigrations(databaseConnection, logger);
        List<Migration> toRollback = new ArrayList<>();
        for (int index = migrations.size() - 1; index >= 0 && toRollback.size() < steps; index--) {
            Migration migration = migrations.get(index);
            if (applied.contains(migration.getClass().getSimpleName())) {
                toRollback.add(migration);
            }
        }

        // Collect every down() first, so a migration without one stops the rollback before
        // anything changed
        Map<Migration, List<Schema>> downSchemas = new LinkedHashMap<>();
        for (Migration migration : toRollback) {
            schemas.clear();
            migration.down();
            if (schemas.isEmpty()) {
                throw new IllegalStateException("Migration " + migration.getClass().getSimpleName() + " has no down() and cannot be rolled back");
            }
            downSchemas.put(migration, new ArrayList<>(schemas));
        }
        schemas.clear();

        List<String> rolledBack = new ArrayList<>();
        for (Map.Entry<Migration, List<Schema>> entry : downSchemas.entrySet()) {
            String name = entry.getKey().getClass().getSimpleName();
            for (Schema schema : entry.getValue()) {
                try {
                    schema.execute(databaseConnection, logger);
                } catch (SQLException exception) {
                    logger.error("Failed to roll back migration " + name + ": " + exception.getMessage());
                    throw new DatabaseException("rollback", schema.getTableName(), exception);
                }
            }
            deleteMigration(databaseConnection, logger, name);
            logger.info("Rolled back migration " + name);
            rolledBack.add(name);
        }
        return rolledBack;
    }

    /**
     * Rolls back the most recently applied migration.
     *
     * @return the name of the migration rolled back, in a list, or an empty list when none is
     * applied
     * @see #rollback(DatabaseConnection, Logger, int)
     */
    public static List<String> rollback(DatabaseConnection databaseConnection, Logger logger) {
        return rollback(databaseConnection, logger, 1);
    }

    private static void deleteMigration(DatabaseConnection databaseConnection, Logger logger, String name) {
        try {
            SchemaBuilder.delete(migrationTableName).where("migration", name).execute(databaseConnection, logger);
        } catch (SQLException exception) {
            logger.error("Failed to delete migration record: " + exception.getMessage());
            throw new DatabaseException("delete-migration", migrationTableName, exception);
        }
    }

    /**
     * Adds the columns of an already applied create-or-alter migration that the table lacks.
     */
    private static void addMissingColumns(DatabaseConnection databaseConnection, Logger logger, Schema schema) {
        if (!schema.getMigration().isAlter()) {
            return;
        }

        List<ColumnDefinition> mustBeAdd = new ArrayList<>();

        String tableName = schema.getTableName();
        tableName = tableName.replace("%prefix%", databaseConnection.getDatabaseConfiguration().getTablePrefix());

        if (databaseConnection.getDatabaseConfiguration().getDatabaseType() == DatabaseType.SQLITE) {
            String query = String.format("PRAGMA table_info(%s)", tableName);
            try (Connection connection = databaseConnection.getConnection();
                 PreparedStatement preparedStatement = connection.prepareStatement(query)) {
                List<ColumnDefinition> columnDefinitions = schema.getColumns();
                if (databaseConnection.getDatabaseConfiguration().isDebug()) {
                    logger.info("Executing SQL: " + query);
                }
                try (ResultSet resultSet = preparedStatement.executeQuery()) {
                    while (resultSet.next()) {
                        String columnName = resultSet.getString("name");
                        columnDefinitions.removeIf(column -> column.getName().equals(columnName));
                    }
                }
                mustBeAdd.addAll(columnDefinitions);
            } catch (SQLException exception) {
                logger.error("Failed to get table info for migration: " + exception.getMessage());
                throw new DatabaseException("migration-table-info", tableName, exception);
            }
        } else {
            for (ColumnDefinition column : schema.getColumns()) {
                Schema columnExistQuery;
                long result;
                if (databaseConnection.getDatabaseConfiguration().getDatabaseType() == DatabaseType.POSTGRESQL) {
                    columnExistQuery = SchemaBuilder.selectCount("information_schema.columns")
                            .where("table_name", tableName)
                            .where("table_catalog", databaseConnection.getDatabaseConfiguration().getDatabase())
                            .where("column_name", column.getName());
                } else {
                    columnExistQuery = SchemaBuilder.selectCount("information_schema.COLUMNS")
                            .where("TABLE_NAME", tableName)
                            .where("TABLE_SCHEMA", databaseConnection.getDatabaseConfiguration().getDatabase())
                            .where("COLUMN_NAME", column.getName());
                }
                try {
                    result = columnExistQuery.executeSelectCount(databaseConnection, logger);
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
                if (result == 0) {
                    mustBeAdd.add(column);
                }
            }
        }

        if (mustBeAdd.isEmpty()) {
            return;
        }

        // The migration is already recorded, so only the columns are added here
        try {
            SchemaBuilder.alter(null, tableName, (schemaAlter) -> {
                for (ColumnDefinition column : mustBeAdd) {
                    schemaAlter.addColumn(column).nullable();
                }
            }).execute(databaseConnection, logger);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Returns the list of migrations that have been registered with the migration manager.
     * These migrations will be executed in order when the migration manager is executed.
     *
     * @return the list of registered migrations
     */
    public static List<Migration> getMigrations() {
        return migrations;
    }

    /**
     * Creates the migration table that keeps track of all the migrations that
     * have been executed.
     *
     * @param databaseConnection the connection to the database
     * @param logger             the logger to use
     */
    private static void createMigrationTable(DatabaseConnection databaseConnection, Logger logger) {
        Schema schema = SchemaBuilder.create(null, migrationTableName, sc -> {
            sc.text("migration");
            sc.createdAt();
        });
        try {
            schema.execute(databaseConnection, logger);
        } catch (SQLException exception) {
            logger.error("Failed to create migration table: " + exception.getMessage());
            throw new DatabaseException("create-migration-table", migrationTableName, exception);
        }
    }

    /**
     * Gets the list of migrations that have been executed on the database.
     *
     * @param databaseConnection the connection to the database
     * @param logger             the logger to use
     * @return the list of executed migrations
     * @throws DatabaseException if the list cannot be read. Treating it as empty would run every
     *                           migration again.
     */
    private static List<String> getMigrations(DatabaseConnection databaseConnection, Logger logger) {
        Schema schema = SchemaBuilder.select(migrationTableName);
        try {
            return schema.executeSelect(MigrationTable.class, databaseConnection, logger).stream().map(MigrationTable::getMigration).collect(Collectors.toList());
        } catch (Exception exception) {
            logger.error("Failed to get migrations list: " + exception.getMessage());
            throw new DatabaseException("get-migrations", migrationTableName, exception);
        }
    }

    /**
     * Inserts a migration record into the migration table.
     * This method records the name of the migration class as a string
     * in the migration table to track its application in the database.
     *
     * @param databaseConnection the connection to the database
     * @param logger             the logger to use for logging any relevant information
     * @param migration          the migration to be recorded
     */
    private static void insertMigration(DatabaseConnection databaseConnection, Logger logger, Migration migration) {
        try {
            SchemaBuilder.insert(migrationTableName, schema -> schema.string("migration", migration.getClass().getSimpleName())).execute(databaseConnection, logger);
        } catch (SQLException exception) {
            logger.error("Failed to insert migration record: " + exception.getMessage());
            throw new DatabaseException("insert-migration", migrationTableName, exception);
        }
    }

    /**
     * Registers a migration with the migration manager.
     * This migration will be added to the list of migrations
     * and will be executed during the migration process.
     *
     * @param migration the migration to register
     */
    public static void registerMigration(Migration migration) {
        migrations.add(migration);
    }

    public static class MigrationTable {

        @Column(value = "migration")
        private final String migration;

        public MigrationTable(String migration) {
            this.migration = migration;
        }

        public String getMigration() {
            return migration;
        }
    }
}
