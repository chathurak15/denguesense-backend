package com.zeylex.denguesense.service;

import com.zeylex.denguesense.dto.requestDTO.DengueCaseSubmitDTO;
import com.zeylex.denguesense.dto.responseDTO.DengueCaseResponseDTO;
import com.zeylex.denguesense.model.BSDSWeekly;
import com.zeylex.denguesense.model.DengueCaseRecord;
import com.zeylex.denguesense.model.District;
import com.zeylex.denguesense.model.WeatherRecord;
import com.zeylex.denguesense.repo.DengueCaseRecordRepo;
import com.zeylex.denguesense.repo.DistrictRepo;
import com.zeylex.denguesense.service.impl.DengueCaseServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("DengueCaseService — admin weekly case + weather + BSDS save")
class DengueCaseServiceImplTest {

    @Mock private DistrictRepo districtRepo;
    @Mock private DengueCaseRecordRepo dengueCaseRecordRepo;
    @Mock private WeatherFetchService weatherFetchService;
    @Mock private BsdsWeeklyService bsdsWeeklyService;

    private DengueCaseServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new DengueCaseServiceImpl(
                districtRepo, dengueCaseRecordRepo, weatherFetchService, bsdsWeeklyService);
    }

    @Test
    @DisplayName("saves dengue cases, weather, and BSDS for the same week start/end dates")
    void addWeeklyCase_fetchesWeatherAndBsdsForWeekWindow() {
        District colombo = new District();
        colombo.setId(1L);
        colombo.setName("Colombo");
        LocalDate start = LocalDate.of(2026, 8, 17);
        LocalDate end = LocalDate.of(2026, 8, 23);

        when(districtRepo.findByNameIgnoreCase("Colombo")).thenReturn(Optional.of(colombo));
        when(weatherFetchService.upsertWeeklyWeather(colombo, start, end)).thenReturn(
                WeatherRecord.builder()
                        .id(9L)
                        .district(colombo)
                        .weekStartDate(start)
                        .weekEndDate(end)
                        .tempMean(28.4)
                        .tempMax(32.1)
                        .tempMin(24.8)
                        .rainfallMm(41.2)
                        .humidityPct(81.5)
                        .build());
        when(bsdsWeeklyService.upsertWeeklyBsds(colombo, start, end)).thenReturn(
                BSDSWeekly.builder()
                        .id(3L)
                        .district(colombo)
                        .weekStartDate(start)
                        .weekEndDate(end)
                        .bsdsScore(0.4218)
                        .reportCount(12)
                        .build());
        when(dengueCaseRecordRepo.findByDistrict_IdAndWeekStartDate(1L, start)).thenReturn(Optional.empty());
        when(dengueCaseRecordRepo.save(any(DengueCaseRecord.class))).thenAnswer(invocation -> {
            DengueCaseRecord record = invocation.getArgument(0);
            record.setId(42L);
            return record;
        });

        DengueCaseSubmitDTO dto = new DengueCaseSubmitDTO();
        dto.setDistrict("Colombo");
        dto.setWeekStartDate(start);
        dto.setWeekEndDate(end);
        dto.setWeekCases(87);
        dto.setCumulativeCases(1200);

        DengueCaseResponseDTO response = service.addWeeklyCase(dto);

        ArgumentCaptor<LocalDate> startCaptor = ArgumentCaptor.forClass(LocalDate.class);
        ArgumentCaptor<LocalDate> endCaptor = ArgumentCaptor.forClass(LocalDate.class);
        verify(weatherFetchService).upsertWeeklyWeather(any(District.class), startCaptor.capture(), endCaptor.capture());
        verify(bsdsWeeklyService).upsertWeeklyBsds(colombo, start, end);
        assertThat(startCaptor.getValue()).isEqualTo(start);
        assertThat(endCaptor.getValue()).isEqualTo(end);

        assertThat(response.created()).isTrue();
        assertThat(response.weekCases()).isEqualTo(87);
        assertThat(response.weather().rainfallMm()).isEqualTo(41.2);
        assertThat(response.bsds().bsdsScore()).isEqualTo(0.4218);
        assertThat(response.bsds().reportCount()).isEqualTo(12);
    }

    @Test
    @DisplayName("defaults week end to start + 6 days when omitted")
    void addWeeklyCase_defaultsWeekEnd() {
        District colombo = new District();
        colombo.setId(1L);
        colombo.setName("Colombo");
        LocalDate start = LocalDate.of(2026, 8, 17);

        when(districtRepo.findById(1L)).thenReturn(Optional.of(colombo));
        when(weatherFetchService.upsertWeeklyWeather(colombo, start, start.plusDays(6)))
                .thenReturn(WeatherRecord.builder().id(1L).district(colombo).weekStartDate(start).build());
        when(bsdsWeeklyService.upsertWeeklyBsds(colombo, start, start.plusDays(6)))
                .thenReturn(BSDSWeekly.builder().id(1L).district(colombo).bsdsScore(0.0).reportCount(0).build());
        when(dengueCaseRecordRepo.findByDistrict_IdAndWeekStartDate(1L, start)).thenReturn(Optional.empty());
        when(dengueCaseRecordRepo.save(any(DengueCaseRecord.class))).thenAnswer(invocation -> {
            DengueCaseRecord record = invocation.getArgument(0);
            record.setId(1L);
            return record;
        });

        DengueCaseSubmitDTO dto = new DengueCaseSubmitDTO();
        dto.setDistrictId(1L);
        dto.setWeekStartDate(start);
        dto.setWeekCases(10);

        service.addWeeklyCase(dto);

        verify(weatherFetchService).upsertWeeklyWeather(colombo, start, start.plusDays(6));
        verify(bsdsWeeklyService).upsertWeeklyBsds(colombo, start, start.plusDays(6));
    }
}
