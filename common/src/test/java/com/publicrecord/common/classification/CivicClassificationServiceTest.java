package com.publicrecord.common.classification;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CivicClassificationServiceTest {

    @Test
    void classifiesProblemSolvingPublicServiceRecords() {
        CivicClassification classification = CivicClassificationService.INSTANCE.classify(new ClassificationInput(
                "Veterans healthcare bill",
                "The senator introduced bipartisan funding to improve veterans healthcare access.",
                "OFFICIAL_RECORD",
                3,
                false,
                true,
                null
        ));

        assertThat(classification.getSentiment()).isEqualTo(SentimentLabel.POSITIVE);
        assertThat(classification.getImpact()).isEqualTo(CivicImpactLabel.PROBLEM_SOLVING);
        assertThat(classification.getProblemSolving()).isTrue();
        assertThat(classification.getReviewWarnings()).isEmpty();
    }

    @Test
    void flagsPotentiallyHarmfulOrProblematicRecordsForReview() {
        CivicClassification classification = CivicClassificationService.INSTANCE.classify(new ClassificationInput(
                "Threat allegation",
                "The public post included a threat and violent language during an ethics investigation.",
                "SOCIAL_MEDIA",
                1,
                true,
                false,
                null
        ));

        assertThat(classification.getSentiment()).isEqualTo(SentimentLabel.NEGATIVE);
        assertThat(classification.getImpact()).isEqualTo(CivicImpactLabel.HARMFUL_RISK);
        assertThat(classification.getHarmRisk()).isEqualTo(HarmRiskLevel.HIGH);
        assertThat(classification.getReviewStatus()).isEqualTo(ReviewStatus.NEEDS_REVIEW);
        assertThat(classification.getReviewWarnings()).contains("Potentially harmful language requires human review.");
    }
}
