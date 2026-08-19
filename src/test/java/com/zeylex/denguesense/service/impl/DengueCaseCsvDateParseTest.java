package com.zeylex.denguesense.service.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DengueCaseCsvDateParseTest {

    @Test
    @DisplayName("accepts training CSV dates like 6/22/2026 and 12/29/2025")
    void parsesSlashMonthDayYear() {
        assertThat(DengueCaseCsvServiceImpl.parseDate("6/22/2026"))
                .isEqualTo(LocalDate.of(2026, 6, 22));
        assertThat(DengueCaseCsvServiceImpl.parseDate("12/29/2025"))
                .isEqualTo(LocalDate.of(2025, 12, 29));
        assertThat(DengueCaseCsvServiceImpl.parseDate("1/4/2026"))
                .isEqualTo(LocalDate.of(2026, 1, 4));
    }

    @Test
    void stillParsesIsoDates() {
        assertThat(DengueCaseCsvServiceImpl.parseDate("2026-06-22"))
                .isEqualTo(LocalDate.of(2026, 6, 22));
    }

    @Test
    void stripsExcelTimeSuffix() {
        assertThat(DengueCaseCsvServiceImpl.parseDate("6/22/2026 0:00:00"))
                .isEqualTo(LocalDate.of(2026, 6, 22));
    }

    @Test
    void rejectsUnknownFormat() {
        assertThatThrownBy(() -> DengueCaseCsvServiceImpl.parseDate("22.06.2026"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("invalid date");
    }
}
