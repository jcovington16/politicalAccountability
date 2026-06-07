package com.publicrecord.ingestion.connectors;

import com.publicrecord.common.events.RawContentEvent;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class GuardianOpenPlatformConnectorTest {

    @Test
    void shouldMapGuardianResultsToArticleEvents() {
        GuardianOpenPlatformConnector connector = new GuardianOpenPlatformConnector(
                "test-key",
                List.of("Marco Rubio healthcare"),
                5,
                "https://guardian.test",
                url -> """
                        {
                          "response": {
                            "status": "ok",
                            "results": [
                              {
                                "id": "us-news/2026/may/29/rubio-healthcare",
                                "sectionId": "us-news",
                                "sectionName": "US news",
                                "webPublicationDate": "2026-05-29T13:15:00Z",
                                "webTitle": "Rubio addresses healthcare legislation",
                                "webUrl": "https://www.theguardian.com/us-news/2026/may/29/rubio-healthcare",
                                "pillarName": "News",
                                "fields": {
                                  "trailText": "A summary of the article.",
                                  "bodyText": "Full article text made available by the API.",
                                  "thumbnail": "https://media.guim.co.uk/image.jpg"
                                },
                                "tags": [
                                  { "webTitle": "Marco Rubio" },
                                  { "webTitle": "Healthcare" }
                                ]
                              }
                            ]
                          }
                        }
                        """
        );

        List<RawContentEvent> items = connector.fetch();

        assertThat(items).hasSize(1);
        RawContentEvent item = items.get(0);
        assertThat(item.getTitle()).isEqualTo("Rubio addresses healthcare legislation");
        assertThat(item.getContentType()).isEqualTo("article");
        assertThat(item.getSource()).isEqualTo("The Guardian");
        assertThat(item.getSourceUrl()).isEqualTo("https://www.theguardian.com/us-news/2026/may/29/rubio-healthcare");
        assertThat(item.getMediaUrl()).isEqualTo("https://media.guim.co.uk/image.jpg");
        assertThat(item.getPublishedDate()).startsWith("2026-05-29T13:15");
        assertThat(item.getTextBody()).isEqualTo("Full article text made available by the API.");
        assertThat(item.getMetadata()).containsEntry("sourceQuality", "PUBLIC_MEDIA");
        assertThat(item.getMetadata()).containsEntry("sourceCollectionRole", "PUBLISHER_API");
        assertThat(item.getMetadata()).containsEntry("query", "Marco Rubio healthcare");
    }
}
