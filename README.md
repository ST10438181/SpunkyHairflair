# Spunky's Hairflair – Booking Website

Team **Coding & Curls** · XISD5319 Work Integrated Learning 3A

A booking website for Spunky's Hairflair (Mandalay, Mitchell's Plain). Customers register / log in, choose a
service and stylist, pick an available date and time, see the 50% deposit and manage their appointments.
Built with HTML5, CSS3 and JavaScript (ES6) in Visual Studio Code. The Android app in the `android` folder mirrors it.

## Pages

| File | Page | What it does |
|---|---|---|
| `index.html` | Home | Hero banner, service preview, meet the stylists, login prompt |
| `book.html` | Book Now | 4 steps: service → stylist → date & time → summary with 50% deposit |
| `bookings.html` | My Bookings | View and cancel your appointments |
| `services.html` | Services | Full catalogue with price, duration and a Book button |
| `contact.html` | Contact | Address, phone, email, social media |
| `help.html` | Help | FAQ / user support |
| `settings.html` | Settings | Change name, phone and password; log out |

## Stylists

Asenathi Valashiya (Braiding Specialist), Siphokazi Nofemele (Cuts & Relaxers),
Rethabile Minnaar (Weaves & Extensions), **Luna Summerton (Barber)**.

## Folder structure

```
index.html  book.html  bookings.html  services.html  contact.html  help.html  settings.html
css/styles.css
js/data.js        services, stylists, time slots, pricing, date + validation logic (unit tested)
js/common.js      navbar, footer, login/register modal, account storage, password hashing, toast
js/book.js        booking flow          js/bookings.js   my bookings
js/services.js    services + home       js/settings.js   settings page
tests/            automated tests (Node built-in test runner)
.github/workflows/website-ci.yml   GitHub Actions: syntax check + tests on every push
```

## Run it (VS Code)

1. Install the **Live Server** extension, right-click `index.html` → *Open with Live Server*
   (password hashing needs `http://localhost`/`127.0.0.1`, not a plain file).
2. Sign up on the Register tab, then book.

## Run the tests

```
npm test
```
(needs Node 18+). The same tests run automatically on GitHub via **Actions → Website CI**.

## Notes

- Data is stored in the browser's `localStorage`; the database/API layer is handled separately by the team.
- Passwords are stored as a salted PBKDF2-SHA256 hash (Web Crypto), never in plain text.
- The salon is open Mon–Sat, so Sundays can't be booked; past times are hidden; a stylist's already-booked
  times are disabled to prevent double-bookings.
