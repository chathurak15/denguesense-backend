package com.zeylex.denguesense.util;

import com.zeylex.denguesense.dto.responseDTO.ClusterResponseDTO;
import com.zeylex.denguesense.model.ReportCluster;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("TelegramAlertMessages")
class TelegramAlertMessagesTest {

    @Test
    void clusterAlert_namesTheHotspotAndLinksTheView() {
        ReportCluster cluster = new ReportCluster();
        cluster.setId(12L);
        cluster.setDistrictId(1L);
        cluster.setReportCount(8);
        cluster.setDetectedAt(LocalDateTime.of(2026, 8, 29, 18, 42));

        ClusterResponseDTO summary = new ClusterResponseDTO();
        summary.setId(12L);
        summary.setDistrictName("Colombo");
        summary.setReportCount(8);
        summary.setRisk("High");
        summary.setInsight("Prioritise PHI dispatch after recent rainfall.");
        summary.setLatitude(6.9271);
        summary.setLongitude(79.8612);
        summary.setDetectedAt(cluster.getDetectedAt());

        String html = TelegramAlertMessages.clusterAlert(cluster, summary, "https://app.denguesense.lk");

        assertThat(html).contains("DENGUE HOTSPOT ALERT");
        assertThat(html).contains("HIGH");
        assertThat(html).contains("Colombo");
        assertThat(html).contains("Cluster #12");
        assertThat(html).contains("Open cluster in DengueSense");
        assertThat(html).contains("https://app.denguesense.lk/phi/clusters/12");
        assertThat(html).contains("google.com/maps");
        assertThat(html).contains("&amp;query=");
        assertThat(html).contains("ඩෙංගු හොට්ස්පොට්");
    }

    @Test
    void clusterAlertKeyboard_hasViewMapAndQuickActions() {
        ClusterResponseDTO summary = new ClusterResponseDTO();
        summary.setId(12L);
        summary.setLatitude(6.9);
        summary.setLongitude(79.8);

        Map<String, Object> markup = TelegramAlertMessages.clusterAlertKeyboard(
                12L, summary, "https://app.denguesense.lk");

        String serialized = String.valueOf(markup);
        assertThat(serialized).contains("View cluster");
        assertThat(serialized).contains("Open map");
        assertThat(serialized).contains("Quick view");
        assertThat(serialized).contains("Live clusters");
        assertThat(serialized).contains("qv:12");
        assertThat(serialized).contains("/phi/clusters/12");
        assertThat(serialized).contains("google.com/maps");
    }

    @Test
    void liveClusters_emptyState_isClear() {
        String html = TelegramAlertMessages.liveClusters("Gampaha", List.of(), "https://app.denguesense.lk");
        assertThat(html).contains("No live dengue clusters");
        assertThat(html).contains("Gampaha");
    }

    @Test
    void htmlEscape_protectsDistrictName() {
        ReportCluster cluster = new ReportCluster();
        cluster.setId(1L);
        cluster.setReportCount(5);
        ClusterResponseDTO summary = new ClusterResponseDTO();
        summary.setDistrictName("Colombo <script>");
        summary.setReportCount(5);

        String html = TelegramAlertMessages.clusterAlert(cluster, summary, "https://app.denguesense.lk");
        assertThat(html).contains("Colombo &lt;script&gt;");
        assertThat(html).doesNotContain("Colombo <script>");
    }

    @Test
    void stripTags_keepsClusterViewUrlForPlainFallback() {
        String html = "<b>Alert</b> <a href=\"https://app.denguesense.lk/phi/clusters/12\">Open cluster in DengueSense</a>";
        assertThat(TelegramHtml.stripTags(html))
                .contains("Alert")
                .contains("Open cluster in DengueSense (https://app.denguesense.lk/phi/clusters/12)");
    }
}
