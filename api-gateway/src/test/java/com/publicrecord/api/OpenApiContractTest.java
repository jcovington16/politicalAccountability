package com.publicrecord.api;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class OpenApiContractTest {

    private final String spec = readSpec();

    @Test
    void documentsCorePublicEndpoints() {
        List<String> requiredPaths = List.of(
                "/search:",
                "/politicians/search/name:",
                "/politicians/{id}:",
                "/politicians/{id}/profile:",
                "/politicians/{id}/timeline:",
                "/politicians/{id}/votes:",
                "/politicians/{id}/claims:",
                "/bills/search:",
                "/bills/{id}:",
                "/bills/{id}/votes:",
                "/claims:",
                "/citations:",
                "/sources:",
                "/statements:",
                "/trust/score:"
        );

        requiredPaths.forEach(path -> assertThat(spec).contains(path));
    }

    @Test
    void doesNotExposeInternalAdminEndpointsInPublicSpec() {
        assertThat(spec).doesNotContain("/imports:");
        assertThat(spec).doesNotContain("/audit-log:");
        assertThat(spec).doesNotContain("/review/");
        assertThat(spec).doesNotContain("/classification/");
    }

    private static String readSpec() {
        try {
            Path current = Path.of("").toAbsolutePath();
            while (current != null) {
                Path candidate = current.resolve("docs/openapi/public-record-api.yaml");
                if (Files.exists(candidate)) {
                    return Files.readString(candidate);
                }
                current = current.getParent();
            }
            throw new IllegalStateException("Unable to find docs/openapi/public-record-api.yaml");
        } catch (Exception e) {
            throw new IllegalStateException("Unable to read public OpenAPI spec", e);
        }
    }
}
