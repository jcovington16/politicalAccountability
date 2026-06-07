package com.publicrecord.ingestion.connectors;

import com.publicrecord.common.events.RawContentEvent;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class GdeltDocConnectorTest {

    @Test
    void shouldMapGdeltArticlesToRawContentEvents() {
        GdeltDocConnector connector = new GdeltDocConnector(
                List.of("Marco Rubio healthcare"),
                5,
                "https://api.gdelt.test/doc",
                url -> """
                        {
                          "articles": [
                            {
                              "url": "https://example.org/story",
                              "title": "Rubio discusses healthcare legislation",
                              "seendate": "20260529T120000Z",
                              "domain": "example.org",
                              "language": "English",
                              "sourcecountry": "United States",
                              "tone": -1.25,
                              "socialimage": "https://example.org/image.jpg"
                            }
                          ]
                        }
                        """
        );

        List<RawContentEvent> items = connector.fetch();

        assertThat(items).hasSize(1);
        RawContentEvent item = items.get(0);
        assertThat(item.getTitle()).isEqualTo("Rubio discusses healthcare legislation");
        assertThat(item.getContentType()).isEqualTo("article");
        assertThat(item.getSource()).isEqualTo("GDELT");
        assertThat(item.getSourceUrl()).isEqualTo("https://example.org/story");
        assertThat(item.getMediaUrl()).isEqualTo("https://example.org/image.jpg");
        assertThat(item.getPublishedDate()).startsWith("2026-05-29T12:00");
        assertThat(item.getMetadata()).containsEntry("sourceQuality", "MEDIA_AGGREGATOR");
        assertThat(item.getMetadata()).containsEntry("sourceCollectionRole", "DISCOVERY");
        assertThat(item.getMetadata()).containsEntry("query", "Marco Rubio healthcare");
    }
}
