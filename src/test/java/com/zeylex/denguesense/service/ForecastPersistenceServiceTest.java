package com.zeylex.denguesense.service;

import com.zeylex.denguesense.model.DistrictForecast;
import com.zeylex.denguesense.model.enums.ForecastStatus;
import com.zeylex.denguesense.model.enums.GenerationSource;
import com.zeylex.denguesense.repo.DistrictForecastRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ForecastPersistenceService — idempotent upsert + STALE marking")
class ForecastPersistenceServiceTest {

    @Mock private DistrictForecastRepo forecastRepo;

    private ForecastPersistenceService service;

    private static final Integer RDHS_ID = 4;                   // Colombo
    private static final LocalDate TARGET = LocalDate.of(2026, 9, 7);

    @BeforeEach
    void setUp() {
        service = new ForecastPersistenceService(forecastRepo);
    }

    @Test
    @DisplayName("Upserting twice for the same (rdhsId, week) updates one row with the latest values")
    void upsertTwice_updatesSameRow() {
        // Stateful fake: the repo holds at most one row for this key.
        AtomicReference<DistrictForecast> store = new AtomicReference<>();
        when(forecastRepo.findByRdhsIdAndTargetWeekStart(RDHS_ID, TARGET))
                .thenAnswer(inv -> Optional.ofNullable(store.get()));
        when(forecastRepo.save(any(DistrictForecast.class))).thenAnswer(inv -> {
            DistrictForecast f = inv.getArgument(0);
            if (f.getId() == null) {
                f.setId(1L); // simulate IDENTITY on first insert
            }
            store.set(f);
            return f;
        });

        DistrictForecast first = service.upsert(RDHS_ID, "Colombo", TARGET,
                List.of(10.0, 11.0, 12.0, 13.0), List.of(8.0, 9.0, 10.0, 11.0),
                List.of(12.0, 13.0, 14.0, 15.0), "lstm-v1", GenerationSource.SCHEDULED);

        DistrictForecast second = service.upsert(RDHS_ID, "Colombo", TARGET,
                List.of(20.0, 21.0, 22.0, 23.0), List.of(18.0, 19.0, 20.0, 21.0),
                List.of(22.0, 23.0, 24.0, 25.0), "lstm-v1", GenerationSource.MANUAL);

        // Same logical row (same id), no duplicate created.
        assertThat(first.getId()).isEqualTo(1L);
        assertThat(second.getId()).isEqualTo(1L);
        assertThat(store.get().getPredictions()).containsExactly(20.0, 21.0, 22.0, 23.0);
        assertThat(store.get().getGenerationSource()).isEqualTo(GenerationSource.MANUAL);
        assertThat(store.get().getStatus()).isEqualTo(ForecastStatus.GENERATED);
        verify(forecastRepo, times(2)).save(any(DistrictForecast.class));
    }

    @Test
    @DisplayName("markLatestStale flags the most recent row STALE without altering its predictions")
    void markLatestStale_updatesStatusOnly() {
        DistrictForecast latest = DistrictForecast.builder()
                .id(7L).rdhsId(RDHS_ID).districtName("Colombo").targetWeekStart(TARGET)
                .predictions(List.of(5.0, 6.0, 7.0, 8.0))
                .lowerBounds(List.of(4.0, 5.0, 6.0, 7.0))
                .upperBounds(List.of(6.0, 7.0, 8.0, 9.0))
                .modelVersion("lstm-v1").status(ForecastStatus.GENERATED)
                .generationSource(GenerationSource.SCHEDULED)
                .build();
        when(forecastRepo.findTopByRdhsIdOrderByTargetWeekStartDesc(RDHS_ID))
                .thenReturn(Optional.of(latest));

        Optional<DistrictForecast> result = service.markLatestStale(RDHS_ID);

        assertThat(result).containsSame(latest);
        assertThat(latest.getStatus()).isEqualTo(ForecastStatus.STALE);
        assertThat(latest.getPredictions()).containsExactly(5.0, 6.0, 7.0, 8.0);
        verify(forecastRepo).save(latest);
    }

    @Test
    @DisplayName("markLatestStale is a no-op when the district has no prior forecast")
    void markLatestStale_noPriorRow() {
        when(forecastRepo.findTopByRdhsIdOrderByTargetWeekStartDesc(RDHS_ID))
                .thenReturn(Optional.empty());

        Optional<DistrictForecast> result = service.markLatestStale(RDHS_ID);

        assertThat(result).isEmpty();
        verify(forecastRepo, times(0)).save(any());
    }
}
