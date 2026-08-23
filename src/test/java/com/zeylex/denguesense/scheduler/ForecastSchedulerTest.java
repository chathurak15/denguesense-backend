package com.zeylex.denguesense.scheduler;

import com.zeylex.denguesense.model.District;
import com.zeylex.denguesense.model.DistrictForecast;
import com.zeylex.denguesense.model.enums.GenerationSource;
import com.zeylex.denguesense.repo.DistrictRepo;
import com.zeylex.denguesense.service.ForecastGenerationResult;
import com.zeylex.denguesense.service.ForecastOrchestrationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ForecastScheduler — per-district failure isolation")
class ForecastSchedulerTest {

    @Mock private ForecastOrchestrationService orchestrationService;
    @Mock private DistrictRepo districtRepo;

    private ForecastScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new ForecastScheduler(orchestrationService, districtRepo);
    }

    private District district(long id, int rdhsId, String name) {
        District d = new District();
        d.setId(id);
        d.setRdhsModelId(rdhsId);
        d.setName(name);
        return d;
    }

    @Test
    @DisplayName("One district throwing does not abort the batch — every district is still attempted")
    void oneFailure_doesNotAbortBatch() {
        District ampara = district(1L, 0, "Ampara");
        District colombo = district(2L, 4, "Colombo");
        District kalmunai = district(3L, 9, "Kalmunai");
        when(districtRepo.findByRdhsModelIdNotNullOrderByRdhsModelIdAsc())
                .thenReturn(List.of(ampara, colombo, kalmunai));

        // Colombo blows up unexpectedly; the other two succeed.
        when(orchestrationService.generateForecast(eq(0), eq(GenerationSource.SCHEDULED)))
                .thenReturn(ForecastGenerationResult.generated(DistrictForecast.builder().id(1L).build()));
        when(orchestrationService.generateForecast(eq(4), eq(GenerationSource.SCHEDULED)))
                .thenThrow(new RuntimeException("boom"));
        when(orchestrationService.generateForecast(eq(9), eq(GenerationSource.SCHEDULED)))
                .thenReturn(ForecastGenerationResult.generated(DistrictForecast.builder().id(2L).build()));

        scheduler.generateWeeklyForecasts();

        verify(orchestrationService, times(1)).generateForecast(eq(0), eq(GenerationSource.SCHEDULED));
        verify(orchestrationService, times(1)).generateForecast(eq(4), eq(GenerationSource.SCHEDULED));
        verify(orchestrationService, times(1)).generateForecast(eq(9), eq(GenerationSource.SCHEDULED));
    }

    @Test
    @DisplayName("All triggers use GenerationSource.SCHEDULED")
    void usesScheduledSource() {
        when(districtRepo.findByRdhsModelIdNotNullOrderByRdhsModelIdAsc())
                .thenReturn(List.of(district(2L, 4, "Colombo")));
        when(orchestrationService.generateForecast(eq(4), eq(GenerationSource.SCHEDULED)))
                .thenReturn(ForecastGenerationResult.incompleteHistory(null, "no history"));

        scheduler.generateWeeklyForecasts();

        verify(orchestrationService).generateForecast(eq(4), eq(GenerationSource.SCHEDULED));
    }
}
