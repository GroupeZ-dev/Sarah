package fr.maxlego08.sarah;

import fr.maxlego08.sarah.logger.Logger;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;

public class SqliteConnection extends DatabaseConnection {

    private final File folder;
    private String fileName = "database.db";

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

        return DriverManager.getConnection(url);
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

    @Override
    public Connection getConnection() {
        try {
            return connectToDatabase();
        } catch (Exception exception) {
            connect();
            return connection;
        }
    }
}
