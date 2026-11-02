package com.flinters.aggregator;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * Streams the input CSV one line at a time and folds it into per-campaign running totals.
 * Memory usage is bounded by the number of distinct campaigns, not the number of rows, so this
 * scales to files far larger than available heap.
 */
final class CsvAggregator {

    AggregationResult aggregate(Path input) throws IOException {
        Map<String, CampaignStats> campaigns = new HashMap<>();
        long totalRows = 0;
        long malformedRows = 0;

        try (BufferedReader reader = Files.newBufferedReader(input, StandardCharsets.UTF_8)) {
            String line = reader.readLine();
            if (line != null && isHeader(line)) {
                line = reader.readLine();
            }

            while (line != null) {
                if (!line.isBlank()) {
                    totalRows++;

                    CsvRow row = CsvRow.parse(line);
                    if (row == null) {
                        malformedRows++;
                    } else {
                        campaigns
                                .computeIfAbsent(row.campaignId(), id -> new CampaignStats())
                                .add(row.impressions(), row.clicks(), row.spend(), row.conversions());
                    }
                }
                line = reader.readLine();
            }
        }

        return new AggregationResult(campaigns, totalRows, malformedRows);
    }

    private static boolean isHeader(String firstLine) {
        return firstLine.trim().toLowerCase().startsWith("campaign_id");
    }
}
