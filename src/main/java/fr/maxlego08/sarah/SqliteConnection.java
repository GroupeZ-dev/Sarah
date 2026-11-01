package fr.maxlego08.sarah;

import fr.maxlego08.sarah.logger.Logger;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Properties;

public class SqliteConnection extends DatabaseConnection {

    private final File folder;
    private String fileName = "database.db";
    private String journalMode;
    private Integer busyTimeout;

    public SqliteConnection(DatabaseConfiguration databaseConfiguration, File folder, Logger logger) {
        super(databaseConfiguration, logger);
        this.folder = folder;
    }

    @Override
    public Connection connectToDatabase() throws Exception {
        // Thread-safe directory creation using Files API
        Files.createDirectories(folder.toPath());

        // SQLite automatically creates the file if it doesn't exist
        Path dbPath = folder.toPath().resolve(fileName);
        String url = "jdbc:sqlite:" + dbPath.toAbsolutePath();

        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException ignored) {
        }

        Properties properties = new Properties();
        properties.putAll(this.databaseConfiguration.getProperties());
        if (this.journalMode != null) {
            properties.setProperty("journal_mode", this.journalMode);
        }
        if (this.busyTimeout != null) {
            properties.setProperty("busy_timeout", String.valueOf(this.busyTimeout));
        }

        return DriverManager.getConnection(url, properties);
    }

    public String getJournalMode() {
        return journalMode;
    }

    /**
     * Sets the journal mode applied to every connection, for example {@code WAL}, which lets
     * readers work while a write is in progress. Null keeps the SQLite default.
     */
    public void setJournalMode(String journalMode) {
        this.journalMode = journalMode;
    }

    public Integer getBusyTimeout() {
        return busyTimeout;
    }

    /**
     * Sets how long, in milliseconds, a connection waits for a lock held by another one before
     * failing with {@code SQLITE_BUSY}. Null keeps the driver default.
     */
    public void setBusyTimeout(Integer busyTimeout) {
        this.busyTimeout = busyTimeout;
    }

    public File getFolder() {
        return folder;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }
}
