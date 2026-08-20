package com.zeylex.denguesense.controller;

import com.zeylex.denguesense.dto.responseDTO.DengueCaseSummaryDTO;
import com.zeylex.denguesense.dto.responseDTO.PaginatedDTO;
import com.zeylex.denguesense.service.WeeklyCaseQueryService;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("api/v1/cases")
@CrossOrigin
public class WeeklyCaseController {

    private final WeeklyCaseQueryService weeklyCaseQueryService;

    public WeeklyCaseController(WeeklyCaseQueryService weeklyCaseQueryService) {
        this.weeklyCaseQueryService = weeklyCaseQueryService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','MOH','EPIDEMIOLOGIST','PHI')")
    public ResponseEntity<PaginatedDTO> listWeeklyCases(
            @AuthenticationPrincipal UserDetails currentUser,
            @RequestParam(required = false) Long districtId,
            @RequestParam(required = false) String district,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @PageableDefault(size = 26, sort = "weekStartDate", direction = Sort.Direction.DESC) Pageable pageable) {

        PaginatedDTO response = weeklyCaseQueryService.listWeeklyCases(
                currentUser.getUsername(), districtId, district, fromDate, toDate, pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/summary")
    @PreAuthorize("hasAnyRole('ADMIN','MOH','EPIDEMIOLOGIST','PHI')")
    public ResponseEntity<DengueCaseSummaryDTO> getSummary(
            @AuthenticationPrincipal UserDetails currentUser) {
        return ResponseEntity.ok(weeklyCaseQueryService.getSummary(currentUser.getUsername()));
    }
}
