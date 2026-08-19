package com.zeylex.denguesense.service.impl;

import com.zeylex.denguesense.exception.WeatherFetchException;
import com.zeylex.denguesense.model.District;
import com.zeylex.denguesense.model.WeatherRecord;
import com.zeylex.denguesense.repo.WeatherRecordRepo;
import com.zeylex.denguesense.service.OpenMeteoClient;
import com.zeylex.denguesense.service.WeatherFetchService;
import com.zeylex.denguesense.weather.DailyWeatherObservation;
import com.zeylex.denguesense.weather.DistrictWeatherCoordinates;
import com.zeylex.denguesense.weather.DistrictWeekWindow;
import com.zeylex.denguesense.weather.LatLon;
import com.zeylex.denguesense.weather.WeeklyWeatherAggregator;
import com.zeylex.denguesense.weather.WeeklyWeatherStats;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class WeatherFetchServiceImpl implements WeatherFetchService {

    private static final Logger log = LoggerFactory.getLogger(WeatherFetchServiceImpl.class);
    private static final ZoneId COLOMBO = ZoneId.of("Asia/Colombo");

    private final OpenMeteoClient openMeteoClient;
    private final WeatherRecordRepo weatherRecordRepo;
    private final int bufferDays;
    private final long interDistrictDelayMs;

    public WeatherFetchServiceImpl(OpenMeteoClient openMeteoClient,
                                   WeatherRecordRepo weatherRecordRepo,
                                   @Value("${open-meteo.buffer-days:5}") int bufferDays,
                                   @Value("${open-meteo.inter-district-delay-ms:1200}") long interDistrictDelayMs) {
        this.openMeteoClient = openMeteoClient;
        this.weatherRecordRepo = weatherRecordRepo;
        this.bufferDays = bufferDays;
        this.interDistrictDelayMs = interDistrictDelayMs;
    }

    @Override
    public WeeklyWeatherStats fetchWeeklyAggregates(District district, LocalDate weekStartDate, LocalDate weekEndDate) {
        LatLon coords = DistrictWeatherCoordinates.requireFor(district);
        LocalDate fetchStart = weekStartDate.minusDays(bufferDays);
        LocalDate fetchEnd = clampToAvailableArchiveEnd(weekEndDate.plusDays(bufferDays));
        if (fetchEnd.isBefore(fetchStart)) {
            fetchStart = fetchEnd;
        }

        log.info("Fetching Open-Meteo daily weather for district='{}' week=[{} to {}] apiRange=[{} to {}]",
                district.getName(), weekStartDate, weekEndDate, fetchStart, fetchEnd);

        List<DailyWeatherObservation> daily = openMeteoClient.fetchDaily(coords, fetchStart, fetchEnd);
        try {
            return WeeklyWeatherAggregator.aggregate(daily, weekStartDate, weekEndDate);
        } catch (IllegalArgumentException ex) {
            throw new WeatherFetchException(ex.getMessage(), ex);
        }
    }

    @Override
    public WeatherRecord upsertWeeklyWeather(District district, LocalDate weekStartDate, LocalDate weekEndDate) {
        WeeklyWeatherStats stats = fetchWeeklyAggregates(district, weekStartDate, weekEndDate);
        return persist(district, weekStartDate, weekEndDate, stats);
    }

    @Override
    public int upsertWeeklyWeather(List<DistrictWeekWindow> weeks, List<String> errors) {
        if (weeks == null || weeks.isEmpty()) {
            return 0;
        }

        Map<Long, List<DistrictWeekWindow>> byDistrict = new LinkedHashMap<>();
        for (DistrictWeekWindow week : weeks) {
            if (week == null || week.district() == null) {
                continue;
            }
            byDistrict.computeIfAbsent(week.district().getId(), id -> new ArrayList<>()).add(week);
        }

        int saved = 0;
        boolean first = true;
        for (List<DistrictWeekWindow> districtWeeks : byDistrict.values()) {
            if (!first) {
                pauseBetweenDistricts();
            }
            first = false;
            District district = districtWeeks.getFirst().district();
            try {
                saved += upsertDistrictWeeks(district, districtWeeks);
            } catch (RuntimeException ex) {
                log.warn("Weather fetch failed for district='{}': {}", district.getName(), ex.getMessage());
                if (errors != null) {
                    errors.add("Weather for " + district.getName() + ": " + ex.getMessage());
                }
            }
        }
        return saved;
    }

    private int upsertDistrictWeeks(District district, List<DistrictWeekWindow> weeks) {
        LocalDate minStart = weeks.stream().map(DistrictWeekWindow::weekStartDate).min(LocalDate::compareTo).orElseThrow();
        LocalDate maxEnd = weeks.stream().map(DistrictWeekWindow::weekEndDate).max(LocalDate::compareTo).orElseThrow();

        LatLon coords = DistrictWeatherCoordinates.requireFor(district);
        LocalDate fetchStart = minStart.minusDays(bufferDays);
        LocalDate fetchEnd = clampToAvailableArchiveEnd(maxEnd.plusDays(bufferDays));
        if (fetchEnd.isBefore(fetchStart)) {
            fetchStart = fetchEnd;
        }

        log.info("Fetching Open-Meteo daily weather for district='{}' covering {} week(s) apiRange=[{} to {}]",
                district.getName(), weeks.size(), fetchStart, fetchEnd);

        List<DailyWeatherObservation> daily = openMeteoClient.fetchDaily(coords, fetchStart, fetchEnd);
        int saved = 0;
        for (DistrictWeekWindow week : weeks) {
            try {
                WeeklyWeatherStats stats = WeeklyWeatherAggregator.aggregate(
                        daily, week.weekStartDate(), week.weekEndDate());
                persist(district, week.weekStartDate(), week.weekEndDate(), stats);
                saved++;
            } catch (IllegalArgumentException ex) {
                throw new WeatherFetchException(
                        "Could not aggregate weather for " + district.getName()
                                + " week " + week.weekStartDate() + ": " + ex.getMessage(), ex);
            }
        }
        return saved;
    }

    private WeatherRecord persist(District district, LocalDate weekStartDate, LocalDate weekEndDate,
                                  WeeklyWeatherStats stats) {
        WeatherRecord record = weatherRecordRepo
                .findByDistrict_IdAndWeekStartDate(district.getId(), weekStartDate)
                .orElseGet(() -> WeatherRecord.builder()
                        .district(district)
                        .weekStartDate(weekStartDate)
                        .build());
        record.setWeekEndDate(weekEndDate);
        record.setTempMean(stats.tempMean());
        record.setTempMax(stats.tempMax());
        record.setTempMin(stats.tempMin());
        record.setRainfallMm(stats.rainfallMm());
        record.setHumidityPct(stats.humidityPct());
        WeatherRecord saved = weatherRecordRepo.save(record);
        log.info("Saved weather for district='{}' weekStart={}: tempMean={}, rainfallMm={}, humidityPct={}",
                district.getName(), weekStartDate, stats.tempMean(), stats.rainfallMm(), stats.humidityPct());
        return saved;
    }

    private LocalDate clampToAvailableArchiveEnd(LocalDate requestedEnd) {
        LocalDate today = LocalDate.now(COLOMBO);
        return requestedEnd.isAfter(today) ? today : requestedEnd;
    }

    private void pauseBetweenDistricts() {
        if (interDistrictDelayMs <= 0) {
            return;
        }
        try {
            Thread.sleep(interDistrictDelayMs);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new WeatherFetchException("Interrupted while waiting between Open-Meteo district requests", ex);
        }
    }
}
