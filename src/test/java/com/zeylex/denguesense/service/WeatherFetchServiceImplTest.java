package com.zeylex.denguesense.service;

import com.zeylex.denguesense.model.District;
import com.zeylex.denguesense.model.WeatherRecord;
import com.zeylex.denguesense.repo.WeatherRecordRepo;
import com.zeylex.denguesense.service.impl.WeatherFetchServiceImpl;
import com.zeylex.denguesense.weather.DailyWeatherObservation;
import com.zeylex.denguesense.weather.LatLon;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("WeatherFetchService — Open-Meteo week window + persist")
class WeatherFetchServiceImplTest {

    @Mock private OpenMeteoClient openMeteoClient;
    @Mock private WeatherRecordRepo weatherRecordRepo;

    private WeatherFetchServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new WeatherFetchServiceImpl(openMeteoClient, weatherRecordRepo, 5, 0);
    }

    @Test
    @DisplayName("calls Open-Meteo with a 5-day buffer and saves weekly aggregates")
    void upsertWeeklyWeather_buffersDatesAndPersists() {
        District colombo = new District();
        colombo.setId(1L);
        colombo.setName("Colombo");
        LocalDate start = LocalDate.of(2026, 1, 5);
        LocalDate end = LocalDate.of(2026, 1, 11);

        when(openMeteoClient.fetchDaily(any(LatLon.class), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of(
                        new DailyWeatherObservation(start, 28.0, 31.0, 25.0, 2.0, 80.0),
                        new DailyWeatherObservation(start.plusDays(1), 30.0, 33.0, 26.0, 4.0, 82.0)
                ));
        when(weatherRecordRepo.findByDistrict_IdAndWeekStartDate(1L, start)).thenReturn(Optional.empty());
        when(weatherRecordRepo.save(any(WeatherRecord.class))).thenAnswer(invocation -> {
            WeatherRecord record = invocation.getArgument(0);
            record.setId(7L);
            return record;
        });

        WeatherRecord saved = service.upsertWeeklyWeather(colombo, start, end);

        ArgumentCaptor<LatLon> coords = ArgumentCaptor.forClass(LatLon.class);
        ArgumentCaptor<LocalDate> apiStart = ArgumentCaptor.forClass(LocalDate.class);
        ArgumentCaptor<LocalDate> apiEnd = ArgumentCaptor.forClass(LocalDate.class);
        verify(openMeteoClient).fetchDaily(coords.capture(), apiStart.capture(), apiEnd.capture());

        assertThat(coords.getValue().latitude()).isEqualTo(6.9271);
        assertThat(coords.getValue().longitude()).isEqualTo(79.8612);
        assertThat(apiStart.getValue()).isEqualTo(start.minusDays(5));
        assertThat(apiEnd.getValue()).isEqualTo(end.plusDays(5));

        assertThat(saved.getTempMean()).isEqualTo(29.0);
        assertThat(saved.getTempMax()).isEqualTo(33.0);
        assertThat(saved.getTempMin()).isEqualTo(25.0);
        assertThat(saved.getRainfallMm()).isEqualTo(6.0);
        assertThat(saved.getHumidityPct()).isEqualTo(81.0);
        assertThat(saved.getWeekStartDate()).isEqualTo(start);
        assertThat(saved.getWeekEndDate()).isEqualTo(end);
    }
}
