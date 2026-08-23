package com.zeylex.denguesense.listener;

import com.zeylex.denguesense.event.ClusterDetectedEvent;
import com.zeylex.denguesense.model.ReportCluster;
import com.zeylex.denguesense.service.TelegramAlertService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class ClusterAlertListener {

    private static final Logger log = LoggerFactory.getLogger(ClusterAlertListener.class);

    private final TelegramAlertService telegramAlertService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onClusterDetected(ClusterDetectedEvent event) {
        ReportCluster cluster = event.getCluster();
        log.info(
                "ClusterDetectedEvent committed: clusterId={}, districtId={}, reportCount={}, isNewCluster={}, alertedAt={}",
                cluster.getId(),
                cluster.getDistrictId(),
                cluster.getReportCount(),
                event.isNewCluster(),
                cluster.getAlertedAt());

        try {
            telegramAlertService.sendClusterAlert(cluster);
        } catch (Exception ex) {
            log.error("Telegram cluster alert failed for cluster id={}: {}", cluster.getId(), ex.getMessage(), ex);
        }
    }
}
