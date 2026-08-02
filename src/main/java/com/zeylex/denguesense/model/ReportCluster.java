package com.zeylex.denguesense.model;

import com.zeylex.denguesense.model.enums.ClusterStatus;
import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
@Table(
        name = "report_cluster",
        indexes = {
                @Index(name = "idx_cluster_district_status", columnList = "district_id, status")
        }
)
@Getter
@Setter
@NoArgsConstructor
public class ReportCluster {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "district_id", nullable = false)
    private Long districtId;

    @Column(name = "report_count", nullable = false)
    private Integer reportCount = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ClusterStatus status;

    @Column(name = "detected_at", nullable = false)
    private LocalDateTime detectedAt;

    @Column(name = "alerted_at")
    private LocalDateTime alertedAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Version
    @Column(name = "version")
    private Long version;

    @OneToMany(mappedBy = "cluster", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<ClusterMembership> memberships = new ArrayList<>();

    public void addMembership(ClusterMembership membership) {
        Objects.requireNonNull(membership, "membership must not be null");
        if (this.memberships == null) {
            this.memberships = new ArrayList<>();
        }
        membership.setCluster(this);
        this.memberships.add(membership);
    }
}
