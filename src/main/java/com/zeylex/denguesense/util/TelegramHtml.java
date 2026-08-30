package com.zeylex.denguesense.util;

public final class TelegramHtml {

    private TelegramHtml() {
    }

    public static String escape(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }

    public static String stripTags(String html) {
        if (html == null || html.isBlank()) {
            return "";
        }
        String withLinks = html.replaceAll("(?is)<a\\s+href=\"([^\"]+)\"[^>]*>(.*?)</a>", "$2 ($1)");
        return withLinks.replaceAll("<[^>]+>", "")
                .replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">");
    }
}
