package com.zeylex.denguesense.service.impl;

import com.zeylex.denguesense.dto.responseDTO.CsvImportResultDTO;
import com.zeylex.denguesense.model.DengueCaseRecord;
import com.zeylex.denguesense.model.District;
import com.zeylex.denguesense.repo.DengueCaseRecordRepo;
import com.zeylex.denguesense.repo.DistrictRepo;
import com.zeylex.denguesense.service.DengueCaseCsvService;
import com.zeylex.denguesense.service.WeeklyCaseEnrichmentService;
import com.zeylex.denguesense.weather.DistrictWeekWindow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@Service
public class DengueCaseCsvServiceImpl implements DengueCaseCsvService {

    private static final Logger log = LoggerFactory.getLogger(DengueCaseCsvServiceImpl.class);
    private static final int MAX_ERRORS = 30;
    private static final int MAX_ROWS = 20_000;
    private static final DateTimeFormatter[] DATE_FORMATS = {
            DateTimeFormatter.ISO_LOCAL_DATE,
            DateTimeFormatter.ofPattern("M/d/yyyy"),
            DateTimeFormatter.ofPattern("d-M-yyyy")
    };

    private final DengueCaseRecordRepo dengueCaseRecordRepo;
    private final DistrictRepo districtRepo;
    private final WeeklyCaseEnrichmentService weeklyCaseEnrichmentService;

    public DengueCaseCsvServiceImpl(DengueCaseRecordRepo dengueCaseRecordRepo,
                                    DistrictRepo districtRepo,
                                    WeeklyCaseEnrichmentService weeklyCaseEnrichmentService) {
        this.dengueCaseRecordRepo = dengueCaseRecordRepo;
        this.districtRepo = districtRepo;
        this.weeklyCaseEnrichmentService = weeklyCaseEnrichmentService;
    }

    @Override
    public CsvImportResultDTO importWeeklyCases(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("CSV file is required");
        }
        String name = Optional.ofNullable(file.getOriginalFilename()).orElse("").toLowerCase(Locale.ROOT);
        if (!name.endsWith(".csv")) {
            throw new IllegalArgumentException("Only .csv files are accepted");
        }

        Map<String, District> districts = indexDistricts();
        int imported = 0;
        int updated = 0;
        int skipped = 0;
        int totalRows = 0;
        List<String> errors = new ArrayList<>();
        List<DistrictWeekWindow> importedWeeks = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {

            String headerLine = reader.readLine();
            if (headerLine == null || headerLine.isBlank()) {
                throw new IllegalArgumentException("CSV is empty");
            }
            Map<String, Integer> cols = indexHeaders(headerLine);
            requireColumn(cols, "district", "rdhs", "district_name");
            requireColumn(cols, "week_cases", "cases", "confirmed_cases");
            requireColumn(cols, "week_start_date", "week_start", "start_date");

            String line;
            int rowNum = 1;
            while ((line = reader.readLine()) != null) {
                rowNum++;
                if (line.isBlank()) {
                    continue;
                }
                totalRows++;
                if (totalRows > MAX_ROWS) {
                    errors.add("Stopped after " + MAX_ROWS + " data rows");
                    break;
                }
                try {
                    UpsertResult result = upsertRow(line, cols, districts);
                    if (result.kind() == UpsertKind.INSERTED) {
                        imported++;
                    } else {
                        updated++;
                    }
                    importedWeeks.add(result.week());
                } catch (IllegalArgumentException ex) {
                    skipped++;
                    if (errors.size() < MAX_ERRORS) {
                        errors.add("Row " + rowNum + ": " + ex.getMessage());
                    }
                }
            }
        } catch (IllegalArgumentException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Weekly case CSV import failed: {}", ex.getMessage(), ex);
            throw new IllegalArgumentException("Could not read CSV file. Check the encoding and column headers.");
        }

        if (!importedWeeks.isEmpty()) {
            try {
                weeklyCaseEnrichmentService.enrichImportedWeeks(List.copyOf(importedWeeks));
            } catch (Exception ex) {
                log.error("Could not start weather/BSDS enrichment: {}", ex.getMessage(), ex);
                if (errors.size() < MAX_ERRORS) {
                    errors.add("Weather/BSDS enrichment could not be started: " + ex.getMessage());
                }
            }
        }
        log.info("Weekly case CSV imported: inserted={}, updated={}, skipped={}",
                imported, updated, skipped);
        return new CsvImportResultDTO(imported, updated, skipped, totalRows, 0, 0, errors);
    }

    private UpsertResult upsertRow(String line, Map<String, Integer> cols, Map<String, District> districts) {
        List<String> cells = parseCsvLine(line);
        String districtName = cell(cells, cols, "district", "rdhs", "district_name");
        if (districtName.isBlank()) {
            throw new IllegalArgumentException("district / rdhs is empty");
        }
        District district = districts.get(districtName.toLowerCase(Locale.ROOT).trim());
        if (district == null) {
            throw new IllegalArgumentException("unknown district '" + districtName + "'");
        }

        LocalDate weekStart = parseDate(cell(cells, cols, "week_start_date", "week_start", "start_date"));
        String endRaw = cell(cells, cols, "week_end_date", "week_end", "end_date");
        LocalDate weekEnd = endRaw.isBlank() ? weekStart.plusDays(6) : parseDate(endRaw);
        int weekCases = parseNonNegativeInt(cell(cells, cols, "week_cases", "cases", "confirmed_cases"), "week_cases");
        Integer cumulative = parseOptionalInt(cell(cells, cols, "cumulative_cases", "cumulative"));

        DengueCaseRecord existing = dengueCaseRecordRepo
                .findByDistrict_IdAndWeekStartDate(district.getId(), weekStart)
                .orElse(null);
        if (existing == null) {
            DengueCaseRecord record = DengueCaseRecord.builder()
                    .district(district)
                    .weekStartDate(weekStart)
                    .weekEndDate(weekEnd)
                    .weekCases(weekCases)
                    .cumulativeCases(cumulative)
                    .build();
            dengueCaseRecordRepo.save(record);
            return new UpsertResult(UpsertKind.INSERTED, new DistrictWeekWindow(district, weekStart, weekEnd));
        }
        existing.setWeekEndDate(weekEnd);
        existing.setWeekCases(weekCases);
        if (cumulative != null) {
            existing.setCumulativeCases(cumulative);
        }
        dengueCaseRecordRepo.save(existing);
        return new UpsertResult(UpsertKind.UPDATED, new DistrictWeekWindow(district, weekStart, weekEnd));
    }

    private Map<String, District> indexDistricts() {
        Map<String, District> map = new HashMap<>();
        for (District d : districtRepo.findAll()) {
            if (d.getName() != null) {
                map.put(d.getName().toLowerCase(Locale.ROOT).trim(), d);
            }
            if (d.getRdhsZone() != null && !d.getRdhsZone().isBlank()) {
                map.putIfAbsent(d.getRdhsZone().toLowerCase(Locale.ROOT).trim(), d);
            }
        }
        return map;
    }

    private static Map<String, Integer> indexHeaders(String headerLine) {
        List<String> headers = parseCsvLine(headerLine);
        Map<String, Integer> cols = new HashMap<>();
        for (int i = 0; i < headers.size(); i++) {
            String key = normalizeHeader(headers.get(i));
            if (!key.isBlank()) {
                cols.put(key, i);
            }
        }
        return cols;
    }

    private static void requireColumn(Map<String, Integer> cols, String... aliases) {
        for (String alias : aliases) {
            if (cols.containsKey(alias)) {
                return;
            }
        }
        throw new IllegalArgumentException(
                "CSV is missing a required column. Expected one of: " + String.join(", ", aliases));
    }

    private static String cell(List<String> cells, Map<String, Integer> cols, String... aliases) {
        for (String alias : aliases) {
            Integer idx = cols.get(alias);
            if (idx != null && idx < cells.size()) {
                return cells.get(idx).trim();
            }
        }
        return "";
    }

    private static String normalizeHeader(String raw) {
        String value = raw.replace("\uFEFF", "").trim().toLowerCase(Locale.ROOT);
        return value.replace(' ', '_');
    }

    static LocalDate parseDate(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("date is empty");
        }
        String value = stripTime(raw.trim());
        for (DateTimeFormatter formatter : DATE_FORMATS) {
            try {
                return LocalDate.parse(value, formatter);
            } catch (DateTimeParseException ignored) {
                // try the next format
            }
        }
        throw new IllegalArgumentException(
                "invalid date '" + raw + "' (use YYYY-MM-DD or M/D/YYYY, e.g. 2026-06-22 or 6/22/2026)");
    }

    private static String stripTime(String value) {
        int space = value.indexOf(' ');
        if (space > 0) {
            value = value.substring(0, space);
        }
        int tIndex = value.indexOf('T');
        if (tIndex > 0) {
            value = value.substring(0, tIndex);
        }
        return value;
    }

    private static int parseNonNegativeInt(String raw, String field) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException(field + " is empty");
        }
        try {
            int value = Integer.parseInt(raw.trim());
            if (value < 0) {
                throw new IllegalArgumentException(field + " cannot be negative");
            }
            return value;
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("invalid " + field + " '" + raw + "'");
        }
    }

    private static Integer parseOptionalInt(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("invalid number '" + raw + "'");
        }
    }

    static List<String> parseCsvLine(String line) {
        List<String> cells = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    current.append('"');
                    i++;
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (c == ',' && !inQuotes) {
                cells.add(current.toString());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        cells.add(current.toString());
        return cells;
    }

    private enum UpsertKind {
        INSERTED,
        UPDATED
    }

    private record UpsertResult(UpsertKind kind, DistrictWeekWindow week) {
    }
}
