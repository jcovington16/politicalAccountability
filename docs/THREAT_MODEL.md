# Threat Model

This app is a neutral public-record system. The main security goal is to let voters inspect what public officials have said, sponsored, voted for, and had credibly reported about them without letting bad data, biased classification, or operational mistakes distort the record.

## Core Assets

- Public politician, bill, vote, statement, article, citation, and timeline records.
- Source provenance, source quality, import checksums, and audit history.
- API keys for official and commercial data providers.
- Admin/import endpoints and operator actions.
- Search indexes and generated trust/confidence labels.

## Trust Boundaries

- Public users can read published records and search results.
- Admin/import operators can start imports and inspect audit/import status.
- External APIs and scraped files are untrusted until normalized and cited.
- PostgreSQL is the source of truth; Elasticsearch/OpenSearch is a derived search index.
- React and React Native clients are untrusted callers of public APIs.

## Primary Threats And Controls

| Threat | Risk | Current Control | Next Control |
| --- | --- | --- | --- |
| Source manipulation | A bad source or changed article can alter the public record. | Source registry, citations, retrieval timestamps, and source quality fields. | Archive URLs, checksums on source payloads, source anomaly alerts. |
| Misinformation laundering | Allegations or opinion can appear as verified fact. | Trust scoring separates verified facts, direct quotes, voting records, allegations, opinion, and unresolved claims. | Enforce publish rules requiring claim type, citations, and confidence labels. |
| Prompt injection in scraped text | Article or statement text may contain instructions aimed at AI summarizers. | Raw events and normalized records are separated. | Add sanitizer/quarantine checks before any LLM processing. |
| Data poisoning through imports | Malformed CSV/JSON can create wrong joins or fake records. | Local dry-run validator checks required fields, IDs, dates, vote values, and cross-file references. | Add importer-level schema validation for every connector. |
| Unauthorized admin access | Import/audit endpoints could expose or mutate internal state. | Admin endpoints require `X-Admin-Token`. | Replace local token with role-based auth before shared/staging exposure. |
| Secrets leakage | API keys could be committed or logged. | `.env` is ignored and `.env.example` contains placeholders only. | Add secret scanning to CI and rotate any exposed keys. |
| Search abuse | High-volume searches can scrape or degrade the API. | Public endpoints are read-only. | Add rate limits, pagination caps, request telemetry, and bot controls. |
| Privacy overreach | The app could ingest private contact info or irrelevant personal details. | MVP focuses on public state/federal records. | Add privacy classification and reject non-public personal data. |
| Audit tampering | Operators could hide import or moderation changes. | `audit_log` has append-only database protection. | Restrict direct database access and alert on failed audit writes. |

## Day 7 Decision

For the Week 1 baseline, the app remains local-first and public-read focused. We can keep ingesting official data into PostgreSQL and exposing read-only search while holding back shared admin features until role-based authorization, rate limiting, and stronger source validation are in place.
