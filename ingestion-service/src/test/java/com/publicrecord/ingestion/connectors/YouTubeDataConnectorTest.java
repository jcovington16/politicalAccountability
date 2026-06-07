package com.publicrecord.ingestion.connectors;

import com.publicrecord.common.events.RawContentEvent;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class YouTubeDataConnectorTest {

    @Test
    void shouldMapYouTubeSearchResultsToVideoEvents() {
        YouTubeDataConnector connector = new YouTubeDataConnector(
                "test-key",
                List.of("senate hearing healthcare"),
                List.of(),
                5,
                "https://youtube.test",
                url -> """
                        {
                          "items": [
                            {
                              "id": { "videoId": "abc123" },
                              "snippet": {
                                "publishedAt": "2026-05-29T16:00:00Z",
                                "channelId": "channel-1",
                                "channelTitle": "Official Hearing Channel",
                                "title": "Healthcare Oversight Hearing",
                                "description": "Public hearing with elected officials.",
                                "thumbnails": {
                                  "high": { "url": "https://img.youtube.test/abc123.jpg" }
                                }
                              }
                            }
                          ]
                        }
                        """
        );

        List<RawContentEvent> items = connector.fetch();

        assertThat(items).hasSize(1);
        RawContentEvent item = items.get(0);
        assertThat(item.getTitle()).isEqualTo("Healthcare Oversight Hearing");
        assertThat(item.getContentType()).isEqualTo("video");
        assertThat(item.getSource()).isEqualTo("YouTube");
        assertThat(item.getSourceUrl()).isEqualTo("https://www.youtube.com/watch?v=abc123");
        assertThat(item.getMediaUrl()).isEqualTo("https://www.youtube.com/watch?v=abc123");
        assertThat(item.getPublishedDate()).startsWith("2026-05-29T16:00");
        assertThat(item.getMetadata()).containsEntry("sourceQuality", "PUBLIC_MEDIA");
        assertThat(item.getMetadata()).containsEntry("sourceCollectionRole", "PUBLIC_VIDEO_DISCOVERY");
        assertThat(item.getMetadata()).containsEntry("videoId", "abc123");
    }
}
