package com.zeylex.denguesense.controller;

import com.zeylex.denguesense.dto.responseDTO.ForecastResultDTO;
import com.zeylex.denguesense.exception.NotFoundException;
import com.zeylex.denguesense.model.District;
import com.zeylex.denguesense.repo.DistrictRepo;
import com.zeylex.denguesense.service.ForecastService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

@RestController
@RequestMapping("/api/forecasts")
public class ForecastController {

    private final ForecastService forecastService;
    private final DistrictRepo districtRepo;

    public ForecastController(ForecastService forecastService, DistrictRepo districtRepo) {
        this.forecastService = forecastService;
        this.districtRepo = districtRepo;
    }


    @GetMapping("/{district}")
    public ResponseEntity<ForecastResultDTO> generateForecast(
            @PathVariable String district,
            @RequestParam(required = false) LocalDate targetWeek) {

        District d = resolveDistrict(district);
        LocalDate target = targetWeek != null ? targetWeek : nextMondayFromToday();

        ForecastResultDTO result = forecastService.generateForecast(d.getId(), target);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{district}/history")
    public ResponseEntity<List<ForecastResultDTO>> getForecastHistory(@PathVariable String district) {
        District d = resolveDistrict(district);
        List<ForecastResultDTO> history = forecastService.getForecastHistory(d.getId());
        return ResponseEntity.ok(history);
    }

    private District resolveDistrict(String districtIdentifier) {
        try {
            Long id = Long.parseLong(districtIdentifier);
            return districtRepo.findById(id)
                    .orElseThrow(() -> new NotFoundException("District not found: " + districtIdentifier));
        } catch (NumberFormatException e) {
            return districtRepo.findByNameIgnoreCase(districtIdentifier)
                    .orElseThrow(() -> new NotFoundException("District not found: " + districtIdentifier));
        }
    }

    private static LocalDate nextMondayFromToday() {
        LocalDate today = LocalDate.now();
        return today.with(TemporalAdjusters.next(DayOfWeek.MONDAY));
    }
}
