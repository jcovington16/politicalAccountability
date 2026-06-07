package com.publicrecord.ingestion.connectors;

import com.publicrecord.common.events.RawContentEvent;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class NewsScraperConnectorTest {

    @Test
    void shouldMapAllowlistedPublicArticleMetadataToRawContentEvent() {
        NewsScraperConnector connector = new NewsScraperConnector(
                List.of(new NewsScraperTarget("https://example.org/politics/story", "Example News")),
                10,
                true,
                java.util.Set.of("example.org"),
                url -> """
                        <html>
                          <head>
                            <title>Fallback Title</title>
                            <link rel="canonical" href="https://example.org/politics/story" />
                            <meta property="og:title" content="Governor signs accountability bill" />
                            <meta property="og:description" content="Coverage of a public accountability bill." />
                            <meta property="og:image" content="https://example.org/image.jpg" />
                            <meta property="article:published_time" content="2026-06-05T10:15:00Z" />
                          </head>
                          <body><article>Public article body.</article></body>
                        </html>
                        """,
                url -> true
        );

        List<RawContentEvent> items = connector.fetch();

        assertThat(items).hasSize(1);
        RawContentEvent item = items.get(0);
        assertThat(item.getTitle()).isEqualTo("Governor signs accountability bill");
        assertThat(item.getContentType()).isEqualTo("article");
        assertThat(item.getSource()).isEqualTo("Example News");
        assertThat(item.getSourceUrl()).isEqualTo("https://example.org/politics/story");
        assertThat(item.getMediaUrl()).isEqualTo("https://example.org/image.jpg");
        assertThat(item.getPublishedDate()).startsWith("2026-06-05T10:15");
        assertThat(item.getMetadata()).containsEntry("sourceQuality", "PUBLIC_MEDIA");
        assertThat(item.getMetadata()).containsEntry("sourceCollectionRole", "ALLOWLISTED_PUBLIC_PAGE");
    }

    @Test
    void shouldSkipNonHttpsTargetsWhenHttpsIsRequired() {
        NewsScraperConnector connector = new NewsScraperConnector(
                List.of(new NewsScraperTarget("http://example.org/politics/story", "Example News")),
                10,
                true,
                java.util.Set.of("example.org"),
                url -> "<html><head><title>Should not fetch</title></head></html>",
                url -> true
        );

        assertThat(connector.fetch()).isEmpty();
    }

    @Test
    void shouldSkipTargetsBlockedByRobotsPolicy() {
        NewsScraperConnector connector = new NewsScraperConnector(
                List.of(new NewsScraperTarget("https://example.org/politics/story", "Example News")),
                10,
                true,
                java.util.Set.of("example.org"),
                url -> "<html><head><title>Should not fetch</title></head></html>",
                url -> false
        );

        assertThat(connector.fetch()).isEmpty();
    }
}
