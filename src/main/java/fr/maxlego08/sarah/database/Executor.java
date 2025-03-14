package fr.maxlego08.sarah.database;

import fr.maxlego08.sarah.DatabaseConfiguration;
import fr.maxlego08.sarah.DatabaseConnection;
import fr.maxlego08.sarah.logger.Logger;

public interface Executor {

    /**
     * Executes a database request using the given database connection, database configuration and logger.
     *
     * @param databaseConnection    the database connection to use
     * @param databaseConfiguration the database configuration to use
     * @param logger                the logger to use
     * @return the number of rows affected by the query
     */
    int execute(DatabaseConnection databaseConnection, DatabaseConfiguration databaseConfiguration, Logger logger);

}
