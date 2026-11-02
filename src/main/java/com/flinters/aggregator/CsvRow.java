package com.flinters.aggregator;

/** A single parsed, valid data row. Values are already range-checked (non-negative). */
record CsvRow(String campaignId, long impressions, long clicks, double spend, long conversions) {

    private static final int FIELD_COUNT = 6;

    /**
     * Parses one CSV line of the form:
     * campaign_id,date,impressions,clicks,spend,conversions
     *
     * @return the parsed row, or null if the line is the header or is malformed in any way
     *         (wrong field count, non-numeric/negative numeric field, blank campaign id).
     */
    static CsvRow parse(String line) {
        String[] fields = splitOnComma(line, FIELD_COUNT);
        if (fields == null) {
            return null;
        }

        String campaignId = fields[0].trim();
        if (campaignId.isEmpty() || campaignId.equalsIgnoreCase("campaign_id")) {
            return null; // blank id, or a stray/duplicated header row
        }

        try {
            long impressions = Long.parseLong(fields[2].trim());
            long clicks = Long.parseLong(fields[3].trim());
            double spend = Double.parseDouble(fields[4].trim());
            long conversions = Long.parseLong(fields[5].trim());

            if (impressions < 0 || clicks < 0 || spend < 0 || conversions < 0) {
                return null;
            }
            return new CsvRow(campaignId, impressions, clicks, spend, conversions);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Splits a line into exactly {@code expectedFields} comma-separated fields without the
     * overhead of {@code String.split}'s regex engine. Returns null if the field count doesn't
     * match (too few or too many commas).
     */
    private static String[] splitOnComma(String line, int expectedFields) {
        String[] fields = new String[expectedFields];
        int fieldIndex = 0;
        int start = 0;
        int length = line.length();

        for (int i = 0; i < length; i++) {
            if (line.charAt(i) == ',') {
                if (fieldIndex >= expectedFields) {
                    return null; // too many fields
                }
                fields[fieldIndex++] = line.substring(start, i);
                start = i + 1;
            }
        }
        if (fieldIndex != expectedFields - 1) {
            return null; // too few commas
        }
        fields[fieldIndex] = line.substring(start);
        return fields;
    }
}
