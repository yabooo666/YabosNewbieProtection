package ge.macegate.yabosNewbieProt.hooks;

import ge.macegate.yabosNewbieProt.YabosNewbieProt;
import ge.macegate.yabosNewbieProt.managers.ProtectionManager;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;

public class PlaceholderAPIHook extends PlaceholderExpansion {

    private final YabosNewbieProt plugin;
    private final ProtectionManager manager;

    public PlaceholderAPIHook(YabosNewbieProt plugin, ProtectionManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    @Override
    public @NotNull String getIdentifier() {
        return plugin.getConfig().getString("hooks.placeholderapi.identifier", "newbieprot");
    }

    @Override
    public @NotNull String getAuthor() {
        return plugin.getDescription().getAuthors().isEmpty() ? "macegate" : plugin.getDescription().getAuthors().get(0);
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return plugin.getConfig().getBoolean("hooks.placeholderapi.persist", true);
    }

    @Override
    public @Nullable String onRequest(OfflinePlayer player, @NotNull String params) {
        if (player == null) {
            return "";
        }

        boolean protectedState = manager.isProtected(player.getUniqueId());
        long remaining = manager.getRemainingSeconds(player.getUniqueId());

        return switch (params.toLowerCase()) {
            case "", "time" -> protectedState
                ? manager.formatTime(remaining)
                : plugin.getConfig().getString("hooks.placeholderapi.unprotected-time-text", "Not protected");
            case "raw", "seconds" -> String.valueOf(remaining);
            case "active", "enabled" -> String.valueOf(protectedState);
            case "status" -> protectedState
                ? plugin.getConfig().getString("hooks.placeholderapi.status-protected", "Protected")
                : plugin.getConfig().getString("hooks.placeholderapi.status-unprotected", "Unprotected");
            case "name" -> manager.getLastKnownName(player.getUniqueId());
            default -> null;
        };
    }
}
