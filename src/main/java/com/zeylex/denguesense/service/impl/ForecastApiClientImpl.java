package com.zeylex.denguesense.service.impl;

import com.zeylex.denguesense.dto.ai.ForecastRequestDTO;
import com.zeylex.denguesense.dto.ai.ForecastResponseDTO;
import com.zeylex.denguesense.exception.AiServiceException;
import com.zeylex.denguesense.exception.ForecastTimeoutException;
import com.zeylex.denguesense.service.ForecastApiClient;
import io.netty.handler.timeout.TimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

@Component
public class ForecastApiClientImpl implements ForecastApiClient {

    private static final Logger log = LoggerFactory.getLogger(ForecastApiClientImpl.class);

    private final WebClient aiServiceWebClient;

    public ForecastApiClientImpl(WebClient aiServiceWebClient) {
        this.aiServiceWebClient = aiServiceWebClient;
    }

    @Override
    public ForecastResponseDTO forecast(ForecastRequestDTO request) {
        try {
            return aiServiceWebClient.post()
                    .uri("/forecast")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(ForecastResponseDTO.class)
                    .block();
        } catch (WebClientRequestException ex) {
            if (isTimeout(ex)) {
                log.error("Forecast API timed out: district='{}', targetWeek={}, historySize={}, error={}",
                        request.districtName(), request.targetWeekStart(), request.history().size(), ex.getMessage());
                throw new ForecastTimeoutException(
                        "Forecast service timed out for district '" + request.districtName() + "'", ex);
            }
            log.error("Forecast API unreachable: district='{}', targetWeek={}, historySize={}, error={}",
                    request.districtName(), request.targetWeekStart(), request.history().size(), ex.getMessage());
            throw new AiServiceException(
                    "Forecast service unreachable for district '" + request.districtName() + "': " + ex.getMessage(), ex);
        } catch (WebClientResponseException ex) {
            log.error("Forecast API error: HTTP {}, district='{}', targetWeek={}, historySize={}, response={}",
                    ex.getStatusCode(), request.districtName(), request.targetWeekStart(),
                    request.history().size(), ex.getResponseBodyAsString());
            throw new AiServiceException(
                    "Forecast service returned HTTP " + ex.getStatusCode()
                            + " for district '" + request.districtName() + "': " + ex.getResponseBodyAsString(), ex);
        }
    }

    private static boolean isTimeout(Throwable ex) {
        for (Throwable cause = ex; cause != null; cause = cause.getCause()) {
            if (cause instanceof TimeoutException
                    || cause instanceof java.util.concurrent.TimeoutException) {
                return true;
            }
        }
        return false;
    }
}
