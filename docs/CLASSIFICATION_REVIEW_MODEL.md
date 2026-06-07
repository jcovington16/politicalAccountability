# Internal Classification And Review Model

Public Record is neutral. The public app should show records, citations, confidence context, review warnings, and source links so voters can decide what matters to them.

This classifier is internal review infrastructure only. It can help staff spot evidence that needs citation review, safety review, or publishability review. It should not be used to publicly judge whether a politician, bill, statement, or action is "good", "bad", "problem-solving", or "problematic."

## Internal Review Axes

Each internally reviewed record can receive:

- `sentiment`: `POSITIVE`, `NEGATIVE`, `MIXED`, `NEUTRAL`, or `UNKNOWN`.
- `civic_impact`: `PROBLEM_SOLVING`, `PROBLEMATIC`, `HARMFUL_RISK`, `PUBLIC_SERVICE`, `ACCOUNTABILITY_CONCERN`, `INFORMATIONAL`, or `UNKNOWN`.
- `harm_risk`: `NONE`, `LOW`, `MEDIUM`, or `HIGH`.
- `problem_solving`: true when the record appears to describe concrete action toward a public problem.
- `problematic`: true when the record appears to involve misconduct, misleading claims, harm risk, or other accountability concerns.
- `review_status`: `MACHINE_CLASSIFIED`, `NEEDS_REVIEW`, `HUMAN_REVIEWED`, `DISPUTED`, or `REJECTED`.
- `confidence`: `HIGH`, `MEDIUM`, or `LOW`.

## Internal Meaning

`PROBLEM_SOLVING` is an internal signal that an item may describe concrete action toward a public problem. Public UI should instead show the underlying action, bill, vote, citation, and timeline entry.

`PROBLEMATIC` is an internal signal that an item may need accountability review, such as ethics concerns, fraud allegations, misleading claims, investigations, or lawsuits. Public UI should show the sourced allegation/status and clearly say when it is unresolved.

`HARMFUL_RISK` is an internal safety warning that text may contain threats, violent language, harassment, hate, dehumanizing language, or other public-safety concerns.

`PUBLIC_SERVICE` is an internal topic signal for government service areas such as healthcare, education, housing, infrastructure, veterans, public safety, jobs, or disaster relief.

`INFORMATIONAL` means the item is a public record or neutral fact pattern without strong review flags.

## Review Rules

- Social posts are evidence of what a public account said, not proof that the claims inside the post are true.
- Allegations, unresolved claims, and harmful-risk labels need human review before they are presented as verified conclusions.
- Problematic conduct labels should have multiple citations unless the source is an official record.
- Prompt-injection-like text is always review-required before AI processing or public summarization.
- The public app should show source context, confidence labels, and neutral warnings instead of public classification labels.
- Manual overrides must eventually write audit-log entries with before/after values.

## API

Protected internal endpoint:

```text
POST /classification/civic
```

Example:

```json
{
  "title": "Veterans healthcare bill",
  "text": "The senator introduced bipartisan funding to improve veterans healthcare access.",
  "sourceQuality": "OFFICIAL_RECORD",
  "citationCount": 3,
  "officialRecord": true
}
```

The response includes sentiment, civic impact, harm risk, labels, explanation, confidence, and review warnings for internal review only.

## Storage

Classifications can be persisted in `civic_classifications`. This table stores the target record, labels, review status, explanation, warnings, reviewer metadata, model version, and timestamps.
