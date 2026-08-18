package com.zeylex.denguesense.service;

import com.zeylex.denguesense.model.Report;
import com.zeylex.denguesense.model.Resolution;

public interface CitizenNotificationService {

    void notifyReportResolved(Report report, Resolution resolution);
}
