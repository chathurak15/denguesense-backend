package com.zeylex.denguesense.model;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "cluster_membership",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_cluster_report", columnNames = {"cluster_id", "report_id"})
        },
        indexes = {
                @Index(name = "idx_membership_cluster", columnList = "cluster_id"),
                @Index(name = "idx_membership_report", columnList = "report_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
public class ClusterMembership {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cluster_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private ReportCluster cluster;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "report_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Report report;

    @CreationTimestamp
    @Column(name = "added_at", updatable = false)
    private LocalDateTime addedAt;
}
