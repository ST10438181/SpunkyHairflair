"""Demo customers and bookings so every table has 10+ rows for marking and demos.

All people below are made-up sample data. Every demo account uses the password
'Curls2026' (change or delete before any real deployment).
"""
from datetime import date

from .booking_rules import TIME_SLOTS, deposit, now_sast, open_days
from .security import hash_password

DEMO_PASSWORD = "Curls2026"

DEMO_USERS = [
    ("Lerato Dlamini", "lerato.dlamini@example.com", "0821234501"),
    ("Thando Mokoena", "thando.mokoena@example.com", "0821234502"),
    ("Naledi Jacobs", "naledi.jacobs@example.com", "0821234503"),
    ("Ayanda Petersen", "ayanda.petersen@example.com", "0821234504"),
    ("Zinhle Khumalo", "zinhle.khumalo@example.com", "0821234505"),
    ("Michaela Adams", "michaela.adams@example.com", "0821234506"),
    ("Sipho Ndlovu", "sipho.ndlovu@example.com", "0821234507"),
    ("Fatima Davids", "fatima.davids@example.com", "0821234508"),
    ("Keegan Williams", "keegan.williams@example.com", "0821234509"),
    ("Busisiwe Zulu", "busisiwe.zulu@example.com", "0821234510"),
]

# (user index, service id, stylist id, day offset into the open days, slot index, status)
DEMO_BOOKINGS = [
    (0, 1, 1, 1, 0, "confirmed"),
    (1, 3, 1, 1, 1, "confirmed"),
    (2, 2, 2, 1, 0, "confirmed"),
    (3, 5, 2, 2, 3, "confirmed"),
    (4, 4, 3, 2, 2, "confirmed"),
    (5, 6, 3, 3, 4, "confirmed"),
    (6, 8, 4, 3, 1, "confirmed"),
    (7, 10, 2, 4, 5, "confirmed"),
    (8, 8, 4, 4, 0, "confirmed"),
    (9, 9, 1, 5, 2, "confirmed"),
    (0, 7, 4, 6, 6, "confirmed"),
    (1, 2, 2, 2, 3, "cancelled"),
]


def seed_demo_data(conn, pbkdf2_iterations: int) -> None:
    if conn.execute("SELECT COUNT(*) FROM users").fetchone()[0] > 0:
        return
    user_ids = []
    for name, email, phone in DEMO_USERS:
        cur = conn.execute(
            "INSERT INTO users (name, email, phone, password_hash) VALUES (?, ?, ?, ?)",
            (name, email, phone, hash_password(DEMO_PASSWORD, pbkdf2_iterations)),
        )
        user_ids.append(cur.lastrowid)

    days = open_days(now_sast().date(), 8)
    for user_idx, service_id, stylist_id, day_idx, slot_idx, status in DEMO_BOOKINGS:
        price = conn.execute("SELECT price FROM services WHERE id = ?", (service_id,)).fetchone()[0]
        conn.execute(
            "INSERT INTO bookings (user_id, service_id, stylist_id, booking_date, time_slot,"
            " price, deposit, status) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
            (user_ids[user_idx], service_id, stylist_id, days[day_idx].isoformat(),
             TIME_SLOTS[slot_idx], price, deposit(price), status),
        )
    conn.commit()
