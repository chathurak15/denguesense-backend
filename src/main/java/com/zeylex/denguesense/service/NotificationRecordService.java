package com.zeylex.denguesense.service;

import com.zeylex.denguesense.model.Notification;

public interface NotificationRecordService {

    Notification saveInNewTransaction(Notification notification);

    void markSent(Long notificationId);

    void markFailed(Long notificationId, String failureReason);
}
