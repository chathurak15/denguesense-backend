package com.zeylex.denguesense.repo;

import com.zeylex.denguesense.model.Report;
import com.zeylex.denguesense.model.enums.ReportStatus;
import com.zeylex.denguesense.model.enums.RiskLabel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ReportRepo extends JpaRepository<Report, Long>, JpaSpecificationExecutor<Report> {
    Page<Report> findByDeviceUUID(String deviceUUID, Pageable pageable);
    Optional<Report> findByIdAndDeviceUUID(Long id, String deviceUUID);
    Page<Report> findByDistrict_Id(Long districtId, Pageable pageable);

    Page<Report> findByDistrict_IdAndReportStatus(Long districtId, ReportStatus status, Pageable pageable);

    Page<Report> findByResolvedBy_IdAndReportStatus(Long resolvedById, ReportStatus status, Pageable pageable);

    /**
     * Spatial neighbourhood used by inline cluster detection. Returns every open HIGH_RISK report
     * (including the reference report itself) that sits within {@code radiusMeters} of the reference
     * report, in the same district, submitted on/after {@code since}.
     *
     * <p>Uses PostGIS {@code ST_DWithin} on the {@code geography(Point,4326)} column, so the radius
     * is expressed directly in metres. Only CLASSIFIED/DISPATCHED reports count as "open" — resolved,
     * dismissed, rejected and still-pending reports are excluded so cleared hotspots do not resurrect.
     */
    @Query(value = """
            SELECT r.*
            FROM reports r
            JOIN cnn_classification c ON c.report_id = r.id
            WHERE r.district_id = :districtId
              AND r.report_status IN ('CLASSIFIED', 'DISPATCHED')
              AND c.risk_label = 'HIGH_RISK'
              AND r.submitted_at >= :since
              AND ST_DWithin(
                    r.location,
                    (SELECT location FROM reports WHERE id = :reportId),
                    :radiusMeters)
            """, nativeQuery = true)
    List<Report> findActiveHighRiskNeighbors(@Param("reportId") Long reportId,
                                             @Param("districtId") Long districtId,
                                             @Param("since") LocalDateTime since,
                                             @Param("radiusMeters") double radiusMeters);

    /**
     * District-wide open HIGH_RISK reports within the detection window. Backs the manual admin
     * re-detection endpoint (ops recovery / demo). Distance grouping is intentionally omitted here
     * because the persistence model keeps a single live cluster per district.
     */
    @Query(value = """
            SELECT r.*
            FROM reports r
            JOIN cnn_classification c ON c.report_id = r.id
            WHERE r.district_id = :districtId
              AND r.report_status IN ('CLASSIFIED', 'DISPATCHED')
              AND c.risk_label = 'HIGH_RISK'
              AND r.submitted_at >= :since
            """, nativeQuery = true)
    List<Report> findActiveHighRiskByDistrict(@Param("districtId") Long districtId,
                                              @Param("since") LocalDateTime since);

    long countByDistrict_IdAndSubmittedAtGreaterThanEqualAndSubmittedAtLessThan(
            Long districtId, LocalDateTime from, LocalDateTime toExclusive);

    @Query("""
            SELECT r FROM Report r
            JOIN FETCH r.cnnClassification c
            LEFT JOIN FETCH r.district
            WHERE r.reportStatus IN :statuses
              AND r.submittedAt >= :since
              AND c.riskLabel <> :invalid
            ORDER BY r.submittedAt DESC
            """)
    List<Report> findRecentClassifiedForMap(
            @Param("statuses") Collection<ReportStatus> statuses,
            @Param("since") LocalDateTime since,
            @Param("invalid") RiskLabel invalid);

    @Query("""
            SELECT COUNT(r) FROM Report r
            JOIN r.cnnClassification c
            WHERE r.district.id = :districtId
              AND r.submittedAt >= :from
              AND r.submittedAt < :toExclusive
              AND r.reportStatus IN :statuses
              AND c.riskLabel = :riskLabel
            """)
    long countConfirmedBreedingSites(
            @Param("districtId") Long districtId,
            @Param("from") LocalDateTime from,
            @Param("toExclusive") LocalDateTime toExclusive,
            @Param("statuses") Collection<ReportStatus> statuses,
            @Param("riskLabel") RiskLabel riskLabel);
}
