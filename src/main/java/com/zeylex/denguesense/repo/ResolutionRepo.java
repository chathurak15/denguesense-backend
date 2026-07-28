package com.zeylex.denguesense.repo;

import com.zeylex.denguesense.model.Resolution;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ResolutionRepo extends JpaRepository<Resolution, Long> {
    boolean existsByReport_Id(Long reportId);
    @Query("""
            SELECT r FROM Resolution r
            JOIN FETCH r.resolvedBy u
            JOIN FETCH r.report rp
            WHERE rp.id = :reportId
            """)
    Optional<Resolution> findByReport_Id(@Param("reportId") Long reportId);

    @Query("""
            SELECT r FROM Resolution r
            JOIN FETCH r.resolvedBy u
            JOIN FETCH r.report rp
            WHERE rp.id = :reportId
              AND rp.district.id = :districtId
            """)
    Optional<Resolution> findByReport_IdAndReport_District_Id(
            @Param("reportId") Long reportId,
            @Param("districtId") Long districtId);
}
