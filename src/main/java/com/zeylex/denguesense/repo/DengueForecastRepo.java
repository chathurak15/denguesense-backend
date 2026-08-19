package com.zeylex.denguesense.repo;

import com.zeylex.denguesense.model.DengueForecast;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface DengueForecastRepo extends JpaRepository<DengueForecast, Long> {

    boolean existsByDistrict_IdAndTargetDateAndModelVersion(
            Long districtId, LocalDate targetDate, String modelVersion);

    @Query("""
            SELECT f FROM DengueForecast f
            WHERE f.district.id = :districtId
            ORDER BY f.forecastDate DESC, f.targetDate ASC
            """)
    List<DengueForecast> findByDistrictIdOrderByForecastDateDesc(
            @Param("districtId") Long districtId);

    @Query("""
            SELECT f FROM DengueForecast f
            WHERE f.district.id = :districtId
              AND f.forecastDate = :forecastDate
            ORDER BY f.targetDate ASC
            """)
    List<DengueForecast> findByDistrictIdAndForecastDate(
            @Param("districtId") Long districtId,
            @Param("forecastDate") LocalDate forecastDate);
}
