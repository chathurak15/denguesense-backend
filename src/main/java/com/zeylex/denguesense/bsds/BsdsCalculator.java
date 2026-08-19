package com.zeylex.denguesense.bsds;

public final class BsdsCalculator {

    public static final double PER_HUNDRED_THOUSAND = 100_000.0;

    private BsdsCalculator() {
    }

    public static double score(long confirmedBreedingSites, double populationTotal) {
        if (confirmedBreedingSites < 0) {
            throw new IllegalArgumentException("confirmed breeding sites cannot be negative");
        }
        if (populationTotal <= 0) {
            throw new IllegalArgumentException("population must be positive");
        }
        return confirmedBreedingSites * PER_HUNDRED_THOUSAND / populationTotal;
    }
}
