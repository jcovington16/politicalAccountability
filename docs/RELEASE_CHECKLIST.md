# Release Checklist

Use this checklist for staging rehearsals, release candidates, and production launch readiness.

## Day 28: Rehearsal

- Confirm `.env` is present locally or environment secrets are present in staging.
- Run migrations with `make db-migrate`.
- Run release checks:

```sh
scripts/release-candidate-check.sh
```

- Run live checks when the API is running:

```sh
RUN_LIVE_CHECKS=true scripts/release-candidate-check.sh
```

- Confirm dashboard production build succeeds.
- Confirm Expo typecheck succeeds.
- Run a rehearsal import using official/civic sources from `docs/IMPORT_OPERATOR_GUIDE.md`.
- Verify search, politician profile, bill detail, timeline, claims, citations, and review metrics.

## Rollback Plan

- Keep the previous image tag available.
- Before risky migrations, tag the database state:

```sh
make db-tag tag=before_release_candidate
```

- If the deployment fails before traffic is moved, stop the new container and restart the previous image.
- If a migration caused the problem, use a tested rollback script or restore from backup. Do not improvise destructive database commands during an incident.
- Record the failure, affected routes, logs, and next action in the release notes.

## Day 29: Freeze

- Freeze the public OpenAPI contract in `docs/openapi/public-record-api.yaml`.
- Do not add new public endpoints without updating the contract test.
- Freeze the voter-facing UI except release-blocking fixes.
- Freeze MVP dataset scope and document known gaps in `docs/MVP_DATASET_FREEZE.md`.
- Run final regression and security checks.
- Confirm `.env` is not staged or committed.

## Day 30: Launch

- Run `scripts/launch-monitor.sh` before and after deployment.
- Watch API errors, search failures, import failures, and slow profile responses.
- Review protected metrics at `GET /review/metrics`.
- Monitor app-store preview build health and crash reports when native previews exist.
- Hold a post-launch review and update the next 30-day scope.
