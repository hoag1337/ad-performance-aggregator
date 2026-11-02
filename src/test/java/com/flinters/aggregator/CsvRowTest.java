package com.flinters.aggregator;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class CsvRowTest {

    @Test
    void parsesValidRow() {
        CsvRow row = CsvRow.parse("camp-1,2024-01-01,1000,50,25.5,3");

        assertEquals("camp-1", row.campaignId());
        assertEquals(1000, row.impressions());
        assertEquals(50, row.clicks());
        assertEquals(25.5, row.spend());
        assertEquals(3, row.conversions());
    }

    @Test
    void trimsWhitespaceAroundFields() {
        CsvRow row = CsvRow.parse(" camp-1 , 2024-01-01 , 1000 , 50 , 25.5 , 3 ");

        assertEquals("camp-1", row.campaignId());
        assertEquals(1000, row.impressions());
    }

    @Test
    void rejectsHeaderRow() {
        assertNull(CsvRow.parse("campaign_id,date,impressions,clicks,spend,conversions"));
        assertNull(CsvRow.parse("Campaign_ID,date,impressions,clicks,spend,conversions"));
    }

    @Test
    void rejectsBlankCampaignId() {
        assertNull(CsvRow.parse(",2024-01-01,1000,50,25.5,3"));
    }

    @Test
    void rejectsTooFewFields() {
        assertNull(CsvRow.parse("camp-1,2024-01-01,1000,50,25.5"));
    }

    @Test
    void rejectsTooManyFields() {
        assertNull(CsvRow.parse("camp-1,2024-01-01,1000,50,25.5,3,extra"));
    }

    @Test
    void rejectsNonNumericField() {
        assertNull(CsvRow.parse("camp-1,2024-01-01,abc,50,25.5,3"));
        assertNull(CsvRow.parse("camp-1,2024-01-01,1000,50,notanumber,3"));
    }

    @Test
    void rejectsNegativeNumericField() {
        assertNull(CsvRow.parse("camp-1,2024-01-01,-1000,50,25.5,3"));
        assertNull(CsvRow.parse("camp-1,2024-01-01,1000,50,-25.5,3"));
    }

    @Test
    void acceptsZeroValues() {
        CsvRow row = CsvRow.parse("camp-1,2024-01-01,0,0,0,0");

        assertEquals(0, row.impressions());
        assertEquals(0, row.conversions());
    }
}
