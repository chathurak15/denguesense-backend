package com.zeylex.denguesense.service.impl;

import com.zeylex.denguesense.dto.responseDTO.ClusterResponseDTO;
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
import com.zeylex.denguesense.service.ClusterQueryService;
import com.zeylex.denguesense.service.NotificationRecordService;
import com.zeylex.denguesense.service.TelegramAlertService;
import com.zeylex.denguesense.service.TelegramClient;
import com.zeylex.denguesense.util.TelegramAlertMessages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class TelegramAlertServiceImpl implements TelegramAlertService {

    private static final Logger log = LoggerFactory.getLogger(TelegramAlertServiceImpl.class);

    private final UserRepo userRepo;
    private final TelegramRegistrationRepo telegramRegistrationRepo;
    private final NotificationRecordService notificationRecordService;
    private final ReportClusterRepo reportClusterRepo;
    private final ClusterQueryService clusterQueryService;
    private final TelegramClient telegramClient;
    private final String frontendBaseUrl;

    public TelegramAlertServiceImpl(UserRepo userRepo,
                                    TelegramRegistrationRepo telegramRegistrationRepo,
                                    NotificationRecordService notificationRecordService,
                                    ReportClusterRepo reportClusterRepo,
                                    ClusterQueryService clusterQueryService,
                                    TelegramClient telegramClient,
                                    @Value("${denguesense.frontend.base.url:http://localhost:3000}") String frontendBaseUrl) {
        this.userRepo = userRepo;
        this.telegramRegistrationRepo = telegramRegistrationRepo;
        this.notificationRecordService = notificationRecordService;
        this.reportClusterRepo = reportClusterRepo;
        this.clusterQueryService = clusterQueryService;
        this.telegramClient = telegramClient;
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

        ClusterResponseDTO summary = loadSummary(cluster, officers.get(0));
        String messageBody = TelegramAlertMessages.clusterAlert(cluster, summary, frontendBaseUrl);
        Map<String, Object> keyboard = TelegramAlertMessages.clusterAlertKeyboard(
                cluster.getId(), summary, frontendBaseUrl);
        log.info("Sending Telegram cluster alert clusterId={} districtId={} phiCount={}",
                cluster.getId(), cluster.getDistrictId(), officers.size());

        for (User officer : officers) {
            try {
                deliverToOfficer(cluster, officer, messageBody, keyboard);
            } catch (Exception ex) {
                log.error("Unexpected error alerting PHI userId={} for cluster id={}: {}",
                        officer.getId(), cluster.getId(), ex.getMessage(), ex);
            }
        }
    }

    private ClusterResponseDTO loadSummary(ReportCluster cluster, User officer) {
        if (officer == null || officer.getEmail() == null) {
            return null;
        }
        try {
            return clusterQueryService.getById(officer.getEmail(), cluster.getId());
        } catch (Exception ex) {
            log.warn("Could not enrich Telegram cluster alert for clusterId={}: {}",
                    cluster.getId(), ex.getMessage());
            return null;
        }
    }

    private void deliverToOfficer(ReportCluster cluster,
                                  User officer,
                                  String messageBody,
                                  Map<String, Object> keyboard) {
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
            telegramClient.sendHtml(telegram.getChatId(), messageBody, keyboard);
        } catch (Exception ex) {
            notificationRecordService.markFailed(saved.getId(), ex.getMessage());
            log.warn("FAILED Telegram alert notificationId={} PHI userId={} clusterId={}: {}",
                    saved.getId(), officer.getId(), cluster.getId(), ex.getMessage());
            return;
        }

        notificationRecordService.markSent(saved.getId());
        try {
            markClusterAlerted(cluster.getId());
        } catch (Exception ex) {
            log.error("Telegram already sent for notificationId={} clusterId={} but ALERTED transition failed: {}",
                    saved.getId(), cluster.getId(), ex.getMessage(), ex);
        }
        log.info("SENT Telegram alert notificationId={} PHI userId={} clusterId={}",
                saved.getId(), officer.getId(), cluster.getId());
    }

    private Notification newNotification(ReportCluster cluster, String messageBody) {
        Notification notification = new Notification();
        notification.setChannel(Channel.TELEGRAM);
        notification.setReferenceType(ReferenceType.CLUSTER);
        notification.setReferenceId(cluster.getId());
        notification.setMessageBody(messageBody);
        return notification;
    }

    static String severityFor(int reportCount) {
        return TelegramAlertMessages.severityFor(reportCount);
    }

    private void markClusterAlerted(Long clusterId) {
        int updated = reportClusterRepo.updateStatusIfCurrent(
                clusterId, ClusterStatus.ACTIVE, ClusterStatus.ALERTED);
        if (updated > 0) {
            log.info("Cluster id={} transitioned ACTIVE → ALERTED after a successful Telegram send", clusterId);
        }
    }
}
