package com.zeylex.denguesense.service.impl;

import com.zeylex.denguesense.dto.ai.ForecastRequestDTO;
import com.zeylex.denguesense.dto.ai.ForecastResponseDTO;
import com.zeylex.denguesense.dto.responseDTO.ForecastResultDTO;
import com.zeylex.denguesense.exception.AiServiceException;
import com.zeylex.denguesense.exception.NotFoundException;
import com.zeylex.denguesense.model.DengueForecast;
import com.zeylex.denguesense.model.District;
import com.zeylex.denguesense.repo.DengueForecastRepo;
import com.zeylex.denguesense.repo.DistrictRepo;
import com.zeylex.denguesense.service.ForecastFeatureAssemblyService;
import com.zeylex.denguesense.service.ForecastService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ForecastServiceImpl implements ForecastService {

    private static final Logger log = LoggerFactory.getLogger(ForecastServiceImpl.class);
    private static final int FORECAST_HORIZON = 4;

    private final ForecastFeatureAssemblyService featureAssemblyService;
    private final DengueForecastRepo forecastRepo;
    private final DistrictRepo districtRepo;
    private final WebClient aiServiceWebClient;

    @Value("${forecast.model-version:lstm-v1}")
    private String defaultModelVersion;

    public ForecastServiceImpl(ForecastFeatureAssemblyService featureAssemblyService,
                               DengueForecastRepo forecastRepo,
                               DistrictRepo districtRepo,
                               WebClient aiServiceWebClient) {
        this.featureAssemblyService = featureAssemblyService;
        this.forecastRepo = forecastRepo;
        this.districtRepo = districtRepo;
        this.aiServiceWebClient = aiServiceWebClient;
    }

    @Override
    @Transactional
    public ForecastResultDTO generateForecast(Long districtId, LocalDate targetWeekStart) {
        District district = districtRepo.findById(districtId)
                .orElseThrow(() -> new NotFoundException("District not found: id=" + districtId));

        ForecastRequestDTO requestPayload = featureAssemblyService.assembleFeatures(districtId, targetWeekStart);

        log.info("Sending forecast request to FastAPI for district='{}', targetWeek={}, " +
                 "historyWeeks={}, rdhsId={}",
                district.getName(), targetWeekStart,
                requestPayload.history().size(),
                requestPayload.rdhsId());

        ForecastResponseDTO response = callForecastApi(requestPayload, district.getName(), targetWeekStart);

        if (response.predictions() == null || response.predictions().size() != FORECAST_HORIZON) {
            throw new AiServiceException(String.format(
                    "FastAPI returned unexpected prediction count: expected %d but got %s for district '%s'",
                    FORECAST_HORIZON,
                    response.predictions() == null ? "null" : response.predictions().size(),
                    district.getName()));
        }

        String modelVersion = response.modelVersion() != null ? response.modelVersion() : defaultModelVersion;
        LocalDate forecastDate = LocalDate.now();

        List<ForecastResultDTO.WeekPrediction> predictions = new ArrayList<>(FORECAST_HORIZON);

        for (int week = 0; week < FORECAST_HORIZON; week++) {
            LocalDate weekTarget = targetWeekStart.plusWeeks(week);
            double predictedCases = response.predictions().get(week);
            Double lowerBound = response.lowerBounds() != null && week < response.lowerBounds().size()
                    ? response.lowerBounds().get(week) : null;
            Double upperBound = response.upperBounds() != null && week < response.upperBounds().size()
                    ? response.upperBounds().get(week) : null;

            if (!forecastRepo.existsByDistrict_IdAndTargetDateAndModelVersion(
                    districtId, weekTarget, modelVersion)) {
                DengueForecast forecast = DengueForecast.builder()
                        .district(district)
                        .forecastDate(forecastDate)
                        .targetDate(weekTarget)
                        .predictedCases(predictedCases)
                        .lowerBound(lowerBound)
                        .upperBound(upperBound)
                        .modelVersion(modelVersion)
                        .build();
                forecastRepo.save(forecast);
            }

            predictions.add(new ForecastResultDTO.WeekPrediction(
                    weekTarget, predictedCases, lowerBound, upperBound));
        }

        return new ForecastResultDTO(district.getName(), forecastDate, modelVersion, predictions);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ForecastResultDTO> getForecastHistory(Long districtId) {
        District district = districtRepo.findById(districtId)
                .orElseThrow(() -> new NotFoundException("District not found: id=" + districtId));

        List<DengueForecast> forecasts = forecastRepo.findByDistrictIdOrderByForecastDateDesc(districtId);

        Map<String, List<DengueForecast>> grouped = forecasts.stream()
                .collect(Collectors.groupingBy(
                        f -> f.getForecastDate() + "|" + f.getModelVersion(),
                        LinkedHashMap::new,
                        Collectors.toList()));

        List<ForecastResultDTO> results = new ArrayList<>();
        for (Map.Entry<String, List<DengueForecast>> entry : grouped.entrySet()) {
            List<DengueForecast> batch = entry.getValue();
            DengueForecast first = batch.get(0);
            List<ForecastResultDTO.WeekPrediction> preds = batch.stream()
                    .map(f -> new ForecastResultDTO.WeekPrediction(
                            f.getTargetDate(), f.getPredictedCases(),
                            f.getLowerBound(), f.getUpperBound()))
                    .toList();
            results.add(new ForecastResultDTO(
                    district.getName(), first.getForecastDate(), first.getModelVersion(), preds));
        }
        return results;
    }

    private ForecastResponseDTO callForecastApi(ForecastRequestDTO request,
                                                String districtName,
                                                LocalDate targetWeekStart) {
        try {
            return aiServiceWebClient.post()
                    .uri("/forecast")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(ForecastResponseDTO.class)
                    .block();
        } catch (WebClientRequestException ex) {
            log.error("Forecast API unreachable: district='{}', targetWeek={}, historySize={}, " +
                      "endpoint=/forecast, error={}",
                    districtName, targetWeekStart, request.history().size(), ex.getMessage());
            throw new AiServiceException(
                    "Forecast service unreachable for district '" + districtName + "': " + ex.getMessage(), ex);
        } catch (WebClientResponseException ex) {
            log.error("Forecast API error: HTTP {}, district='{}', targetWeek={}, historySize={}, " +
                      "endpoint=/forecast, response={}",
                    ex.getStatusCode(), districtName, targetWeekStart,
                    request.history().size(), ex.getResponseBodyAsString());
            throw new AiServiceException(
                    "Forecast service returned HTTP " + ex.getStatusCode()
                    + " for district '" + districtName + "': " + ex.getResponseBodyAsString(), ex);
        }
    }
}
