package com.zeylex.denguesense.service.impl;

import com.zeylex.denguesense.exception.NotFoundException;
import com.zeylex.denguesense.model.Notification;
import com.zeylex.denguesense.model.enums.DeliveryStatus;
import com.zeylex.denguesense.repo.NotificationRepo;
import com.zeylex.denguesense.service.NotificationRecordService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class NotificationRecordServiceImpl implements NotificationRecordService {

    private static final int FAILURE_REASON_MAX = 1000;

    private final NotificationRepo notificationRepo;

    public NotificationRecordServiceImpl(NotificationRepo notificationRepo) {
        this.notificationRepo = notificationRepo;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Notification saveInNewTransaction(Notification notification) {
        return notificationRepo.saveAndFlush(notification);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markSent(Long notificationId) {
        Notification notification = notificationRepo.findById(notificationId)
                .orElseThrow(() -> new NotFoundException("Notification not found with id: " + notificationId));
        notification.setStatus(DeliveryStatus.SENT);
        notification.setSentAt(LocalDateTime.now());
        notification.setFailureReason(null);
        notificationRepo.saveAndFlush(notification);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markFailed(Long notificationId, String failureReason) {
        Notification notification = notificationRepo.findById(notificationId)
                .orElseThrow(() -> new NotFoundException("Notification not found with id: " + notificationId));
        notification.setStatus(DeliveryStatus.FAILED);
        notification.setFailureReason(truncate(failureReason));
        notificationRepo.saveAndFlush(notification);
    }

    private static String truncate(String reason) {
        if (reason == null) {
            return "unknown failure";
        }
        return reason.length() <= FAILURE_REASON_MAX ? reason : reason.substring(0, FAILURE_REASON_MAX);
    }
}
