package com.flinters.aggregator;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryType;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * CLI entry point. Streams a large ad-performance CSV and writes top10_ctr.csv and
 * top10_cpa.csv into the given output directory.
 *
 * Usage: aggregator --input ad_data.csv --output results/
 */
public final class Aggregator {

    public static void main(String[] args) {
        CliArgs cliArgs;
        try {
            cliArgs = CliArgs.parse(args);
        } catch (IllegalArgumentException e) {
            System.err.println("Error: " + e.getMessage());
            System.err.println(CliArgs.USAGE);
            System.exit(1);
            return;
        }

        Path input = Path.of(cliArgs.input());
        Path outputDir = Path.of(cliArgs.output());

        if (!Files.isRegularFile(input)) {
            System.err.println("Error: input file not found: " + input);
            System.exit(1);
            return;
        }

        try {
            Files.createDirectories(outputDir);
        } catch (IOException e) {
            System.err.println("Error: could not create output directory " + outputDir + ": " + e.getMessage());
            System.exit(1);
            return;
        }

        List<MemoryPoolMXBean> heapPools = ManagementFactory.getMemoryPoolMXBeans().stream()
                .filter(pool -> pool.getType() == MemoryType.HEAP)
                .toList();
        heapPools.forEach(MemoryPoolMXBean::resetPeakUsage);
        long startTime = System.nanoTime();

        AggregationResult result;
        try {
            result = new CsvAggregator().aggregate(input);
        } catch (IOException e) {
            System.err.println("Error: failed reading " + input + ": " + e.getMessage());
            System.exit(1);
            return;
        }

        try {
            ResultWriter writer = new ResultWriter();
            writer.writeTop10Ctr(result.campaigns(), outputDir);
            writer.writeTop10Cpa(result.campaigns(), outputDir);
        } catch (IOException e) {
            System.err.println("Error: failed writing output to " + outputDir + ": " + e.getMessage());
            System.exit(1);
            return;
        }

        long elapsedMs = (System.nanoTime() - startTime) / 1_000_000;
        long peakHeapBytes = heapPools.stream().mapToLong(pool -> pool.getPeakUsage().getUsed()).sum();
        long peakHeapMb = peakHeapBytes / (1024 * 1024);

        System.out.println("Rows processed:      " + result.totalRows());
        System.out.println("Malformed rows:       " + result.malformedRows());
        System.out.println("Distinct campaigns:   " + result.campaigns().size());
        System.out.println("Processing time (ms): " + elapsedMs);
        System.out.println("Peak heap usage (MB): " + peakHeapMb);
        System.out.println("Output written to:    " + outputDir.toAbsolutePath());
    }
}
