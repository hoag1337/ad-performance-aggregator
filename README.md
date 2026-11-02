# Ad Performance Aggregator

A command-line tool that streams a large ad-performance CSV, aggregates metrics per campaign,
and writes out the top 10 campaigns by click-through rate (CTR) and the top 10 by cost-per-acquisition (CPA).

## Input

`ad_data.csv` with columns:

```
campaign_id,date,impressions,clicks,spend,conversions
```

A campaign can appear on multiple rows (one per day); all rows for the same `campaign_id` are summed.

## Output

Two CSV files written to the output directory:

- `top10_ctr.csv` — the 10 campaigns with the highest CTR (`total_clicks / total_impressions`)
- `top10_cpa.csv` — the 10 campaigns with the lowest CPA (`total_spend / total_conversions`),
  excluding campaigns with zero conversions (CPA is undefined for them)

Each row contains: `campaign_id,impressions,clicks,spend,conversions,<ctr|cpa>`.

## Requirements

- Java 25 (JDK)
- Maven 3.9+ (only needed to build; the build has no runtime dependencies besides the JDK)

## Build

```bash
mvn package
```

This produces `target/aggregator.jar`.

## Run

```bash
java -jar target/aggregator.jar --input ad_data.csv --output results/
```

The tool creates the output directory if it doesn't exist, and prints a summary to stdout:
rows processed, malformed rows skipped, distinct campaigns found, processing time, and peak
heap usage.

## Design

- **Streaming, single pass.** The file is read line-by-line with a buffered reader
  (`CsvAggregator`); nothing is loaded into memory as a whole. Memory usage is bounded by the
  number of **distinct campaigns**, not the number of rows — a per-campaign running total
  (`CampaignStats`: sums of impressions/clicks/spend/conversions) is kept in a `HashMap` and
  updated incrementally as each row is read. This lets the tool process files much larger than
  available heap.
- **No external CSV/number-crunching libraries.** The input format is simple, well-specified,
  comma-separated with no quoting, so a small hand-rolled splitter (`CsvRow.parse`, see
  `CsvRow.java`) avoids the overhead of `String.split`'s regex engine and avoids pulling in a
  CSV library dependency for something this constrained.
- **Malformed rows are skipped, not fatal.** A row is dropped (and counted) if it doesn't have
  exactly 6 fields, has a blank `campaign_id`, has a non-numeric impressions/clicks/spend/conversions
  field, or has a negative value in any of those fields. The header row (detected by its first
  field) is skipped once; a stray/duplicated header elsewhere in the file is also treated as
  malformed and skipped, rather than crashing the run.
- **Missing-file / bad-arguments handling.** The CLI validates `--input` points to an existing
  file and that `--output` can be created, and prints a clear error (exit code 1) instead of a
  stack trace otherwise.
- **CTR/CPA edge cases.** A campaign with zero total impressions has an undefined CTR and is
  excluded from the CTR ranking; a campaign with zero total conversions has an undefined CPA
  (division by zero) and is excluded from the CPA ranking, per the spec.
- **Peak memory** is measured via `java.lang.management.MemoryPoolMXBean` peak-usage tracking
  (heap pools), reset at the start of the run, rather than a rough `Runtime.totalMemory()`
  before/after delta.

## Benchmark (on the provided ~1 GB `ad_data.csv`, ~26.8M rows)

Run with default JVM heap settings (no `-Xmx` override), on a Windows machine:

```
Rows processed:       26,843,544
Malformed rows:       0
Distinct campaigns:   50
Processing time (ms): 13,892 (~13.9 s)
Peak heap usage (MB): 147
```

Memory stays flat regardless of row count because only one `CampaignStats` accumulator per
distinct campaign is kept in memory (50 here) — the single-pass streaming reader never holds
more than one line of the file at a time.

## Run with Docker (optional)

```bash
docker build -t ad-aggregator .
docker run --rm -v "$(pwd):/data" ad-aggregator --input /data/ad_data.csv --output /data/results
```

## Tests

```bash
mvn test
```

Covers: CSV row parsing (valid rows, whitespace, header detection, wrong field count,
non-numeric/negative fields), per-campaign accumulation across multiple rows, CTR/CPA edge
cases (zero impressions / zero conversions), end-to-end aggregation over a small CSV with
mixed valid/malformed/blank lines, and top-10 ranking/truncation/exclusion logic in the
output writer.
