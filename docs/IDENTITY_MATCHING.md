# Politician Identity Matching

The ingestion pipeline must avoid merging two public officials just because they share a similar name. These rules apply to state and federal MVP imports.

## Required Matching Inputs

Prefer source-owned stable IDs whenever available:

- Federal: Bioguide ID, FEC candidate ID, Congress.gov member ID, court identifier.
- State: secretary of state candidate ID, legislative member ID, judicial office ID.
- Internal fallback: deterministic UUID generated from source system, office, state, district, and normalized name.

## Match Confidence

| Confidence | Required Evidence | Import Behavior |
| --- | --- | --- |
| Exact | Stable external ID matches an existing politician. | Upsert automatically. |
| Strong | Same normalized full name, same state, same office or seat identifier, overlapping term/election context. | Upsert, log match basis. |
| Possible | Same name and state, but office, district, or election context is missing. | Skip automatic merge and require analyst review. |
| Conflict | Same external ID with different name/state, or same name with conflicting office context. | Reject row and log validation error. |

## Normalization

- Trim whitespace.
- Case-fold names, parties, states, branches, and office levels.
- Normalize common suffixes like `Jr.`, `Sr.`, `II`, `III`.
- Store original source values in import metadata when import batch tables are added.
- Never infer party, ideology, or position from name, district, article text, or source tone.

## Source Provenance Rule

Every imported politician, office, election, bill, vote, article, or statement must include a source URL or source citation before it can be published.
