# API Documentation

The public API contract lives at:

```text
docs/openapi/public-record-api.yaml
```

This OpenAPI file documents voter-facing read/search endpoints only. Internal routes for imports, audit logs, review queues, review metrics, and optional classification infrastructure are intentionally excluded from the public contract and remain protected by `X-Admin-Token`.

## Public Endpoint Groups

- Search: grouped search across public record types.
- Politicians: lookup, profile aggregation, timeline aggregation, votes, statements, and claims.
- Bills: bill search, bill detail aggregation, actions, citations, and votes.
- Sources: source registry and source citation lookup.
- Trust: evidence-quality trust scoring.

## Contract Test

`api-gateway/src/test/java/com/publicrecord/api/OpenApiContractTest.java` verifies that the public contract includes the core endpoints and does not accidentally publish internal/admin routes.

Run it with:

```sh
./gradlew :api-gateway:test
```

## Boundary Rule

The public API should show evidence and source context. It should not expose operator controls, import/audit history, moderation queues, or internal review implementation details without explicit authorization.
