package com.zeylex.denguesense.controller;

import com.zeylex.denguesense.dto.responseDTO.CitizenAlertDTO;
import com.zeylex.denguesense.dto.responseDTO.CitizenDistrictDTO;
import com.zeylex.denguesense.dto.responseDTO.CitizenDistrictStatusDTO;
import com.zeylex.denguesense.dto.responseDTO.CitizenHotspotDTO;
import com.zeylex.denguesense.dto.responseDTO.CitizenOutbreakSummaryDTO;
import com.zeylex.denguesense.dto.responseDTO.DistrictForecastResponseDTO;
import com.zeylex.denguesense.service.CitizenPublicService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("api/v1/public")
@CrossOrigin
public class CitizenPublicController {

    private final CitizenPublicService citizenPublicService;

    public CitizenPublicController(CitizenPublicService citizenPublicService) {
        this.citizenPublicService = citizenPublicService;
    }

    @GetMapping("/outbreak-summary")
    public ResponseEntity<CitizenOutbreakSummaryDTO> outbreakSummary() {
        return ResponseEntity.ok(citizenPublicService.outbreakSummary());
    }

    @GetMapping("/hotspots")
    public ResponseEntity<Map<String, List<CitizenHotspotDTO>>> hotspots() {
        return ResponseEntity.ok(Map.of("hotspots", citizenPublicService.hotspots()));
    }

    @GetMapping("/alerts")
    public ResponseEntity<Map<String, List<CitizenAlertDTO>>> alerts() {
        return ResponseEntity.ok(Map.of("alerts", citizenPublicService.alerts()));
    }

    @GetMapping("/districts")
    public ResponseEntity<Map<String, List<CitizenDistrictDTO>>> districts() {
        return ResponseEntity.ok(Map.of("districts", citizenPublicService.districts()));
    }

    @GetMapping("/forecasts")
    public ResponseEntity<Map<String, List<DistrictForecastResponseDTO>>> forecasts() {
        return ResponseEntity.ok(Map.of("forecasts", citizenPublicService.latestForecasts()));
    }

    @GetMapping("/district-status")
    public ResponseEntity<CitizenDistrictStatusDTO> districtStatus(
            @RequestParam(required = false) Double latitude,
            @RequestParam(required = false) Double longitude,
            @RequestParam(required = false) Integer rdhsId) {
        return ResponseEntity.ok(citizenPublicService.districtStatus(latitude, longitude, rdhsId));
    }
}
