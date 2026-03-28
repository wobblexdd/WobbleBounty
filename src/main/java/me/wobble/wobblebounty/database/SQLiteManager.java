package me.wobble.wobblebounty.database;

import me.wobble.wobblebounty.WobbleBounty;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public final class SQLiteManager {

    private final WobbleBounty plugin;
    private Connection connection;

    public SQLiteManager(WobbleBounty plugin) {
        this.plugin = plugin;
    }

    public void connect() {
        try {
            if (!plugin.getDataFolder().exists() && !plugin.getDataFolder().mkdirs()) {
                plugin.getLogger().warning("Could not create plugin data folder.");
            }

            String fileName = plugin.getConfig().getString("database.file", "data.db");
            File dbFile = new File(plugin.getDataFolder(), fileName);

            this.connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());
            plugin.getLogger().info("Connected to SQLite database.");
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not connect to SQLite database", exception);
        }
    }

    public void createTables() {
        String createBounties = """
                CREATE TABLE IF NOT EXISTS bounties (
                    target_uuid TEXT PRIMARY KEY,
                    amount REAL NOT NULL,
                    created_at INTEGER NOT NULL DEFAULT 0
                );
                """;

        String createContributions = """
                CREATE TABLE IF NOT EXISTS bounty_contributions (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    target_uuid TEXT NOT NULL,
                    placer_uuid TEXT NOT NULL,
                    amount REAL NOT NULL
                );
                """;

        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate(createBounties);
            statement.executeUpdate(createContributions);
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not create tables", exception);
        }

        ensureColumnExists("bounties", "created_at", "INTEGER NOT NULL DEFAULT 0");
    }

    private void ensureColumnExists(String tableName, String columnName, String columnDefinition) {
        if (hasColumn(tableName, columnName)) {
            return;
        }

        String sql = "ALTER TABLE " + tableName + " ADD COLUMN " + columnName + " " + columnDefinition;

        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate(sql);
            plugin.getLogger().info("Added missing column " + columnName + " to " + tableName + ".");
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not add missing column " + columnName + " to " + tableName, exception);
        }
    }

    private boolean hasColumn(String tableName, String columnName) {
        String sql = "PRAGMA table_info(" + tableName + ")";

        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {

            while (resultSet.next()) {
                String existing = resultSet.getString("name");
                if (columnName.equalsIgnoreCase(existing)) {
                    return true;
                }
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not inspect table structure for " + tableName, exception);
        }

        return false;
    }

    public Connection getConnection() {
        return connection;
    }

    public PreparedStatement prepare(String sql) throws SQLException {
        return connection.prepareStatement(sql);
    }

    public void close() {
        if (connection == null) {
            return;
        }

        try {
            connection.close();
        } catch (SQLException exception) {
            plugin.getLogger().severe("Could not close SQLite connection.");
        }
    }
}
