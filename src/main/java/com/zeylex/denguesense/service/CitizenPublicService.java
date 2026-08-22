package com.zeylex.denguesense.service;

import com.zeylex.denguesense.dto.responseDTO.CitizenAlertDTO;
import com.zeylex.denguesense.dto.responseDTO.CitizenDistrictDTO;
import com.zeylex.denguesense.dto.responseDTO.CitizenDistrictStatusDTO;
import com.zeylex.denguesense.dto.responseDTO.CitizenHotspotDTO;
import com.zeylex.denguesense.dto.responseDTO.CitizenOutbreakSummaryDTO;
import com.zeylex.denguesense.dto.responseDTO.CitizenWeeklyCaseDTO;
import com.zeylex.denguesense.dto.responseDTO.DistrictForecastResponseDTO;
import com.zeylex.denguesense.model.DengueCaseRecord;
import com.zeylex.denguesense.model.District;
import com.zeylex.denguesense.model.DistrictForecast;
import com.zeylex.denguesense.model.Report;
import com.zeylex.denguesense.model.ReportCluster;
import com.zeylex.denguesense.model.enums.ClusterStatus;
import com.zeylex.denguesense.model.enums.ReportStatus;
import com.zeylex.denguesense.model.enums.RiskLabel;
import com.zeylex.denguesense.repo.DengueCaseRecordRepo;
import com.zeylex.denguesense.repo.DistrictForecastRepo;
import com.zeylex.denguesense.repo.DistrictRepo;
import com.zeylex.denguesense.repo.ReportClusterRepo;
import com.zeylex.denguesense.repo.ReportRepo;
import com.zeylex.denguesense.weather.DistrictWeatherCoordinates;
import com.zeylex.denguesense.weather.LatLon;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class CitizenPublicService {

    private static final Set<ReportStatus> OPEN_STATUSES =
            EnumSet.of(ReportStatus.CLASSIFIED, ReportStatus.DISPATCHED);
    private static final Set<ClusterStatus> LIVE_CLUSTER_STATUSES =
            EnumSet.of(ClusterStatus.ACTIVE, ClusterStatus.ALERTED);

    private final DengueCaseRecordRepo dengueCaseRecordRepo;
    private final ReportRepo reportRepo;
    private final DistrictRepo districtRepo;
    private final DistrictForecastRepo districtForecastRepo;
    private final ReportClusterRepo reportClusterRepo;

    public CitizenPublicService(DengueCaseRecordRepo dengueCaseRecordRepo,
                                ReportRepo reportRepo,
                                DistrictRepo districtRepo,
                                DistrictForecastRepo districtForecastRepo,
                                ReportClusterRepo reportClusterRepo) {
        this.dengueCaseRecordRepo = dengueCaseRecordRepo;
        this.reportRepo = reportRepo;
        this.districtRepo = districtRepo;
        this.districtForecastRepo = districtForecastRepo;
        this.reportClusterRepo = reportClusterRepo;
    }

    @Transactional(readOnly = true)
    public CitizenOutbreakSummaryDTO outbreakSummary() {
        int year = LocalDate.now().getYear();
        LocalDate yearStart = LocalDate.of(year, 1, 1);
        LocalDate yearEnd = LocalDate.of(year, 12, 31);

        LocalDate lastWeekStart = dengueCaseRecordRepo.findLatestWeekStartDate().orElse(null);
        List<DengueCaseRecord> lastWeekRows = lastWeekStart == null
                ? List.of()
                : dengueCaseRecordRepo.findByWeekStartDateWithDistrict(lastWeekStart);

        LocalDate lastWeekEnd = lastWeekRows.stream()
                .map(DengueCaseRecord::getWeekEndDate)
                .filter(Objects::nonNull)
                .max(LocalDate::compareTo)
                .orElse(lastWeekStart == null ? null : lastWeekStart.plusDays(6));

        long lastWeekCases = lastWeekRows.stream()
                .mapToLong(r -> r.getWeekCases() == null ? 0L : r.getWeekCases())
                .sum();
        long previousWeekCases = 0L;
        if (lastWeekStart != null) {
            LocalDate previousWeekStart = lastWeekStart.minusWeeks(1);
            previousWeekCases = dengueCaseRecordRepo.sumWeekCasesBetween(
                    previousWeekStart, previousWeekStart, false, 0L);
        }

        Double weekChangePercent = null;
        if (previousWeekCases > 0) {
            weekChangePercent = ((lastWeekCases - previousWeekCases) * 100.0) / previousWeekCases;
        } else if (lastWeekCases > 0) {
            weekChangePercent = 100.0;
        }

        long yearCases = nationalYearToDate(yearStart, yearEnd, lastWeekRows);
        List<CitizenHotspotDTO> hotspots = hotspots();
        int hotspotCount = hotspots.size();

        List<DengueCaseRecord> ranked = lastWeekRows.stream()
                .sorted(Comparator.comparingInt((DengueCaseRecord r) ->
                        r.getWeekCases() == null ? 0 : r.getWeekCases()).reversed())
                .toList();
        long cutoff = (long) ranked.stream()
                .mapToLong(r -> r.getWeekCases() == null ? 0L : r.getWeekCases())
                .average()
                .orElse(0);
        List<String> highDistricts = ranked.stream()
                .filter(r -> (r.getWeekCases() == null ? 0 : r.getWeekCases()) > cutoff
                        && (r.getWeekCases() == null ? 0 : r.getWeekCases()) > 0)
                .limit(4)
                .map(r -> displayName(r.getDistrict()))
                .distinct()
                .toList();

        String nationalRisk = nationalRisk(weekChangePercent, hotspots);
        String banner = bannerFor(highDistricts, lastWeekRows, nationalRisk);

        return new CitizenOutbreakSummaryDTO(
                lastWeekStart,
                lastWeekEnd,
                year,
                lastWeekCases,
                previousWeekCases,
                weekChangePercent,
                yearCases,
                hotspotCount,
                nationalRisk,
                banner,
                highDistricts
        );
    }

    @Transactional(readOnly = true)
    public List<CitizenHotspotDTO> hotspots() {
        LocalDateTime since = LocalDateTime.now().minusDays(30);
        List<Report> reports = reportRepo.findRecentClassifiedForMap(
                OPEN_STATUSES, since, RiskLabel.INVALID);
        if (reports.size() > 400) {
            reports = reports.subList(0, 400);
        }

        Map<String, List<Report>> cells = new LinkedHashMap<>();
        for (Report report : reports) {
            if (report.getLatitude() == null || report.getLongitude() == null) continue;
            String key = String.format(Locale.ROOT, "%.2f_%.2f",
                    Math.round(report.getLatitude() * 25.0) / 25.0,
                    Math.round(report.getLongitude() * 25.0) / 25.0);
            cells.computeIfAbsent(key, ignored -> new ArrayList<>()).add(report);
        }

        List<CitizenHotspotDTO> result = new ArrayList<>();
        int index = 1;
        for (List<Report> group : cells.values()) {
            if (group.isEmpty()) continue;
            double lat = group.stream().mapToDouble(Report::getLatitude).average().orElse(0);
            double lng = group.stream().mapToDouble(Report::getLongitude).average().orElse(0);
            long high = group.stream()
                    .filter(r -> r.getCnnClassification() != null
                            && r.getCnnClassification().getRiskLabel() == RiskLabel.HIGH_RISK)
                    .count();
            String districtName = group.stream()
                    .map(Report::getDistrict)
                    .filter(Objects::nonNull)
                    .map(CitizenPublicService::displayName)
                    .findFirst()
                    .orElse("Unknown district");
            result.add(new CitizenHotspotDTO(
                    "HS-" + String.format(Locale.ROOT, "%02d", index++),
                    districtName,
                    roundCoord(lat),
                    roundCoord(lng),
                    clusterRisk((int) high, group.size()),
                    group.size()
            ));
        }
        result.sort(Comparator.comparingInt(CitizenHotspotDTO::reportCount).reversed());
        return result;
    }

    @Transactional(readOnly = true)
    public List<CitizenAlertDTO> alerts() {
        List<CitizenAlertDTO> alerts = new ArrayList<>();
        CitizenOutbreakSummaryDTO outbreak = outbreakSummary();
        if ("HIGH".equals(outbreak.nationalRisk())) {
            alerts.add(new CitizenAlertDTO(
                    "national-week",
                    "HIGH",
                    "National outbreak pressure is high",
                    outbreak.banner(),
                    null,
                    LocalDateTime.now()
            ));
        } else if (outbreak.banner() != null && !outbreak.banner().isBlank()) {
            alerts.add(new CitizenAlertDTO(
                    "national-week",
                    outbreak.nationalRisk(),
                    "Weekly dengue update",
                    outbreak.banner(),
                    null,
                    LocalDateTime.now()
            ));
        }

        List<ReportCluster> live = reportClusterRepo.findByStatusInOrderByDetectedAtDesc(LIVE_CLUSTER_STATUSES);
        for (ReportCluster cluster : live) {
            District district = districtRepo.findById(cluster.getDistrictId()).orElse(null);
            String name = district == null ? "a local district" : displayName(district);
            alerts.add(new CitizenAlertDTO(
                    "cluster-" + cluster.getId(),
                    cluster.getReportCount() != null && cluster.getReportCount() >= 5 ? "HIGH" : "MEDIUM",
                    "Breeding-site hotspot in " + name,
                    cluster.getReportCount() + " confirmed high-risk reports were grouped in "
                            + name + ". Avoid standing water and report any breeding sites nearby.",
                    name,
                    cluster.getDetectedAt()
            ));
        }

        for (DistrictForecast forecast : districtForecastRepo.findLatestPerRdhs()) {
            String risk = forecastRisk(forecast, null);
            if (!"HIGH".equals(risk)) continue;
            alerts.add(new CitizenAlertDTO(
                    "forecast-" + forecast.getRdhsId(),
                    "HIGH",
                    forecast.getDistrictName() + " forecast is elevated",
                    "The 4-week case forecast for " + forecast.getDistrictName()
                            + " is trending higher. Check standing water around the home this week.",
                    forecast.getDistrictName(),
                    forecast.getGeneratedAt() == null ? LocalDateTime.now()
                            : forecast.getGeneratedAt().atZone(java.time.ZoneId.of("Asia/Colombo")).toLocalDateTime()
            ));
        }

        return alerts.stream().limit(20).toList();
    }

    @Transactional(readOnly = true)
    public List<CitizenDistrictDTO> districts() {
        return districtRepo.findAll().stream()
                .sorted(Comparator.comparing(District::getName, String.CASE_INSENSITIVE_ORDER))
                .map(this::toDistrictDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<DistrictForecastResponseDTO> latestForecasts() {
        return districtForecastRepo.findLatestPerRdhs().stream()
                .map(DistrictForecastResponseDTO::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public CitizenDistrictStatusDTO districtStatus(Double latitude, Double longitude, Integer rdhsId) {
        District district = resolveDistrict(latitude, longitude, rdhsId);
        LatLon coords = DistrictWeatherCoordinates.findFor(district)
                .orElse(new LatLon(6.9271, 79.8612));

        LocalDate latestWeek = dengueCaseRecordRepo.findLatestWeekStartDateByDistrictId(district.getId())
                .orElse(dengueCaseRecordRepo.findLatestWeekStartDate().orElse(LocalDate.now().minusWeeks(1)));
        LocalDate from = latestWeek.minusWeeks(5);
        List<CitizenWeeklyCaseDTO> weekly = dengueCaseRecordRepo
                .findByDistrictAndDateRange(district.getId(), from, latestWeek)
                .stream()
                .map(r -> new CitizenWeeklyCaseDTO(r.getWeekStartDate(), r.getWeekCases() == null ? 0 : r.getWeekCases()))
                .toList();
        long lastWeekCases = weekly.isEmpty() ? 0L : weekly.get(weekly.size() - 1).weekCases();

        DistrictForecast forecast = district.getRdhsModelId() == null
                ? null
                : districtForecastRepo.findTopByRdhsIdOrderByTargetWeekStartDesc(district.getRdhsModelId())
                .orElse(null);
        DistrictForecastResponseDTO forecastDto = forecast == null ? null : DistrictForecastResponseDTO.from(forecast);
        String risk = forecastRisk(forecast, lastWeekCases);
        String trend = forecastTrend(forecast, weekly);
        String summary = districtSummary(displayName(district), risk, trend, lastWeekCases);

        return new CitizenDistrictStatusDTO(
                district.getId(),
                displayName(district),
                district.getProvince(),
                district.getRdhsModelId(),
                coords.latitude(),
                coords.longitude(),
                lastWeekCases,
                risk,
                trend,
                summary,
                weekly,
                forecastDto
        );
    }

    private long nationalYearToDate(LocalDate yearStart,
                                    LocalDate yearEnd,
                                    List<DengueCaseRecord> lastWeekRows) {
        Object latestByDistrict = dengueCaseRecordRepo.sumLatestYearToDateCases(yearStart, yearEnd);
        if (asLong(latestByDistrict) > 0) {
            return asLong(latestByDistrict);
        }
        long fromLatestWeek = lastWeekRows.stream()
                .mapToLong(r -> r.getCumulativeCases() == null ? 0L : r.getCumulativeCases())
                .sum();
        if (fromLatestWeek > 0) {
            return fromLatestWeek;
        }
        return dengueCaseRecordRepo.sumWeekCasesBetween(yearStart, yearEnd, false, 0L);
    }

    private static long asLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value == null) {
            return 0L;
        }
        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException ignored) {
            return 0L;
        }
    }

    private District resolveDistrict(Double latitude, Double longitude, Integer rdhsId) {
        if (rdhsId != null) {
            return districtRepo.findByRdhsModelId(rdhsId)
                    .or(() -> districtRepo.findByNameIgnoreCase("Colombo"))
                    .orElseThrow();
        }
        if (latitude != null && longitude != null) {
            District nearest = districtRepo.findNearestByCoordinates(latitude, longitude).orElse(null);
            if (nearest != null) return nearest;
        }
        return districtRepo.findByNameIgnoreCase("Colombo")
                .or(() -> districtRepo.findAll().stream().findFirst())
                .orElseThrow();
    }

    private CitizenDistrictDTO toDistrictDto(District district) {
        LatLon coords = DistrictWeatherCoordinates.findFor(district).orElse(null);
        return new CitizenDistrictDTO(
                district.getId(),
                displayName(district),
                district.getProvince(),
                district.getRdhsModelId(),
                coords == null ? null : coords.latitude(),
                coords == null ? null : coords.longitude()
        );
    }

    private static String nationalRisk(Double weekChangePercent, List<CitizenHotspotDTO> hotspots) {
        long highHotspots = hotspots.stream().filter(h -> "HIGH".equals(h.risk())).count();
        if ((weekChangePercent != null && weekChangePercent >= 10) || highHotspots >= 3) {
            return "HIGH";
        }
        if ((weekChangePercent != null && weekChangePercent >= 0) || highHotspots >= 1) {
            return "MEDIUM";
        }
        return "LOW";
    }

    private static String bannerFor(List<String> highDistricts,
                                    List<DengueCaseRecord> lastWeekRows,
                                    String nationalRisk) {
        if (!highDistricts.isEmpty()) {
            Set<String> provinces = lastWeekRows.stream()
                    .filter(r -> highDistricts.contains(displayName(r.getDistrict())))
                    .map(r -> r.getDistrict().getProvince())
                    .filter(p -> p != null && !p.isBlank())
                    .collect(Collectors.toCollection(java.util.LinkedHashSet::new));
            if (!provinces.isEmpty()) {
                return "Active transmission reported in " + joinAnd(List.copyOf(provinces)) + ".";
            }
            return "Higher weekly cases reported in " + joinAnd(highDistricts) + ".";
        }
        if ("HIGH".equals(nationalRisk)) {
            return "National weekly cases are rising. Check standing water around your home.";
        }
        if ("MEDIUM".equals(nationalRisk)) {
            return "Dengue transmission is ongoing. Report breeding sites if you see standing water.";
        }
        return "Weekly dengue pressure is currently lower. Continue removing standing water.";
    }

    private static String clusterRisk(int high, int total) {
        if (total <= 0) return "LOW";
        double ratio = high / (double) total;
        if (ratio >= 0.4 || high >= 3) return "HIGH";
        if (high > 0 || ratio >= 0.2) return "MEDIUM";
        return "LOW";
    }

    private static String forecastRisk(DistrictForecast forecast, Long lastWeekCases) {
        if (forecast == null || forecast.getPredictions() == null || forecast.getPredictions().isEmpty()) {
            return lastWeekCases != null && lastWeekCases >= 80 ? "MEDIUM" : "LOW";
        }
        double week1 = forecast.getPredictions().get(0);
        if (lastWeekCases != null && lastWeekCases > 0) {
            if (week1 >= lastWeekCases * 1.25) return "HIGH";
            if (week1 >= lastWeekCases) return "MEDIUM";
            return "LOW";
        }
        if (week1 >= 80) return "HIGH";
        if (week1 >= 30) return "MEDIUM";
        return "LOW";
    }

    private static String forecastTrend(DistrictForecast forecast, List<CitizenWeeklyCaseDTO> weekly) {
        if (forecast != null && forecast.getPredictions() != null && forecast.getPredictions().size() >= 2) {
            double first = forecast.getPredictions().get(0);
            double last = forecast.getPredictions().get(forecast.getPredictions().size() - 1);
            if (last > first * 1.08) return "INCREASING";
            if (last < first * 0.92) return "DECREASING";
            return "STABLE";
        }
        if (weekly.size() >= 2) {
            int first = weekly.get(0).weekCases();
            int last = weekly.get(weekly.size() - 1).weekCases();
            if (last > first) return "INCREASING";
            if (last < first) return "DECREASING";
        }
        return "STABLE";
    }

    private static String districtSummary(String name, String risk, String trend, long lastWeekCases) {
        String trendText = switch (trend) {
            case "INCREASING" -> "an increasing";
            case "DECREASING" -> "a decreasing";
            default -> "a stable";
        };
        if ("HIGH".equals(risk)) {
            return name + " is at high dengue risk this week (" + lastWeekCases
                    + " cases in the latest week), with " + trendText
                    + " 4-week forecast. Remove standing water around the home.";
        }
        if ("MEDIUM".equals(risk)) {
            return name + " is at moderate dengue risk. Latest week: " + lastWeekCases
                    + " cases, with " + trendText + " forecast. Keep checking containers and drains.";
        }
        return "Environmental risk parameters remain lower in " + name
                + " this week. Continue monitoring standing water.";
    }

    private static String displayName(District district) {
        if (district.getRdhsZone() != null && !district.getRdhsZone().isBlank()
                && !district.getRdhsZone().equalsIgnoreCase(district.getName())) {
            return district.getRdhsZone();
        }
        return district.getName();
    }

    private static String joinAnd(List<String> parts) {
        if (parts.isEmpty()) return "";
        if (parts.size() == 1) return parts.get(0);
        if (parts.size() == 2) return parts.get(0) + " & " + parts.get(1);
        return String.join(", ", parts.subList(0, parts.size() - 1)) + " & " + parts.get(parts.size() - 1);
    }

    private static double roundCoord(double value) {
        return Math.round(value * 1000.0) / 1000.0;
    }
}
