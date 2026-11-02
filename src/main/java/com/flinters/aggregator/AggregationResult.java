package com.flinters.aggregator;

import java.util.Map;

/** Outcome of a full streaming pass over the input file. */
record AggregationResult(Map<String, CampaignStats> campaigns, long totalRows, long malformedRows) {
}
