package com.zeylex.denguesense.service.impl;

import com.zeylex.denguesense.bsds.BsdsCalculator;
import com.zeylex.denguesense.bsds.DistrictPopulations;
import com.zeylex.denguesense.model.BSDSWeekly;
import com.zeylex.denguesense.model.District;
import com.zeylex.denguesense.model.enums.ReportStatus;
import com.zeylex.denguesense.model.enums.RiskLabel;
import com.zeylex.denguesense.repo.BSDSWeeklyRepo;
import com.zeylex.denguesense.repo.ReportRepo;
import com.zeylex.denguesense.service.BsdsWeeklyService;
import com.zeylex.denguesense.weather.DistrictWeekWindow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Service
public class BsdsWeeklyServiceImpl implements BsdsWeeklyService {

    private static final Logger log = LoggerFactory.getLogger(BsdsWeeklyServiceImpl.class);
    private static final Set<ReportStatus> CONFIRMED_STATUSES = Set.of(
            ReportStatus.CLASSIFIED,
            ReportStatus.DISPATCHED,
            ReportStatus.RESOLVED
    );

    private final ReportRepo reportRepo;
    private final BSDSWeeklyRepo bsdsWeeklyRepo;

    public BsdsWeeklyServiceImpl(ReportRepo reportRepo, BSDSWeeklyRepo bsdsWeeklyRepo) {
        this.reportRepo = reportRepo;
        this.bsdsWeeklyRepo = bsdsWeeklyRepo;
    }

    @Override
    public BSDSWeekly upsertWeeklyBsds(District district, LocalDate weekStartDate, LocalDate weekEndDate) {
        LocalDateTime from = weekStartDate.atStartOfDay();
        LocalDateTime toExclusive = weekEndDate.plusDays(1).atStartOfDay();

        int reportCount = Math.toIntExact(reportRepo
                .countByDistrict_IdAndSubmittedAtGreaterThanEqualAndSubmittedAtLessThan(
                        district.getId(), from, toExclusive));
        long confirmedSites = reportRepo.countConfirmedBreedingSites(
                district.getId(), from, toExclusive, CONFIRMED_STATUSES, RiskLabel.HIGH_RISK);

        double population = DistrictPopulations.requireFor(district);
        double score = BsdsCalculator.score(confirmedSites, population);

        BSDSWeekly record = persist(district, weekStartDate, weekEndDate, score, reportCount);
        log.info("Saved BSDS for district='{}' week=[{} to {}]: reports={}, confirmedHighRisk={}, score={}",
                district.getName(), weekStartDate, weekEndDate, reportCount, confirmedSites, score);
        return record;
    }

    @Override
    public int upsertWeeklyBsds(List<DistrictWeekWindow> weeks, List<String> errors) {
        if (weeks == null || weeks.isEmpty()) {
            return 0;
        }
        int saved = 0;
        for (DistrictWeekWindow week : weeks) {
            if (week == null || week.district() == null) {
                continue;
            }
            try {
                upsertWeeklyBsds(week.district(), week.weekStartDate(), week.weekEndDate());
                saved++;
            } catch (RuntimeException ex) {
                log.warn("BSDS calculation failed for district='{}' weekStart={}: {}",
                        week.district().getName(), week.weekStartDate(), ex.getMessage());
                if (errors != null) {
                    errors.add("BSDS for " + week.district().getName()
                            + " week " + week.weekStartDate() + ": " + ex.getMessage());
                }
            }
        }
        return saved;
    }

    private BSDSWeekly persist(District district, LocalDate weekStartDate, LocalDate weekEndDate,
                               double score, int reportCount) {
        BSDSWeekly record = bsdsWeeklyRepo
                .findByDistrict_IdAndWeekStartDate(district.getId(), weekStartDate)
                .orElseGet(() -> BSDSWeekly.builder()
                        .district(district)
                        .weekStartDate(weekStartDate)
                        .build());
        record.setWeekEndDate(weekEndDate);
        record.setBsdsScore(score);
        record.setReportCount(reportCount);
        return bsdsWeeklyRepo.save(record);
    }
}
