package com.publicrecord.ingestion.connectors;

import com.publicrecord.common.events.RawContentEvent;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class GovInfoPackageConnectorTest {

    @Test
    void shouldMapGovInfoPackageSummariesToRawContentEvents() {
        GovInfoPackageConnector connector = new GovInfoPackageConnector(
                "test-key",
                "BILLS",
                "2026-01-01T00:00:00Z",
                5,
                "https://api.govinfo.gov",
                url -> {
                    if (url.contains("/collections/BILLS/")) {
                        return """
                                {
                                  "packages": [
                                    {
                                      "packageId": "BILLS-119hr204ih",
                                      "lastModified": "2026-01-22T10:00:00Z",
                                      "packageLink": "https://api.govinfo.gov/packages/BILLS-119hr204ih/summary"
                                    }
                                  ]
                                }
                                """;
                    }

                    return """
                            {
                              "title": "Public Contract Disclosure Act",
                              "collectionCode": "BILLS",
                              "collectionName": "Congressional Bills",
                              "category": "Bills and Statutes",
                              "dateIssued": "2026-01-22",
                              "detailsLink": "https://www.govinfo.gov/app/details/BILLS-119hr204ih",
                              "branch": "legislative",
                              "documentType": "BILLS",
                              "congress": "119",
                              "session": "1",
                              "lastModified": "2026-01-22T10:00:00Z",
                              "download": {
                                "pdfLink": "https://api.govinfo.gov/packages/BILLS-119hr204ih/pdf",
                                "txtLink": "https://api.govinfo.gov/packages/BILLS-119hr204ih/htm",
                                "modsLink": "https://api.govinfo.gov/packages/BILLS-119hr204ih/mods",
                                "zipLink": "https://api.govinfo.gov/packages/BILLS-119hr204ih/zip"
                              }
                            }
                            """;
                }
        );

        List<RawContentEvent> items = connector.fetch();

        assertThat(items).hasSize(1);
        RawContentEvent item = items.get(0);
        assertThat(item.getId()).isEqualTo("govinfo:package:BILLS-119hr204ih");
        assertThat(item.getTitle()).isEqualTo("Public Contract Disclosure Act");
        assertThat(item.getContentType()).isEqualTo("official_document");
        assertThat(item.getSource()).isEqualTo("GovInfo");
        assertThat(item.getMediaUrl()).isEqualTo("https://api.govinfo.gov/packages/BILLS-119hr204ih/pdf");
        assertThat(item.getMetadata()).containsEntry("sourceQuality", "OFFICIAL_RECORD");
        assertThat(item.getMetadata()).containsEntry("packageId", "BILLS-119hr204ih");
        assertThat(item.getMetadata()).containsEntry("collectionCode", "BILLS");
    }
}
