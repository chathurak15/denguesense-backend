package com.zeylex.denguesense.service.impl;

import com.zeylex.denguesense.dto.requestDTO.DengueCaseSubmitDTO;
import com.zeylex.denguesense.dto.responseDTO.DengueCaseResponseDTO;
import com.zeylex.denguesense.exception.NotFoundException;
import com.zeylex.denguesense.model.BSDSWeekly;
import com.zeylex.denguesense.model.DengueCaseRecord;
import com.zeylex.denguesense.model.District;
import com.zeylex.denguesense.model.WeatherRecord;
import com.zeylex.denguesense.repo.DengueCaseRecordRepo;
import com.zeylex.denguesense.repo.DistrictRepo;
import com.zeylex.denguesense.service.BsdsWeeklyService;
import com.zeylex.denguesense.service.DengueCaseService;
import com.zeylex.denguesense.service.WeatherFetchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
public class DengueCaseServiceImpl implements DengueCaseService {

    private static final Logger log = LoggerFactory.getLogger(DengueCaseServiceImpl.class);

    private final DistrictRepo districtRepo;
    private final DengueCaseRecordRepo dengueCaseRecordRepo;
    private final WeatherFetchService weatherFetchService;
    private final BsdsWeeklyService bsdsWeeklyService;

    public DengueCaseServiceImpl(DistrictRepo districtRepo,
                                 DengueCaseRecordRepo dengueCaseRecordRepo,
                                 WeatherFetchService weatherFetchService,
                                 BsdsWeeklyService bsdsWeeklyService) {
        this.districtRepo = districtRepo;
        this.dengueCaseRecordRepo = dengueCaseRecordRepo;
        this.weatherFetchService = weatherFetchService;
        this.bsdsWeeklyService = bsdsWeeklyService;
    }

    @Override
    public DengueCaseResponseDTO addWeeklyCase(DengueCaseSubmitDTO dto) {
        District district = resolveDistrict(dto);
        LocalDate weekStart = dto.getWeekStartDate();
        LocalDate weekEnd = dto.getWeekEndDate() != null ? dto.getWeekEndDate() : weekStart.plusDays(6);

        WeatherRecord weather = weatherFetchService.upsertWeeklyWeather(district, weekStart, weekEnd);
        BSDSWeekly bsds = bsdsWeeklyService.upsertWeeklyBsds(district, weekStart, weekEnd);
        SavedCase savedCase = upsertCase(district, weekStart, weekEnd, dto);

        log.info("Admin weekly dengue record {} for district='{}' weekStart={}, weekCases={}, bsdsScore={}",
                savedCase.created() ? "created" : "updated",
                district.getName(), weekStart, dto.getWeekCases(), bsds.getBsdsScore());

        return toResponse(savedCase.record(), savedCase.created(), weather, bsds);
    }

    private SavedCase upsertCase(District district, LocalDate weekStart, LocalDate weekEnd, DengueCaseSubmitDTO dto) {
        DengueCaseRecord existing = dengueCaseRecordRepo
                .findByDistrict_IdAndWeekStartDate(district.getId(), weekStart)
                .orElse(null);
        if (existing == null) {
            DengueCaseRecord created = dengueCaseRecordRepo.save(DengueCaseRecord.builder()
                    .district(district)
                    .weekStartDate(weekStart)
                    .weekEndDate(weekEnd)
                    .weekCases(dto.getWeekCases())
                    .cumulativeCases(dto.getCumulativeCases())
                    .build());
            return new SavedCase(created, true);
        }
        existing.setWeekEndDate(weekEnd);
        existing.setWeekCases(dto.getWeekCases());
        if (dto.getCumulativeCases() != null) {
            existing.setCumulativeCases(dto.getCumulativeCases());
        }
        return new SavedCase(dengueCaseRecordRepo.save(existing), false);
    }

    private District resolveDistrict(DengueCaseSubmitDTO dto) {
        if (dto.getDistrictId() != null) {
            return districtRepo.findById(dto.getDistrictId())
                    .orElseThrow(() -> new NotFoundException("District not found: id=" + dto.getDistrictId()));
        }
        String name = dto.getDistrict().trim();
        return districtRepo.findByNameIgnoreCase(name)
                .or(() -> districtRepo.findByRdhsZoneIgnoreCase(name))
                .orElseThrow(() -> new NotFoundException("District not found: " + name));
    }

    private static DengueCaseResponseDTO toResponse(DengueCaseRecord record, boolean created,
                                                    WeatherRecord weather, BSDSWeekly bsds) {
        return new DengueCaseResponseDTO(
                record.getId(),
                record.getDistrict().getId(),
                record.getDistrict().getName(),
                record.getWeekStartDate(),
                record.getWeekEndDate(),
                record.getWeekCases(),
                record.getCumulativeCases(),
                created,
                new DengueCaseResponseDTO.WeatherSummary(
                        weather.getId(),
                        weather.getTempMean(),
                        weather.getTempMax(),
                        weather.getTempMin(),
                        weather.getRainfallMm(),
                        weather.getHumidityPct()
                ),
                new DengueCaseResponseDTO.BsdsSummary(
                        bsds.getId(),
                        bsds.getBsdsScore(),
                        bsds.getReportCount()
                )
        );
    }

    private record SavedCase(DengueCaseRecord record, boolean created) {
    }
}
