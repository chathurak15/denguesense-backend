package com.zeylex.denguesense.repo;

import com.zeylex.denguesense.model.DengueCaseRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface DengueCaseRecordRepo extends JpaRepository<DengueCaseRecord, Long> {

    @Query("""
            SELECT d FROM DengueCaseRecord d
            WHERE d.district.id = :districtId
              AND d.weekStartDate BETWEEN :startDate AND :endDate
            ORDER BY d.weekStartDate ASC
            """)
    List<DengueCaseRecord> findByDistrictAndDateRange(
            @Param("districtId") Long districtId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    boolean existsByDistrict_IdAndWeekStartDate(Long districtId, LocalDate weekStartDate);

    Optional<DengueCaseRecord> findByDistrict_IdAndWeekStartDate(Long districtId, LocalDate weekStartDate);

    @Query("SELECT MAX(d.weekStartDate) FROM DengueCaseRecord d")
    Optional<LocalDate> findLatestWeekStartDate();

    @Query("SELECT MAX(d.weekStartDate) FROM DengueCaseRecord d WHERE d.district.id = :districtId")
    Optional<LocalDate> findLatestWeekStartDateByDistrictId(@Param("districtId") Long districtId);

    @Query("""
            SELECT d FROM DengueCaseRecord d
            JOIN FETCH d.district
            WHERE d.weekStartDate = :weekStartDate
            ORDER BY d.district.name ASC
            """)
    List<DengueCaseRecord> findByWeekStartDateWithDistrict(@Param("weekStartDate") LocalDate weekStartDate);

    @Query(
            value = """
                    SELECT d FROM DengueCaseRecord d
                    JOIN FETCH d.district dist
                    WHERE (:hasDistrict = false OR dist.id = :districtId)
                      AND (:hasFrom = false OR d.weekStartDate >= :fromDate)
                      AND (:hasTo = false OR d.weekStartDate <= :toDate)
                    """,
            countQuery = """
                    SELECT COUNT(d) FROM DengueCaseRecord d
                    WHERE (:hasDistrict = false OR d.district.id = :districtId)
                      AND (:hasFrom = false OR d.weekStartDate >= :fromDate)
                      AND (:hasTo = false OR d.weekStartDate <= :toDate)
                    """
    )
    Page<DengueCaseRecord> findFiltered(
            @Param("hasDistrict") boolean hasDistrict,
            @Param("districtId") long districtId,
            @Param("hasFrom") boolean hasFrom,
            @Param("fromDate") LocalDate fromDate,
            @Param("hasTo") boolean hasTo,
            @Param("toDate") LocalDate toDate,
            Pageable pageable);

    @Query("""
            SELECT COALESCE(SUM(d.weekCases), 0)
            FROM DengueCaseRecord d
            WHERE d.weekStartDate >= :fromDate
              AND d.weekStartDate <= :toDate
              AND (:hasDistrict = false OR d.district.id = :districtId)
            """)
    long sumWeekCasesBetween(
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate,
            @Param("hasDistrict") boolean hasDistrict,
            @Param("districtId") long districtId);

    /**
     * National year-to-date: latest cumulative_cases per district in the year.
     * Summing week_cases across every week overweight districts (often Colombo)
     * that have a full import history.
     */
    @Query(value = """
            SELECT COALESCE(SUM(COALESCE(latest.cumulative_cases, latest.week_cases)), 0)
            FROM (
                SELECT DISTINCT ON (district_id)
                       cumulative_cases,
                       week_cases
                FROM dengue_case_records
                WHERE week_start_date >= :yearStart
                  AND week_start_date <= :yearEnd
                ORDER BY district_id ASC, week_start_date DESC
            ) latest
            """, nativeQuery = true)
    Object sumLatestYearToDateCases(
            @Param("yearStart") LocalDate yearStart,
            @Param("yearEnd") LocalDate yearEnd);

    Optional<DengueCaseRecord> findFirstByDistrict_IdAndWeekStartDateGreaterThanEqualOrderByWeekStartDateDesc(
            Long districtId, LocalDate yearStart);
}
