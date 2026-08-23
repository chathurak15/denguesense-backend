package com.zeylex.denguesense.service.impl;

import com.zeylex.denguesense.service.BsdsWeeklyService;
import com.zeylex.denguesense.service.WeatherFetchService;
import com.zeylex.denguesense.service.WeeklyCaseEnrichmentService;
import com.zeylex.denguesense.weather.DistrictWeekWindow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class WeeklyCaseEnrichmentServiceImpl implements WeeklyCaseEnrichmentService {

    private static final Logger log = LoggerFactory.getLogger(WeeklyCaseEnrichmentServiceImpl.class);

    private final WeatherFetchService weatherFetchService;
    private final BsdsWeeklyService bsdsWeeklyService;

    public WeeklyCaseEnrichmentServiceImpl(WeatherFetchService weatherFetchService,
                                           BsdsWeeklyService bsdsWeeklyService) {
        this.weatherFetchService = weatherFetchService;
        this.bsdsWeeklyService = bsdsWeeklyService;
    }

    @Override
    @Async("taskExecutor")
    public void enrichImportedWeeks(List<DistrictWeekWindow> weeks) {
        if (weeks == null || weeks.isEmpty()) {
            return;
        }
        List<String> errors = new ArrayList<>();
        try {
            int weatherImported = weatherFetchService.upsertWeeklyWeather(weeks, errors);
            log.info("Background weather enrichment saved {} week(s)", weatherImported);
        } catch (Exception ex) {
            log.error("Background weather enrichment failed: {}", ex.getMessage(), ex);
        }
        try {
            int bsdsImported = bsdsWeeklyService.upsertWeeklyBsds(weeks, errors);
            log.info("Background BSDS enrichment saved {} week(s)", bsdsImported);
        } catch (Exception ex) {
            log.error("Background BSDS enrichment failed: {}", ex.getMessage(), ex);
        }
        if (!errors.isEmpty()) {
            log.warn("Weekly case enrichment finished with {} issue(s): {}", errors.size(), errors);
        }
    }
}
