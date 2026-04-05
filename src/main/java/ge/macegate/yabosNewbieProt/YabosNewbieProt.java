package ge.macegate.yabosNewbieProt;

import ge.macegate.yabosNewbieProt.commands.NewbieProtCommand;
import ge.macegate.yabosNewbieProt.hooks.PlaceholderAPIHook;
import ge.macegate.yabosNewbieProt.listeners.DamageListener;
import ge.macegate.yabosNewbieProt.listeners.PlayerListener;
import ge.macegate.yabosNewbieProt.managers.ProtectionManager;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public final class YabosNewbieProt extends JavaPlugin {

    private static YabosNewbieProt instance;
    private ProtectionManager protectionManager;

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();

        protectionManager = new ProtectionManager(this);
        protectionManager.loadData();

        // Start AFK detection & countdown ticker
        protectionManager.startTicker();

        // Register listeners
        Bukkit.getPluginManager().registerEvents(new DamageListener(protectionManager), this);
        Bukkit.getPluginManager().registerEvents(new PlayerListener(protectionManager, this), this);

        // Register command
        NewbieProtCommand cmd = new NewbieProtCommand(protectionManager, this);
        getCommand("newbieprot").setExecutor(cmd);
        getCommand("newbieprot").setTabCompleter(cmd);

        // Hook PlaceholderAPI
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new PlaceholderAPIHook(protectionManager).register();
            getLogger().info("Hooked into PlaceholderAPI successfully.");
        } else {
            getLogger().warning("PlaceholderAPI not found. Placeholders will not work.");
        }

        getLogger().info("YabosNewbieProt enabled!");
    }

    @Override
    public void onDisable() {
        if (protectionManager != null) {
            protectionManager.saveData();
        }
        getLogger().info("YabosNewbieProt disabled. Data saved.");
    }

    public static YabosNewbieProt getInstance() {
        return instance;
    }

    public ProtectionManager getProtectionManager() {
        return protectionManager;
    }
}
