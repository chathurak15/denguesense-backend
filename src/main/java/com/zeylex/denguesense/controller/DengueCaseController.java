package com.zeylex.denguesense.controller;

import com.zeylex.denguesense.dto.requestDTO.DengueCaseSubmitDTO;
import com.zeylex.denguesense.dto.responseDTO.CsvImportResultDTO;
import com.zeylex.denguesense.dto.responseDTO.DengueCaseResponseDTO;
import com.zeylex.denguesense.service.DengueCaseCsvService;
import com.zeylex.denguesense.service.DengueCaseService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("api/v1/admin/cases")
public class DengueCaseController {

    private final DengueCaseCsvService dengueCaseCsvService;
    private final DengueCaseService dengueCaseService;

    public DengueCaseController(DengueCaseCsvService dengueCaseCsvService,
                                DengueCaseService dengueCaseService) {
        this.dengueCaseCsvService = dengueCaseCsvService;
        this.dengueCaseService = dengueCaseService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','MOH','EPIDEMIOLOGIST')")
    public ResponseEntity<DengueCaseResponseDTO> addWeeklyCase(@Valid @RequestBody DengueCaseSubmitDTO dto) {
        DengueCaseResponseDTO response = dengueCaseService.addWeeklyCase(dto);
        HttpStatus status = response.created() ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity.status(status).body(response);
    }

    @PostMapping(value = "/csv", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN','MOH','EPIDEMIOLOGIST')")
    public ResponseEntity<CsvImportResultDTO> uploadWeeklyCases(@RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(dengueCaseCsvService.importWeeklyCases(file));
    }
}
