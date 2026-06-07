package com.publicrecord.ingestion.connectors;

import com.publicrecord.common.events.RawContentEvent;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RSSFeedConnectorTest {

    @Test
    void shouldMapRssItemsToRawContentEvents() {
        RSSFeedConnector connector = new RSSFeedConnector(
                "https://example.org/feed.xml",
                "Example News",
                url -> """
                        <?xml version="1.0" encoding="UTF-8"?>
                        <rss version="2.0">
                          <channel>
                            <title>Example News</title>
                            <item>
                              <title>Governor signs public records bill</title>
                              <link>https://example.org/public-records-bill</link>
                              <guid>story-1</guid>
                              <pubDate>Fri, 29 May 2026 14:30:00 GMT</pubDate>
                              <description>Coverage of a public records bill signing.</description>
                              <enclosure url="https://example.org/audio.mp3" type="audio/mpeg" />
                            </item>
                          </channel>
                        </rss>
                        """
        );

        List<RawContentEvent> items = connector.fetch();

        assertThat(items).hasSize(1);
        RawContentEvent item = items.get(0);
        assertThat(item.getTitle()).isEqualTo("Governor signs public records bill");
        assertThat(item.getContentType()).isEqualTo("article");
        assertThat(item.getSource()).isEqualTo("Example News");
        assertThat(item.getSourceUrl()).isEqualTo("https://example.org/public-records-bill");
        assertThat(item.getMediaUrl()).isEqualTo("https://example.org/audio.mp3");
        assertThat(item.getPublishedDate()).startsWith("2026-05-29T14:30");
        assertThat(item.getMetadata()).containsEntry("sourceQuality", "PUBLIC_MEDIA");
        assertThat(item.getMetadata()).containsEntry("feedUrl", "https://example.org/feed.xml");
    }
}
