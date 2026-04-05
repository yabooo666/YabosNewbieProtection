package ge.macegate.yabosNewbieProt.listeners;

import ge.macegate.yabosNewbieProt.YabosNewbieProt;
import ge.macegate.yabosNewbieProt.managers.ProtectionManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerListener implements Listener {

    private final YabosNewbieProt plugin;
    private final ProtectionManager manager;

    public PlayerListener(YabosNewbieProt plugin, ProtectionManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        manager.handleJoin(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        manager.handleQuit(event.getPlayer());
    }
}
