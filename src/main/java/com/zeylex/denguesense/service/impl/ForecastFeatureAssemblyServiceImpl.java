package com.zeylex.denguesense.service.impl;

import com.zeylex.denguesense.dto.ai.ForecastRequestDTO;
import com.zeylex.denguesense.exception.InsufficientHistoryException;
import com.zeylex.denguesense.exception.NotFoundException;
import com.zeylex.denguesense.model.DengueCaseRecord;
import com.zeylex.denguesense.model.District;
import com.zeylex.denguesense.model.WeatherRecord;
import com.zeylex.denguesense.repo.DengueCaseRecordRepo;
import com.zeylex.denguesense.repo.DistrictRepo;
import com.zeylex.denguesense.repo.WeatherRecordRepo;
import com.zeylex.denguesense.service.ForecastFeatureAssemblyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
@Service
public class ForecastFeatureAssemblyServiceImpl implements ForecastFeatureAssemblyService {

    private static final Logger log = LoggerFactory.getLogger(ForecastFeatureAssemblyServiceImpl.class);
    private static final int REQUIRED_WEEKS = 8;

    private final DistrictRepo districtRepo;
    private final WeatherRecordRepo weatherRecordRepo;
    private final DengueCaseRecordRepo dengueCaseRecordRepo;

    public ForecastFeatureAssemblyServiceImpl(DistrictRepo districtRepo,
                                              WeatherRecordRepo weatherRecordRepo,
                                              DengueCaseRecordRepo dengueCaseRecordRepo) {
        this.districtRepo = districtRepo;
        this.weatherRecordRepo = weatherRecordRepo;
        this.dengueCaseRecordRepo = dengueCaseRecordRepo;
    }

    @Override
    public ForecastRequestDTO assembleFeatures(Long districtId, LocalDate targetWeekStart) {
        District district = districtRepo.findById(districtId)
                .orElseThrow(() -> new NotFoundException("District not found: id=" + districtId));

        if (district.getRdhsModelId() == null) {
            throw new IllegalStateException(
                    "District '" + district.getName() + "' has no rdhs_model_id configured for forecasting");
        }

        LocalDate windowEnd = targetWeekStart.minusWeeks(1);
        LocalDate windowStart = windowEnd.minusWeeks(REQUIRED_WEEKS - 1);

        log.debug("Retrieving history for district='{}' (id={}), target={}, window=[{}, {}]",
                district.getName(), districtId, targetWeekStart, windowStart, windowEnd);

        List<WeatherRecord> weatherRecords = weatherRecordRepo.findByDistrictAndDateRange(
                districtId, windowStart, windowEnd);
        List<DengueCaseRecord> caseRecords = dengueCaseRecordRepo.findByDistrictAndDateRange(
                districtId, windowStart, windowEnd);

        validateHistoryCompleteness(district.getName(), weatherRecords, caseRecords, windowStart, windowEnd);
        validateConsecutiveWeeks(district.getName(), weatherRecords, caseRecords, windowStart);

        List<ForecastRequestDTO.WeeklyRecord> history = mergeWeeklyRecords(weatherRecords, caseRecords);
        ForecastRequestDTO.StaticFeatures staticFeatures = buildStaticFeatures(district);

        log.info("Assembled forecast payload for district='{}': {} history weeks, targetWeek={}",
                district.getName(), history.size(), targetWeekStart);

        return new ForecastRequestDTO(
                district.getRdhsModelId(),
                district.getName(),
                targetWeekStart,
                staticFeatures,
                history
        );
    }

    private void validateHistoryCompleteness(String districtName,
                                             List<WeatherRecord> weather,
                                             List<DengueCaseRecord> cases,
                                             LocalDate windowStart,
                                             LocalDate windowEnd) {
        if (weather.size() < REQUIRED_WEEKS) {
            throw new InsufficientHistoryException(String.format(
                    "Insufficient weather history for district '%s': found %d week(s) but need %d " +
                    "consecutive weeks [%s to %s].",
                    districtName, weather.size(), REQUIRED_WEEKS, windowStart, windowEnd));
        }
        if (cases.size() < REQUIRED_WEEKS) {
            throw new InsufficientHistoryException(String.format(
                    "Insufficient dengue case history for district '%s': found %d week(s) but need %d " +
                    "consecutive weeks [%s to %s].",
                    districtName, cases.size(), REQUIRED_WEEKS, windowStart, windowEnd));
        }
    }

    private void validateConsecutiveWeeks(String districtName,
                                          List<WeatherRecord> weather,
                                          List<DengueCaseRecord> cases,
                                          LocalDate expectedStart) {
        for (int i = 0; i < REQUIRED_WEEKS; i++) {
            LocalDate expectedWeek = expectedStart.plusWeeks(i);

            if (!weather.get(i).getWeekStartDate().equals(expectedWeek)) {
                throw new InsufficientHistoryException(String.format(
                        "Gap in weather history for district '%s': expected week_start_date=%s at position %d " +
                        "but found %s. All %d weeks must be consecutive.",
                        districtName, expectedWeek, i, weather.get(i).getWeekStartDate(), REQUIRED_WEEKS));
            }
            if (!cases.get(i).getWeekStartDate().equals(expectedWeek)) {
                throw new InsufficientHistoryException(String.format(
                        "Gap in dengue case history for district '%s': expected week_start_date=%s at position %d " +
                        "but found %s. All %d weeks must be consecutive.",
                        districtName, expectedWeek, i, cases.get(i).getWeekStartDate(), REQUIRED_WEEKS));
            }
        }
    }

    private List<ForecastRequestDTO.WeeklyRecord> mergeWeeklyRecords(List<WeatherRecord> weather,
                                                                     List<DengueCaseRecord> cases) {
        Map<LocalDate, DengueCaseRecord> casesByWeek = cases.stream()
                .collect(Collectors.toMap(DengueCaseRecord::getWeekStartDate, Function.identity()));

        List<ForecastRequestDTO.WeeklyRecord> history = new ArrayList<>(REQUIRED_WEEKS);
        for (WeatherRecord w : weather) {
            DengueCaseRecord c = casesByWeek.get(w.getWeekStartDate());
            history.add(new ForecastRequestDTO.WeeklyRecord(
                    w.getWeekStartDate().get(WeekFields.ISO.weekOfWeekBasedYear()),
                    w.getWeekStartDate(),
                    w.getWeekEndDate(),
                    w.getTempMean(),
                    w.getTempMax(),
                    w.getTempMin(),
                    w.getRainfallMm(),
                    w.getHumidityPct(),
                    c != null ? c.getWeekCases() : null,
                    c != null ? c.getCumulativeCases() : null
            ));
        }
        return history;
    }

    private ForecastRequestDTO.StaticFeatures buildStaticFeatures(District district) {
        return new ForecastRequestDTO.StaticFeatures(
                Boolean.TRUE.equals(district.getZoneDryZone()) ? 1.0 : 0.0,
                Boolean.TRUE.equals(district.getZoneIntermediateZone()) ? 1.0 : 0.0,
                Boolean.TRUE.equals(district.getZoneWetZone()) ? 1.0 : 0.0,
                district.getPopulationDensity() != null ? district.getPopulationDensity() : 0.0
        );
    }
}
