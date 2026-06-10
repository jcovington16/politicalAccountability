# Staging Hardening Checklist

Use this before exposing a shared staging API.

## Configuration

- Set `APP_ENV=production` for production-like config validation.
- Set a long random `ADMIN_API_TOKEN`; do not use `local-admin-token`.
- Set explicit `cors.allowedOrigins`; do not use `*`.
- Keep `cors.allowCredentials=false` unless account auth is intentionally enabled.
- Keep provider keys in environment variables only.
- Confirm `.env` is ignored and no real secrets are committed.

## Public/Internal Boundaries

- Public: `/search`, `/politicians`, `/bills`, `/claims`, `/citations`, `/sources`, `/trust/score`.
- Internal/admin: `/imports`, `/audit-log`, `/review`, `/classification`.
- Internal routes require `X-Admin-Token`.
- The dashboard hides the Security tab unless `VITE_SHOW_INTERNAL=true`.
- Mobile exposes no admin or review controls.

## Rate Limits And Abuse Prevention

- Public search routes use `PUBLIC_SEARCH_RATE_LIMIT_PER_MINUTE`.
- Start staging at `60`; lower it if traffic is noisy.
- Confirm `GET /review/metrics` shows rate-limited request counts.
- Watch repeated empty searches, high-cardinality query bursts, and provider-throttling spikes.

## Deployment Checks

- `make db-validate`
- `make db-migrate`
- `./gradlew :common:test :storage-service:test :api-gateway:test`
- `cd dashboard && npm run build`
- `make health-check`
- Smoke-test `GET /search?query=Marco`
- Smoke-test `GET /review/queue` without token returns `401`
- Smoke-test `GET /review/queue` with token returns `200`
