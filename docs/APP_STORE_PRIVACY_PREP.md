# App Store Privacy Prep

The mobile MVP is configured for Expo preview/production builds, but app-store submission still needs policy and store-account work.

## Current Mobile Posture

- No account system in the MVP.
- Saved politicians are local-first.
- No backend sync for voter interests.
- Android permissions are empty.
- iOS camera/photo descriptions state that the MVP does not require those permissions.
- API keys stay server-side and should never ship in the mobile bundle.

## Required Before Store Submission

- Public privacy policy URL.
- Support/contact URL.
- App icon and store screenshots.
- Google Play Data Safety form.
- Apple App Privacy labels.
- Production API base URL in Expo config/build profile.
- Crash reporting decision and disclosure.
- Review of whether analytics are used; if used, disclose collection and retention.

## Suggested Privacy Labels

For the current MVP, the intended answer is:

- No user account data collected.
- No contacts, precise location, photos, camera, microphone, health, financial, or payment data collected.
- Saved politicians stay on-device.
- Server logs may process IP address, user agent, route, status code, and timestamp for security and abuse prevention.

Final store answers must match the deployed app, not this draft.
