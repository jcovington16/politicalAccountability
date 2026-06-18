# Public API Freeze

The MVP public API is frozen around the endpoints documented in:

```text
docs/openapi/public-record-api.yaml
```

## Freeze Rules

- Public endpoint additions require an OpenAPI update and contract-test update.
- Public response field removals are breaking changes.
- Public field renames are breaking changes.
- Internal/admin routes must stay out of the public OpenAPI file.
- Review, import, audit, classification, and metrics routes stay protected by `X-Admin-Token`.

## Frozen Public Surface

- `GET /search`
- `GET /politicians/search/name`
- `GET /politicians/{id}`
- `GET /politicians/{id}/profile`
- `GET /politicians/{id}/timeline`
- `GET /politicians/{id}/votes`
- `GET /politicians/{id}/statements`
- `GET /politicians/{id}/claims`
- `GET /bills/search`
- `GET /bills/{id}`
- `GET /bills/{id}/votes`
- `GET /claims`
- `GET /citations`
- `GET /citations/{citationType}/{targetId}`
- `GET /sources`
- `GET /statements`
- `POST /trust/score`

## Neutrality Rule

The API exposes records, citations, warnings, and evidence quality. It should not expose public labels that tell voters which politicians are good, bad, aligned, or opposed to their personal ideology.
