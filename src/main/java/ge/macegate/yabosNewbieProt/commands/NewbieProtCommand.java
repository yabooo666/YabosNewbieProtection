package ge.macegate.yabosNewbieProt.commands;

import ge.macegate.yabosNewbieProt.YabosNewbieProt;
import ge.macegate.yabosNewbieProt.model.RemovalReason;
import ge.macegate.yabosNewbieProt.managers.ProtectionManager;
import ge.macegate.yabosNewbieProt.util.MessageUtil;
import ge.macegate.yabosNewbieProt.util.TimeUtil;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

public class NewbieProtCommand implements CommandExecutor, TabCompleter {

    private static final List<String> CANONICAL_SUBCOMMANDS = List.of(
        "help", "grant", "remove", "check", "list", "reload", "set", "addtime", "removetime", "status"
    );

    private final YabosNewbieProt plugin;
    private final ProtectionManager manager;

    public NewbieProtCommand(YabosNewbieProt plugin, ProtectionManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String subcommand = resolveSubcommand(args[0]);
        if (subcommand == null) {
            sendHelp(sender);
            return true;
        }

        return switch (subcommand) {
            case "help" -> executeHelp(sender);
            case "status" -> executeStatus(sender);
            case "grant" -> executeGrant(sender, args);
            case "remove" -> executeRemove(sender, args);
            case "check" -> executeCheck(sender, args);
            case "list" -> executeList(sender, args);
            case "reload" -> executeReload(sender);
            case "set" -> executeSet(sender, args);
            case "addtime" -> executeAddTime(sender, args);
            case "removetime" -> executeRemoveTime(sender, args);
            default -> {
                sendHelp(sender);
                yield true;
            }
        };
    }

    private boolean executeHelp(CommandSender sender) {
        sendHelp(sender);
        return true;
    }

    private boolean executeStatus(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            MessageUtil.send(sender, plugin.getConfig(), "messages.command.players-only", "<red>This command can only be used by a player.</red>");
            return true;
        }
        if (!hasPermission(sender, false)) {
            sendNoPermission(sender);
            return true;
        }

        if (manager.isProtected(player.getUniqueId())) {
            MessageUtil.send(sender, plugin.getConfig(), "messages.command.status-protected",
                "<green>You are protected for <white>{time}</white>.</green>",
                Map.of("player", player.getName(), "time", manager.formatTime(manager.getRemainingSeconds(player.getUniqueId()))));
        } else {
            MessageUtil.send(sender, plugin.getConfig(), "messages.command.status-unprotected",
                "<yellow>You do not currently have newbie protection.</yellow>",
                Map.of("player", player.getName(), "time", manager.formatTime(0L)));
        }
        return true;
    }

    private boolean executeGrant(CommandSender sender, String[] args) {
        if (!hasPermission(sender, true)) {
            sendNoPermission(sender);
            return true;
        }
        if (args.length < 2) {
            sendUsage(sender, "grant");
            return true;
        }

        OfflinePlayer target = resolveTarget(sender, args[1]);
        if (target == null) {
            return true;
        }

        long seconds = args.length >= 3 ? parseDuration(sender, args[2]) : manager.getDefaultDurationSeconds();
        if (seconds < 0L) {
            return true;
        }

        String name = target.getName() == null ? args[1] : target.getName();
        manager.grantProtection(target.getUniqueId(), name, seconds, true, target.isOnline());
        MessageUtil.send(sender, plugin.getConfig(), "messages.command.granted-admin",
            "<green>Granted newbie protection to <white>{player}</white> for <white>{time}</white>.</green>",
            Map.of("player", name, "time", manager.formatTime(seconds)));
        return true;
    }

    private boolean executeSet(CommandSender sender, String[] args) {
        if (!hasPermission(sender, true)) {
            sendNoPermission(sender);
            return true;
        }
        if (args.length < 3) {
            sendUsage(sender, "set");
            return true;
        }

        OfflinePlayer target = resolveTarget(sender, args[1]);
        if (target == null) {
            return true;
        }

        long seconds = parseDuration(sender, args[2]);
        if (seconds < 0L) {
            return true;
        }

        String name = target.getName() == null ? args[1] : target.getName();
        manager.setProtectionSeconds(target.getUniqueId(), name, seconds, target.isOnline());
        MessageUtil.send(sender, plugin.getConfig(), "messages.command.set-admin",
            "<green>Set <white>{player}</white>'s protection to <white>{time}</white>.</green>",
            Map.of("player", name, "time", manager.formatTime(seconds)));
        return true;
    }

    private boolean executeAddTime(CommandSender sender, String[] args) {
        if (!hasPermission(sender, true)) {
            sendNoPermission(sender);
            return true;
        }
        if (args.length < 3) {
            sendUsage(sender, "addtime");
            return true;
        }

        OfflinePlayer target = resolveTarget(sender, args[1]);
        if (target == null) {
            return true;
        }

        long seconds = parseDuration(sender, args[2]);
        if (seconds <= 0L) {
            return true;
        }

        String name = target.getName() == null ? args[1] : target.getName();
        manager.addProtectionSeconds(target.getUniqueId(), name, seconds, target.isOnline());
        MessageUtil.send(sender, plugin.getConfig(), "messages.command.added-time-admin",
            "<green>Added <white>{time}</white> to <white>{player}</white>.</green>",
            Map.of("player", name, "time", manager.formatTime(seconds)));
        return true;
    }

    private boolean executeRemoveTime(CommandSender sender, String[] args) {
        if (!hasPermission(sender, true)) {
            sendNoPermission(sender);
            return true;
        }
        if (args.length < 3) {
            sendUsage(sender, "removetime");
            return true;
        }

        OfflinePlayer target = resolveTarget(sender, args[1]);
        if (target == null) {
            return true;
        }

        long seconds = parseDuration(sender, args[2]);
        if (seconds <= 0L) {
            return true;
        }

        String name = target.getName() == null ? args[1] : target.getName();
        if (!manager.removeProtectionSeconds(target.getUniqueId(), seconds, target.isOnline())) {
            MessageUtil.send(sender, plugin.getConfig(), "messages.command.not-protected",
                "<yellow><white>{player}</white> is not protected.</yellow>",
                Map.of("player", name, "time", manager.formatTime(0L)));
            return true;
        }

        MessageUtil.send(sender, plugin.getConfig(), "messages.command.removed-time-admin",
            "<yellow>Removed <white>{time}</white> from <white>{player}</white>.</yellow>",
            Map.of("player", name, "time", manager.formatTime(seconds)));
        return true;
    }

    private boolean executeRemove(CommandSender sender, String[] args) {
        if (!hasPermission(sender, true)) {
            sendNoPermission(sender);
            return true;
        }
        if (args.length < 2) {
            sendUsage(sender, "remove");
            return true;
        }

        OfflinePlayer target = resolveTarget(sender, args[1]);
        if (target == null) {
            return true;
        }

        String name = target.getName() == null ? args[1] : target.getName();
        if (!manager.removeProtection(target.getUniqueId(), RemovalReason.ADMIN, target.isOnline())) {
            MessageUtil.send(sender, plugin.getConfig(), "messages.command.not-protected",
                "<yellow><white>{player}</white> is not protected.</yellow>",
                Map.of("player", name));
            return true;
        }

        MessageUtil.send(sender, plugin.getConfig(), "messages.command.removed-admin",
            "<green>Removed newbie protection from <white>{player}</white>.</green>",
            Map.of("player", name));
        return true;
    }

    private boolean executeCheck(CommandSender sender, String[] args) {
        if (!hasPermission(sender, true)) {
            sendNoPermission(sender);
            return true;
        }
        if (args.length < 2) {
            sendUsage(sender, "check");
            return true;
        }

        OfflinePlayer target = resolveTarget(sender, args[1]);
        if (target == null) {
            return true;
        }

        String name = target.getName() == null ? args[1] : target.getName();
        long remaining = manager.getRemainingSeconds(target.getUniqueId());
        if (remaining > 0L) {
            MessageUtil.send(sender, plugin.getConfig(), "messages.command.check-protected",
                "<green><white>{player}</white> has <white>{time}</white> remaining.</green>",
                Map.of("player", name, "time", manager.formatTime(remaining)));
        } else {
            MessageUtil.send(sender, plugin.getConfig(), "messages.command.check-unprotected",
                "<yellow><white>{player}</white> is not protected.</yellow>",
                Map.of("player", name, "time", manager.formatTime(0L)));
        }
        return true;
    }

    private boolean executeList(CommandSender sender, String[] args) {
        if (!hasPermission(sender, true)) {
            sendNoPermission(sender);
            return true;
        }

        Map<java.util.UUID, Long> snapshot = manager.getProtectedPlayersSnapshot();
        if (snapshot.isEmpty()) {
            MessageUtil.send(sender, plugin.getConfig(), "messages.command.list-empty", "<yellow>No protected players found.</yellow>");
            return true;
        }

        int pageSize = Math.max(1, plugin.getConfig().getInt("commands.page-size", 10));
        int requestedPage = 1;
        if (args.length >= 2 && args[1].matches("\\d+")) {
            requestedPage = Math.max(1, Integer.parseInt(args[1]));
        }

        List<Map.Entry<java.util.UUID, Long>> entries = snapshot.entrySet().stream()
            .sorted(Comparator.comparing(entry -> manager.getLastKnownName(entry.getKey()), String.CASE_INSENSITIVE_ORDER))
            .collect(Collectors.toList());

        int totalPages = (int) Math.ceil(entries.size() / (double) pageSize);
        int safePage = Math.min(requestedPage, totalPages);
        int fromIndex = (safePage - 1) * pageSize;
        int toIndex = Math.min(entries.size(), fromIndex + pageSize);

        MessageUtil.send(sender, plugin.getConfig(), "messages.command.list-header",
            "<gold><bold>Protected Players</bold></gold> <gray>({page}/{pages})</gray>",
            Map.of("page", String.valueOf(safePage), "pages", String.valueOf(totalPages)));

        String lineFormat = plugin.getConfig().getString("messages.command.list-line",
            "<gray>-</gray> <white>{player}</white> <gray>→</gray> <aqua>{time}</aqua> <dark_gray>({status})</dark_gray>");

        for (Map.Entry<java.util.UUID, Long> entry : entries.subList(fromIndex, toIndex)) {
            String playerName = manager.getLastKnownName(entry.getKey());
            String status = Bukkit.getPlayer(entry.getKey()) != null
                ? plugin.getConfig().getString("messages.command.list-status-online", "online")
                : plugin.getConfig().getString("messages.command.list-status-offline", "offline");
            sender.sendMessage(MessageUtil.component(MessageUtil.applyPlaceholders(lineFormat, Map.of(
                "player", playerName,
                "time", manager.formatTime(entry.getValue()),
                "status", status
            ))));
        }

        MessageUtil.send(sender, plugin.getConfig(), "messages.command.list-footer",
            "<gray>Total: <white>{total}</white></gray>",
            Map.of("total", String.valueOf(entries.size()), "page", String.valueOf(safePage), "pages", String.valueOf(totalPages)));
        return true;
    }

    private boolean executeReload(CommandSender sender) {
        if (!hasPermission(sender, true)) {
            sendNoPermission(sender);
            return true;
        }

        plugin.reloadConfig();
        manager.reloadRuntime();
        MessageUtil.send(sender, plugin.getConfig(), "messages.command.reload-success", "<green>Configuration reloaded successfully.</green>");
        return true;
    }

    private void sendHelp(CommandSender sender) {
        FileConfiguration config = plugin.getConfig();
        MessageUtil.send(sender, config, "messages.command.help-title", "<gold><bold>NewbieProt Commands</bold></gold>");
        List<String> lines = config.getStringList("messages.command.help-lines");
        if (lines.isEmpty()) {
            lines = List.of(
                "<yellow>/newbieprot status</yellow> <gray>- Check your own protection</gray>",
                "<yellow>/newbieprot grant <player> [time]</yellow> <gray>- Grant protection</gray>",
                "<yellow>/newbieprot set <player> <time></yellow> <gray>- Set protection exactly</gray>",
                "<yellow>/newbieprot addtime <player> <time></yellow> <gray>- Add protection time</gray>",
                "<yellow>/newbieprot removetime <player> <time></yellow> <gray>- Remove protection time</gray>",
                "<yellow>/newbieprot remove <player></yellow> <gray>- Remove protection</gray>",
                "<yellow>/newbieprot check <player></yellow> <gray>- Check remaining time</gray>",
                "<yellow>/newbieprot list [page]</yellow> <gray>- List protected players</gray>",
                "<yellow>/newbieprot reload</yellow> <gray>- Reload config</gray>"
            );
        }

        boolean admin = hasPermission(sender, true);
        for (String line : lines) {
            boolean adminOnly = line.contains("{admin-only}");
            if (adminOnly && !admin) {
                continue;
            }
            sender.sendMessage(MessageUtil.component(line.replace("{admin-only}", "")));
        }
    }

    private void sendUsage(CommandSender sender, String subcommand) {
        String path = "messages.command.usage." + subcommand;
        String fallback = switch (subcommand) {
            case "grant" -> "<red>Usage: /newbieprot grant <player> [time]</red>";
            case "set" -> "<red>Usage: /newbieprot set <player> <time></red>";
            case "addtime" -> "<red>Usage: /newbieprot addtime <player> <time></red>";
            case "removetime" -> "<red>Usage: /newbieprot removetime <player> <time></red>";
            case "remove" -> "<red>Usage: /newbieprot remove <player></red>";
            case "check" -> "<red>Usage: /newbieprot check <player></red>";
            default -> "<red>Invalid usage.</red>";
        };
        MessageUtil.send(sender, plugin.getConfig(), path, fallback);
    }

    private void sendNoPermission(CommandSender sender) {
        MessageUtil.send(sender, plugin.getConfig(), "messages.command.no-permission", "<red>You do not have permission to use this command.</red>");
    }

    private OfflinePlayer resolveTarget(CommandSender sender, String input) {
        OfflinePlayer target = manager.resolveOfflinePlayer(input);
        if (target == null) {
            MessageUtil.send(sender, plugin.getConfig(), "messages.command.player-not-found",
                "<red>Player <white>{player}</white> was not found.</red>",
                Map.of("player", input));
            return null;
        }

        boolean allowNeverJoined = plugin.getConfig().getBoolean("commands.allow-targeting-never-joined-players", false);
        if (!allowNeverJoined && !target.hasPlayedBefore() && !target.isOnline()) {
            MessageUtil.send(sender, plugin.getConfig(), "messages.command.player-never-joined",
                "<red>That player has never joined before.</red>",
                Map.of("player", input));
            return null;
        }
        return target;
    }

    private long parseDuration(CommandSender sender, String input) {
        long parsed = TimeUtil.parseDurationToSeconds(input);
        if (parsed < 0L) {
            MessageUtil.send(sender, plugin.getConfig(), "messages.command.invalid-duration",
                "<red>Invalid duration. Use values like 90, 30s, 15m, 2h, 1d, or 1h30m.</red>",
                Map.of("input", input));
        }
        return parsed;
    }

    private boolean hasPermission(CommandSender sender, boolean admin) {
        String node = admin
            ? plugin.getConfig().getString("commands.permissions.admin", "newbieprot.admin")
            : plugin.getConfig().getString("commands.permissions.self", "newbieprot.use");
        return node == null || node.isBlank() || sender.hasPermission(node);
    }

    private String resolveSubcommand(String input) {
        String normalized = input.toLowerCase(Locale.ROOT);
        for (String canonical : CANONICAL_SUBCOMMANDS) {
            List<String> aliases = new ArrayList<>();
            aliases.add(canonical);
            aliases.addAll(plugin.getConfig().getStringList("commands.aliases." + canonical));
            if (aliases.stream().anyMatch(alias -> alias.equalsIgnoreCase(normalized))) {
                return canonical;
            }
        }
        return null;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 1) {
            return CANONICAL_SUBCOMMANDS.stream()
                .filter(sub -> !isAdminOnly(sub) || hasPermission(sender, true))
                .filter(sub -> sub.startsWith(args[0].toLowerCase(Locale.ROOT)))
                .collect(Collectors.toList());
        }

        String subcommand = resolveSubcommand(args[0]);
        if (subcommand == null) {
            return List.of();
        }

        if (args.length == 2 && Arrays.asList("grant", "remove", "check", "set", "addtime", "removetime").contains(subcommand)) {
            List<String> names = new ArrayList<>();
            for (Player player : Bukkit.getOnlinePlayers()) {
                names.add(player.getName());
            }
            return names.stream().filter(name -> name.toLowerCase(Locale.ROOT).startsWith(args[1].toLowerCase(Locale.ROOT))).collect(Collectors.toList());
        }

        if (args.length == 3 && Arrays.asList("grant", "set", "addtime", "removetime").contains(subcommand)) {
            return List.of("30s", "5m", "15m", "30m", "1h", "2h").stream()
                .filter(s -> s.startsWith(args[2].toLowerCase(Locale.ROOT)))
                .collect(Collectors.toList());
        }

        if (args.length == 2 && subcommand.equals("list")) {
            return List.of("1", "2", "3");
        }

        return List.of();
    }

    private boolean isAdminOnly(String subcommand) {
        return !subcommand.equals("help") && !subcommand.equals("status");
    }
}
