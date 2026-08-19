package com.zeylex.denguesense.service;

import com.zeylex.denguesense.model.BSDSWeekly;
import com.zeylex.denguesense.model.District;
import com.zeylex.denguesense.model.enums.ReportStatus;
import com.zeylex.denguesense.model.enums.RiskLabel;
import com.zeylex.denguesense.repo.BSDSWeeklyRepo;
import com.zeylex.denguesense.repo.ReportRepo;
import com.zeylex.denguesense.service.impl.BsdsWeeklyServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("BsdsWeeklyService — reports in week window → BSDS")
class BsdsWeeklyServiceImplTest {

    @Mock private ReportRepo reportRepo;
    @Mock private BSDSWeeklyRepo bsdsWeeklyRepo;

    private BsdsWeeklyServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new BsdsWeeklyServiceImpl(reportRepo, bsdsWeeklyRepo);
    }

    @Test
    @DisplayName("counts reports between week start (inclusive) and week end (inclusive) and saves BSDS")
    void upsertWeeklyBsds_usesWeekDateRange() {
        District colombo = new District();
        colombo.setId(1L);
        colombo.setName("Colombo");
        LocalDate start = LocalDate.of(2026, 8, 17);
        LocalDate end = LocalDate.of(2026, 8, 23);
        LocalDateTime from = start.atStartOfDay();
        LocalDateTime toExclusive = end.plusDays(1).atStartOfDay();

        when(reportRepo.countByDistrict_IdAndSubmittedAtGreaterThanEqualAndSubmittedAtLessThan(1L, from, toExclusive))
                .thenReturn(12L);
        when(reportRepo.countConfirmedBreedingSites(
                eq(1L), eq(from), eq(toExclusive), any(), eq(RiskLabel.HIGH_RISK)))
                .thenReturn(10L);
        when(bsdsWeeklyRepo.findByDistrict_IdAndWeekStartDate(1L, start)).thenReturn(Optional.empty());
        when(bsdsWeeklyRepo.save(any(BSDSWeekly.class))).thenAnswer(invocation -> {
            BSDSWeekly record = invocation.getArgument(0);
            record.setId(8L);
            return record;
        });

        BSDSWeekly saved = service.upsertWeeklyBsds(colombo, start, end);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Collection<ReportStatus>> statuses = ArgumentCaptor.forClass(Collection.class);
        verify(reportRepo).countConfirmedBreedingSites(
                eq(1L), eq(from), eq(toExclusive), statuses.capture(), eq(RiskLabel.HIGH_RISK));
        assertThat(statuses.getValue()).containsExactlyInAnyOrderElementsOf(Set.of(
                ReportStatus.CLASSIFIED, ReportStatus.DISPATCHED, ReportStatus.RESOLVED));

        assertThat(saved.getReportCount()).isEqualTo(12);
        assertThat(saved.getBsdsScore()).isEqualTo(10.0 * 100_000.0 / 2_371_000.0);
        assertThat(saved.getWeekStartDate()).isEqualTo(start);
        assertThat(saved.getWeekEndDate()).isEqualTo(end);
    }

    @Test
    @DisplayName("saves BSDS of 0 when the week has no citizen reports")
    void upsertWeeklyBsds_zeroReports() {
        District colombo = new District();
        colombo.setId(1L);
        colombo.setName("Colombo");
        LocalDate start = LocalDate.of(2026, 8, 17);
        LocalDate end = LocalDate.of(2026, 8, 23);

        when(reportRepo.countByDistrict_IdAndSubmittedAtGreaterThanEqualAndSubmittedAtLessThan(
                eq(1L), any(), any())).thenReturn(0L);
        when(reportRepo.countConfirmedBreedingSites(eq(1L), any(), any(), any(), eq(RiskLabel.HIGH_RISK)))
                .thenReturn(0L);
        when(bsdsWeeklyRepo.findByDistrict_IdAndWeekStartDate(1L, start)).thenReturn(Optional.empty());
        when(bsdsWeeklyRepo.save(any(BSDSWeekly.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BSDSWeekly saved = service.upsertWeeklyBsds(colombo, start, end);

        assertThat(saved.getReportCount()).isZero();
        assertThat(saved.getBsdsScore()).isZero();
    }
}
