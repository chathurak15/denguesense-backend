package com.zeylex.denguesense.repo;

import com.zeylex.denguesense.model.Report;
import com.zeylex.denguesense.model.enums.ReportStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface ReportRepo extends JpaRepository<Report, Long>, JpaSpecificationExecutor<Report> {
    Page<Report> findByDeviceUUID(String deviceUUID, Pageable pageable);
    Optional<Report> findByIdAndDeviceUUID(Long id, String deviceUUID);
    Page<Report> findByDistrict_Id(Long districtId, Pageable pageable);

    Page<Report> findByDistrict_IdAndReportStatus(Long districtId, ReportStatus status, Pageable pageable);

    Page<Report> findByResolvedBy_IdAndReportStatus(Long resolvedById, ReportStatus status, Pageable pageable);
}
