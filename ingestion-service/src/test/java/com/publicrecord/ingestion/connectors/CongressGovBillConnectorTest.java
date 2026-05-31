package com.publicrecord.ingestion.connectors;

import com.publicrecord.common.events.RawContentEvent;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CongressGovBillConnectorTest {

    @Test
    void shouldMapCongressGovBillsToRawContentEvents() {
        CongressGovBillConnector connector = new CongressGovBillConnector(
                "test-key",
                119,
                "hr",
                5,
                "https://api.congress.gov/v3",
                url -> """
                        {
                          "bills": [
                            {
                              "congress": 119,
                              "number": "204",
                              "type": "HR",
                              "title": "Public Contract Disclosure Act",
                              "originChamber": "House",
                              "updateDate": "2026-04-18",
                              "url": "https://api.congress.gov/v3/bill/119/hr/204",
                              "latestAction": {
                                "actionDate": "2026-04-18",
                                "text": "Passed House by recorded vote."
                              }
                            }
                          ]
                        }
                        """
        );

        List<RawContentEvent> items = connector.fetch();

        assertThat(items).hasSize(1);
        RawContentEvent item = items.get(0);
        assertThat(item.getId()).isEqualTo("congress.gov:bill:119:hr:204");
        assertThat(item.getTitle()).contains("HR-204");
        assertThat(item.getContentType()).isEqualTo("bill");
        assertThat(item.getSource()).isEqualTo("Congress.gov");
        assertThat(item.getSourceUrl()).isEqualTo("https://api.congress.gov/v3/bill/119/hr/204");
        assertThat(item.getMetadata()).containsEntry("sourceQuality", "OFFICIAL_RECORD");
        assertThat(item.getMetadata()).containsEntry("billNumber", "HR-204");
    }
}
