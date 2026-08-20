package com.zeylex.denguesense.service;

import com.zeylex.denguesense.dto.responseDTO.DengueCaseSummaryDTO;
import com.zeylex.denguesense.dto.responseDTO.PaginatedDTO;
import com.zeylex.denguesense.dto.responseDTO.WeeklyCaseRowDTO;
import com.zeylex.denguesense.model.DengueCaseRecord;
import com.zeylex.denguesense.model.District;
import com.zeylex.denguesense.model.User;
import com.zeylex.denguesense.model.enums.RoleType;
import com.zeylex.denguesense.repo.DengueCaseRecordRepo;
import com.zeylex.denguesense.repo.DistrictRepo;
import com.zeylex.denguesense.repo.UserRepo;
import com.zeylex.denguesense.service.impl.WeeklyCaseQueryServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("WeeklyCaseQueryService — last-week national totals and PHI year totals")
class WeeklyCaseQueryServiceImplTest {

    @Mock private DengueCaseRecordRepo dengueCaseRecordRepo;
    @Mock private DistrictRepo districtRepo;
    @Mock private UserRepo userRepo;

    private WeeklyCaseQueryServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new WeeklyCaseQueryServiceImpl(dengueCaseRecordRepo, districtRepo, userRepo);
    }

    @Test
    @DisplayName("sums last-week cumulative_cases across all RDHS as the national dengue count")
    void getSummary_sumsLastWeekCumulativeAcrossRdhs() {
        User admin = user("admin@health.gov.lk", RoleType.ADMIN, null);
        when(userRepo.findByEmail("admin@health.gov.lk")).thenReturn(admin);

        LocalDate weekStart = LocalDate.of(2026, 8, 17);
        LocalDate weekEnd = LocalDate.of(2026, 8, 23);
        when(dengueCaseRecordRepo.findLatestWeekStartDate()).thenReturn(Optional.of(weekStart));
        when(dengueCaseRecordRepo.findByWeekStartDateWithDistrict(weekStart)).thenReturn(List.of(
                record(district(1L, "Colombo"), weekStart, weekEnd, 268, 4120),
                record(district(2L, "Gampaha"), weekStart, weekEnd, 154, 1980)
        ));
        int year = LocalDate.now().getYear();
        when(dengueCaseRecordRepo.sumWeekCasesBetween(
                LocalDate.of(year, 1, 1), LocalDate.of(year, 12, 31), false, 0L))
                .thenReturn(6100L);

        DengueCaseSummaryDTO summary = service.getSummary("admin@health.gov.lk");

        assertThat(summary.lastWeekStartDate()).isEqualTo(weekStart);
        assertThat(summary.lastWeekEndDate()).isEqualTo(weekEnd);
        assertThat(summary.lastWeekRdhsCount()).isEqualTo(2);
        assertThat(summary.lastWeekRdhsExpected()).isEqualTo(26);
        assertThat(summary.lastWeekCases()).isEqualTo(422L);
        assertThat(summary.lastWeekCumulativeTotal()).isEqualTo(6100L);
        assertThat(summary.scopedToDistrict()).isFalse();
        assertThat(summary.districtYearCumulative()).isNull();
    }

    @Test
    @DisplayName("PHI summary uses the officer district year-to-date cumulative")
    void getSummary_phiUsesDistrictYearCumulative() {
        District colombo = district(1L, "Colombo");
        User phi = user("phi@health.gov.lk", RoleType.PHI, colombo);
        when(userRepo.findByEmail("phi@health.gov.lk")).thenReturn(phi);

        LocalDate weekStart = LocalDate.of(2026, 8, 17);
        LocalDate weekEnd = LocalDate.of(2026, 8, 23);
        when(dengueCaseRecordRepo.findLatestWeekStartDate()).thenReturn(Optional.of(weekStart));
        when(dengueCaseRecordRepo.findByWeekStartDateWithDistrict(weekStart)).thenReturn(List.of(
                record(colombo, weekStart, weekEnd, 268, 4120),
                record(district(2L, "Gampaha"), weekStart, weekEnd, 154, 1980)
        ));
        int year = LocalDate.now().getYear();
        when(dengueCaseRecordRepo.sumWeekCasesBetween(
                LocalDate.of(year, 1, 1), LocalDate.of(year, 12, 31), false, 0L))
                .thenReturn(6100L);
        when(dengueCaseRecordRepo.sumWeekCasesBetween(
                LocalDate.of(year, 1, 1), LocalDate.of(year, 12, 31), true, 1L))
                .thenReturn(268L);
        when(dengueCaseRecordRepo.findFirstByDistrict_IdAndWeekStartDateGreaterThanEqualOrderByWeekStartDateDesc(
                eq(1L), eq(LocalDate.of(year, 1, 1))))
                .thenReturn(Optional.of(record(colombo, weekStart, weekEnd, 268, 4120)));

        DengueCaseSummaryDTO summary = service.getSummary("phi@health.gov.lk");

        assertThat(summary.scopedToDistrict()).isTrue();
        assertThat(summary.districtId()).isEqualTo(1L);
        assertThat(summary.districtName()).isEqualTo("Colombo");
        assertThat(summary.districtYearCumulative()).isEqualTo(4120L);
        assertThat(summary.districtLastWeekCases()).isEqualTo(268L);
        assertThat(summary.lastWeekCumulativeTotal()).isEqualTo(6100L);
    }

    @Test
    @DisplayName("PHI list is always scoped to the officer district even if another district is requested")
    void listWeeklyCases_phiIgnoresRequestedDistrict() {
        District colombo = district(1L, "Colombo");
        User phi = user("phi@health.gov.lk", RoleType.PHI, colombo);
        when(userRepo.findByEmail("phi@health.gov.lk")).thenReturn(phi);

        LocalDate weekStart = LocalDate.of(2026, 8, 17);
        DengueCaseRecord row = record(colombo, weekStart, weekStart.plusDays(6), 268, 4120);
        when(dengueCaseRecordRepo.findFiltered(
                eq(true), eq(1L), eq(false), any(), eq(false), any(), any()))
                .thenReturn(new PageImpl<>(List.of(row), PageRequest.of(0, 26), 1));

        PaginatedDTO page = service.listWeeklyCases(
                "phi@health.gov.lk",
                2L,
                "Gampaha",
                null,
                null,
                PageRequest.of(0, 26, Sort.by(Sort.Direction.DESC, "weekStartDate")));

        assertThat(page.getTotalItems()).isEqualTo(1);
        WeeklyCaseRowDTO dto = (WeeklyCaseRowDTO) page.getContent().get(0);
        assertThat(dto.districtName()).isEqualTo("Colombo");
        assertThat(dto.cumulativeCases()).isEqualTo(4120);
    }

    private static User user(String email, RoleType role, District district) {
        User user = new User();
        user.setEmail(email);
        user.setRole(role);
        user.setDistrict(district);
        return user;
    }

    private static District district(Long id, String name) {
        District district = new District();
        district.setId(id);
        district.setName(name);
        return district;
    }

    private static DengueCaseRecord record(District district,
                                           LocalDate start,
                                           LocalDate end,
                                           int weekCases,
                                           int cumulative) {
        return DengueCaseRecord.builder()
                .id(district.getId())
                .district(district)
                .weekStartDate(start)
                .weekEndDate(end)
                .weekCases(weekCases)
                .cumulativeCases(cumulative)
                .build();
    }
}
