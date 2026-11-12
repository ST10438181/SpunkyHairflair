# Changes to make in the written report

The marker found places where the report did not match the delivered code. Update these before resubmitting.

1. **Technology section (Flutter/Dart)** – the app is **native Android in Kotlin** (XML layouts, Material 3, Gradle).
   Replace every mention of Flutter/Dart. The API is **Python 3 + Flask**, the database is **SQLite**.
2. **Add an architecture paragraph + diagram** – Android app → REST API → SQL database (diagram is in `README.md`).
3. **Add an "API" section** – list the endpoints from `docs/API.md`, show one request/response (e.g. login and create booking),
   and include a screenshot of the API tests passing (`python -m unittest discover -s tests -v`).
4. **Add a "Database design" section** – paste the ER diagram and table list from `docs/DATABASE.md`, mention the unique index that
   prevents double-booking, and include a screenshot of the tables with their sample rows (DB Browser for SQLite).
5. **Sign-in section** – passwords are now hashed on the server (salted PBKDF2-HMAC-SHA256) and the app keeps a bearer token,
   not the password. Remove any statement that data is stored in SharedPreferences.
6. **Installation section** – copy the "Run it" steps from `README.md` (start the API, `adb reverse`, run the app).
7. **Repository section** – the repository contains `app/`, `api/`, `docs/` only. Do not mention website files or an `android/` folder.
8. **Demonstration video** – show the API running, a registration, a booking appearing in the database, a double-booking being
   refused, the tests passing, and the green GitHub Actions run.
