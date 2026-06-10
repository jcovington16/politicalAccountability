# Saved Politicians Strategy

Saved politicians are local-first for the MVP.

## Why Local-First

- No account system is required for early voters.
- No backend table of voter interests is created.
- Privacy risk is lower because saved choices stay on the user's device/browser.
- The feature stays fast and works even when the API is unavailable.

## Current Behavior

- Web dashboard stores saved politician snapshots in browser `localStorage`.
- Expo web stores saved politician snapshots in browser `localStorage`.
- Native mobile keeps saved state in memory for now; add AsyncStorage when we intentionally add that dependency.
- Saved records include the politician snapshot, saved timestamp, latest known activity, and profile data gaps.

## Backend Sync Later

Only add backend sync when user accounts exist and the user opts in.

Before enabling sync:

- define user account model and consent flow
- encrypt transport and protect account APIs
- document saved-politician privacy implications
- add delete/export controls
- audit access to saved-politician data
- avoid selling or sharing saved-politician interest data

## MVP Rule

Saved politicians are a voter convenience feature, not analytics fuel.
