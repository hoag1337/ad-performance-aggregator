package com.flinters.aggregator;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class CampaignStatsTest {

    @Test
    void accumulatesAcrossMultipleRows() {
        CampaignStats stats = new CampaignStats();

        stats.add(1000, 50, 25.0, 2);
        stats.add(2000, 150, 75.0, 3);

        assertEquals(3000, stats.impressions());
        assertEquals(200, stats.clicks());
        assertEquals(100.0, stats.spend());
        assertEquals(5, stats.conversions());
    }

    @Test
    void computesCtr() {
        CampaignStats stats = new CampaignStats();
        stats.add(1000, 50, 0, 0);

        assertEquals(0.05, stats.ctr());
    }

    @Test
    void ctrIsNullWhenNoImpressions() {
        CampaignStats stats = new CampaignStats();
        stats.add(0, 0, 0, 0);

        assertNull(stats.ctr());
    }

    @Test
    void computesCpa() {
        CampaignStats stats = new CampaignStats();
        stats.add(1000, 50, 100.0, 4);

        assertEquals(25.0, stats.cpa());
    }

    @Test
    void cpaIsNullWhenZeroConversions() {
        CampaignStats stats = new CampaignStats();
        stats.add(1000, 50, 100.0, 0);

        assertNull(stats.cpa());
    }
}
