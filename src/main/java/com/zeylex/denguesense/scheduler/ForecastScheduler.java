package com.zeylex.denguesense.scheduler;

import com.zeylex.denguesense.model.District;
import com.zeylex.denguesense.model.enums.GenerationSource;
import com.zeylex.denguesense.repo.DistrictRepo;
import com.zeylex.denguesense.service.ForecastGenerationResult;
import com.zeylex.denguesense.service.ForecastOrchestrationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ForecastScheduler {

    private static final Logger log = LoggerFactory.getLogger(ForecastScheduler.class);

    private final ForecastOrchestrationService orchestrationService;
    private final DistrictRepo districtRepo;

    public ForecastScheduler(ForecastOrchestrationService orchestrationService, DistrictRepo districtRepo) {
        this.orchestrationService = orchestrationService;
        this.districtRepo = districtRepo;
    }
    @Scheduled(
            cron = "${forecast.scheduler.cron:0 0 2 * * MON}",
            zone = "${forecast.scheduler.zone:Asia/Colombo}")
    public void generateWeeklyForecasts() {
        List<District> districts = districtRepo.findByRdhsModelIdNotNullOrderByRdhsModelIdAsc();
        log.info("Weekly forecast run starting: {} district(s), target = week after each district's latest dengue record",
                districts.size());

        int generated = 0;
        int incompleteHistory = 0;
        int upstreamFailed = 0;
        int unexpectedFailed = 0;

        for (District district : districts) {
            Integer rdhsId = district.getRdhsModelId();
            try {
                ForecastGenerationResult result = orchestrationService.generateForecast(
                        rdhsId, GenerationSource.SCHEDULED);
                switch (result.outcome()) {
                    case GENERATED -> generated++;
                    case INCOMPLETE_HISTORY -> incompleteHistory++;
                    case UPSTREAM_TIMEOUT, UPSTREAM_ERROR, CONFLICT -> {
                        upstreamFailed++;
                        log.warn("Forecast not generated for rdhsId={} district='{}': {} — {}",
                                rdhsId, district.getName(), result.outcome(), result.message());
                    }
                }
            } catch (Exception e) {
                // Backstop: one district's unexpected failure must never abort the batch.
                unexpectedFailed++;
                log.error("Unexpected forecast failure for rdhsId={} district='{}'",
                        rdhsId, district.getName(), e);
            }
        }

        log.info("Weekly forecast run complete: generated={}, staleKept(incompleteHistory)={}, "
                        + "upstreamFailed={}, unexpectedFailed={}",
                generated, incompleteHistory, upstreamFailed, unexpectedFailed);
    }
}
