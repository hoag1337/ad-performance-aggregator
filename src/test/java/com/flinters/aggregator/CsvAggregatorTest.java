package com.flinters.aggregator;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CsvAggregatorTest {

    @Test
    void aggregatesMultipleRowsPerCampaignAndSkipsMalformedOnes(@TempDir Path tempDir) throws IOException {
        Path csv = tempDir.resolve("ad_data.csv");
        Files.write(csv, List.of(
                "campaign_id,date,impressions,clicks,spend,conversions",
                "camp-a,2024-01-01,1000,50,25.00,2",
                "camp-a,2024-01-02,2000,100,50.00,3",
                "camp-b,2024-01-01,500,10,5.00,0",
                "", // blank line, ignored
                "camp-c,2024-01-01,not-a-number,10,5.00,1", // malformed: non-numeric
                "camp-c,2024-01-02,100,10,5.00" // malformed: too few fields
        ), StandardCharsets.UTF_8);

        AggregationResult result = new CsvAggregator().aggregate(csv);

        assertEquals(5, result.totalRows());
        assertEquals(2, result.malformedRows());

        Map<String, CampaignStats> campaigns = result.campaigns();
        assertEquals(2, campaigns.size());

        CampaignStats campA = campaigns.get("camp-a");
        assertEquals(3000, campA.impressions());
        assertEquals(150, campA.clicks());
        assertEquals(75.0, campA.spend());
        assertEquals(5, campA.conversions());

        CampaignStats campB = campaigns.get("camp-b");
        assertEquals(500, campB.impressions());
        assertEquals(0, campB.conversions());
    }

    @Test
    void handlesFileWithoutHeader(@TempDir Path tempDir) throws IOException {
        Path csv = tempDir.resolve("ad_data.csv");
        Files.write(csv, List.of("camp-a,2024-01-01,1000,50,25.00,2"), StandardCharsets.UTF_8);

        AggregationResult result = new CsvAggregator().aggregate(csv);

        assertEquals(1, result.totalRows());
        assertEquals(0, result.malformedRows());
        assertTrue(result.campaigns().containsKey("camp-a"));
    }
}
