package com.zeylex.denguesense.util;

import com.zeylex.denguesense.dto.responseDTO.ClusterResponseDTO;
import com.zeylex.denguesense.model.ReportCluster;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class TelegramAlertMessages {

    public static final String BTN_LIVE_CLUSTERS = "🔴 Live clusters";
    public static final String BTN_HELP = "ℹ️ Help";

    public static final String CALLBACK_LIVE = "live";
    public static final String CALLBACK_HELP = "help";
    public static final String CALLBACK_QUICK_VIEW_PREFIX = "qv:";

    static final DateTimeFormatter DETECTED_AT =
            DateTimeFormatter.ofPattern("d MMM yyyy, h:mm a", Locale.ENGLISH);

    private TelegramAlertMessages() {
    }

    public static String clusterAlert(ReportCluster cluster, ClusterResponseDTO summary, String frontendBaseUrl) {
        int count = reportCount(cluster, summary);
        String severity = severityFor(count);
        String district = districtName(cluster, summary);
        String detected = formatDetected(cluster, summary);
        String aiRisk = summary == null || blank(summary.getRisk()) ? "—" : summary.getRisk();
        String insight = summary == null ? null : summary.getInsight();
        String clusterUrl = clusterViewUrl(frontendBaseUrl, cluster.getId());
        String mapsUrl = mapsUrl(summary);

        StringBuilder body = new StringBuilder();
        body.append(severityBanner(severity)).append('\n');
        body.append("<b>DENGUE HOTSPOT ALERT</b>\n");
        body.append("<i>Live breeding-site cluster in your district — field action needed.</i>\n\n");
        body.append(severityEmoji(severity)).append(" <b>").append(TelegramHtml.escape(severity)).append("</b>");
        body.append("  ·  Cluster #").append(cluster.getId()).append('\n');
        body.append("━━━━━━━━━━━━━━━━\n");
        body.append("📍 <b>").append(TelegramHtml.escape(district)).append("</b>\n");
        body.append("🦟 <b>").append(count).append("</b> reports clustered nearby\n");
        body.append("🧠 AI risk: <b>").append(TelegramHtml.escape(aiRisk)).append("</b>\n");
        body.append("🕐 ").append(TelegramHtml.escape(detected)).append('\n');
        if (insight != null && !insight.isBlank()) {
            body.append("\n⚠️ ").append(TelegramHtml.escape(insight)).append('\n');
        }
        body.append('\n');
        body.append("Tap <b>View cluster</b> to open the map, photos, and dispatch tools.\n");
        if (clusterUrl != null) {
            body.append("🔗 <a href=\"").append(TelegramHtml.escape(clusterUrl)).append("\">Open cluster in DengueSense</a>\n");
        }
        if (mapsUrl != null) {
            body.append("🗺 <a href=\"").append(TelegramHtml.escape(mapsUrl)).append("\">Open location in Google Maps</a>\n");
        }
        body.append("\n────────\n");
        body.append("මෙය ඔබේ දිස්ත්‍රික්කයේ ඩෙංගු හොට්ස්පොට් අනතුරු ඇඟවීමකි. ");
        body.append("පහත බොත්තම් වලින් cluster එක බලන්න.");
        return body.toString();
    }

    public static String clusterQuickView(ClusterResponseDTO cluster, String frontendBaseUrl) {
        if (cluster == null || cluster.getId() == null) {
            return "That cluster is no longer available.";
        }
        int count = cluster.getReportCount() == null ? 0 : cluster.getReportCount();
        String severity = severityFor(count);
        String district = blank(cluster.getDistrictName()) ? "Unknown district" : cluster.getDistrictName();
        String aiRisk = blank(cluster.getRisk()) ? "—" : cluster.getRisk();
        String status = cluster.getStatus() == null ? "—" : cluster.getStatus().name();
        String clusterUrl = clusterViewUrl(frontendBaseUrl, cluster.getId());
        String mapsUrl = mapsUrl(cluster);

        StringBuilder body = new StringBuilder();
        body.append("🗺 <b>Cluster #").append(cluster.getId()).append(" — quick view</b>\n");
        body.append(severityEmoji(severity)).append(" ").append(TelegramHtml.escape(severity));
        body.append("  ·  AI risk <b>").append(TelegramHtml.escape(aiRisk)).append("</b>\n\n");
        body.append("📍 ").append(TelegramHtml.escape(district)).append('\n');
        body.append("🦟 ").append(count).append(" reports\n");
        body.append("📌 Status: ").append(TelegramHtml.escape(status)).append('\n');
        body.append("🕐 ").append(TelegramHtml.escape(formatTime(cluster.getDetectedAt()))).append('\n');
        if (cluster.getLatitude() != null && cluster.getLongitude() != null) {
            body.append(String.format(Locale.US, "🧭 %.5f, %.5f\n", cluster.getLatitude(), cluster.getLongitude()));
        }
        if (!blank(cluster.getInsight())) {
            body.append("\n⚠️ ").append(TelegramHtml.escape(cluster.getInsight())).append('\n');
        }
        body.append("\nNext step: open the cluster, inspect photos, and dispatch a visit.");
        if (clusterUrl != null) {
            body.append("\n🔗 <a href=\"").append(TelegramHtml.escape(clusterUrl)).append("\">Open cluster in DengueSense</a>");
        }
        if (mapsUrl != null) {
            body.append("\n🗺 <a href=\"").append(TelegramHtml.escape(mapsUrl)).append("\">Open in Google Maps</a>");
        }
        return body.toString();
    }

    public static String liveClusters(String districtName, List<ClusterResponseDTO> clusters, String frontendBaseUrl) {
        String district = blank(districtName) ? "your district" : districtName;
        if (clusters == null || clusters.isEmpty()) {
            return "🟢 <b>No live dengue clusters</b> in " + TelegramHtml.escape(district)
                    + " right now.\n\nYou will get an alert here when a hotspot is detected.";
        }
        String listUrl = clustersListUrl(frontendBaseUrl);
        StringBuilder body = new StringBuilder();
        body.append("🔴 <b>Live dengue clusters</b> — ").append(TelegramHtml.escape(district)).append('\n');
        body.append(clusters.size()).append(clusters.size() == 1 ? " hotspot needs attention.\n\n" : " hotspots need attention.\n\n");
        int shown = Math.min(clusters.size(), 8);
        for (int i = 0; i < shown; i++) {
            ClusterResponseDTO cluster = clusters.get(i);
            int count = cluster.getReportCount() == null ? 0 : cluster.getReportCount();
            String severity = severityFor(count);
            String risk = blank(cluster.getRisk()) ? severity : cluster.getRisk();
            body.append(severityEmoji(severity)).append(" <b>#").append(cluster.getId()).append("</b>");
            body.append("  ").append(TelegramHtml.escape(risk));
            body.append("  ·  ").append(count).append(" reports");
            body.append("  ·  ").append(TelegramHtml.escape(formatTime(cluster.getDetectedAt())));
            body.append('\n');
        }
        if (clusters.size() > shown) {
            body.append("\n…and ").append(clusters.size() - shown).append(" more in DengueSense.");
        }
        if (listUrl != null) {
            body.append("\n🔗 <a href=\"").append(TelegramHtml.escape(listUrl)).append("\">Open all clusters</a>");
        }
        body.append("\n\nTap a button below for a quick view, or open the cluster map.");
        return body.toString();
    }

    public static String help(boolean connected) {
        if (!connected) {
            return """
                    Welcome to <b>DengueSense LK</b>.

                    Go back to the DengueSense website and tap <b>Enable Telegram alerts</b>, then Start here.

                    DengueSense website එකට ගිහින් Enable Telegram alerts button එක tap කරන්න.""";
        }
        return """
                <b>DengueSense LK</b> — PHI hotspot assistant

                When a dengue breeding-site cluster is found in your district, you get an alert with:
                • <b>View cluster</b> — open the map, photos, and dispatch tools
                • <b>Open map</b> — Google Maps at the hotspot
                • <b>Quick view</b> — summary here in Telegram

                Use the buttons under the chat, or:
                /clusters — live hotspots in your district
                /help — this guide

                🔴 Live clusters වලින් දැන් තියෙන hotspot බලන්න.""";
    }

    public static String connectedConfirmation() {
        return """
                ✅ <b>DengueSense LK alerts are ON.</b>

                If a dengue hotspot is found in your district, you will get a message here with <b>View cluster</b> and map buttons.

                Use <b>Live clusters</b> anytime to check current hotspots.

                DengueSense LK alerts දැන් ON.
                ඔබේ district එකේ hotspot එකක් හමු වුණාම මෙතනට message එකක් එනවා.""";
    }

    public static String notConnected() {
        return "This chat is not linked to a PHI account yet.\n\n"
                + "Go back to the DengueSense website and tap <b>Enable Telegram alerts</b>.";
    }

    public static String clusterNotFound() {
        return "That cluster is not in your district, or it is no longer live.";
    }

    public static Map<String, Object> clusterAlertKeyboard(Long clusterId, ClusterResponseDTO summary, String frontendBaseUrl) {
        String clusterUrl = clusterId == null ? null : clusterViewUrl(frontendBaseUrl, clusterId);
        String mapsUrl = mapsUrl(summary);
        String listUrl = clustersListUrl(frontendBaseUrl);

        List<List<Map<String, Object>>> rows = new ArrayList<>();
        List<Map<String, Object>> viewRow = new ArrayList<>();
        urlButton(viewRow, "🗺 View cluster", clusterUrl);
        urlButton(viewRow, "📍 Open map", mapsUrl);
        addRow(rows, viewRow);

        List<Map<String, Object>> actionRow = new ArrayList<>();
        if (clusterId != null) {
            actionRow.add(callbackButton("⚡ Quick view", CALLBACK_QUICK_VIEW_PREFIX + clusterId));
        }
        actionRow.add(callbackButton("📋 Live clusters", CALLBACK_LIVE));
        addRow(rows, actionRow);

        List<Map<String, Object>> dashRow = new ArrayList<>();
        urlButton(dashRow, "📂 All clusters", listUrl);
        addRow(rows, dashRow);

        return inlineKeyboard(rows);
    }

    public static Map<String, Object> clusterViewKeyboard(ClusterResponseDTO cluster, String frontendBaseUrl) {
        if (cluster == null || cluster.getId() == null) {
            return clusterAlertKeyboard(null, null, frontendBaseUrl);
        }
        return clusterAlertKeyboard(cluster.getId(), cluster, frontendBaseUrl);
    }

    public static Map<String, Object> liveClustersKeyboard(List<ClusterResponseDTO> clusters, String frontendBaseUrl) {
        List<List<Map<String, Object>>> rows = new ArrayList<>();
        if (clusters != null) {
            int shown = Math.min(clusters.size(), 6);
            for (int i = 0; i < shown; i++) {
                ClusterResponseDTO cluster = clusters.get(i);
                if (cluster == null || cluster.getId() == null) {
                    continue;
                }
                String label = "⚡ #" + cluster.getId() + " quick view";
                List<Map<String, Object>> row = new ArrayList<>();
                row.add(callbackButton(label, CALLBACK_QUICK_VIEW_PREFIX + cluster.getId()));
                urlButton(row, "🗺 Open", clusterViewUrl(frontendBaseUrl, cluster.getId()));
                addRow(rows, row);
            }
        }
        List<Map<String, Object>> nav = new ArrayList<>();
        urlButton(nav, "📂 All clusters", clustersListUrl(frontendBaseUrl));
        nav.add(callbackButton("ℹ️ Help", CALLBACK_HELP));
        addRow(rows, nav);
        return inlineKeyboard(rows);
    }

    public static Map<String, Object> navReplyKeyboard() {
        Map<String, Object> live = new LinkedHashMap<>();
        live.put("text", BTN_LIVE_CLUSTERS);
        Map<String, Object> help = new LinkedHashMap<>();
        help.put("text", BTN_HELP);

        List<Map<String, Object>> row = new ArrayList<>();
        row.add(live);
        row.add(help);

        List<List<Map<String, Object>>> keyboard = new ArrayList<>();
        keyboard.add(row);

        Map<String, Object> markup = new LinkedHashMap<>();
        markup.put("keyboard", keyboard);
        markup.put("resize_keyboard", true);
        markup.put("is_persistent", true);
        return markup;
    }

    public static String clusterViewUrl(String frontendBaseUrl, Long clusterId) {
        if (clusterId == null) {
            return null;
        }
        return trimSlash(frontendBaseUrl) + "/phi/clusters/" + clusterId;
    }

    public static String clustersListUrl(String frontendBaseUrl) {
        return trimSlash(frontendBaseUrl) + "/phi/clusters";
    }

    public static String mapsUrl(ClusterResponseDTO summary) {
        if (summary == null || summary.getLatitude() == null || summary.getLongitude() == null) {
            return null;
        }
        return String.format(Locale.US,
                "https://www.google.com/maps/search/?api=1&query=%f,%f",
                summary.getLatitude(), summary.getLongitude());
    }

    public static String quickViewCallbackData(String data) {
        if (data == null || !data.startsWith(CALLBACK_QUICK_VIEW_PREFIX)) {
            return null;
        }
        String id = data.substring(CALLBACK_QUICK_VIEW_PREFIX.length()).trim();
        return id.isBlank() ? null : id;
    }

    public static String severityFor(int reportCount) {
        if (reportCount >= 10) {
            return "CRITICAL";
        }
        if (reportCount >= 5) {
            return "HIGH";
        }
        return "MODERATE";
    }

    static String severityBanner(String severity) {
        if ("CRITICAL".equals(severity)) {
            return "🚨🚨🚨";
        }
        if ("HIGH".equals(severity)) {
            return "🚨🚨";
        }
        return "⚠️";
    }

    static String severityEmoji(String severity) {
        if ("CRITICAL".equals(severity)) {
            return "🔴";
        }
        if ("HIGH".equals(severity)) {
            return "🟠";
        }
        return "🟡";
    }

    private static int reportCount(ReportCluster cluster, ClusterResponseDTO summary) {
        if (summary != null && summary.getReportCount() != null) {
            return summary.getReportCount();
        }
        if (cluster != null && cluster.getReportCount() != null) {
            return cluster.getReportCount();
        }
        return 0;
    }

    private static String districtName(ReportCluster cluster, ClusterResponseDTO summary) {
        if (summary != null && !blank(summary.getDistrictName())) {
            return summary.getDistrictName();
        }
        if (cluster != null && cluster.getDistrictId() != null) {
            return "District " + cluster.getDistrictId();
        }
        return "Unknown district";
    }

    private static String formatDetected(ReportCluster cluster, ClusterResponseDTO summary) {
        if (summary != null && summary.getDetectedAt() != null) {
            return formatTime(summary.getDetectedAt());
        }
        if (cluster != null) {
            return formatTime(cluster.getDetectedAt());
        }
        return "just now";
    }

    private static String formatTime(LocalDateTime time) {
        if (time == null) {
            return "just now";
        }
        return time.format(DETECTED_AT);
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private static String trimSlash(String url) {
        if (url == null || url.isBlank()) {
            return "http://localhost:3000";
        }
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    private static Map<String, Object> callbackButton(String text, String data) {
        Map<String, Object> button = new LinkedHashMap<>();
        button.put("text", text);
        button.put("callback_data", data);
        return button;
    }

    private static void urlButton(List<Map<String, Object>> row, String text, String url) {
        if (url == null || url.isBlank()) {
            return;
        }
        Map<String, Object> button = new LinkedHashMap<>();
        button.put("text", text);
        button.put("url", url);
        row.add(button);
    }

    private static void addRow(List<List<Map<String, Object>>> rows, List<Map<String, Object>> row) {
        if (row != null && !row.isEmpty()) {
            rows.add(row);
        }
    }

    private static Map<String, Object> inlineKeyboard(List<List<Map<String, Object>>> rows) {
        Map<String, Object> markup = new LinkedHashMap<>();
        markup.put("inline_keyboard", rows);
        return markup;
    }
}
