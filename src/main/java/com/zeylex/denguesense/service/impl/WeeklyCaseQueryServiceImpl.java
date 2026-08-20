package com.zeylex.denguesense.service.impl;

import com.zeylex.denguesense.dto.responseDTO.DengueCaseSummaryDTO;
import com.zeylex.denguesense.dto.responseDTO.PaginatedDTO;
import com.zeylex.denguesense.dto.responseDTO.WeeklyCaseRowDTO;
import com.zeylex.denguesense.exception.NotFoundException;
import com.zeylex.denguesense.model.DengueCaseRecord;
import com.zeylex.denguesense.model.District;
import com.zeylex.denguesense.model.User;
import com.zeylex.denguesense.model.enums.RoleType;
import com.zeylex.denguesense.repo.DengueCaseRecordRepo;
import com.zeylex.denguesense.repo.DistrictRepo;
import com.zeylex.denguesense.repo.UserRepo;
import com.zeylex.denguesense.service.WeeklyCaseQueryService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class WeeklyCaseQueryServiceImpl implements WeeklyCaseQueryService {

    private final DengueCaseRecordRepo dengueCaseRecordRepo;
    private final DistrictRepo districtRepo;
    private final UserRepo userRepo;

    public WeeklyCaseQueryServiceImpl(DengueCaseRecordRepo dengueCaseRecordRepo,
                                      DistrictRepo districtRepo,
                                      UserRepo userRepo) {
        this.dengueCaseRecordRepo = dengueCaseRecordRepo;
        this.districtRepo = districtRepo;
        this.userRepo = userRepo;
    }

    @Override
    @Transactional(readOnly = true)
    public PaginatedDTO listWeeklyCases(String userEmail,
                                        Long districtId,
                                        String district,
                                        LocalDate fromDate,
                                        LocalDate toDate,
                                        Pageable pageable) {
        User user = requireUser(userEmail);
        Long effectiveDistrictId = resolveListDistrictId(user, districtId, district);
        Page<DengueCaseRecord> page = dengueCaseRecordRepo.findFiltered(
                effectiveDistrictId != null,
                effectiveDistrictId != null ? effectiveDistrictId : 0L,
                fromDate != null,
                fromDate != null ? fromDate : LocalDate.EPOCH,
                toDate != null,
                toDate != null ? toDate : LocalDate.EPOCH,
                pageable);
        List<WeeklyCaseRowDTO> rows = page.getContent().stream()
                .map(WeeklyCaseQueryServiceImpl::toRow)
                .toList();
        return new PaginatedDTO(rows, page.getTotalPages(), page.getTotalElements());
    }

    @Override
    @Transactional(readOnly = true)
    public DengueCaseSummaryDTO getSummary(String userEmail) {
        User user = requireUser(userEmail);
        boolean phi = user.getRole() == RoleType.PHI;
        District phiDistrict = phi ? requireDistrict(user) : null;

        int year = LocalDate.now().getYear();
        LocalDate yearStart = LocalDate.of(year, 1, 1);
        LocalDate yearEnd = LocalDate.of(year, 12, 31);

        LocalDate lastWeekStart = dengueCaseRecordRepo.findLatestWeekStartDate().orElse(null);
        List<DengueCaseRecord> lastWeekRows = lastWeekStart == null
                ? List.of()
                : dengueCaseRecordRepo.findByWeekStartDateWithDistrict(lastWeekStart);

        LocalDate lastWeekEnd = lastWeekRows.stream()
                .map(DengueCaseRecord::getWeekEndDate)
                .filter(date -> date != null)
                .max(LocalDate::compareTo)
                .orElse(lastWeekStart == null ? null : lastWeekStart.plusDays(6));

        long lastWeekCases = lastWeekRows.stream()
                .mapToLong(r -> r.getWeekCases() == null ? 0L : r.getWeekCases())
                .sum();
        long lastWeekCumulativeTotal = lastWeekRows.stream()
                .mapToLong(r -> r.getCumulativeCases() == null ? 0L : r.getCumulativeCases())
                .sum();
        int lastWeekRdhsCount = (int) lastWeekRows.stream()
                .map(r -> r.getDistrict().getId())
                .distinct()
                .count();

        long nationalYearCases = dengueCaseRecordRepo.sumWeekCasesBetween(
                yearStart, yearEnd, false, 0L);

        Long districtYearCumulative = null;
        Long districtYearCases = null;
        Long districtLastWeekCases = null;
        Long districtId = null;
        String districtName = null;

        if (phiDistrict != null) {
            final Long phiDistrictId = phiDistrict.getId();
            districtId = phiDistrictId;
            districtName = displayName(phiDistrict);
            final long yearCases = dengueCaseRecordRepo.sumWeekCasesBetween(
                    yearStart, yearEnd, true, phiDistrictId);
            districtYearCases = yearCases;
            districtYearCumulative = dengueCaseRecordRepo
                    .findFirstByDistrict_IdAndWeekStartDateGreaterThanEqualOrderByWeekStartDateDesc(
                            phiDistrictId, yearStart)
                    .map(r -> r.getCumulativeCases() != null ? r.getCumulativeCases().longValue() : yearCases)
                    .orElse(yearCases);
            districtLastWeekCases = lastWeekRows.stream()
                    .filter(r -> phiDistrictId.equals(r.getDistrict().getId()))
                    .mapToLong(r -> r.getWeekCases() == null ? 0L : r.getWeekCases())
                    .sum();
        }

        return new DengueCaseSummaryDTO(
                lastWeekStart,
                lastWeekEnd,
                lastWeekRdhsCount,
                DengueCaseSummaryDTO.RDHS_EXPECTED,
                lastWeekCases,
                lastWeekCumulativeTotal,
                year,
                nationalYearCases,
                phi,
                districtId,
                districtName,
                districtYearCumulative,
                districtYearCases,
                districtLastWeekCases
        );
    }

    private Long resolveListDistrictId(User user, Long districtId, String district) {
        if (user.getRole() == RoleType.PHI) {
            return requireDistrict(user).getId();
        }
        if (districtId != null) {
            return districtRepo.findById(districtId)
                    .orElseThrow(() -> new NotFoundException("District not found: id=" + districtId))
                    .getId();
        }
        if (district == null || district.isBlank()) {
            return null;
        }
        String name = district.trim();
        return districtRepo.findByNameIgnoreCase(name)
                .or(() -> districtRepo.findByRdhsZoneIgnoreCase(name))
                .orElseThrow(() -> new NotFoundException("District not found: " + name))
                .getId();
    }

    private User requireUser(String userEmail) {
        User user = userRepo.findByEmail(userEmail);
        if (user == null) {
            throw new NotFoundException("Authenticated user not found in the system: " + userEmail);
        }
        return user;
    }

    private static District requireDistrict(User user) {
        if (user.getDistrict() == null) {
            throw new NotFoundException(
                    "PHI user '" + user.getEmail() + "' has no district assigned. " +
                            "Contact your administrator to assign a district.");
        }
        return user.getDistrict();
    }

    private static WeeklyCaseRowDTO toRow(DengueCaseRecord record) {
        District district = record.getDistrict();
        return new WeeklyCaseRowDTO(
                record.getId(),
                district.getId(),
                district.getName(),
                district.getRdhsZone(),
                record.getWeekStartDate(),
                record.getWeekEndDate(),
                record.getWeekCases(),
                record.getCumulativeCases()
        );
    }

    private static String displayName(District district) {
        if (district.getRdhsZone() != null && !district.getRdhsZone().isBlank()
                && !district.getRdhsZone().equalsIgnoreCase(district.getName())) {
            return district.getRdhsZone();
        }
        return district.getName();
    }
}
