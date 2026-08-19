package com.zeylex.denguesense.service;

import com.zeylex.denguesense.dto.responseDTO.CsvImportResultDTO;
import org.springframework.web.multipart.MultipartFile;

public interface DengueCaseCsvService {
    CsvImportResultDTO importWeeklyCases(MultipartFile file);
}
