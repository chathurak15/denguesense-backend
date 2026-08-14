package com.zeylex.denguesense.service.impl;

import com.zeylex.denguesense.model.Notification;
import com.zeylex.denguesense.model.ReportCluster;
import com.zeylex.denguesense.model.TelegramRegistration;
import com.zeylex.denguesense.model.User;
import com.zeylex.denguesense.model.enums.Channel;
import com.zeylex.denguesense.model.enums.ClusterStatus;
import com.zeylex.denguesense.model.enums.DeliveryStatus;
import com.zeylex.denguesense.model.enums.ReferenceType;
import com.zeylex.denguesense.model.enums.RoleType;
import com.zeylex.denguesense.repo.ReportClusterRepo;
import com.zeylex.denguesense.repo.TelegramRegistrationRepo;
import com.zeylex.denguesense.repo.UserRepo;
import com.zeylex.denguesense.service.NotificationRecordService;
import com.zeylex.denguesense.service.TelegramAlertService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class TelegramAlertServiceImpl implements TelegramAlertService {

    private static final Logger log = LoggerFactory.getLogger(TelegramAlertServiceImpl.class);

    private final UserRepo userRepo;
    private final TelegramRegistrationRepo telegramRegistrationRepo;
    private final NotificationRecordService notificationRecordService;
    private final ReportClusterRepo reportClusterRepo;
    private final WebClient telegramWebClient;
    private final String botToken;
    private final String frontendBaseUrl;

    public TelegramAlertServiceImpl(UserRepo userRepo,
                                    TelegramRegistrationRepo telegramRegistrationRepo,
                                    NotificationRecordService notificationRecordService,
                                    ReportClusterRepo reportClusterRepo,
                                    @Qualifier("telegramWebClient") WebClient telegramWebClient,
                                    @Value("${telegram.bot.token:}") String botToken,
                                    @Value("${denguesense.frontend.base.url:http://localhost:3000}") String frontendBaseUrl) {
        this.userRepo = userRepo;
        this.telegramRegistrationRepo = telegramRegistrationRepo;
        this.notificationRecordService = notificationRecordService;
        this.reportClusterRepo = reportClusterRepo;
        this.telegramWebClient = telegramWebClient;
        this.botToken = botToken;
        this.frontendBaseUrl = frontendBaseUrl;
    }

    @Override
    public void sendClusterAlert(ReportCluster cluster) {
        if (cluster == null || cluster.getId() == null || cluster.getDistrictId() == null) {
            log.warn("Skipping Telegram cluster alert: cluster or districtId is null");
            return;
        }

        List<User> officers = userRepo.findByRoleAndDistrict_Id(RoleType.PHI, cluster.getDistrictId());
        if (officers == null || officers.isEmpty()) {
            log.warn("No PHI officers assigned to districtId={} for cluster id={}; no notification rows written",
                    cluster.getDistrictId(), cluster.getId());
            return;
        }

        String messageBody = buildAlertMessage(cluster);
        log.info("Sending Telegram cluster alert clusterId={} districtId={} phiCount={}",
                cluster.getId(), cluster.getDistrictId(), officers.size());

        for (User officer : officers) {
            try {
                deliverToOfficer(cluster, officer, messageBody);
            } catch (Exception ex) {
                log.error("Unexpected error alerting PHI userId={} for cluster id={}: {}",
                        officer.getId(), cluster.getId(), ex.getMessage(), ex);
            }
        }
    }

    private void deliverToOfficer(ReportCluster cluster, User officer, String messageBody) {
        var registration = telegramRegistrationRepo.findByUser_Id(officer.getId());
        if (registration.isEmpty()) {
            Notification skipped = newNotification(cluster, messageBody);
            skipped.setRecipient("user:" + officer.getId() + " (no chat_id)");
            skipped.setStatus(DeliveryStatus.SKIPPED_NOT_REGISTERED);
            skipped.setFailureReason(
                    "PHI user id=" + officer.getId()
                            + " has not connected Telegram. No chat_id is registered.");
            notificationRecordService.saveInNewTransaction(skipped);
            log.info("SKIPPED_NOT_REGISTERED Telegram alert for PHI userId={} clusterId={}",
                    officer.getId(), cluster.getId());
            return;
        }

        TelegramRegistration telegram = registration.get();
        Notification pending = newNotification(cluster, messageBody);
        pending.setRecipient(telegram.getChatId());
        pending.setStatus(DeliveryStatus.PENDING);
        Notification saved = notificationRecordService.saveInNewTransaction(pending);

        try {
            sendTelegramMessage(telegram.getChatId(), messageBody);
            notificationRecordService.markSent(saved.getId());
            markClusterAlerted(cluster.getId());
            log.info("SENT Telegram alert notificationId={} PHI userId={} clusterId={}",
                    saved.getId(), officer.getId(), cluster.getId());
        } catch (Exception ex) {
            notificationRecordService.markFailed(saved.getId(), ex.getMessage());
            log.warn("FAILED Telegram alert notificationId={} PHI userId={} clusterId={}: {}",
                    saved.getId(), officer.getId(), cluster.getId(), ex.getMessage());
        }
    }

    private Notification newNotification(ReportCluster cluster, String messageBody) {
        Notification notification = new Notification();
        notification.setChannel(Channel.TELEGRAM);
        notification.setReferenceType(ReferenceType.CLUSTER);
        notification.setReferenceId(cluster.getId());
        notification.setMessageBody(messageBody);
        return notification;
    }

    private String buildAlertMessage(ReportCluster cluster) {
        int count = cluster.getReportCount() == null ? 0 : cluster.getReportCount();
        String severity = severityFor(count);
        String dashboardUrl = trimSlash(frontendBaseUrl) + "/phi/clusters/" + cluster.getId();
        return """
                DengueSense LK cluster alert
                Severity: %s
                District ID: %s
                Reports in cluster: %s
                Detected at: %s
                Dashboard: %s
                """.formatted(severity, cluster.getDistrictId(), count, cluster.getDetectedAt(), dashboardUrl);
    }

    static String severityFor(int reportCount) {
        if (reportCount >= 10) {
            return "CRITICAL";
        }
        if (reportCount >= 5) {
            return "HIGH";
        }
        return "MODERATE";
    }

    private void sendTelegramMessage(String chatId, String text) {
        if (botToken == null || botToken.isBlank()) {
            throw new IllegalStateException("telegram.bot.token is not configured");
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("chat_id", chatId);
        payload.put("text", text);

        @SuppressWarnings("unchecked")
        Map<String, Object> response = telegramWebClient.post()
                .uri("/bot{token}/sendMessage", botToken)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(payload)
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        if (response == null || !Boolean.TRUE.equals(response.get("ok"))) {
            String description = response == null ? "empty Telegram response" : String.valueOf(response.get("description"));
            throw new IllegalStateException("Telegram sendMessage failed: " + description);
        }
    }

    private void markClusterAlerted(Long clusterId) {
        reportClusterRepo.findById(clusterId).ifPresent(cluster -> {
            if (cluster.getStatus() == ClusterStatus.ACTIVE) {
                cluster.setStatus(ClusterStatus.ALERTED);
                reportClusterRepo.save(cluster);
                log.info("Cluster id={} transitioned ACTIVE → ALERTED after a successful Telegram send", clusterId);
            }
        });
    }

    private static String trimSlash(String url) {
        if (url == null || url.isBlank()) {
            return "http://localhost:3000";
        }
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}
