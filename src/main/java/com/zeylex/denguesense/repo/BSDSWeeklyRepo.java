package com.zeylex.denguesense.repo;

import com.zeylex.denguesense.model.BSDSWeekly;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface BSDSWeeklyRepo extends JpaRepository<BSDSWeekly, Long> {

    @Query("""
            SELECT b FROM BSDSWeekly b
            WHERE b.district.id = :districtId
              AND b.weekStartDate BETWEEN :startDate AND :endDate
            ORDER BY b.weekStartDate ASC
            """)
    List<BSDSWeekly> findByDistrictAndDateRange(
            @Param("districtId") Long districtId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    boolean existsByDistrict_IdAndWeekStartDate(Long districtId, LocalDate weekStartDate);

    Optional<BSDSWeekly> findByDistrict_IdAndWeekStartDate(Long districtId, LocalDate weekStartDate);
}
