package com.zeylex.denguesense.repo;

import com.zeylex.denguesense.model.DistrictForecast;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface DistrictForecastRepo extends JpaRepository<DistrictForecast, Long> {

    Optional<DistrictForecast> findByRdhsIdAndTargetWeekStart(Integer rdhsId, LocalDate targetWeekStart);

    Optional<DistrictForecast> findTopByRdhsIdOrderByTargetWeekStartDesc(Integer rdhsId);
}
