package com.publicrecord.ingestion.connectors;

import com.publicrecord.common.events.RawContentEvent;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SocialMediaConnectorsTest {

    @Test
    void shouldMapBlueskyPostsToSocialPostEvents() {
        BlueskyPostConnector connector = new BlueskyPostConnector(
                List.of("Marco Rubio"),
                10,
                "https://public.api.bsky.test/xrpc/app.bsky.feed.searchPosts",
                url -> """
                        {
                          "posts": [
                            {
                              "uri": "at://did:plc:abc/app.bsky.feed.post/3abc",
                              "cid": "cid-1",
                              "author": {
                                "did": "did:plc:abc",
                                "handle": "rubio.example",
                                "displayName": "Marco Rubio"
                              },
                              "record": {
                                "text": "Public statement about a committee hearing.",
                                "createdAt": "2026-06-01T12:00:00Z"
                              }
                            }
                          ]
                        }
                        """
        );

        List<RawContentEvent> items = connector.fetch();

        assertThat(items).hasSize(1);
        RawContentEvent item = items.get(0);
        assertThat(item.getContentType()).isEqualTo("social_post");
        assertThat(item.getSource()).isEqualTo("Bluesky");
        assertThat(item.getSourceUrl()).isEqualTo("https://bsky.app/profile/rubio.example/post/3abc");
        assertThat(item.getMetadata()).containsEntry("sourceQuality", "SOCIAL_MEDIA");
        assertThat(item.getMetadata()).containsEntry("platform", "BLUESKY");
        assertThat(item.getMetadata()).containsEntry("handle", "rubio.example");
    }

    @Test
    void shouldMapXPostsToSocialPostEventsWithBearerToken() {
        XPostConnector connector = new XPostConnector(
                "test-token",
                List.of("from:marcorubio"),
                10,
                "https://api.x.test/2/tweets/search/recent",
                new HeaderAwareConnectorHttpClient() {
                    @Override
                    public String get(String url) {
                        throw new AssertionError("X connector should use the header-aware GET method");
                    }

                    @Override
                    public String get(String url, Map<String, String> headers) {
                        assertThat(headers).containsEntry("Authorization", "Bearer test-token");
                        return """
                                {
                                  "data": [
                                    {
                                      "id": "123",
                                      "text": "Public post about constituent services.",
                                      "author_id": "42",
                                      "created_at": "2026-06-01T13:00:00Z"
                                    }
                                  ],
                                  "includes": {
                                    "users": [
                                      {
                                        "id": "42",
                                        "username": "marcorubio",
                                        "name": "Marco Rubio",
                                        "verified": true
                                      }
                                    ]
                                  }
                                }
                                """;
                    }
                }
        );

        List<RawContentEvent> items = connector.fetch();

        assertThat(items).hasSize(1);
        RawContentEvent item = items.get(0);
        assertThat(item.getContentType()).isEqualTo("social_post");
        assertThat(item.getSource()).isEqualTo("X");
        assertThat(item.getSourceUrl()).isEqualTo("https://x.com/marcorubio/status/123");
        assertThat(item.getMetadata()).containsEntry("sourceQuality", "SOCIAL_MEDIA");
        assertThat(item.getMetadata()).containsEntry("platform", "X");
        assertThat(item.getMetadata()).containsEntry("username", "marcorubio");
    }
}
