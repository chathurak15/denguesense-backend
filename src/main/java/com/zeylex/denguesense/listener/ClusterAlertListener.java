package com.zeylex.denguesense.listener;

import com.zeylex.denguesense.event.ClusterDetectedEvent;
import com.zeylex.denguesense.model.ReportCluster;
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

    // TODO: private final TelegramBotService telegramBotService;
    // TODO: private final FirebaseFcmService firebaseFcmService;

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

        // TODO: telegramBotService.sendClusterAlert(cluster);
        // TODO: firebaseFcmService.pushClusterAlert(cluster);
    }
}
