# Publishing to Google Play – checklist

What the project already does for Play:

| Requirement | Status |
|---|---|
| Target API 36 (required for new apps from 31 Aug 2026) | Done – `compileSdk`/`targetSdk` 36, edge-to-edge screens |
| Android App Bundle (`.aab`) | `gradle bundleRelease` (also built by GitHub Actions) |
| Release signing | Reads `keystore.properties` (see below) |
| HTTPS only in release builds | Done – `network_security_config.xml` |
| Account creation ⇒ in-app account deletion | Done – Settings ▸ Delete my account (`POST /me/delete`) |
| Privacy policy | Template in `docs/PRIVACY_POLICY.md` (needs a public web address) |
| No ads, no analytics, no sensitive permissions | Only the INTERNET permission is used |

## 1. Host the API first
Follow `docs/DEPLOY.md`, then set `releaseApiBaseUrl=https://<username>.pythonanywhere.com/api/` in `gradle.properties`.
A release build that still points at `localhost` will not work for anyone.

## 2. Create your signing key (once)
Easiest: Android Studio ▸ **Build ▸ Generate Signed App Bundle / APK ▸ Android App Bundle ▸ Create new…**
Or on the command line (Android Studio ships `keytool` in its `jbr\bin` folder):
```
keytool -genkeypair -v -keystore spunkys-release.jks -alias spunkys -keyalg RSA -keysize 2048 -validity 10000
```
Put `spunkys-release.jks` in the project root, copy `keystore.properties.example` to `keystore.properties` and fill it in.
Both are in `.gitignore`. **Back the keystore up and never commit it or share the passwords.**

## 3. Build the bundle
Android Studio ▸ **Build ▸ Generate Signed App Bundle**, or `gradle bundleRelease`.
The file is `app/build/outputs/bundle/release/app-release.aab`. Raise `versionCode` in `app/build.gradle.kts` for every upload.

## 4. Play Console
1. Create a developer account at <https://play.google.com/console> (a one-time registration fee applies).
2. **Create app** ▸ fill in name, language, free, app not game.
3. Upload the `.aab` to **Internal testing** first and install it from the Play Store on a phone.
4. Store listing: short description, full description, app icon 512×512, feature graphic 1024×500 and at least two phone screenshots.
5. **App content**: privacy policy URL, ads = no, target audience (not children), content rating questionnaire,
   data safety form, and **App access** – give reviewers the demo login (`lerato.dlamini@example.com` / `Curls2026`).
6. Data safety answers that match this app: collects name, email and phone number plus booking details; used for account
   management and app functionality; not shared with third parties; encrypted in transit; users can request deletion
   (in-app and by email).
7. New personal developer accounts may have to run a closed test with a minimum number of testers for a set period before
   they can publish to production. Check the current rules in the Play Console.

Your brief only says the app must be *ready* to publish, so confirm with your lecturer whether you must actually submit it.
