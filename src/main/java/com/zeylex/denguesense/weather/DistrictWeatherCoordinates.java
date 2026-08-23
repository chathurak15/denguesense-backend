package com.zeylex.denguesense.weather;

import com.zeylex.denguesense.model.District;
import org.locationtech.jts.geom.Point;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;
public final class DistrictWeatherCoordinates {

    private static final Map<String, LatLon> COORDS = Map.ofEntries(
            entry("Colombo", 6.9271, 79.8612),
            entry("Gampaha", 7.0873, 80.0144),
            entry("Kalutara", 6.5854, 79.9607),
            entry("Kandy", 7.2906, 80.6337),
            entry("Matale", 7.4675, 80.6234),
            entry("Nuwara Eliya", 6.9497, 80.7891),
            entry("Galle", 6.0535, 80.2210),
            entry("Hambantota", 6.1241, 81.1185),
            entry("Matara", 5.9549, 80.5550),
            entry("Jaffna", 9.6615, 80.0255),
            entry("Kilinochchi", 9.3803, 80.4036),
            entry("Mannar", 8.9810, 79.9044),
            entry("Vavuniya", 8.7514, 80.4971),
            entry("Mullaitivu", 9.2671, 80.8128),
            entry("Batticaloa", 7.7170, 81.6924),
            entry("Ampara", 7.2913, 81.6722),
            entry("Trincomalee", 8.5874, 81.2152),
            entry("Kurunegala", 7.4818, 80.3609),
            entry("Puttalam", 8.0408, 79.8394),
            entry("Anuradhapura", 8.3114, 80.4037),
            entry("Polonnaruwa", 7.9403, 81.0188),
            entry("Badulla", 6.9934, 81.0550),
            entry("Monaragala", 6.8728, 81.3507),
            entry("Ratnapura", 6.6828, 80.3992),
            entry("Kegalle", 7.2513, 80.3464),
            entry("Kalmunai", 7.4148, 81.8266)
    );

    private DistrictWeatherCoordinates() {
    }

    public static LatLon requireFor(District district) {
        return findFor(district).orElseThrow(() -> new IllegalArgumentException(
                "No weather coordinates configured for district '" + district.getName() + "'"));
    }

    public static Optional<LatLon> findFor(District district) {
        if (district == null) {
            return Optional.empty();
        }
        Optional<LatLon> fromName = lookup(district.getName());
        if (fromName.isPresent()) {
            return fromName;
        }
        Optional<LatLon> fromRdhs = lookup(district.getRdhsZone());
        if (fromRdhs.isPresent()) {
            return fromRdhs;
        }
        return fromCentroid(district.getCentroid());
    }

    static Optional<LatLon> lookup(String name) {
        if (name == null || name.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(COORDS.get(normalize(name)));
    }

    private static Optional<LatLon> fromCentroid(Point centroid) {
        if (centroid == null) {
            return Optional.empty();
        }
        return Optional.of(new LatLon(centroid.getY(), centroid.getX()));
    }

    private static Map.Entry<String, LatLon> entry(String name, double lat, double lon) {
        return Map.entry(normalize(name), new LatLon(lat, lon));
    }

    private static String normalize(String name) {
        return name.trim().toLowerCase(Locale.ROOT);
    }
}
