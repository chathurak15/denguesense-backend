package com.zeylex.denguesense.repo;

import com.zeylex.denguesense.model.DistrictForecast;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface DistrictForecastRepo extends JpaRepository<DistrictForecast, Long> {

    Optional<DistrictForecast> findByRdhsIdAndTargetWeekStart(Integer rdhsId, LocalDate targetWeekStart);

    Optional<DistrictForecast> findTopByRdhsIdOrderByTargetWeekStartDesc(Integer rdhsId);

    @Query("""
            SELECT f FROM DistrictForecast f
            WHERE f.targetWeekStart = (
                SELECT MAX(f2.targetWeekStart) FROM DistrictForecast f2 WHERE f2.rdhsId = f.rdhsId
            )
            ORDER BY f.districtName ASC
            """)
    List<DistrictForecast> findLatestPerRdhs();
}
