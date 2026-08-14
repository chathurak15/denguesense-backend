package com.zeylex.denguesense.service;

import com.zeylex.denguesense.model.ReportCluster;

public interface TelegramAlertService {

    void sendClusterAlert(ReportCluster cluster);
}
