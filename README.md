# Spunky's Hairflair – Android App (Kotlin)

Team **Coding & Curls** · XISD5319 Work Integrated Learning 3A · Task 2 (mobile app)

Android app that mirrors the website: same services, stylists (including **Luna Summerton – Barber**),
50% deposit, Mon–Sat opening hours, double-booking protection and colours.

## Open and run

1. Android Studio → **File ▸ Open** → select this `android` folder. Let Gradle sync
   (Android Studio may offer to update the Gradle / Android Gradle Plugin versions – accept).
2. Plug in a real Android phone (USB debugging on) and press **Run ▶**. Task 2 requires the demo on a phone, not an emulator.
3. Tests: right-click `app/src/test` → *Run Tests* (or `./gradlew testDebugUnitTest`).

Requirements: Android 8.0+ (API 26), JDK 17 (bundled with Android Studio).

## Website ↔ app

| Website page | App screen | Class |
|---|---|---|
| Login / Register modal | Login + Register screens (after a splash screen) | `LoginActivity`, `RegisterActivity` |
| `index.html` Home | Home tab | `HomeFragment` |
| `book.html` 4-step booking | Book tab, 4 step-through screens | `BookFragment` |
| `bookings.html` | My Bookings tab | `BookingsFragment` |
| `services.html` | Services tab | `ServicesFragment` |
| `contact.html` | Contact tab (tap to call / email / map / Instagram) | `ContactFragment` |
| `help.html` | Help (top-right ⋮ menu) | `HelpActivity` |
| `settings.html` | Settings (⋮ menu): profile, change password, log out | `SettingsActivity` |

## Code layout (program modules)

```
model/   Models.kt, Catalog.kt        data classes + services/stylists/time slots
logic/   Validators, PasswordHasher, BookingLogic   pure Kotlin, unit tested
data/    Repositories.kt (interfaces) + Local*Repository (SharedPreferences)
ui/      activities, fragments, adapters
```

## Task 2 checklist (what this covers)

| Requirement | Status |
|---|---|
| Android app code, GUI, input controls, program modules | Done |
| Register/login with **encrypted password** | Done – salted PBKDF2-SHA256 hash |
| User can change their settings | Done – Settings screen |
| User support / help system | Done – Help screen |
| Invalid input handled without crashing | Done – field-level validation on every form |
| Automated tests + GitHub Actions | Unit tests + `.github/workflows/android-ci.yml` (push the repo to see it run) |
| Database (DBMS), 10+ records per table, hosted API/auth service | **Teammates** – see below |
| Single sign-on (SSO) | Needs an online identity provider – **teammates** |
| Azure DevOps updates, demo video with voice-over | **Team** |

## Hand-off for the database / API teammates

All screens use only the two interfaces in `data/Repositories.kt`:

- `UserRepository` – register, login, current user, update profile, change password, logout
- `BookingRepository` – list for user, list all, add, cancel

Today they are backed by `SharedPreferences`. To plug in the DBMS/API, create classes implementing those
interfaces and return them from `Repos` – no screen code has to change. Keep passwords hashed
(`PasswordHasher`) before they leave the phone/server.
