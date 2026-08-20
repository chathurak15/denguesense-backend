package com.zeylex.denguesense.service.impl;

import com.zeylex.denguesense.dto.ai.ForecastRequestDTO;
import com.zeylex.denguesense.dto.ai.ForecastResponseDTO;
import com.zeylex.denguesense.exception.AiServiceException;
import com.zeylex.denguesense.exception.ForecastTimeoutException;
import com.zeylex.denguesense.exception.InsufficientHistoryException;
import com.zeylex.denguesense.exception.NotFoundException;
import com.zeylex.denguesense.model.District;
import com.zeylex.denguesense.model.DistrictForecast;
import com.zeylex.denguesense.model.enums.GenerationSource;
import com.zeylex.denguesense.repo.DengueCaseRecordRepo;
import com.zeylex.denguesense.repo.DistrictForecastRepo;
import com.zeylex.denguesense.repo.DistrictRepo;
import com.zeylex.denguesense.service.ForecastApiClient;
import com.zeylex.denguesense.service.ForecastFeatureAssemblyService;
import com.zeylex.denguesense.service.ForecastGenerationResult;
import com.zeylex.denguesense.service.ForecastOrchestrationService;
import com.zeylex.denguesense.service.ForecastPersistenceService;
import com.zeylex.denguesense.util.ForecastWeek;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class ForecastOrchestrationServiceImpl implements ForecastOrchestrationService {

    private static final Logger log = LoggerFactory.getLogger(ForecastOrchestrationServiceImpl.class);
    private static final int FORECAST_HORIZON = 4;

    private final DistrictRepo districtRepo;
    private final ForecastFeatureAssemblyService featureAssemblyService;
    private final ForecastApiClient forecastApiClient;
    private final ForecastPersistenceService persistenceService;
    private final DistrictForecastRepo forecastRepo;
    private final DengueCaseRecordRepo dengueCaseRecordRepo;
    private final String defaultModelVersion;

    public ForecastOrchestrationServiceImpl(DistrictRepo districtRepo,
                                            ForecastFeatureAssemblyService featureAssemblyService,
                                            ForecastApiClient forecastApiClient,
                                            ForecastPersistenceService persistenceService,
                                            DistrictForecastRepo forecastRepo,
                                            DengueCaseRecordRepo dengueCaseRecordRepo,
                                            @Value("${forecast.model-version:lstm-v1}") String defaultModelVersion) {
        this.districtRepo = districtRepo;
        this.featureAssemblyService = featureAssemblyService;
        this.forecastApiClient = forecastApiClient;
        this.persistenceService = persistenceService;
        this.forecastRepo = forecastRepo;
        this.dengueCaseRecordRepo = dengueCaseRecordRepo;
        this.defaultModelVersion = defaultModelVersion;
    }

    @Override
    public ForecastGenerationResult generateForecast(Integer rdhsId, GenerationSource source) {
        District district = districtRepo.findByRdhsModelId(rdhsId)
                .orElseThrow(() -> new NotFoundException("No district mapped to rdhsId=" + rdhsId));

        Optional<LocalDate> lastDengueWeek = dengueCaseRecordRepo
                .findLatestWeekStartDateByDistrictId(district.getId());
        if (lastDengueWeek.isEmpty()) {
            return handleIncompleteHistory(rdhsId, district.getName(),
                    new InsufficientHistoryException(
                            "No dengue case records for district '" + district.getName() + "'."));
        }

        LocalDate targetWeekStart = ForecastWeek.targetWeekAfter(lastDengueWeek.get());
        log.info("Forecast target for rdhsId={} district='{}' from last dengue week {} → targetWeek={}",
                rdhsId, district.getName(), lastDengueWeek.get(), targetWeekStart);
        return generateForecast(rdhsId, targetWeekStart, source);
    }

    @Override
    public ForecastGenerationResult generateForecast(Integer rdhsId, LocalDate targetWeekStart, GenerationSource source) {
        District district = districtRepo.findByRdhsModelId(rdhsId)
                .orElseThrow(() -> new NotFoundException("No district mapped to rdhsId=" + rdhsId));

        // 1-2. Retrieve + assemble 8 weeks of history. Incomplete history is an expected outcome.
        ForecastRequestDTO payload;
        try {
            payload = featureAssemblyService.assembleFeatures(district.getId(), targetWeekStart);
        } catch (InsufficientHistoryException ex) {
            return handleIncompleteHistory(rdhsId, district.getName(), ex);
        }

        // 3-4. Call FastAPI. Failures are isolated (returned, not thrown) so a batch keeps going.
        ForecastResponseDTO response;
        try {
            response = forecastApiClient.forecast(payload);
        } catch (ForecastTimeoutException ex) {
            log.error("Forecast generation timed out for rdhsId={} district='{}' targetWeek={}",
                    rdhsId, district.getName(), targetWeekStart);
            return ForecastGenerationResult.upstreamTimeout(ex.getMessage());
        } catch (AiServiceException ex) {
            log.error("Forecast generation failed (upstream) for rdhsId={} district='{}' targetWeek={}: {}",
                    rdhsId, district.getName(), targetWeekStart, ex.getMessage());
            return ForecastGenerationResult.upstreamError(ex.getMessage());
        }

        if (response == null || response.predictions() == null
                || response.predictions().size() != FORECAST_HORIZON) {
            String detail = String.format(
                    "FastAPI returned unexpected prediction count for district '%s': expected %d but got %s",
                    district.getName(), FORECAST_HORIZON,
                    (response == null || response.predictions() == null)
                            ? "null" : response.predictions().size());
            log.error(detail);
            return ForecastGenerationResult.upstreamError(detail);
        }

        String modelVersion = response.modelVersion() != null ? response.modelVersion() : defaultModelVersion;
        List<Double> predictions = response.predictions();
        List<Double> lowerBounds = boundsOrPointEstimate(response.lowerBounds(), predictions, "lower", district.getName());
        List<Double> upperBounds = boundsOrPointEstimate(response.upperBounds(), predictions, "upper", district.getName());

        // 5. Idempotent upsert, with a single retry on optimistic-lock clash.
        return upsertWithRetry(rdhsId, district.getName(), targetWeekStart,
                predictions, lowerBounds, upperBounds, modelVersion, source);
    }

    @Override
    public DistrictForecast getLatestForecast(Integer rdhsId) {
        return forecastRepo.findTopByRdhsIdOrderByTargetWeekStartDesc(rdhsId)
                .orElseThrow(() -> new NotFoundException(
                        "No forecast generated yet for rdhsId=" + rdhsId));
    }

    private ForecastGenerationResult handleIncompleteHistory(Integer rdhsId, String districtName,
                                                             InsufficientHistoryException ex) {
        Optional<DistrictForecast> stale = persistenceService.markLatestStale(rdhsId);
        if (stale.isPresent()) {
            log.warn("Incomplete history for rdhsId={} district='{}' — kept last forecast and marked it STALE. {}",
                    rdhsId, districtName, ex.getMessage());
        } else {
            log.warn("Incomplete history for rdhsId={} district='{}' — no prior forecast to keep. {}",
                    rdhsId, districtName, ex.getMessage());
        }
        return ForecastGenerationResult.incompleteHistory(stale.orElse(null), ex.getMessage());
    }

    private ForecastGenerationResult upsertWithRetry(Integer rdhsId, String districtName, LocalDate targetWeekStart,
                                                     List<Double> predictions, List<Double> lowerBounds,
                                                     List<Double> upperBounds, String modelVersion,
                                                     GenerationSource source) {
        try {
            return ForecastGenerationResult.generated(persistenceService.upsert(
                    rdhsId, districtName, targetWeekStart, predictions, lowerBounds, upperBounds, modelVersion, source));
        } catch (ObjectOptimisticLockingFailureException firstClash) {
            log.warn("Optimistic lock clash upserting forecast for rdhsId={} targetWeek={} — retrying once",
                    rdhsId, targetWeekStart);
            try {
                return ForecastGenerationResult.generated(persistenceService.upsert(
                        rdhsId, districtName, targetWeekStart, predictions, lowerBounds, upperBounds, modelVersion, source));
            } catch (ObjectOptimisticLockingFailureException secondClash) {
                log.error("Optimistic lock clash persisted after retry for rdhsId={} targetWeek={} — giving up",
                        rdhsId, targetWeekStart, secondClash);
                return ForecastGenerationResult.conflict(
                        "Concurrent forecast update for rdhsId=" + rdhsId + " week=" + targetWeekStart
                                + " could not be reconciled after one retry.");
            }
        }
    }

    /**
     * FastAPI may omit prediction intervals. Since the persisted columns are NOT NULL, fall back to
     * the point estimate (a zero-width interval) and log, rather than failing the whole generation.
     */
    private List<Double> boundsOrPointEstimate(List<Double> bounds, List<Double> predictions,
                                               String which, String districtName) {
        if (bounds != null && bounds.size() == predictions.size()) {
            return bounds;
        }
        log.warn("FastAPI returned no usable {} bounds for district='{}' — defaulting to point estimates",
                which, districtName);
        return List.copyOf(predictions);
    }
}
