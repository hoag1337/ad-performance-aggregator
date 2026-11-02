package com.flinters.aggregator;

/** Mutable running totals for a single campaign. Not thread-safe by design: each campaign's
 * accumulator is only ever touched by the single-threaded streaming reader. */
final class CampaignStats {

    private long impressions;
    private long clicks;
    private double spend;
    private long conversions;

    void add(long impressions, long clicks, double spend, long conversions) {
        this.impressions += impressions;
        this.clicks += clicks;
        this.spend += spend;
        this.conversions += conversions;
    }

    long impressions() {
        return impressions;
    }

    long clicks() {
        return clicks;
    }

    double spend() {
        return spend;
    }

    long conversions() {
        return conversions;
    }

    /** Click-through rate, or null when total impressions is zero (undefined, e.g. all-zero rows). */
    Double ctr() {
        if (impressions == 0) {
            return null;
        }
        return (double) clicks / (double) impressions;
    }

    /** Cost per acquisition, or null when the campaign has zero conversions (undefined). */
    Double cpa() {
        if (conversions == 0) {
            return null;
        }
        return spend / (double) conversions;
    }
}
