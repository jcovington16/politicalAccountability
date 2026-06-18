# MVP Dataset Freeze

The MVP dataset should prioritize reliable public-record coverage over breadth.

## In Scope

- Federal executive seed profiles for recent Presidents.
- Congress.gov members and bills.
- GovInfo bill/document package metadata.
- Open States state-level people and bills within provider rate limits.
- Google Civic representative lookups when an address is configured.
- GDELT, Guardian, RSS, YouTube, Bluesky, X, and allowlisted article discovery as citation candidates.
- Local CSV/JSON imports using `data/templates`.

## Out Of Scope For MVP

- Private citizens.
- Private contact details or non-public personal data.
- Login-gated social or media content.
- Unreviewed allegations presented as facts.
- Automated ideology scoring or politician ranking.
- Full local-level coverage until the state/federal pipeline is stable.

## Known Gaps

- Presidential/executive records are seeded for search, but executive orders, appointments, vetoes, schedules, and White House statements need richer ingestion.
- State-level coverage depends on Open States limits and source availability.
- Media records are discovery candidates until they are linked to politicians and citations.
- YouTube/social records need account/channel identity matching before becoming public statements.
- Full OpenSearch indexing remains a future hardening path; PostgreSQL remains the source of truth.

## Operator Rule

If a record cannot be tied to a source, source quality, and target politician/bill with reasonable confidence, keep it out of public voter views or show it with clear review warnings.
