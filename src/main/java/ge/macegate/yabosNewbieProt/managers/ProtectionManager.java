package ge.macegate.yabosNewbieProt.managers;

import ge.macegate.yabosNewbieProt.YabosNewbieProt;
import ge.macegate.yabosNewbieProt.model.ProtectionEntry;
import ge.macegate.yabosNewbieProt.model.RemovalReason;
import ge.macegate.yabosNewbieProt.model.TrackedPosition;
import ge.macegate.yabosNewbieProt.util.MessageUtil;
import ge.macegate.yabosNewbieProt.util.TimeUtil;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public class ProtectionManager {

    private final YabosNewbieProt plugin;
    private final Map<UUID, ProtectionEntry> protections = new HashMap<>();
    private final Map<UUID, TrackedPosition> lastTrackedPositions = new HashMap<>();
    private final Map<UUID, Integer> stillSeconds = new HashMap<>();
    private final Map<UUID, Set<Long>> deliveredWarnings = new HashMap<>();
    private final Map<UUID, BossBar> bossBars = new HashMap<>();

    private File dataFile;
    private YamlConfiguration dataConfig;
    private BukkitTask countdownTask;
    private BukkitTask saveTask;
    private boolean dirty;

    public ProtectionManager(YabosNewbieProt plugin) {
        this.plugin = plugin;
    }

    public void loadData() {
        this.dataFile = resolveDataFile();
        ensureDataFileExists();
        this.dataConfig = YamlConfiguration.loadConfiguration(dataFile);

        protections.clear();
        ConfigurationSection section = dataConfig.getConfigurationSection("protections");
        if (section != null) {
            for (String key : section.getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(key);
                    String base = "protections." + key + ".";
                    long seconds = dataConfig.getLong(base + "remaining-seconds", 0L);
                    String name = dataConfig.getString(base + "name", key);
                    if (seconds > 0L) {
                        protections.put(uuid, new ProtectionEntry(name, seconds));
                    }
                } catch (IllegalArgumentException ex) {
                    plugin.getLogger().warning("Skipping invalid UUID in data file: " + key);
                }
            }
        }

        dirty = false;
        plugin.getLogger().info("Loaded " + protections.size() + " protection record(s).");
    }

    public void startTasks() {
        stopTasks();

        long intervalSeconds = Math.max(1L, plugin.getConfig().getLong("protection.countdown-interval-seconds", 1L));
        this.countdownTask = Bukkit.getScheduler().runTaskTimer(plugin, this::tickCountdowns, 20L, intervalSeconds * 20L);

        long autoSaveSeconds = Math.max(5L, plugin.getConfig().getLong("storage.auto-save-seconds", 180L));
        this.saveTask = Bukkit.getScheduler().runTaskTimer(plugin, this::saveDataIfNeeded, autoSaveSeconds * 20L, autoSaveSeconds * 20L);
    }

    public void stopTasks() {
        if (countdownTask != null) {
            countdownTask.cancel();
            countdownTask = null;
        }
        if (saveTask != null) {
            saveTask.cancel();
            saveTask = null;
        }
    }

    public void shutdown() {
        clearBossBars();
        stopTasks();
        if (plugin.getConfig().getBoolean("storage.save-on-disable", true)) {
            saveData();
        }
    }

    public void reloadRuntime() {
        stopTasks();
        clearBossBars();
        startTasks();
        refreshOnlineProtectedPlayers();
    }

    public void saveDataIfNeeded() {
        boolean saveOnlyWhenDirty = plugin.getConfig().getBoolean("storage.save-only-when-dirty", true);
        if (!saveOnlyWhenDirty || dirty) {
            saveData();
        }
    }

    public void saveData() {
        if (dataConfig == null || dataFile == null) {
            return;
        }

        dataConfig.set("protections", null);
        for (Map.Entry<UUID, ProtectionEntry> entry : protections.entrySet()) {
            String base = "protections." + entry.getKey() + ".";
            dataConfig.set(base + "name", entry.getValue().getLastKnownName());
            dataConfig.set(base + "remaining-seconds", entry.getValue().getRemainingSeconds());
        }

        try {
            dataConfig.save(dataFile);
            dirty = false;
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save protection data: " + e.getMessage());
        }
    }

    public boolean isProtected(UUID uuid) {
        ProtectionEntry entry = protections.get(uuid);
        return entry != null && entry.getRemainingSeconds() > 0L;
    }

    public boolean isProtectionActiveInWorld(World world) {
        String mode = plugin.getConfig().getString("protection.world-filter.mode", "disabled");
        List<String> worlds = plugin.getConfig().getStringList("protection.world-filter.worlds");
        if (mode == null || mode.equalsIgnoreCase("disabled")) {
            return true;
        }

        boolean contains = worlds.stream().anyMatch(name -> name.equalsIgnoreCase(world.getName()));
        return switch (mode.toLowerCase(Locale.ROOT)) {
            case "whitelist" -> contains;
            case "blacklist" -> !contains;
            default -> true;
        };
    }

    public long getRemainingSeconds(UUID uuid) {
        ProtectionEntry entry = protections.get(uuid);
        return entry == null ? 0L : entry.getRemainingSeconds();
    }

    public String getLastKnownName(UUID uuid) {
        ProtectionEntry entry = protections.get(uuid);
        return entry == null ? uuid.toString() : entry.getLastKnownName();
    }

    public Map<UUID, Long> getProtectedPlayersSnapshot() {
        Map<UUID, Long> snapshot = new HashMap<>();
        for (Map.Entry<UUID, ProtectionEntry> entry : protections.entrySet()) {
            snapshot.put(entry.getKey(), entry.getValue().getRemainingSeconds());
        }
        return Collections.unmodifiableMap(snapshot);
    }

    public void handleJoin(Player player) {
        updateLastKnownName(player.getUniqueId(), player.getName());
        stillSeconds.put(player.getUniqueId(), 0);
        lastTrackedPositions.remove(player.getUniqueId());

        if (plugin.getConfig().getBoolean("protection.auto-grant-on-first-join", true) && !player.hasPlayedBefore()) {
            grantProtection(player.getUniqueId(), player.getName(), getDefaultDurationSeconds(), true, true);
            return;
        }

        if (isProtected(player.getUniqueId())) {
            sendJoinStatus(player);
            refreshVisuals(player, getRemainingSeconds(player.getUniqueId()), false);
        }
    }

    public void handleQuit(Player player) {
        updateLastKnownName(player.getUniqueId(), player.getName());
        stillSeconds.remove(player.getUniqueId());
        lastTrackedPositions.remove(player.getUniqueId());
        hideBossBar(player.getUniqueId(), player);

        if (plugin.getConfig().getBoolean("storage.save-on-quit", false)) {
            saveDataIfNeeded();
        }
    }

    public void grantProtection(UUID uuid, String name, long seconds, boolean overwrite, boolean notifyPlayer) {
        long safeSeconds = Math.max(0L, seconds);
        if (safeSeconds <= 0L) {
            return;
        }

        ProtectionEntry entry = protections.get(uuid);
        if (entry == null) {
            protections.put(uuid, new ProtectionEntry(name, safeSeconds));
        } else {
            entry.setLastKnownName(name);
            if (overwrite) {
                entry.setRemainingSeconds(safeSeconds);
            } else {
                entry.addSeconds(safeSeconds);
            }
        }

        deliveredWarnings.remove(uuid);
        stillSeconds.put(uuid, 0);
        lastTrackedPositions.remove(uuid);
        dirty = true;

        Player player = Bukkit.getPlayer(uuid);
        if (notifyPlayer && player != null && player.isOnline()) {
            MessageUtil.send(player, plugin.getConfig(), "messages.player.granted",
                "<green>You received newbie protection for <white>{time}</white>.</green>",
                placeholders(player.getName(), safeSeconds, 1, 1));
        }

        if (player != null && player.isOnline()) {
            refreshVisuals(player, getRemainingSeconds(uuid), false);
        }
    }

    public boolean setProtectionSeconds(UUID uuid, String name, long seconds, boolean notifyPlayer) {
        if (seconds <= 0L) {
            removeProtection(uuid, RemovalReason.MANUAL, notifyPlayer);
            return true;
        }

        protections.put(uuid, new ProtectionEntry(name, seconds));
        deliveredWarnings.remove(uuid);
        stillSeconds.put(uuid, 0);
        lastTrackedPositions.remove(uuid);
        dirty = true;

        Player player = Bukkit.getPlayer(uuid);
        if (notifyPlayer && player != null && player.isOnline()) {
            MessageUtil.send(player, plugin.getConfig(), "messages.player.set",
                "<yellow>Your newbie protection was set to <white>{time}</white>.</yellow>",
                placeholders(player.getName(), seconds, 1, 1));
        }
        if (player != null && player.isOnline()) {
            refreshVisuals(player, seconds, false);
        }
        return true;
    }

    public boolean addProtectionSeconds(UUID uuid, String name, long amount, boolean notifyPlayer) {
        if (amount <= 0L) {
            return false;
        }

        ProtectionEntry entry = protections.get(uuid);
        if (entry == null) {
            protections.put(uuid, new ProtectionEntry(name, amount));
        } else {
            entry.setLastKnownName(name);
            entry.addSeconds(amount);
        }

        dirty = true;
        Player player = Bukkit.getPlayer(uuid);
        if (notifyPlayer && player != null && player.isOnline()) {
            MessageUtil.send(player, plugin.getConfig(), "messages.player.added-time",
                "<yellow>Your newbie protection increased by <white>{time}</white>.</yellow>",
                placeholders(player.getName(), amount, 1, 1));
        }
        if (player != null && player.isOnline()) {
            refreshVisuals(player, getRemainingSeconds(uuid), false);
        }
        return true;
    }

    public boolean removeProtectionSeconds(UUID uuid, long amount, boolean notifyPlayer) {
        if (amount <= 0L || !protections.containsKey(uuid)) {
            return false;
        }

        ProtectionEntry entry = protections.get(uuid);
        entry.subtractSeconds(amount);
        dirty = true;

        if (entry.getRemainingSeconds() <= 0L) {
            removeProtection(uuid, RemovalReason.MANUAL, notifyPlayer);
            return true;
        }

        Player player = Bukkit.getPlayer(uuid);
        if (notifyPlayer && player != null && player.isOnline()) {
            MessageUtil.send(player, plugin.getConfig(), "messages.player.removed-time",
                "<red><white>{time}</white> was removed from your newbie protection.</red>",
                placeholders(player.getName(), amount, 1, 1));
        }
        if (player != null && player.isOnline()) {
            refreshVisuals(player, entry.getRemainingSeconds(), false);
        }
        return true;
    }

    public boolean removeProtection(UUID uuid, RemovalReason reason, boolean notifyPlayer) {
        ProtectionEntry removed = protections.remove(uuid);
        if (removed == null) {
            return false;
        }

        stillSeconds.remove(uuid);
        lastTrackedPositions.remove(uuid);
        deliveredWarnings.remove(uuid);
        dirty = true;

        Player player = Bukkit.getPlayer(uuid);
        if (player != null && player.isOnline()) {
            hideBossBar(uuid, player);
            if (notifyPlayer) {
                sendRemovalFeedback(player, reason);
            }
        }
        return true;
    }

    public void updateLastKnownName(UUID uuid, String name) {
        if (name == null || name.isBlank()) {
            return;
        }

        ProtectionEntry entry = protections.get(uuid);
        if (entry != null) {
            entry.setLastKnownName(name);
            dirty = true;
        }
    }

    public String formatTime(long totalSeconds) {
        return TimeUtil.formatDuration(totalSeconds, plugin.getConfig());
    }

    public long getDefaultDurationSeconds() {
        return Math.max(1L, plugin.getConfig().getLong("protection.default-duration-seconds", 900L));
    }

    private void tickCountdowns() {
        if (protections.isEmpty()) {
            return;
        }

        long intervalSeconds = Math.max(1L, plugin.getConfig().getLong("protection.countdown-interval-seconds", 1L));
        boolean pauseWhileAfk = plugin.getConfig().getBoolean("protection.pause-countdown-while-afk", true);
        boolean pauseInDisabledWorlds = plugin.getConfig().getBoolean("protection.pause-countdown-in-disabled-worlds", false);

        List<UUID> expired = new ArrayList<>();

        for (Map.Entry<UUID, ProtectionEntry> mapEntry : protections.entrySet()) {
            UUID uuid = mapEntry.getKey();
            ProtectionEntry protectionEntry = mapEntry.getValue();
            Player player = Bukkit.getPlayer(uuid);

            if (player == null || !player.isOnline()) {
                continue;
            }

            long currentRemaining = protectionEntry.getRemainingSeconds();
            if (currentRemaining <= 0L) {
                expired.add(uuid);
                continue;
            }

            boolean activeWorld = isProtectionActiveInWorld(player.getWorld());
            boolean paused = false;
            if (!activeWorld && pauseInDisabledWorlds) {
                paused = true;
            }
            if (!paused && pauseWhileAfk) {
                paused = isAfk(player, intervalSeconds);
            }

            if (!paused) {
                long newRemaining = Math.max(0L, currentRemaining - intervalSeconds);
                protectionEntry.setRemainingSeconds(newRemaining);
                dirty = true;
                deliverWarnings(player, currentRemaining, newRemaining);

                if (newRemaining <= 0L) {
                    expired.add(uuid);
                    continue;
                }
            }

            refreshVisuals(player, protectionEntry.getRemainingSeconds(), paused);
        }

        for (UUID uuid : expired) {
            removeProtection(uuid, RemovalReason.EXPIRED, true);
        }
    }

    private boolean isAfk(Player player, long intervalSeconds) {
        boolean useBlockPosition = plugin.getConfig().getBoolean("protection.afk-detection.use-block-position", true);
        boolean useLookDirection = plugin.getConfig().getBoolean("protection.afk-detection.use-look-direction", false);
        int threshold = Math.max(0, plugin.getConfig().getInt("protection.afk-threshold-seconds", 10));
        if (threshold <= 0 || (!useBlockPosition && !useLookDirection)) {
            return false;
        }

        UUID uuid = player.getUniqueId();
        TrackedPosition currentPosition = TrackedPosition.from(player.getLocation(), useBlockPosition, useLookDirection);
        TrackedPosition previousPosition = lastTrackedPositions.put(uuid, currentPosition);

        int currentStillSeconds = stillSeconds.getOrDefault(uuid, 0);
        if (Objects.equals(previousPosition, currentPosition)) {
            currentStillSeconds += (int) intervalSeconds;
        } else {
            currentStillSeconds = 0;
        }
        stillSeconds.put(uuid, currentStillSeconds);
        return currentStillSeconds >= threshold;
    }

    private void deliverWarnings(Player player, long previousRemaining, long newRemaining) {
        if (!plugin.getConfig().getBoolean("notifications.warning.enabled", true)) {
            return;
        }

        List<Long> warningTimes = plugin.getConfig().getLongList("notifications.warning.times-seconds");
        if (warningTimes.isEmpty()) {
            return;
        }

        UUID uuid = player.getUniqueId();
        Set<Long> sent = deliveredWarnings.computeIfAbsent(uuid, ignored -> new HashSet<>());
        for (Long warnAt : warningTimes) {
            if (warnAt == null || warnAt <= 0L || sent.contains(warnAt)) {
                continue;
            }
            if (newRemaining <= warnAt && previousRemaining >= warnAt) {
                MessageUtil.send(player, plugin.getConfig(), "messages.player.warn",
                    "<yellow>Your newbie protection expires in <white>{time}</white>.</yellow>",
                    placeholders(player.getName(), newRemaining, 1, 1));
                sent.add(warnAt);
            }
        }
    }

    private void refreshVisuals(Player player, long remainingSeconds, boolean paused) {
        if (!player.isOnline()) {
            return;
        }

        if (!isProtectionActiveInWorld(player.getWorld()) && plugin.getConfig().getBoolean("protection.hide-visuals-in-disabled-worlds", true)) {
            hideBossBar(player.getUniqueId(), player);
            return;
        }

        Map<String, String> placeholders = placeholders(player.getName(), remainingSeconds, 1, 1);

        if (plugin.getConfig().getBoolean("notifications.action-bar.enabled", true)) {
            String path = paused ? "notifications.action-bar.paused-format" : "notifications.action-bar.format";
            String fallback = paused
                ? "<yellow>Protection paused: <white>{time}</white></yellow>"
                : "<green>Protection: <white>{time}</white></green>";
            player.sendActionBar(MessageUtil.fromConfig(plugin.getConfig(), path, fallback, placeholders));
        }

        if (plugin.getConfig().getBoolean("notifications.boss-bar.enabled", false)) {
            BossBar bossBar = bossBars.computeIfAbsent(player.getUniqueId(), ignored -> createBossBar());
            bossBar.name(MessageUtil.fromConfig(plugin.getConfig(), paused ? "notifications.boss-bar.paused-format" : "notifications.boss-bar.format",
                paused ? "<yellow>Protection paused: <white>{time}</white></yellow>" : "<green>Protection: <white>{time}</white></green>", placeholders));
            bossBar.progress(calculateBossBarProgress(remainingSeconds));
            bossBar.color(parseBossBarColor(plugin.getConfig().getString("notifications.boss-bar.color", "GREEN")));
            bossBar.overlay(parseBossBarOverlay(plugin.getConfig().getString("notifications.boss-bar.overlay", "PROGRESS")));
            player.showBossBar(bossBar);
        } else {
            hideBossBar(player.getUniqueId(), player);
        }
    }

    private float calculateBossBarProgress(long remainingSeconds) {
        long defaultDuration = Math.max(1L, getDefaultDurationSeconds());
        double ratio = Math.min(1.0D, Math.max(0.0D, (double) remainingSeconds / (double) defaultDuration));
        if (plugin.getConfig().getBoolean("notifications.boss-bar.reverse-progress", false)) {
            ratio = 1.0D - ratio;
        }
        return (float) ratio;
    }

    private BossBar createBossBar() {
        return BossBar.bossBar(
            MessageUtil.component(plugin.getConfig().getString("notifications.boss-bar.format", "<green>Protection: <white>{time}</white></green>")),
            1.0f,
            parseBossBarColor(plugin.getConfig().getString("notifications.boss-bar.color", "GREEN")),
            parseBossBarOverlay(plugin.getConfig().getString("notifications.boss-bar.overlay", "PROGRESS"))
        );
    }

    private void hideBossBar(UUID uuid, Player player) {
        BossBar bossBar = bossBars.remove(uuid);
        if (bossBar != null) {
            player.hideBossBar(bossBar);
        }
    }

    private void clearBossBars() {
        for (Map.Entry<UUID, BossBar> entry : bossBars.entrySet()) {
            Player player = Bukkit.getPlayer(entry.getKey());
            if (player != null) {
                player.hideBossBar(entry.getValue());
            }
        }
        bossBars.clear();
    }

    private void refreshOnlineProtectedPlayers() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (isProtected(player.getUniqueId())) {
                refreshVisuals(player, getRemainingSeconds(player.getUniqueId()), false);
            }
        }
    }

    private void sendJoinStatus(Player player) {
        if (!plugin.getConfig().getBoolean("notifications.join-status.enabled", true)) {
            return;
        }

        MessageUtil.send(player, plugin.getConfig(), "messages.player.join-status",
            "<gray>Your newbie protection is active for <white>{time}</white>.</gray>",
            placeholders(player.getName(), getRemainingSeconds(player.getUniqueId()), 1, 1));
    }

    private void sendRemovalFeedback(Player player, RemovalReason reason) {
        switch (reason) {
            case EXPIRED -> {
                MessageUtil.send(player, plugin.getConfig(), "messages.player.expired",
                    "<red>Your newbie protection expired.</red>");
                showConfiguredTitle(player, "notifications.title.expired", "<red><bold>Protection Ended</bold></red>", "<yellow>You are now vulnerable.</yellow>");
                playConfiguredSound(player, "notifications.sound.expired", "BLOCK_BEACON_DEACTIVATE", 1.0f, 1.0f);
            }
            case ADMIN, MANUAL -> {
                MessageUtil.send(player, plugin.getConfig(), "messages.player.removed",
                    "<red>Your newbie protection was removed.</red>");
                showConfiguredTitle(player, "notifications.title.removed", "<red><bold>Protection Removed</bold></red>", "<yellow>An admin removed your protection.</yellow>");
                playConfiguredSound(player, "notifications.sound.removed", "BLOCK_NOTE_BLOCK_BASS", 1.0f, 1.0f);
            }
            case ATTACK_ACTION -> {
                MessageUtil.send(player, plugin.getConfig(), "messages.player.removed-on-attack",
                    "<red>You attacked another player and lost your newbie protection.</red>");
                showConfiguredTitle(player, "notifications.title.removed-on-attack", "<red><bold>Protection Removed</bold></red>", "<yellow>You attacked while protected.</yellow>");
                playConfiguredSound(player, "notifications.sound.removed-on-attack", "ENTITY_PLAYER_ATTACK_STRONG", 1.0f, 1.0f);
            }
        }
    }

    private void showConfiguredTitle(Player player, String basePath, String defaultTitle, String defaultSubtitle) {
        if (!plugin.getConfig().getBoolean(basePath + ".enabled", plugin.getConfig().getBoolean("notifications.title.enabled", true))) {
            return;
        }

        int fadeIn = plugin.getConfig().getInt(basePath + ".fade-in-ticks", 10);
        int stay = plugin.getConfig().getInt(basePath + ".stay-ticks", 60);
        int fadeOut = plugin.getConfig().getInt(basePath + ".fade-out-ticks", 10);

        player.showTitle(Title.title(
            MessageUtil.fromConfig(plugin.getConfig(), basePath + ".title", defaultTitle, Collections.emptyMap()),
            MessageUtil.fromConfig(plugin.getConfig(), basePath + ".subtitle", defaultSubtitle, Collections.emptyMap()),
            Title.Times.times(Duration.ofMillis(fadeIn * 50L), Duration.ofMillis(stay * 50L), Duration.ofMillis(fadeOut * 50L))
        ));
    }

    private void playConfiguredSound(Player player, String basePath, String defaultSound, float defaultVolume, float defaultPitch) {
        if (!plugin.getConfig().getBoolean(basePath + ".enabled", plugin.getConfig().getBoolean("notifications.sound.enabled", true))) {
            return;
        }

        String soundName = plugin.getConfig().getString(basePath + ".name", defaultSound);
        float volume = (float) plugin.getConfig().getDouble(basePath + ".volume", defaultVolume);
        float pitch = (float) plugin.getConfig().getDouble(basePath + ".pitch", defaultPitch);

        try {
            Sound sound = Sound.valueOf(soundName == null ? defaultSound : soundName.toUpperCase(Locale.ROOT));
            player.playSound(player.getLocation(), sound, volume, pitch);
        } catch (IllegalArgumentException ex) {
            plugin.getLogger().warning("Invalid sound in config at " + basePath + ": " + soundName);
        }
    }

    private BossBar.Color parseBossBarColor(String input) {
        try {
            return BossBar.Color.valueOf(input == null ? "GREEN" : input.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return BossBar.Color.GREEN;
        }
    }

    private BossBar.Overlay parseBossBarOverlay(String input) {
        try {
            return BossBar.Overlay.valueOf(input == null ? "PROGRESS" : input.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return BossBar.Overlay.PROGRESS;
        }
    }

    private File resolveDataFile() {
        String fileName = plugin.getConfig().getString("storage.file", "data.yml");
        File file = new File(plugin.getDataFolder(), fileName == null ? "data.yml" : fileName);
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }
        return file;
    }

    private void ensureDataFileExists() {
        try {
            if (!plugin.getDataFolder().exists()) {
                plugin.getDataFolder().mkdirs();
            }
            if (!dataFile.exists()) {
                dataFile.createNewFile();
            }
        } catch (IOException e) {
            plugin.getLogger().severe("Could not create data file: " + e.getMessage());
        }
    }

    private Map<String, String> placeholders(String playerName, long seconds, int page, int pages) {
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("player", playerName == null ? "Unknown" : playerName);
        placeholders.put("time", formatTime(seconds));
        placeholders.put("seconds", String.valueOf(seconds));
        placeholders.put("page", String.valueOf(page));
        placeholders.put("pages", String.valueOf(pages));
        return placeholders;
    }

    public OfflinePlayer resolveOfflinePlayer(String input) {
        if (input == null || input.isBlank()) {
            return null;
        }

        Player online = Bukkit.getPlayerExact(input);
        if (online != null) {
            return online;
        }

        for (OfflinePlayer offline : Bukkit.getOfflinePlayers()) {
            if (offline.getName() != null && offline.getName().equalsIgnoreCase(input)) {
                return offline;
            }
        }

        return null;
    }
}
