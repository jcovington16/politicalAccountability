package com.publicrecord.common.privacy;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PrivacySafetyServiceTest {

    @Test
    void redactsPrivateContactDetails() {
        PrivacySafetyResult result = PrivacySafetyService.INSTANCE.evaluate(
                "Personal email jane@example.com and phone 303-555-1212 were included."
        );

        assertThat(result.getRisk()).isEqualTo(PrivacyRiskLevel.MEDIUM);
        assertThat(result.getRedactedText()).contains("[redacted email]");
        assertThat(result.getRedactedText()).contains("[redacted phone]");
        assertThat(result.getWarnings()).hasSize(2);
    }

    @Test
    void blocksSensitivePrivateIdentifiers() {
        PrivacySafetyResult result = PrivacySafetyService.INSTANCE.evaluate(
                "The leaked private message listed SSN 123-45-6789 and home address 123 Main Street."
        );

        assertThat(result.getSafeForPublicDisplay()).isFalse();
        assertThat(result.getRisk()).isEqualTo(PrivacyRiskLevel.HIGH);
        assertThat(result.getRedactedText()).contains("[redacted ssn]");
        assertThat(result.getRedactedText()).contains("[redacted private address]");
    }
}
