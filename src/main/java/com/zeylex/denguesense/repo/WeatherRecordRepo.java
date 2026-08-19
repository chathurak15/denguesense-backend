package com.zeylex.denguesense.repo;

import com.zeylex.denguesense.model.WeatherRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface WeatherRecordRepo extends JpaRepository<WeatherRecord, Long> {

    @Query("""
            SELECT w FROM WeatherRecord w
            WHERE w.district.id = :districtId
              AND w.weekStartDate BETWEEN :startDate AND :endDate
            ORDER BY w.weekStartDate ASC
            """)
    List<WeatherRecord> findByDistrictAndDateRange(
            @Param("districtId") Long districtId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    boolean existsByDistrict_IdAndWeekStartDate(Long districtId, LocalDate weekStartDate);

    Optional<WeatherRecord> findByDistrict_IdAndWeekStartDate(Long districtId, LocalDate weekStartDate);
}
