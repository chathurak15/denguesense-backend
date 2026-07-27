package com.zeylex.denguesense.service.impl;

import com.zeylex.denguesense.model.Report;
import com.zeylex.denguesense.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class NotificationServiceImpl implements NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationServiceImpl.class);

    @Override
    public void notifyResolved(Report report) {
        log.info("[NOTIFICATION-STUB] Report id={} has been RESOLVED. " +
                 "Device UUID={} should receive a push notification. " +
                 "FCM integration pending.",
                report.getId(), report.getDeviceUUID());
    }
}
