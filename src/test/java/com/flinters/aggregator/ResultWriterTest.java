package com.flinters.aggregator;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResultWriterTest {

    @Test
    void ranksByCtrDescendingAndCapsAtTen(@TempDir Path tempDir) throws IOException {
        Map<String, CampaignStats> campaigns = new LinkedHashMap<>();
        for (int i = 0; i < 15; i++) {
            CampaignStats stats = new CampaignStats();
            // campaign i has CTR = i / 100 (0%, 1%, ..., 14%)
            stats.add(100, i, 0, 0);
            campaigns.put("camp-" + i, stats);
        }

        new ResultWriter().writeTop10Ctr(campaigns, tempDir);

        List<String> lines = Files.readAllLines(tempDir.resolve("top10_ctr.csv"));
        assertEquals(11, lines.size()); // header + 10 rows
        assertTrue(lines.get(1).startsWith("camp-14,")); // highest CTR first
        assertTrue(lines.get(10).startsWith("camp-5,")); // 10th highest
    }

    @Test
    void ranksByCpaAscendingAndExcludesZeroConversions(@TempDir Path tempDir) throws IOException {
        Map<String, CampaignStats> campaigns = new LinkedHashMap<>();

        CampaignStats cheap = new CampaignStats();
        cheap.add(1000, 50, 10.0, 5); // cpa = 2.0
        campaigns.put("cheap", cheap);

        CampaignStats expensive = new CampaignStats();
        expensive.add(1000, 50, 100.0, 5); // cpa = 20.0
        campaigns.put("expensive", expensive);

        CampaignStats noConversions = new CampaignStats();
        noConversions.add(1000, 50, 100.0, 0); // cpa undefined, must be excluded
        campaigns.put("no-conversions", noConversions);

        new ResultWriter().writeTop10Cpa(campaigns, tempDir);

        List<String> lines = Files.readAllLines(tempDir.resolve("top10_cpa.csv"));
        assertEquals(3, lines.size()); // header + 2 rows (zero-conversion campaign excluded)
        assertTrue(lines.get(1).startsWith("cheap,"));
        assertTrue(lines.get(2).startsWith("expensive,"));
        assertFalse(lines.stream().anyMatch(l -> l.startsWith("no-conversions")));
    }
}
