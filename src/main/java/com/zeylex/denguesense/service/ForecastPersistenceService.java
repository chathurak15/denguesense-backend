package com.zeylex.denguesense.service;

import com.zeylex.denguesense.model.DistrictForecast;
import com.zeylex.denguesense.model.enums.ForecastStatus;
import com.zeylex.denguesense.model.enums.GenerationSource;
import com.zeylex.denguesense.repo.DistrictForecastRepo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
@Service
public class ForecastPersistenceService {

    private static final Logger log = LoggerFactory.getLogger(ForecastPersistenceService.class);

    private final DistrictForecastRepo forecastRepo;

    public ForecastPersistenceService(DistrictForecastRepo forecastRepo) {
        this.forecastRepo = forecastRepo;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public DistrictForecast upsert(Integer rdhsId,
                                   String districtName,
                                   LocalDate targetWeekStart,
                                   List<Double> predictions,
                                   List<Double> lowerBounds,
                                   List<Double> upperBounds,
                                   String modelVersion,
                                   GenerationSource source) {
        DistrictForecast forecast = forecastRepo
                .findByRdhsIdAndTargetWeekStart(rdhsId, targetWeekStart)
                .orElseGet(DistrictForecast::new);

        boolean isNew = forecast.getId() == null;

        forecast.setRdhsId(rdhsId);
        forecast.setDistrictName(districtName);
        forecast.setTargetWeekStart(targetWeekStart);
        forecast.setPredictions(predictions);
        forecast.setLowerBounds(lowerBounds);
        forecast.setUpperBounds(upperBounds);
        forecast.setModelVersion(modelVersion);
        forecast.setStatus(ForecastStatus.GENERATED);
        forecast.setGenerationSource(source);
        forecast.setGeneratedAt(Instant.now());

        DistrictForecast saved = forecastRepo.save(forecast);
        log.info("Forecast {} for rdhsId={} district='{}' targetWeek={} source={} modelVersion={}",
                isNew ? "inserted" : "updated", rdhsId, districtName, targetWeekStart, source, modelVersion);
        return saved;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Optional<DistrictForecast> markLatestStale(Integer rdhsId) {
        Optional<DistrictForecast> latest = forecastRepo.findTopByRdhsIdOrderByTargetWeekStartDesc(rdhsId);
        latest.ifPresent(forecast -> {
            if (forecast.getStatus() != ForecastStatus.STALE) {
                forecast.setStatus(ForecastStatus.STALE);
                forecastRepo.save(forecast);
                log.info("Marked latest forecast STALE for rdhsId={} targetWeek={}",
                        rdhsId, forecast.getTargetWeekStart());
            }
        });
        return latest;
    }
}
