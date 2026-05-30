package com.publicrecord.api.resources;

import com.publicrecord.api.dto.TrustScoreRequest;
import com.publicrecord.common.trust.InformationType;
import com.publicrecord.common.trust.SourceQuality;
import com.publicrecord.common.trust.TrustScore;
import org.junit.jupiter.api.Test;

import javax.ws.rs.core.Response;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class TrustResourceTest {

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
}
