package com.zeylex.denguesense.controller;

import com.zeylex.denguesense.dto.responseDTO.DistrictForecastResponseDTO;
import com.zeylex.denguesense.model.DistrictForecast;
import com.zeylex.denguesense.model.enums.GenerationSource;
import com.zeylex.denguesense.service.ForecastGenerationResult;
import com.zeylex.denguesense.service.ForecastOrchestrationService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class DistrictForecastController {

    private final ForecastOrchestrationService orchestrationService;

    public DistrictForecastController(ForecastOrchestrationService orchestrationService) {
        this.orchestrationService = orchestrationService;
    }

    @PostMapping("/admin/forecasts/{rdhsId}/regenerate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Object> regenerate(@PathVariable Integer rdhsId, HttpServletRequest request) {
        ForecastGenerationResult result = orchestrationService.generateForecast(
                rdhsId, GenerationSource.MANUAL);

        return switch (result.outcome()) {
            case GENERATED -> ResponseEntity.ok(DistrictForecastResponseDTO.from(result.forecast()));
            case INCOMPLETE_HISTORY -> error(HttpStatus.UNPROCESSABLE_ENTITY, result.message(), request);
            case UPSTREAM_TIMEOUT -> error(HttpStatus.GATEWAY_TIMEOUT, result.message(), request);
            case UPSTREAM_ERROR -> error(HttpStatus.BAD_GATEWAY, result.message(), request);
            case CONFLICT -> error(HttpStatus.CONFLICT, result.message(), request);
        };
    }

    @GetMapping("/forecasts/{rdhsId}/latest")
    @PreAuthorize("hasAnyRole('ADMIN','PHI','MOH','EPIDEMIOLOGIST')")
    public ResponseEntity<DistrictForecastResponseDTO> latest(@PathVariable Integer rdhsId) {
        DistrictForecast forecast = orchestrationService.getLatestForecast(rdhsId);
        return ResponseEntity.ok(DistrictForecastResponseDTO.from(forecast));
    }

    private static ResponseEntity<Object> error(HttpStatus status, String message, HttpServletRequest request) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now().toString());
        body.put("status", status.value());
        body.put("message", message);
        body.put("path", request.getRequestURI());
        return ResponseEntity.status(status).body(body);
    }
}
