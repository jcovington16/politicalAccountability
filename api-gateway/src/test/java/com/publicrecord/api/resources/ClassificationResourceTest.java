package com.publicrecord.api.resources;

import com.publicrecord.api.dto.CivicClassificationRequest;
import com.publicrecord.common.classification.CivicClassification;
import com.publicrecord.common.classification.CivicImpactLabel;
import org.junit.jupiter.api.Test;

import javax.ws.rs.core.Response;

import static org.assertj.core.api.Assertions.assertThat;

class ClassificationResourceTest {
    private final ClassificationResource resource = new ClassificationResource();

    @Test
    void classifyRequiresText() {
        Response response = resource.classify(new CivicClassificationRequest(
                "Empty",
                " ",
                "OFFICIAL_RECORD",
                1,
                false,
                true,
                null
        ));

        assertThat(response.getStatus()).isEqualTo(400);
        assertThat(response.getEntity()).isEqualTo("text is required");
    }

    @Test
    void classifyReturnsCivicClassification() {
        Response response = resource.classify(new CivicClassificationRequest(
                "Infrastructure",
                "The bill passed with bipartisan support to improve infrastructure and public safety.",
                "OFFICIAL_RECORD",
                2,
                false,
                true,
                null
        ));

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getEntity()).isInstanceOf(CivicClassification.class);
        CivicClassification classification = (CivicClassification) response.getEntity();
        assertThat(classification.getImpact()).isEqualTo(CivicImpactLabel.PROBLEM_SOLVING);
    }
}
