package com.flinters.aggregator;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Ranks the aggregated campaigns and writes the two required top-10 CSV reports. */
final class ResultWriter {

    private static final int TOP_N = 10;
    private static final String CTR_HEADER = "campaign_id,impressions,clicks,spend,conversions,ctr";
    private static final String CPA_HEADER = "campaign_id,impressions,clicks,spend,conversions,cpa";

    void writeTop10Ctr(Map<String, CampaignStats> campaigns, Path outputDir) throws IOException {
        List<Map.Entry<String, CampaignStats>> ranked = campaigns.entrySet().stream()
                .filter(e -> e.getValue().ctr() != null)
                .sorted(Comparator.<Map.Entry<String, CampaignStats>>comparingDouble(e -> e.getValue().ctr())
                        .reversed())
                .limit(TOP_N)
                .toList();

        writeCsv(outputDir.resolve("top10_ctr.csv"), CTR_HEADER, ranked,
                (id, stats) -> formatRow(id, stats, stats.ctr()));
    }

    void writeTop10Cpa(Map<String, CampaignStats> campaigns, Path outputDir) throws IOException {
        List<Map.Entry<String, CampaignStats>> ranked = campaigns.entrySet().stream()
                .filter(e -> e.getValue().cpa() != null)
                .sorted(Comparator.comparingDouble(e -> e.getValue().cpa()))
                .limit(TOP_N)
                .toList();

        writeCsv(outputDir.resolve("top10_cpa.csv"), CPA_HEADER, ranked,
                (id, stats) -> formatRow(id, stats, stats.cpa()));
    }

    private interface RowFormatter {
        String format(String campaignId, CampaignStats stats);
    }

    private void writeCsv(Path file, String header, List<Map.Entry<String, CampaignStats>> ranked,
                           RowFormatter formatter) throws IOException {
        List<String> lines = new ArrayList<>(ranked.size() + 1);
        lines.add(header);
        for (Map.Entry<String, CampaignStats> entry : ranked) {
            lines.add(formatter.format(entry.getKey(), entry.getValue()));
        }

        try (BufferedWriter writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            for (String line : lines) {
                writer.write(line);
                writer.newLine();
            }
        }
    }

    private static String formatRow(String campaignId, CampaignStats stats, double metric) {
        return "%s,%d,%d,%s,%d,%s".formatted(
                campaignId,
                stats.impressions(),
                stats.clicks(),
                formatNumber(stats.spend()),
                stats.conversions(),
                formatNumber(metric));
    }

    private static String formatNumber(double value) {
        return String.format(Locale.ROOT, "%.6f", value);
    }
}
