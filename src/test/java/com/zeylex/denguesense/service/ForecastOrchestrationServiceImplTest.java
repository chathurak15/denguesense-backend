package com.zeylex.denguesense.service;

import com.zeylex.denguesense.dto.ai.ForecastRequestDTO;
import com.zeylex.denguesense.dto.ai.ForecastResponseDTO;
import com.zeylex.denguesense.exception.AiServiceException;
import com.zeylex.denguesense.exception.ForecastTimeoutException;
import com.zeylex.denguesense.exception.InsufficientHistoryException;
import com.zeylex.denguesense.exception.NotFoundException;
import com.zeylex.denguesense.model.District;
import com.zeylex.denguesense.model.DistrictForecast;
import com.zeylex.denguesense.model.enums.ForecastStatus;
import com.zeylex.denguesense.model.enums.GenerationSource;
import com.zeylex.denguesense.repo.DengueCaseRecordRepo;
import com.zeylex.denguesense.repo.DistrictForecastRepo;
import com.zeylex.denguesense.repo.DistrictRepo;
import com.zeylex.denguesense.service.impl.ForecastOrchestrationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ForecastOrchestrationService — shared generate method + read path")
class ForecastOrchestrationServiceImplTest {

    @Mock private DistrictRepo districtRepo;
    @Mock private ForecastFeatureAssemblyService featureAssemblyService;
    @Mock private ForecastApiClient forecastApiClient;
    @Mock private ForecastPersistenceService persistenceService;
    @Mock private DistrictForecastRepo forecastRepo;
    @Mock private DengueCaseRecordRepo dengueCaseRecordRepo;

    private ForecastOrchestrationServiceImpl service;

    private static final Integer RDHS_ID = 4;                    // Colombo
    private static final Long DISTRICT_ID = 5L;
    private static final LocalDate TARGET = LocalDate.of(2026, 9, 7);

    @BeforeEach
    void setUp() {
        service = new ForecastOrchestrationServiceImpl(
                districtRepo, featureAssemblyService, forecastApiClient,
                persistenceService, forecastRepo, dengueCaseRecordRepo, "lstm-v1");
    }

    private District colombo() {
        District d = new District();
        d.setId(DISTRICT_ID);
        d.setName("Colombo");
        d.setRdhsModelId(RDHS_ID);
        return d;
    }

    private ForecastRequestDTO payload() {
        return new ForecastRequestDTO(RDHS_ID, "Colombo", TARGET,
                new ForecastRequestDTO.StaticFeatures(0, 0, 1, 3392.0), List.of());
    }

    private ForecastResponseDTO response() {
        return new ForecastResponseDTO(
                List.of(10.0, 11.0, 12.0, 13.0),
                List.of(8.0, 9.0, 10.0, 11.0),
                List.of(12.0, 13.0, 14.0, 15.0),
                "lstm-v1");
    }

    @Test
    @DisplayName("Happy path → GENERATED, upserted once, source recorded")
    void happyPath_generates() {
        when(districtRepo.findByRdhsModelId(RDHS_ID)).thenReturn(Optional.of(colombo()));
        when(featureAssemblyService.assembleFeatures(DISTRICT_ID, TARGET)).thenReturn(payload());
        when(forecastApiClient.forecast(any())).thenReturn(response());
        DistrictForecast saved = DistrictForecast.builder().id(1L).rdhsId(RDHS_ID).build();
        when(persistenceService.upsert(eq(RDHS_ID), eq("Colombo"), eq(TARGET),
                anyList(), anyList(), anyList(), eq("lstm-v1"), eq(GenerationSource.MANUAL)))
                .thenReturn(saved);

        ForecastGenerationResult result = service.generateForecast(RDHS_ID, TARGET, GenerationSource.MANUAL);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.forecast()).isSameAs(saved);
        verify(persistenceService, times(1)).upsert(eq(RDHS_ID), eq("Colombo"), eq(TARGET),
                anyList(), anyList(), anyList(), eq("lstm-v1"), eq(GenerationSource.MANUAL));
        verify(persistenceService, never()).markLatestStale(any());
    }

    @Test
    @DisplayName("From latest dengue week: history ends on that week, target is the following week")
    void derivesTargetFromLastDengueWeek() {
        LocalDate lastDengueWeek = LocalDate.of(2026, 8, 3);
        LocalDate expectedTarget = LocalDate.of(2026, 8, 10);
        when(districtRepo.findByRdhsModelId(RDHS_ID)).thenReturn(Optional.of(colombo()));
        when(dengueCaseRecordRepo.findLatestWeekStartDateByDistrictId(DISTRICT_ID))
                .thenReturn(Optional.of(lastDengueWeek));
        when(featureAssemblyService.assembleFeatures(DISTRICT_ID, expectedTarget)).thenReturn(
                new ForecastRequestDTO(RDHS_ID, "Colombo", expectedTarget,
                        new ForecastRequestDTO.StaticFeatures(0, 0, 1, 3392.0), List.of()));
        when(forecastApiClient.forecast(any())).thenReturn(response());
        DistrictForecast saved = DistrictForecast.builder().id(1L).rdhsId(RDHS_ID).build();
        when(persistenceService.upsert(eq(RDHS_ID), eq("Colombo"), eq(expectedTarget),
                anyList(), anyList(), anyList(), eq("lstm-v1"), eq(GenerationSource.MANUAL)))
                .thenReturn(saved);

        ForecastGenerationResult result = service.generateForecast(RDHS_ID, GenerationSource.MANUAL);

        assertThat(result.isSuccess()).isTrue();
        verify(featureAssemblyService).assembleFeatures(DISTRICT_ID, expectedTarget);
        verify(persistenceService).upsert(eq(RDHS_ID), eq("Colombo"), eq(expectedTarget),
                anyList(), anyList(), anyList(), eq("lstm-v1"), eq(GenerationSource.MANUAL));
    }

    @Test
    @DisplayName("No dengue records for the district → INCOMPLETE_HISTORY, FastAPI not called")
    void noDengueRecords() {
        when(districtRepo.findByRdhsModelId(RDHS_ID)).thenReturn(Optional.of(colombo()));
        when(dengueCaseRecordRepo.findLatestWeekStartDateByDistrictId(DISTRICT_ID))
                .thenReturn(Optional.empty());
        when(persistenceService.markLatestStale(RDHS_ID)).thenReturn(Optional.empty());

        ForecastGenerationResult result = service.generateForecast(RDHS_ID, GenerationSource.MANUAL);

        assertThat(result.outcome()).isEqualTo(ForecastGenerationResult.Outcome.INCOMPLETE_HISTORY);
        assertThat(result.message()).contains("No dengue case records");
        verify(forecastApiClient, never()).forecast(any());
        verify(featureAssemblyService, never()).assembleFeatures(any(), any());
    }

    @Test
    @DisplayName("Incomplete history → no upsert, latest row marked STALE, INCOMPLETE_HISTORY returned")
    void incompleteHistory_marksStale() {
        when(districtRepo.findByRdhsModelId(RDHS_ID)).thenReturn(Optional.of(colombo()));
        when(featureAssemblyService.assembleFeatures(DISTRICT_ID, TARGET))
                .thenThrow(new InsufficientHistoryException("Only 6 of 8 weeks available [2026-07-13 to 2026-08-31]"));
        DistrictForecast stale = DistrictForecast.builder()
                .id(9L).rdhsId(RDHS_ID).status(ForecastStatus.STALE).build();
        when(persistenceService.markLatestStale(RDHS_ID)).thenReturn(Optional.of(stale));

        ForecastGenerationResult result = service.generateForecast(RDHS_ID, TARGET, GenerationSource.SCHEDULED);

        assertThat(result.outcome()).isEqualTo(ForecastGenerationResult.Outcome.INCOMPLETE_HISTORY);
        assertThat(result.forecast()).isSameAs(stale);
        assertThat(result.message()).contains("8 weeks");
        verify(persistenceService).markLatestStale(RDHS_ID);
        verify(persistenceService, never()).upsert(any(), anyString(), any(),
                anyList(), anyList(), anyList(), anyString(), any());
        verify(forecastApiClient, never()).forecast(any());
    }

    @Test
    @DisplayName("Incomplete history with no prior forecast → INCOMPLETE_HISTORY, no row")
    void incompleteHistory_noPriorRow() {
        when(districtRepo.findByRdhsModelId(RDHS_ID)).thenReturn(Optional.of(colombo()));
        when(featureAssemblyService.assembleFeatures(DISTRICT_ID, TARGET))
                .thenThrow(new InsufficientHistoryException("no weeks"));
        when(persistenceService.markLatestStale(RDHS_ID)).thenReturn(Optional.empty());

        ForecastGenerationResult result = service.generateForecast(RDHS_ID, TARGET, GenerationSource.MANUAL);

        assertThat(result.outcome()).isEqualTo(ForecastGenerationResult.Outcome.INCOMPLETE_HISTORY);
        assertThat(result.forecast()).isNull();
    }

    @Test
    @DisplayName("FastAPI HTTP/unreachable failure → UPSTREAM_ERROR, nothing persisted")
    void upstreamError_notPersisted() {
        when(districtRepo.findByRdhsModelId(RDHS_ID)).thenReturn(Optional.of(colombo()));
        when(featureAssemblyService.assembleFeatures(DISTRICT_ID, TARGET)).thenReturn(payload());
        when(forecastApiClient.forecast(any()))
                .thenThrow(new AiServiceException("Forecast service returned HTTP 500"));

        ForecastGenerationResult result = service.generateForecast(RDHS_ID, TARGET, GenerationSource.SCHEDULED);

        assertThat(result.outcome()).isEqualTo(ForecastGenerationResult.Outcome.UPSTREAM_ERROR);
        verify(persistenceService, never()).upsert(any(), anyString(), any(),
                anyList(), anyList(), anyList(), anyString(), any());
    }

    @Test
    @DisplayName("FastAPI timeout → UPSTREAM_TIMEOUT")
    void upstreamTimeout() {
        when(districtRepo.findByRdhsModelId(RDHS_ID)).thenReturn(Optional.of(colombo()));
        when(featureAssemblyService.assembleFeatures(DISTRICT_ID, TARGET)).thenReturn(payload());
        when(forecastApiClient.forecast(any()))
                .thenThrow(new ForecastTimeoutException("timed out", null));

        ForecastGenerationResult result = service.generateForecast(RDHS_ID, TARGET, GenerationSource.SCHEDULED);

        assertThat(result.outcome()).isEqualTo(ForecastGenerationResult.Outcome.UPSTREAM_TIMEOUT);
    }

    @Test
    @DisplayName("Optimistic-lock clash then success → retried once, GENERATED")
    void optimisticLock_retriesOnce() {
        when(districtRepo.findByRdhsModelId(RDHS_ID)).thenReturn(Optional.of(colombo()));
        when(featureAssemblyService.assembleFeatures(DISTRICT_ID, TARGET)).thenReturn(payload());
        when(forecastApiClient.forecast(any())).thenReturn(response());
        DistrictForecast saved = DistrictForecast.builder().id(1L).build();
        when(persistenceService.upsert(any(), anyString(), any(), anyList(), anyList(), anyList(), anyString(), any()))
                .thenThrow(new ObjectOptimisticLockingFailureException("clash", null))
                .thenReturn(saved);

        ForecastGenerationResult result = service.generateForecast(RDHS_ID, TARGET, GenerationSource.SCHEDULED);

        assertThat(result.isSuccess()).isTrue();
        verify(persistenceService, times(2)).upsert(any(), anyString(), any(),
                anyList(), anyList(), anyList(), anyString(), any());
    }

    @Test
    @DisplayName("Optimistic-lock clash twice → CONFLICT")
    void optimisticLock_failsTwice() {
        when(districtRepo.findByRdhsModelId(RDHS_ID)).thenReturn(Optional.of(colombo()));
        when(featureAssemblyService.assembleFeatures(DISTRICT_ID, TARGET)).thenReturn(payload());
        when(forecastApiClient.forecast(any())).thenReturn(response());
        when(persistenceService.upsert(any(), anyString(), any(), anyList(), anyList(), anyList(), anyString(), any()))
                .thenThrow(new ObjectOptimisticLockingFailureException("clash", null));

        ForecastGenerationResult result = service.generateForecast(RDHS_ID, TARGET, GenerationSource.SCHEDULED);

        assertThat(result.outcome()).isEqualTo(ForecastGenerationResult.Outcome.CONFLICT);
        verify(persistenceService, times(2)).upsert(any(), anyString(), any(),
                anyList(), anyList(), anyList(), anyString(), any());
    }

    @Test
    @DisplayName("Unknown rdhsId → NotFoundException")
    void unknownDistrict_throws() {
        when(districtRepo.findByRdhsModelId(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.generateForecast(99, TARGET, GenerationSource.MANUAL))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("getLatestForecast → returns row when present, 404 when absent")
    void getLatestForecast() {
        DistrictForecast row = DistrictForecast.builder().id(3L).rdhsId(RDHS_ID).build();
        when(forecastRepo.findTopByRdhsIdOrderByTargetWeekStartDesc(RDHS_ID))
                .thenReturn(Optional.of(row));
        assertThat(service.getLatestForecast(RDHS_ID)).isSameAs(row);

        when(forecastRepo.findTopByRdhsIdOrderByTargetWeekStartDesc(7))
                .thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.getLatestForecast(7))
                .isInstanceOf(NotFoundException.class);
    }
}
