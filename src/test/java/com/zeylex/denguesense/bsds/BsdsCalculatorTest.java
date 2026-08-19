package com.zeylex.denguesense.bsds;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

class BsdsCalculatorTest {

    @Test
    @DisplayName("BSDS = confirmed sites / population * 100_000")
    void scoreMatchesNotebookFormula() {
        double score = BsdsCalculator.score(10, 2_371_000);
        assertThat(score).isCloseTo(10.0 * 100_000.0 / 2_371_000.0, within(1e-12));
    }

    @Test
    void zeroConfirmedSitesIsZeroScore() {
        assertThat(BsdsCalculator.score(0, 2_371_000)).isZero();
    }

    @Test
    void rejectsNonPositivePopulation() {
        assertThatThrownBy(() -> BsdsCalculator.score(1, 0))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
