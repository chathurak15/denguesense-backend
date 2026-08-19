package com.zeylex.denguesense.bsds;

import com.zeylex.denguesense.model.District;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;
public final class DistrictPopulations {

    private static final Map<String, Double> POPULATION_2025 = Map.ofEntries(
            entry("Colombo", 2_371_000),
            entry("Gampaha", 2_431_000),
            entry("Kalutara", 1_302_000),
            entry("Kandy", 1_460_000),
            entry("Matale", 526_000),
            entry("Nuwara Eliya", 726_000),
            entry("Galle", 1_096_000),
            entry("Hambantota", 671_000),
            entry("Matara", 836_000),
            entry("Jaffna", 595_000),
            entry("Kilinochchi", 137_000),
            entry("Mannar", 124_000),
            entry("Vavuniya", 173_000),
            entry("Mullaitivu", 123_000),
            entry("Batticaloa", 596_000),
            entry("Ampara", 447_000),
            entry("Trincomalee", 443_000),
            entry("Kurunegala", 1_764_000),
            entry("Puttalam", 818_000),
            entry("Anuradhapura", 959_000),
            entry("Polonnaruwa", 447_000),
            entry("Badulla", 873_000),
            entry("Monaragala", 528_000),
            entry("Ratnapura", 1_144_000),
            entry("Kegalle", 868_000),
            entry("Kalmunai", 298_000)
    );

    private DistrictPopulations() {
    }

    public static double requireFor(District district) {
        return findFor(district).orElseThrow(() -> new IllegalArgumentException(
                "No population figure configured for district '" + district.getName() + "'"));
    }

    public static Optional<Double> findFor(District district) {
        if (district == null) {
            return Optional.empty();
        }
        Optional<Double> fromName = lookup(district.getName());
        if (fromName.isPresent()) {
            return fromName;
        }
        return lookup(district.getRdhsZone());
    }

    static Optional<Double> lookup(String name) {
        if (name == null || name.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(POPULATION_2025.get(normalize(name)));
    }

    private static Map.Entry<String, Double> entry(String name, double population) {
        return Map.entry(normalize(name), population);
    }

    private static String normalize(String name) {
        return name.trim().toLowerCase(Locale.ROOT);
    }
}
