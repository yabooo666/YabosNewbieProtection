package ge.macegate.yabosNewbieProt;

import ge.macegate.yabosNewbieProt.commands.NewbieProtCommand;
import ge.macegate.yabosNewbieProt.hooks.PlaceholderAPIHook;
import ge.macegate.yabosNewbieProt.listeners.DamageListener;
import ge.macegate.yabosNewbieProt.listeners.PlayerListener;
import ge.macegate.yabosNewbieProt.managers.ProtectionManager;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class YabosNewbieProt extends JavaPlugin {

    private ProtectionManager protectionManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        this.protectionManager = new ProtectionManager(this);
        this.protectionManager.loadData();
        this.protectionManager.startTasks();

        Bukkit.getPluginManager().registerEvents(new DamageListener(this, protectionManager), this);
        Bukkit.getPluginManager().registerEvents(new PlayerListener(this, protectionManager), this);

        PluginCommand command = getCommand("newbieprot");
        if (command == null) {
            getLogger().severe("Command 'newbieprot' is missing from plugin.yml. Disabling plugin.");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        NewbieProtCommand newbieProtCommand = new NewbieProtCommand(this, protectionManager);
        command.setExecutor(newbieProtCommand);
        command.setTabCompleter(newbieProtCommand);

        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new PlaceholderAPIHook(this, protectionManager).register();
            getLogger().info("Hooked into PlaceholderAPI successfully.");
        } else if (getConfig().getBoolean("hooks.placeholderapi.warn-if-missing", true)) {
            getLogger().warning("PlaceholderAPI not found. Placeholder expansion was not registered.");
        }

        getLogger().info("YabosNewbieProt enabled.");
    }

    @Override
    public void onDisable() {
        if (protectionManager != null) {
            protectionManager.shutdown();
        }
        getLogger().info("YabosNewbieProt disabled.");
    }

    public ProtectionManager getProtectionManager() {
        return protectionManager;
    }
}
