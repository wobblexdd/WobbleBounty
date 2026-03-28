package me.wobble.wobblebounty;

import me.wobble.wobblebounty.command.BountyCommand;
import me.wobble.wobblebounty.database.SQLiteManager;
import me.wobble.wobblebounty.economy.EconomyProvider;
import me.wobble.wobblebounty.listener.BountyMenuListener;
import me.wobble.wobblebounty.listener.PlayerKillListener;
import me.wobble.wobblebounty.listener.PlayerSearchListener;
import me.wobble.wobblebounty.repository.BountyRepository;
import me.wobble.wobblebounty.service.BountyService;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.Objects;
import java.util.logging.Level;

public final class WobbleBounty extends JavaPlugin {

    private FileConfiguration messagesConfig;
    private File messagesFile;

    private EconomyProvider economyProvider;
    private SQLiteManager sqliteManager;
    private BountyRepository bountyRepository;
    private BountyService bountyService;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        saveResourceIfNotExists("messages.yml");
        loadMessages();

        this.economyProvider = new EconomyProvider(this);
        if (!economyProvider.setup()) {
            getLogger().severe("Vault economy provider not found. Disabling plugin.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        this.sqliteManager = new SQLiteManager(this);
        sqliteManager.connect();
        sqliteManager.createTables();

        this.bountyRepository = new BountyRepository(sqliteManager);
        this.bountyService = new BountyService(this, economyProvider, bountyRepository);

        PluginCommand bountyCommand = getCommand("bounty");
        if (bountyCommand != null) {
            BountyCommand executor = new BountyCommand(this, bountyService);
            bountyCommand.setExecutor(executor);
            bountyCommand.setTabCompleter(executor);
        }

        BountyMenuListener bountyMenuListener = new BountyMenuListener(this);

        getServer().getPluginManager().registerEvents(new PlayerKillListener(this, bountyService), this);
        getServer().getPluginManager().registerEvents(bountyMenuListener, this);
        getServer().getPluginManager().registerEvents(new PlayerSearchListener(this, bountyMenuListener), this);

        getLogger().info("WobbleBounty enabled.");
    }

    @Override
    public void onDisable() {
        if (sqliteManager != null) {
            sqliteManager.close();
        }
        getLogger().info("WobbleBounty disabled.");
    }

    public void reloadPlugin() {
        reloadConfig();
        loadMessages();
    }

    private void saveResourceIfNotExists(String resourcePath) {
        File file = new File(getDataFolder(), resourcePath);
        if (!file.exists()) {
            saveResource(resourcePath, false);
        }
    }

    private void loadMessages() {
        if (!getDataFolder().exists() && !getDataFolder().mkdirs()) {
            getLogger().warning("Could not create plugin data folder.");
        }

        this.messagesFile = new File(getDataFolder(), "messages.yml");
        if (!messagesFile.exists()) {
            saveResourceIfNotExists("messages.yml");
        }

        this.messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);
    }

    public void saveMessages() {
        if (messagesConfig == null || messagesFile == null) {
            return;
        }

        try {
            messagesConfig.save(messagesFile);
        } catch (IOException exception) {
            getLogger().log(Level.SEVERE, "Could not save messages.yml", exception);
        }
    }

    public FileConfiguration getMessagesConfig() {
        return Objects.requireNonNull(messagesConfig, "messagesConfig");
    }

    public EconomyProvider getEconomyProvider() {
        return economyProvider;
    }

    public SQLiteManager getSqliteManager() {
        return sqliteManager;
    }

    public BountyRepository getBountyRepository() {
        return bountyRepository;
    }

    public BountyService getBountyService() {
        return bountyService;
    }
}
