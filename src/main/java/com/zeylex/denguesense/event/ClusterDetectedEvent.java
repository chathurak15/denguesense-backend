package com.zeylex.denguesense.event;

import com.zeylex.denguesense.model.ReportCluster;
import org.springframework.context.ApplicationEvent;

public class ClusterDetectedEvent extends ApplicationEvent {

    private final ReportCluster cluster;
    private final boolean isNewCluster;

    public ClusterDetectedEvent(Object source, ReportCluster cluster, boolean isNewCluster) {
        super(source);
        this.cluster = cluster;
        this.isNewCluster = isNewCluster;
    }

    public ReportCluster getCluster() {
        return cluster;
    }

    public boolean isNewCluster() {
        return isNewCluster;
    }
}
