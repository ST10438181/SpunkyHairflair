# Spunky's Hairflair Booking System

A booking platform for **Spunky's Hairflair**, a hair salon based in Mandalay, Mitchell's Plain. The system replaces manual WhatsApp bookings with a website and a companion Android app that let customers register, browse services and stylists, select an available date and time, pay a 50% deposit, and manage their own appointments.

**Goal:** reduce no-shows by 80% within three months of launch.

## Team — Coding & Curls (XISD5319)

| Name | Student Number | Focus Area |
|---|---|---|
| Asenathi Valashiya | ST10403979 | Website wireframes (Home, Login/Register, Booking flow); repo setup, folder structure & README |
| Siphokazi Nofemele | ST10438181 | Project plan narrative; DevOps lifecycle diagram & tooling |
| Rethabile Minnaar | ST10441779 | Website wireframes (My Bookings, Services, Contact); site maps & mobile wireframes |
| Given Luna Summerton | ST10434685 | Submission checklist, GitHub compliance review, proofreading & formatting |

Lecturer: Mr Smanga Ngcamu

## Features

**Website and Android app**

- Registration / login (name, email, phone) with a hashed password
- Service catalogue: Wash & Blow, Trim & Style, Box Braids, Sew-in Weave, Relaxer, Full Style Blowout
- Stylists, including **Luna Summerton – Barber**
- Booking flow: Select Service → Select Stylist → Select Date & Time → Booking Summary
- Mon–Sat opening hours and double-booking protection (booked times can't be chosen again)
- Automatic 50% deposit calculation
- My Bookings: view upcoming/past appointments, cancel a booking
- Contact page with salon details

**Android app extras**

- Help screen (user support) and Settings screen (edit profile, change password, log out)
- Field-level validation on every form, so invalid input never crashes the app
- Tap-to-call, email, map and Instagram links on the Contact tab

## Tech Stack

| Layer | Website | Android App |
|---|---|---|
| Structure & UI | HTML5, CSS3, Font Awesome, Google Fonts (Inter) | Android Views + Material Components |
| Logic | JavaScript (ES6) | Kotlin (coroutines for data calls) |
| Data Persistence | Browser LocalStorage | `SharedPreferences` today; database + hosted API in progress (see below) |
| Security | – | Salted PBKDF2-SHA256 password hashing |
| Testing | Manual functional testing | JUnit unit tests, run automatically by GitHub Actions |
| Hosting / Build | GitHub Pages | Debug APK built by GitHub Actions |
| Design Tools | Figma / draw.io | Figma / draw.io |

## Folder Structure

```
spunkys-hairflair/
├── .github/
│   └── workflows/
│       └── android-ci.yml        # runs unit tests and builds the APK
├── website/
│   ├── index.html
│   ├── css/
│   │   └── styles.css
│   ├── js/
│   │   ├── auth.js
│   │   ├── booking.js
│   │   └── storage.js
│   └── assets/
│       └── images/
├── android/
│   └── app/src/
│       ├── main/java/.../
│       │   ├── model/   Models.kt, Catalog.kt
│       │   ├── logic/   Validators, PasswordHasher, BookingLogic
│       │   ├── data/    Repositories.kt + Local*Repository
│       │   └── ui/      activities, fragments, adapters
│       └── test/        unit tests
├── docs/
│   ├── project-plan.pdf
│   ├── site-maps/
│   └── wireframes/
└── README.md
```

## Getting Started

### Website

1. Clone the repository:
   ```
   https://github.com/ST10438181/Spunky-s-Hairflair.git
   ```
2. Open `website/index.html` in a browser, or serve the `website/` folder with a local dev server.
3. No build step is required — the site runs on plain HTML/CSS/JS with LocalStorage.

### Android app

Requirements: Android 8.0+ (API 26), JDK 17 (bundled with Android Studio).

1. Android Studio → **File ▸ Open** → select the `android` folder. Let Gradle sync
   (Android Studio may offer to update the Gradle / Android Gradle Plugin versions – accept).
2. Plug in a real Android phone (USB debugging on) and press **Run ▶**. The Task 2 demo must be on a phone, not an emulator.
3. Tests: right-click `app/src/test` → *Run Tests* (or `./gradlew testDebugUnitTest`).

## Website ↔ App

| Website page | App screen | Class |
|---|---|---|
| Login / Register modal | Login + Register screens (after a splash screen) | `LoginActivity`, `RegisterActivity` |
| `index.html` Home | Home tab | `HomeFragment` |
| `book.html` 4-step booking | Book tab, 4 step-through screens | `BookFragment` |
| `bookings.html` | My Bookings tab | `BookingsFragment` |
| `services.html` | Services tab | `ServicesFragment` |
| `contact.html` | Contact tab | `ContactFragment` |
| `help.html` | Help (top-right ⋮ menu) | `HelpActivity` |
| `settings.html` | Settings (⋮ menu): profile, change password, log out | `SettingsActivity` |

## Development Workflow (DevOps Lifecycle)

1. **Plan** — user stories from the requirement analysis broken into epics, user stories and tasks on the Azure DevOps board
2. **Code** — HTML5/CSS3/JS (website) and Kotlin (Android) written on individual feature branches
3. **Build** — feature branch merged into `develop` via Pull Request after peer review; GitHub Actions builds the Android app
4. **Test** — unit tests run automatically on every push and pull request; manual functional testing of the booking flow, validation and responsiveness
5. **Release** — approved PR merged into `main`, tagged with a version number (e.g. v1.1)
6. **Deploy** — website auto-published via GitHub Pages; Android debug APK produced by GitHub Actions
7. **Operate** — live site and test APK used by test customers
8. **Monitor** — commit history, work items and feedback reviewed to plan the next sprint

### Continuous integration

`.github/workflows/android-ci.yml` runs on pushes to `main`, `master` and `develop`, and on pull requests.
It runs the unit tests, builds the debug APK and uploads it as a build artifact. The workflow file must stay in
the root `.github/workflows/` folder of the repository; its Gradle steps run against the `android` folder.

## Android Task 2 Status

| Requirement | Status |
|---|---|
| Android app code, GUI, input controls, program modules | Done |
| Register/login with **encrypted password** | Done – salted PBKDF2-SHA256 hash |
| User can change their settings | Done – Settings screen |
| User support / help system | Done – Help screen |
| Invalid input handled without crashing | Done – field-level validation on every form |
| Automated tests + GitHub Actions | Unit tests and workflow written; passing run to be confirmed on GitHub |
| Database (DBMS), 10+ records per table, hosted API/auth service | In progress |
| Single sign-on (SSO) | In progress (needs an online identity provider) |
| Azure DevOps updates | Backlog imported; keep task states up to date |
| Demo video with voice-over | To do |

## Data Layer Hand-off (Database / API / SSO)

All Android screens use only the two interfaces in `data/Repositories.kt`:

- `UserRepository` – register, login, current user, update profile, change password, logout
- `BookingRepository` – list for user, list all, add, cancel

Today they are backed by `SharedPreferences`. To plug in the DBMS/API, create classes implementing those
interfaces and return them from `Repos`; almost no screen code has to change. Keep passwords hashed
(`PasswordHasher`) before they leave the phone/server.

**Threading:** every method that may touch a database or the network is a `suspend` function. The screens call
them inside `lifecycleScope.launch { ... }` through the `attempt { }` helper (`ui/Extensions.kt`), which shows
"check your connection" instead of crashing if an implementation throws (for example an `IOException`). Throw on
failure; return `null` / `false` only for normal outcomes such as "wrong password" or "email already used".

**Session:** `getCurrentUser()` and `logout()` are deliberately *not* `suspend` (they are called while screens
start up). An API version should keep the session token and the cached user in local storage and read them there.

**Also needed for an online API:** add `<uses-permission android:name="android.permission.INTERNET" />` to
`AndroidManifest.xml` plus an HTTP client (for example Retrofit/OkHttp). Single sign-on becomes a second way to
obtain that token; add it next to `login()` in the API-backed `UserRepository`.

## Project Timeline

| Week | Sprint Focus |
|---|---|
| 1 | Plan & Design — project plan, site maps, wireframes |
| 2 | Core Build — HTML/CSS/JS booking flow structure, repo setup |
| 3 | Feature Complete — booking logic, LocalStorage, validation, responsive styling |
| 4 | Test & Refine — cross-browser/device testing, peer review, bug fixes |
| 5 | Documentation & Submission — final docs, Task 2 submission, release tag |
