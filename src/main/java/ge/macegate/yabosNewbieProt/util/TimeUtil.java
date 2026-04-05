package ge.macegate.yabosNewbieProt.util;

import org.bukkit.configuration.file.FileConfiguration;

import java.util.Locale;

public final class TimeUtil {

    private TimeUtil() {
    }

    public static long parseDurationToSeconds(String input) {
        if (input == null || input.isBlank()) {
            return -1L;
        }

        String normalized = input.trim().toLowerCase(Locale.ROOT);
        if (normalized.matches("\\d+")) {
            return Long.parseLong(normalized);
        }

        long total = 0L;
        StringBuilder current = new StringBuilder();
        boolean matchedAny = false;

        for (int i = 0; i < normalized.length(); i++) {
            char c = normalized.charAt(i);
            if (Character.isDigit(c)) {
                current.append(c);
                continue;
            }

            if (Character.isWhitespace(c)) {
                continue;
            }

            if (current.isEmpty()) {
                return -1L;
            }

            long value = Long.parseLong(current.toString());
            current.setLength(0);
            matchedAny = true;

            switch (c) {
                case 's' -> total += value;
                case 'm' -> total += value * 60L;
                case 'h' -> total += value * 3600L;
                case 'd' -> total += value * 86400L;
                default -> {
                    return -1L;
                }
            }
        }

        if (!current.isEmpty()) {
            return -1L;
        }

        return matchedAny ? total : -1L;
    }

    public static String formatDuration(long totalSeconds, FileConfiguration config) {
        long seconds = Math.max(0L, totalSeconds);
        long days = seconds / 86400L;
        long hours = (seconds % 86400L) / 3600L;
        long minutes = (seconds % 3600L) / 60L;
        long remainingSeconds = seconds % 60L;

        String spacer = config.getString("time-format.spacer", " ");
        boolean showZeroUnits = config.getBoolean("time-format.show-zero-units", false);
        String zeroFormat = config.getString("time-format.zero-format", "0s");

        StringBuilder builder = new StringBuilder();
        appendUnit(builder, days, config.getString("time-format.days-suffix", "d"), showZeroUnits, spacer);
        appendUnit(builder, hours, config.getString("time-format.hours-suffix", "h"), showZeroUnits, spacer);
        appendUnit(builder, minutes, config.getString("time-format.minutes-suffix", "m"), showZeroUnits, spacer);
        appendUnit(builder, remainingSeconds, config.getString("time-format.seconds-suffix", "s"), true, spacer);

        String result = builder.toString().trim();
        return result.isEmpty() ? zeroFormat : result;
    }

    private static void appendUnit(StringBuilder builder, long value, String suffix, boolean include, String spacer) {
        if (!include && value <= 0L) {
            return;
        }
        if (builder.length() > 0) {
            builder.append(spacer);
        }
        builder.append(value).append(suffix);
    }
}
