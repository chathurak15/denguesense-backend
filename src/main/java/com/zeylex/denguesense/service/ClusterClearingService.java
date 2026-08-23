package com.zeylex.denguesense.service;

import com.zeylex.denguesense.model.Report;

public interface ClusterClearingService {

    void checkAndClearAffectedClusters(Report resolvedReport);
}
