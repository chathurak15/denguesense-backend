package com.zeylex.denguesense.bsds;

import com.zeylex.denguesense.model.District;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DistrictPopulationsTest {

    @Test
    void looksUpColombo2025Population() {
        District colombo = new District();
        colombo.setName("Colombo");
        assertThat(DistrictPopulations.requireFor(colombo)).isEqualTo(2_371_000.0);
    }

    @Test
    void looksUpKalmunaiViaRdhsZone() {
        District district = new District();
        district.setName("Unknown");
        district.setRdhsZone("Kalmunai");
        assertThat(DistrictPopulations.requireFor(district)).isEqualTo(298_000.0);
    }
}
