package ge.macegate.yabosNewbieProt.util;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.Collections;
import java.util.Map;

public final class MessageUtil {

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    private MessageUtil() {
    }

    public static Component component(String input) {
        return MINI_MESSAGE.deserialize(input == null ? "" : input);
    }

    public static Component fromConfig(FileConfiguration config, String path, String fallback, Map<String, String> replacements) {
        String raw = config.getString(path, fallback);
        return component(applyPlaceholders(raw, replacements));
    }

    public static void send(Audience audience, FileConfiguration config, String path, String fallback) {
        send(audience, config, path, fallback, Collections.emptyMap());
    }

    public static void send(Audience audience, FileConfiguration config, String path, String fallback, Map<String, String> replacements) {
        audience.sendMessage(fromConfig(config, path, fallback, replacements));
    }

    public static String applyPlaceholders(String input, Map<String, String> replacements) {
        String result = input == null ? "" : input;
        if (replacements == null || replacements.isEmpty()) {
            return result;
        }

        for (Map.Entry<String, String> entry : replacements.entrySet()) {
            result = result.replace("{" + entry.getKey() + "}", entry.getValue() == null ? "" : entry.getValue());
        }
        return result;
    }
}
