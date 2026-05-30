# Public Record Mobile MVP

React Native/Expo voter app for the Political Accountability platform.

## Screens

- Search
- Politician profile
- Voting record
- Issue stance
- Timeline
- Compare two politicians
- Saved politicians
- Source citations

The MVP uses local sample data and mirrors the web dashboard visual system. The next step is wiring the same Dropwizard API endpoints used by the web dashboard.

## Run Locally

```sh
npm install
npm start
```

Then open with Expo Go, Android emulator, iOS simulator, or web preview. For a browser-only preview, run:

```sh
npm run web
```

## App Store Path

This project includes Expo config for Android and iOS:

- Android package: `com.publicrecord.voter`
- iOS bundle id: `com.publicrecord.voter`
- EAS config: `eas.json`

Production build commands once Expo/EAS credentials are configured:

```sh
npx eas build --platform android --profile production
npx eas build --platform ios --profile production
npx eas submit --platform android --profile production
npx eas submit --platform ios --profile production
```

Before store submission:

- Replace placeholder app metadata and screenshots.
- Add app icons and splash assets.
- Configure production API URL.
- Add privacy policy URL.
- Complete Apple App Store and Google Play data safety forms.
- Add authentication if saved profiles sync across devices.
