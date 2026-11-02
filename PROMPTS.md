# AI Prompt

This solution was built with the help of Claude Code (Anthropic). Below is the structured
prompt used to drive the implementation.

---

## 1. Goal

Build a command-line tool that processes a large (~1GB) ad-performance CSV file and produces
two ranked reports: the top 10 campaigns by click-through rate (CTR) and the top 10 campaigns
by cost-per-acquisition (CPA). The tool must aggregate per-campaign totals across all rows
(a campaign appears on multiple rows, one per day) and must handle a file of this size without
loading it entirely into memory.

## 2. Acceptance criteria

- Reads `ad_data.csv` with columns `campaign_id,date,impressions,clicks,spend,conversions`.
- For each distinct `campaign_id`, computes: total impressions, total clicks, total spend,
  total conversions, CTR (`total_clicks / total_impressions`), and CPA
  (`total_spend / total_conversions`).
- Writes `top10_ctr.csv`: the 10 campaigns with the highest CTR.
- Writes `top10_cpa.csv`: the 10 campaigns with the lowest CPA, **excluding** any campaign
  with zero conversions (CPA is undefined in that case, not zero or infinite).
- Runs as a CLI: `<entrypoint> --input ad_data.csv --output results/`.
- Processes the file in a memory-efficient way — memory use must not scale with the number of
  rows in the file, only with the number of distinct campaigns.
- Finishes in a reasonable time on a ~1GB / tens-of-millions-of-rows file.
- Exits with a clear, non-crashing error (not a stack trace) when the input file is missing or
  the output directory can't be created.
- Has unit tests covering parsing, aggregation, and ranking/output logic, including the edge
  cases below.
- Ships with a README covering setup, build/run instructions, libraries used, and the measured
  processing time and peak memory from a real run against the full dataset.
- No dead code, no commented-out code, no unused abstractions.

## 3. Tech stack

- **Language/runtime:** Java 25 (JDK), no external CSV or data-processing libraries — the
  input format is simple enough that a hand-rolled, allocation-light parser is preferable to a
  dependency.
- **Build tool:** Maven.
- **Testing:** JUnit 5.
- **Packaging:** a single executable jar (`maven-jar-plugin` with a `Main-Class` manifest
  entry); no fat-jar/shade plugin needed since there are no runtime dependencies.
- **Optional:** a multi-stage Dockerfile (Maven build stage + slim JRE runtime stage) for
  running without a local JDK installed.

## 4. Coding conventions

- Small, single-responsibility classes: one class per concern (row parsing, per-campaign
  accumulation, the streaming read loop, ranking/output, CLI argument parsing, the CLI entry
  point) rather than one monolithic `Main`.
- Prefer immutable `record` types for plain data carriers (a parsed row, an aggregation
  result); use a small mutable accumulator only where running totals genuinely need to mutate
  in place for performance (the per-campaign stats object).
- Package-private visibility by default; only the CLI entry point class is `public`.
- No comments explaining *what* code does — names should make that obvious. Only comment a
  genuinely non-obvious *why* (e.g. a performance tradeoff or an edge-case rule that isn't
  visible from the code itself).
- Validate only at the boundaries: CLI arguments and file I/O are checked and fail with a
  clear message; internal method calls trust their inputs.
- No speculative abstractions, config flags, or extensibility hooks beyond what the stated
  requirements call for.
- Tests are organized one test class per production class, named for the behavior under test
  rather than implementation details, and use descriptive method names instead of comments.

## 5. Edge cases the agent must handle explicitly

When implementing and testing, explicitly handle (and write a test for) each of these:

- **Malformed row:** wrong number of fields, non-numeric numeric field, a negative value in a
  numeric field. Skip the row and count it as malformed; do not abort the run.
- **Blank or missing `campaign_id`.** Skip the row.
- **Header row.** Detect and skip it once at the top of the file; also treat a stray/duplicated
  header row appearing later in the file as malformed input, not a crash.
- **Blank lines.** Skip them without counting them as data or malformed rows.
- **Zero total impressions for a campaign.** CTR is undefined (would divide by zero) — exclude
  that campaign from the CTR ranking rather than producing `NaN`/`Infinity`.
- **Zero total conversions for a campaign.** CPA is undefined — exclude that campaign from the
  CPA ranking per the acceptance criteria, rather than erroring or outputting a sentinel value.
- **Fewer than 10 eligible campaigns.** Output whatever is available (fewer than 10 rows)
  rather than erroring or padding.
- **Missing input file.** Print a clear error and exit non-zero; do not throw an uncaught
  exception.
- **Output directory doesn't exist.** Create it rather than failing.
- **Whitespace around field values.** Trim before parsing so rows like `" camp-1 , 1000 "`
  still parse correctly.
