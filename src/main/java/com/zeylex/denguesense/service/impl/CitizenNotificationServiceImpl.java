package com.zeylex.denguesense.service.impl;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MessagingErrorCode;
import com.zeylex.denguesense.config.FirebaseMessagingHolder;
import com.zeylex.denguesense.model.Notification;
import com.zeylex.denguesense.model.Report;
import com.zeylex.denguesense.model.Resolution;
import com.zeylex.denguesense.model.enums.Channel;
import com.zeylex.denguesense.model.enums.DeliveryStatus;
import com.zeylex.denguesense.model.enums.ReferenceType;
import com.zeylex.denguesense.repo.ReportRepo;
import com.zeylex.denguesense.service.CitizenNotificationService;
import com.zeylex.denguesense.service.NotificationRecordService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class CitizenNotificationServiceImpl implements CitizenNotificationService {

    private static final Logger log = LoggerFactory.getLogger(CitizenNotificationServiceImpl.class);

    private final NotificationRecordService notificationRecordService;
    private final ReportRepo reportRepo;
    private final FirebaseMessaging firebaseMessaging;

    public CitizenNotificationServiceImpl(NotificationRecordService notificationRecordService,
                                          ReportRepo reportRepo,
                                          FirebaseMessagingHolder firebaseMessagingHolder) {
        this.notificationRecordService = notificationRecordService;
        this.reportRepo = reportRepo;
        this.firebaseMessaging = firebaseMessagingHolder.get().orElse(null);
    }

    @Override
    public void notifyReportResolved(Report report, Resolution resolution) {
        if (report == null || resolution == null || resolution.getId() == null) {
            log.warn("Skipping citizen resolution notification: report or resolution id is null");
            return;
        }

        String messageBody = buildMessageBody(report, resolution);
        String token = report.getFcmDeviceToken();

        if (token == null || token.isBlank()) {
            Notification fallback = baseNotification(resolution.getId(), messageBody);
            fallback.setRecipient("device:" + report.getDeviceUUID() + " (no fcm token)");
            fallback.setStatus(DeliveryStatus.FALLBACK_NO_TOKEN);
            fallback.setFailureReason(
                    "No FCM token on file; citizen relies on public status endpoint "
                            + "GET /api/v1/reports/my/" + report.getId()
                            + " with X-Device-UUID tracking code");
            notificationRecordService.saveInNewTransaction(fallback);
            log.info("FALLBACK_NO_TOKEN citizen notification for report id={} resolution id={} deviceUUID={}",
                    report.getId(), resolution.getId(), report.getDeviceUUID());
            return;
        }

        Notification pending = baseNotification(resolution.getId(), messageBody);
        pending.setRecipient(token);
        pending.setStatus(DeliveryStatus.PENDING);
        Notification saved = notificationRecordService.saveInNewTransaction(pending);

        try {
            sendPush(token, report, resolution);
            notificationRecordService.markSent(saved.getId());
            log.info("SENT FCM resolution notification id={} report id={} resolution id={}",
                    saved.getId(), report.getId(), resolution.getId());
        } catch (Exception ex) {
            notificationRecordService.markFailed(saved.getId(), ex.getMessage());
            log.warn("FAILED FCM resolution notification id={} report id={}: {}",
                    saved.getId(), report.getId(), ex.getMessage());
            if (isUnregisteredToken(ex)) {
                report.setFcmDeviceToken(null);
                reportRepo.save(report);
                log.info("Cleared unregistered FCM token on report id={}", report.getId());
            }
        }
    }

    private void sendPush(String token, Report report, Resolution resolution) throws FirebaseMessagingException {
        if (firebaseMessaging == null) {
            throw new IllegalStateException("Firebase Admin SDK is not configured (firebase.credentials-file)");
        }

        LocalDateTime resolvedAt = resolution.getResolvedAt();
        Message message = Message.builder()
                .setToken(token)
                .setNotification(com.google.firebase.messaging.Notification.builder()
                        .setTitle("Report Resolved")
                        .setBody(buildMessageBody(report, resolution))
                        .build())
                .putData("reportId", String.valueOf(report.getId()))
                .putData("trackingCode", report.getDeviceUUID())
                .putData("resolvedAt", resolvedAt == null ? "" : resolvedAt.toString())
                .build();
        firebaseMessaging.send(message);
    }

    private static Notification baseNotification(Long resolutionId, String messageBody) {
        Notification notification = new Notification();
        notification.setChannel(Channel.FCM);
        notification.setReferenceType(ReferenceType.RESOLUTION);
        notification.setReferenceId(resolutionId);
        notification.setMessageBody(messageBody);
        return notification;
    }

    private static String buildMessageBody(Report report, Resolution resolution) {
        LocalDateTime resolvedAt = resolution.getResolvedAt();
        return "Your dengue breeding-site report (tracking code " + report.getDeviceUUID()
                + ") was resolved"
                + (resolvedAt == null ? "." : " on " + resolvedAt + ".")
                + " Check status at GET /api/v1/reports/my/" + report.getId();
    }

    private static boolean isUnregisteredToken(Exception ex) {
        if (!(ex instanceof FirebaseMessagingException fcmEx)) {
            return false;
        }
        MessagingErrorCode code = fcmEx.getMessagingErrorCode();
        if (code == MessagingErrorCode.UNREGISTERED) {
            return true;
        }
        String message = fcmEx.getMessage();
        return code == MessagingErrorCode.INVALID_ARGUMENT
                && message != null
                && message.toLowerCase().contains("registration token");
    }
}
