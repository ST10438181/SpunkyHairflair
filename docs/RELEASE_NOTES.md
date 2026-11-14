# Release notes

## Version 3.0 (current)

Changes since the prototype, which kept all data on the phone:

### New features
* **Online accounts and bookings.** The app now talks to a REST API (Flask) backed by a relational database,
  so accounts and bookings are shared across devices and users.
* **Live availability.** Time slots come from the server for the chosen stylist and date, so a slot someone else
  booked shows as "(booked)", and past times are hidden.
* **Double-booking prevention in the database.** A unique index guarantees one active booking per stylist, date and
  time, even if two customers press Confirm at the same moment (the second gets a clear message).
* **Secure sign-in.** Passwords are hashed on the server (salted PBKDF2-HMAC-SHA256) and the app keeps a revocable
  token instead of the password. Changing your password signs out your other devices.
* **Delete my account.** Settings ▸ Delete my account permanently removes the account and its bookings.
* **Clear error handling.** Per-field messages from the server, friendly "can't reach the server" messages and automatic
  return to Login when a session expires.

### Improvements
* Services and stylists load from the database instead of being hard-coded.
* Updated to Android 16 (API 36) with edge-to-edge screens, as Google Play now requires.
* HTTPS-only release builds; plain HTTP is allowed only in debug builds for local development.
* Release signing and Android App Bundle build for Google Play.

### Quality
* Automated tests: 38 API and database tests, plus Android unit tests for validation, booking maths and the API client.
* GitHub Actions workflow runs both test suites and builds the debug APK and release bundle on every push.

### Removed
* On-device storage of users and bookings, and on-device password hashing (now done by the server).

## Version 2.0 (Task 2)
REST API, database design and app integration added; local storage replaced.

## Version 1.0 (prototype)
Kotlin app with screens for login, registration, services, booking, my bookings, contact and help, with data stored on the phone.
