package com.publicrecord.common.trust;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class TrustScoringServiceTest {

    @Test
    void votingRecordsFromOfficialSourcesScoreHigh() {
        TrustScore score = TrustScoringService.INSTANCE.score(new TrustScoreInput(
                InformationType.VOTING_RECORD,
                SourceQuality.OFFICIAL_RECORD,
                3,
                LocalDate.now(),
                LocalDate.now()
        ));

        assertThat(score.getConfidenceLevel()).isEqualTo(ConfidenceLevel.HIGH);
    }

    @Test
    void unresolvedClaimsFromUnknownSourcesScoreLow() {
        TrustScore score = TrustScoringService.INSTANCE.score(new TrustScoreInput(
                InformationType.UNRESOLVED_CLAIM,
                SourceQuality.UNKNOWN,
                0,
                null,
                LocalDate.now()
        ));

        assertThat(score.getConfidenceLevel()).isEqualTo(ConfidenceLevel.LOW);
    }
}
