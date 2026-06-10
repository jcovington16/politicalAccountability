# Privacy And Public-Record Policy Draft

Public Record is built for public accountability, not private exposure.

## Public-Record Scope

The app should collect and display:

- official votes, bills, offices, elections, and public records
- public statements made in official, campaign, hearing, debate, interview, press, or verified social contexts
- source citations from official records, primary sources, reputable news, and reviewed public media
- claims, allegations, and fact checks only with citation and review context

The app should not collect or display:

- private home addresses
- personal phone numbers or personal emails
- SSNs, payment details, or sensitive identifiers
- private messages, leaked private content, or login-gated material
- family/private-person details that are not directly relevant public records
- paywalled, bypassed, or technically restricted content

## Review Rules

- Private/sensitive findings should block public display until reviewed.
- Official public office contact information may be allowed, but should be clearly sourced.
- Media and social posts are citation candidates until linked to verified identities and reviewed.
- Missing data must be labeled as missing data, never as "no issues."

## Implementation Notes

The shared `PrivacySafetyService` detects and redacts obvious private contact data and high-risk identifiers. API responses can include neutral warnings; ingestion skips high-risk local records instead of importing them.

This is not a legal review system. Before launch, this draft should be reviewed against the app's actual sources, jurisdictions, privacy policy, and app-store disclosures.
