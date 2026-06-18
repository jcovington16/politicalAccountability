package com.publicrecord.api.resources;

import com.publicrecord.api.dto.TrustScoreRequest;
import com.publicrecord.common.trust.InformationType;
import com.publicrecord.common.trust.SourceQuality;
import com.publicrecord.common.trust.TrustScore;
import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import io.dropwizard.testing.junit5.ResourceExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.Test;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response;
import java.time.LocalDate;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(DropwizardExtensionsSupport.class)
class TrustResourceTest {

    private static final ResourceExtension RESOURCES = ResourceExtension.builder()
            .addResource(new TrustResource())
            .build();

    private final TrustResource resource = new TrustResource();

    @Test
    void scoreRequiresInformationTypeAndSourceQuality() {
        Response response = resource.score(new TrustScoreRequest(null, SourceQuality.OFFICIAL_RECORD, 1, null));

        assertThat(response.getStatus()).isEqualTo(400);
        assertThat(response.getEntity()).isEqualTo("informationType and sourceQuality are required");
    }

    @Test
    void scoreReturnsTrustScore() {
        Response response = resource.score(new TrustScoreRequest(
                InformationType.VOTING_RECORD,
                SourceQuality.OFFICIAL_RECORD,
                4,
                LocalDate.now()
        ));

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getEntity()).isInstanceOf(TrustScore.class);
        TrustScore score = (TrustScore) response.getEntity();
        assertThat(score.getInformationType()).isEqualTo(InformationType.VOTING_RECORD);
        assertThat(score.getScore()).isGreaterThan(0.8);
    }

    @Test
    void scoreAcceptsJsonRequest() {
        Response response = RESOURCES.target("/trust/score")
                .request()
                .post(Entity.json(Map.of(
                        "informationType", "VOTING_RECORD",
                        "sourceQuality", "OFFICIAL_RECORD",
                        "citationCount", 2
                )));

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.readEntity(String.class)).contains("\"score\"");
    }
}
