package com.zeylex.denguesense.util;


public final class TelegramCommandParser {

    public enum Command {
        CONNECT,
        CLUSTERS,
        HELP,
        UNKNOWN
    }

    private TelegramCommandParser() {
    }

    public static Command classify(String text) {
        if (isConnectCommand(text)) {
            return Command.CONNECT;
        }
        if (isClustersCommand(text)) {
            return Command.CLUSTERS;
        }
        if (isHelpCommand(text)) {
            return Command.HELP;
        }
        return Command.UNKNOWN;
    }

    public static boolean isConnectCommand(String text) {
        if (text == null) {
            return false;
        }
        String trimmed = text.trim();
        return startsWithCommand(trimmed, "/start") || startsWithCommand(trimmed, "/register");
    }

    public static boolean isClustersCommand(String text) {
        if (text == null) {
            return false;
        }
        String trimmed = text.trim();
        return startsWithCommand(trimmed, "/clusters")
                || startsWithCommand(trimmed, "/live")
                || equalsIgnoreCase(trimmed, TelegramAlertMessages.BTN_LIVE_CLUSTERS)
                || equalsIgnoreCase(trimmed, "Live clusters");
    }

    public static boolean isHelpCommand(String text) {
        if (text == null) {
            return false;
        }
        String trimmed = text.trim();
        return startsWithCommand(trimmed, "/help")
                || startsWithCommand(trimmed, "/menu")
                || equalsIgnoreCase(trimmed, TelegramAlertMessages.BTN_HELP)
                || equalsIgnoreCase(trimmed, "Help");
    }

    public static String extractCode(String text) {
        if (!isConnectCommand(text)) {
            return null;
        }
        String[] parts = text.trim().split("\\s+");
        if (parts.length < 2) {
            return null;
        }
        String code = parts[1].trim();
        return code.isBlank() ? null : code;
    }

    private static boolean equalsIgnoreCase(String text, String expected) {
        return expected != null && expected.equalsIgnoreCase(text);
    }

    private static boolean startsWithCommand(String text, String command) {
        return text.equals(command) || text.startsWith(command + " ") || text.startsWith(command + "@");
    }
}
