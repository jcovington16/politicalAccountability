# Launch Monitoring

The MVP launch should watch whether voters can search, open profiles, inspect bills, and see source-backed records without internal/admin data leaking.

## Signals

- API errors by route.
- Search success/failure rate.
- Profile and timeline latency.
- Bill search/detail latency.
- Import batch failures.
- Source-provider rate limits.
- Review queue volume.
- Audit-log write failures.
- Rate-limit and abuse signals.

## Local Snapshot

Run:

```sh
scripts/launch-monitor.sh
```

With admin metrics:

```sh
ADMIN_API_TOKEN=... scripts/launch-monitor.sh
```

## First Launch Cadence

- First hour: check every 10 minutes.
- First day: check at least every 2 hours.
- First week: daily review of import failures, API errors, abuse signals, and data gaps.

## Incident Response

1. Identify affected route, source, or import.
2. Stop or pause the failing import if data quality is affected.
3. Keep voter-facing claims conservative: hide or warning-gate questionable records.
4. Roll back the deploy if core search/profile/bill routes are down.
5. Document timeline, root cause, fix, and follow-up.
