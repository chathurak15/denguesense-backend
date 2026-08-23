package com.zeylex.denguesense.dto.responseDTO;

import java.util.List;

public record CsvImportResultDTO(
        int imported,
        int updated,
        int skipped,
        int totalRows,
        int weatherImported,
        int bsdsImported,
        List<String> errors
) {}
