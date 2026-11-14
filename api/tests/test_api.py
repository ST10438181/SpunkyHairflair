"""API + database tests. Run from the api/ folder:  python -m unittest discover -s tests -v"""
import os
import sqlite3
import tempfile
import unittest
from datetime import datetime

from spunkys_api import create_app
from spunkys_api import booking_rules, validators
from spunkys_api.security import hash_password, verify_password

# Monday 9 November 2026, 10:00 - fixed so date rules are deterministic.
FIXED_NOW = datetime(2026, 11, 9, 10, 0)
TUESDAY = "2026-11-10"
SUNDAY = "2026-11-15"
GOOD_USER = {"name": "Test Customer", "email": "test@example.com",
             "phone": "0812345678", "password": "Curls2026"}


class ApiTestCase(unittest.TestCase):
    seed_demo = False

    def setUp(self):
        fd, self.db_path = tempfile.mkstemp(suffix=".db")
        os.close(fd)
        os.unlink(self.db_path)  # let create_app build a fresh database
        self.app = create_app({
            "DATABASE": self.db_path,
            "PBKDF2_ITERATIONS": 1000,   # fast hashing for tests only
            "SEED_DEMO_DATA": self.seed_demo,
            "NOW": lambda: FIXED_NOW,
            "TESTING": True,
        })
        self.client = self.app.test_client()

    def tearDown(self):
        if os.path.exists(self.db_path):
            os.unlink(self.db_path)

    # helpers
    def register(self, **overrides):
        body = {**GOOD_USER, **overrides}
        return self.client.post("/api/auth/register", json=body)

    def auth(self, token):
        return {"Authorization": f"Bearer {token}"}

    def new_user_token(self, email="test@example.com"):
        res = self.register(email=email)
        self.assertEqual(res.status_code, 201, res.get_json())
        return res.get_json()["token"]

    def book(self, token, **overrides):
        body = {"serviceId": 1, "stylistId": 1, "date": TUESDAY, "time": "10:00 AM", **overrides}
        return self.client.post("/api/bookings", json=body, headers=self.auth(token))


class CatalogTests(ApiTestCase):
    def test_health(self):
        res = self.client.get("/api/health")
        self.assertEqual(res.status_code, 200)
        self.assertEqual(res.get_json()["status"], "ok")

    def test_services_and_stylists_come_from_database(self):
        services = self.client.get("/api/services").get_json()
        stylists = self.client.get("/api/stylists").get_json()
        self.assertEqual(len(services), 10)
        self.assertEqual(services[0], {"id": 1, "name": "Wash & Blow", "price": 150, "duration": "45 min"})
        self.assertIn("Luna Summerton", [s["name"] for s in stylists])
        self.assertEqual(next(s for s in stylists if s["name"] == "Luna Summerton")["specialty"], "Barber")

    def test_unknown_route_returns_json_404(self):
        res = self.client.get("/api/nope")
        self.assertEqual(res.status_code, 404)
        self.assertIn("error", res.get_json())


class AuthTests(ApiTestCase):
    def test_register_returns_token_and_never_exposes_password(self):
        res = self.register()
        self.assertEqual(res.status_code, 201)
        body = res.get_json()
        self.assertTrue(body["token"])
        self.assertEqual(body["user"]["email"], "test@example.com")
        self.assertNotIn("password", str(body["user"]).lower())

    def test_password_is_stored_salted_and_hashed(self):
        self.register()
        conn = sqlite3.connect(self.db_path)
        stored = conn.execute("SELECT password_hash FROM users").fetchone()[0]
        conn.close()
        self.assertNotIn("Curls2026", stored)
        self.assertTrue(stored.startswith("pbkdf2_sha256$"))

    def test_register_validation_errors_are_reported_per_field(self):
        res = self.register(name="A", email="bad", phone="123", password="short")
        self.assertEqual(res.status_code, 400)
        self.assertEqual(set(res.get_json()["fields"]), {"name", "email", "phone", "password"})

    def test_duplicate_email_is_rejected_case_insensitively(self):
        self.assertEqual(self.register().status_code, 201)
        res = self.register(email="TEST@Example.com")
        self.assertEqual(res.status_code, 409)
        self.assertIn("email", res.get_json()["fields"])

    def test_login_success_and_failure(self):
        self.register()
        ok = self.client.post("/api/auth/login", json={"email": " Test@example.com ", "password": "Curls2026"})
        self.assertEqual(ok.status_code, 200)
        self.assertTrue(ok.get_json()["token"])
        bad = self.client.post("/api/auth/login", json={"email": "test@example.com", "password": "Wrong1234"})
        self.assertEqual(bad.status_code, 401)
        unknown = self.client.post("/api/auth/login", json={"email": "who@example.com", "password": "Curls2026"})
        self.assertEqual(unknown.get_json()["error"], bad.get_json()["error"])

    def test_protected_routes_need_a_valid_token(self):
        for method, url in [("get", "/api/me"), ("get", "/api/bookings"), ("post", "/api/bookings"),
                            ("delete", "/api/bookings/1"), ("post", "/api/auth/logout")]:
            self.assertEqual(getattr(self.client, method)(url).status_code, 401, url)
        res = self.client.get("/api/me", headers=self.auth("not-a-real-token"))
        self.assertEqual(res.status_code, 401)

    def test_logout_invalidates_token(self):
        token = self.new_user_token()
        self.assertEqual(self.client.post("/api/auth/logout", headers=self.auth(token)).status_code, 204)
        self.assertEqual(self.client.get("/api/me", headers=self.auth(token)).status_code, 401)

    def test_malformed_json_body_does_not_crash(self):
        res = self.client.post("/api/auth/login", data="not json", content_type="application/json")
        self.assertEqual(res.status_code, 401)


class ProfileTests(ApiTestCase):
    def test_update_profile(self):
        token = self.new_user_token()
        res = self.client.put("/api/me", json={"name": "New Name", "phone": "082 111 2222"}, headers=self.auth(token))
        self.assertEqual(res.status_code, 200)
        self.assertEqual(res.get_json()["name"], "New Name")
        self.assertEqual(res.get_json()["phone"], "0821112222")

    def test_update_profile_validates(self):
        token = self.new_user_token()
        res = self.client.put("/api/me", json={"name": "1", "phone": "x"}, headers=self.auth(token))
        self.assertEqual(res.status_code, 400)
        self.assertEqual(set(res.get_json()["fields"]), {"name", "phone"})

    def test_change_password_flow(self):
        token = self.new_user_token()
        other_session = self.client.post("/api/auth/login", json={"email": "test@example.com", "password": "Curls2026"}).get_json()["token"]

        wrong = self.client.put("/api/me/password", json={"currentPassword": "Nope12345", "newPassword": "Fresh2026"}, headers=self.auth(token))
        self.assertEqual(wrong.status_code, 400)
        weak = self.client.put("/api/me/password", json={"currentPassword": "Curls2026", "newPassword": "weak"}, headers=self.auth(token))
        self.assertEqual(weak.status_code, 400)

        ok = self.client.put("/api/me/password", json={"currentPassword": "Curls2026", "newPassword": "Fresh2026"}, headers=self.auth(token))
        self.assertEqual(ok.status_code, 204)
        self.assertEqual(self.client.get("/api/me", headers=self.auth(token)).status_code, 200)
        self.assertEqual(self.client.get("/api/me", headers=self.auth(other_session)).status_code, 401)
        self.assertEqual(self.client.post("/api/auth/login", json={"email": "test@example.com", "password": "Curls2026"}).status_code, 401)
        self.assertEqual(self.client.post("/api/auth/login", json={"email": "test@example.com", "password": "Fresh2026"}).status_code, 200)


class DeleteAccountTests(ApiTestCase):
    def test_wrong_password_does_not_delete(self):
        token = self.new_user_token()
        res = self.client.post("/api/me/delete", json={"password": "Nope12345"}, headers=self.auth(token))
        self.assertEqual(res.status_code, 400)
        self.assertIn("password", res.get_json()["fields"])
        self.assertEqual(self.client.get("/api/me", headers=self.auth(token)).status_code, 200)

    def test_delete_removes_account_bookings_and_sessions(self):
        token = self.new_user_token()
        self.book(token)
        self.assertEqual(self.client.post("/api/me/delete", json={"password": "Curls2026"}, headers=self.auth(token)).status_code, 204)
        self.assertEqual(self.client.get("/api/me", headers=self.auth(token)).status_code, 401)
        self.assertEqual(self.client.post("/api/auth/login", json={"email": "test@example.com", "password": "Curls2026"}).status_code, 401)
        conn = sqlite3.connect(self.db_path)
        self.assertEqual(conn.execute("SELECT COUNT(*) FROM users").fetchone()[0], 0)
        self.assertEqual(conn.execute("SELECT COUNT(*) FROM bookings").fetchone()[0], 0)
        conn.close()

    def test_slot_is_free_again_and_email_can_be_reused(self):
        token = self.new_user_token("a@example.com")
        self.book(token)
        self.client.post("/api/me/delete", json={"password": "Curls2026"}, headers=self.auth(token))
        other = self.new_user_token("b@example.com")
        self.assertEqual(self.book(other).status_code, 201)
        self.assertEqual(self.register(email="a@example.com").status_code, 201)

    def test_requires_login(self):
        self.assertEqual(self.client.post("/api/me/delete", json={"password": "Curls2026"}).status_code, 401)


class BookingTests(ApiTestCase):
    def test_create_and_list_booking(self):
        token = self.new_user_token()
        res = self.book(token, serviceId=3)           # Box Braids R450
        self.assertEqual(res.status_code, 201, res.get_json())
        b = res.get_json()
        self.assertEqual((b["service"], b["price"], b["deposit"], b["status"]), ("Box Braids", 450, 225, "confirmed"))
        self.assertEqual(b["stylistName"], "Asenathi Valashiya")
        listed = self.client.get("/api/bookings", headers=self.auth(token)).get_json()
        self.assertEqual([x["id"] for x in listed], [b["id"]])

    def test_bookings_are_sorted_by_date_then_time(self):
        token = self.new_user_token()
        self.book(token, date="2026-11-11", time="9:00 AM")
        self.book(token, date=TUESDAY, time="2:00 PM")
        self.book(token, date=TUESDAY, time="9:00 AM", stylistId=2)
        order = [(b["date"], b["time"]) for b in self.client.get("/api/bookings", headers=self.auth(token)).get_json()]
        self.assertEqual(order, [(TUESDAY, "9:00 AM"), (TUESDAY, "2:00 PM"), ("2026-11-11", "9:00 AM")])

    def test_double_booking_same_stylist_is_rejected_but_other_stylist_is_fine(self):
        a = self.new_user_token("a@example.com")
        b = self.new_user_token("b@example.com")
        self.assertEqual(self.book(a).status_code, 201)
        clash = self.book(b)
        self.assertEqual(clash.status_code, 409)
        self.assertIn("just taken", clash.get_json()["error"])
        self.assertEqual(self.book(b, stylistId=2).status_code, 201)

    def test_cancelling_frees_the_slot(self):
        a = self.new_user_token("a@example.com")
        b = self.new_user_token("b@example.com")
        booking_id = self.book(a).get_json()["id"]
        self.assertEqual(self.client.delete(f"/api/bookings/{booking_id}", headers=self.auth(a)).status_code, 204)
        self.assertEqual(self.client.get("/api/bookings", headers=self.auth(a)).get_json(), [])
        self.assertEqual(self.book(b).status_code, 201)
        conn = sqlite3.connect(self.db_path)   # soft delete: the row is kept
        self.assertEqual(conn.execute("SELECT status FROM bookings WHERE id = ?", (booking_id,)).fetchone()[0], "cancelled")
        conn.close()

    def test_cannot_cancel_someone_elses_booking_or_cancel_twice(self):
        a = self.new_user_token("a@example.com")
        b = self.new_user_token("b@example.com")
        booking_id = self.book(a).get_json()["id"]
        self.assertEqual(self.client.delete(f"/api/bookings/{booking_id}", headers=self.auth(b)).status_code, 404)
        self.assertEqual(self.client.delete(f"/api/bookings/{booking_id}", headers=self.auth(a)).status_code, 204)
        self.assertEqual(self.client.delete(f"/api/bookings/{booking_id}", headers=self.auth(a)).status_code, 404)

    def test_users_only_see_their_own_bookings(self):
        a = self.new_user_token("a@example.com")
        b = self.new_user_token("b@example.com")
        self.book(a)
        self.assertEqual(self.client.get("/api/bookings", headers=self.auth(b)).get_json(), [])

    def test_sunday_past_and_invalid_input_are_rejected(self):
        token = self.new_user_token()
        sunday = self.book(token, date=SUNDAY)
        self.assertEqual(sunday.status_code, 400)
        self.assertIn("date", sunday.get_json()["fields"])
        past = self.book(token, date="2026-11-09", time="9:00 AM")      # now is 10:00 on that day
        self.assertEqual(past.status_code, 400)
        self.assertIn("time", past.get_json()["fields"])
        self.assertEqual(self.book(token, date="2026-11-09", time="11:00 AM").status_code, 201)
        self.assertEqual(self.book(token, time="7:00 PM").status_code, 400)
        self.assertEqual(self.book(token, date="not-a-date").status_code, 400)
        self.assertEqual(self.book(token, serviceId="1").status_code, 400)
        self.assertEqual(self.book(token, serviceId=999).status_code, 400)
        self.assertEqual(self.book(token, stylistId=999).status_code, 400)

    def test_availability_marks_booked_and_past_slots(self):
        token = self.new_user_token()
        self.book(token, date="2026-11-09", time="12:00 PM")
        res = self.client.get("/api/availability?stylistId=1&date=2026-11-09")
        self.assertEqual(res.status_code, 200)
        slots = {s["time"]: s for s in res.get_json()["slots"]}
        self.assertEqual(slots["9:00 AM"]["reason"], "past")
        self.assertEqual(slots["12:00 PM"]["reason"], "booked")
        self.assertTrue(slots["1:00 PM"]["available"])
        other = {s["time"]: s for s in self.client.get("/api/availability?stylistId=2&date=2026-11-09").get_json()["slots"]}
        self.assertTrue(other["12:00 PM"]["available"])

    def test_availability_validates_parameters(self):
        self.assertEqual(self.client.get("/api/availability").status_code, 400)
        self.assertEqual(self.client.get(f"/api/availability?stylistId=1&date={SUNDAY}").status_code, 400)
        self.assertEqual(self.client.get(f"/api/availability?stylistId=99&date={TUESDAY}").status_code, 404)


class DatabaseTests(ApiTestCase):
    """The database itself enforces the rules, not just the Python code."""

    def conn(self):
        c = sqlite3.connect(self.db_path)
        c.execute("PRAGMA foreign_keys = ON")
        return c

    def test_all_tables_exist(self):
        c = self.conn()
        names = {r[0] for r in c.execute("SELECT name FROM sqlite_master WHERE type='table'")}
        c.close()
        self.assertTrue({"users", "services", "stylists", "bookings", "auth_tokens"} <= names)

    def test_unique_index_blocks_double_booking_even_without_the_api(self):
        token = self.new_user_token()
        self.book(token)
        c = self.conn()
        with self.assertRaises(sqlite3.IntegrityError):
            c.execute("INSERT INTO bookings (user_id, service_id, stylist_id, booking_date, time_slot, price, deposit)"
                      " VALUES (1, 1, 1, ?, '10:00 AM', 150, 75)", (TUESDAY,))
        c.close()

    def test_foreign_keys_are_enforced(self):
        c = self.conn()
        with self.assertRaises(sqlite3.IntegrityError):
            c.execute("INSERT INTO bookings (user_id, service_id, stylist_id, booking_date, time_slot, price, deposit)"
                      " VALUES (999, 1, 1, ?, '10:00 AM', 150, 75)", (TUESDAY,))
        c.close()

    def test_check_constraints(self):
        c = self.conn()
        with self.assertRaises(sqlite3.IntegrityError):
            c.execute("INSERT INTO services (name, price, duration_label) VALUES ('Free', 0, '1h')")
        c.close()


class SeedDataTests(ApiTestCase):
    seed_demo = True

    def test_every_transactional_table_has_at_least_ten_rows(self):
        c = sqlite3.connect(self.db_path)
        counts = {t: c.execute(f"SELECT COUNT(*) FROM {t}").fetchone()[0]
                  for t in ("users", "services", "stylists", "bookings")}
        c.close()
        self.assertGreaterEqual(counts["users"], 10)
        self.assertGreaterEqual(counts["services"], 10)
        self.assertGreaterEqual(counts["bookings"], 10)
        self.assertEqual(counts["stylists"], 4)

    def test_demo_account_can_log_in_and_has_bookings(self):
        res = self.client.post("/api/auth/login", json={"email": "lerato.dlamini@example.com", "password": "Curls2026"})
        self.assertEqual(res.status_code, 200)
        bookings = self.client.get("/api/bookings", headers=self.auth(res.get_json()["token"])).get_json()
        self.assertGreaterEqual(len(bookings), 1)


class RuleTests(unittest.TestCase):
    def test_deposit_rounds_half_up_like_the_app(self):
        self.assertEqual([booking_rules.deposit(p) for p in (450, 120, 175, 125, 1)], [225, 60, 88, 63, 1])

    def test_open_days_skip_sundays(self):
        days = booking_rules.open_days(datetime(2026, 11, 9).date(), 14)
        self.assertEqual(len(days), 14)
        self.assertTrue(all(d.weekday() != 6 for d in days))

    def test_slot_parsing(self):
        d = datetime(2026, 11, 9).date()
        self.assertEqual(booking_rules.slot_start(d, "12:00 PM").hour, 12)
        self.assertEqual(booking_rules.slot_start(d, "1:00 PM").hour, 13)
        self.assertEqual(booking_rules.slot_start(d, "9:00 AM").hour, 9)

    def test_validators_match_the_app(self):
        self.assertIsNone(validators.phone_error("081 234 5678"))
        self.assertIsNone(validators.phone_error("+27812345678"))
        self.assertIsNotNone(validators.phone_error("12345"))
        self.assertIsNotNone(validators.password_error("onlyletters"))
        self.assertIsNone(validators.name_error("Luna Summerton"))
        self.assertIsNotNone(validators.name_error("Luna123"))

    def test_password_hash_roundtrip(self):
        stored = hash_password("Curls2026", 1000)
        self.assertTrue(verify_password("Curls2026", stored))
        self.assertFalse(verify_password("Curls2027", stored))
        self.assertFalse(verify_password("Curls2026", "garbage"))


if __name__ == "__main__":
    unittest.main()
