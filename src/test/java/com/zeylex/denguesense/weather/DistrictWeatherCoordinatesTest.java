package com.zeylex.denguesense.weather;

import com.zeylex.denguesense.model.District;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DistrictWeatherCoordinatesTest {

    @Test
    void looksUpTrainingScriptCoordinatesByDistrictName() {
        District colombo = new District();
        colombo.setName("colombo");

        LatLon coords = DistrictWeatherCoordinates.requireFor(colombo);

        assertThat(coords.latitude()).isEqualTo(6.9271);
        assertThat(coords.longitude()).isEqualTo(79.8612);
    }

    @Test
    void looksUpKalmunaiViaRdhsZone() {
        District ampara = new District();
        ampara.setName("Eastern extra");
        ampara.setRdhsZone("Kalmunai");

        LatLon coords = DistrictWeatherCoordinates.requireFor(ampara);

        assertThat(coords.latitude()).isEqualTo(7.4148);
        assertThat(coords.longitude()).isEqualTo(81.8266);
    }
}
