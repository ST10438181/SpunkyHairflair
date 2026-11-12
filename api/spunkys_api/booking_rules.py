"""Business rules shared by the API: deposit, opening days, time slots, past slots."""
from datetime import date, datetime, timedelta, timezone

# Same slots as the website / app. The salon is open Monday to Saturday.
TIME_SLOTS = [
    "9:00 AM", "10:00 AM", "11:00 AM", "12:00 PM",
    "1:00 PM", "2:00 PM", "3:00 PM", "4:00 PM",
]

# South Africa has no daylight saving, so a fixed UTC+2 offset is always correct.
SAST = timezone(timedelta(hours=2))


def now_sast() -> datetime:
    """Current local salon time (naive datetime in SAST)."""
    return datetime.now(SAST).replace(tzinfo=None)


def deposit(price: int) -> int:
    """50% deposit rounded half-up, identical to BookingCalculator.deposit in the app."""
    return (price + 1) // 2


def is_open_day(day: date) -> bool:
    return day.weekday() != 6  # 6 = Sunday


def slot_start(day: date, slot: str) -> datetime:
    hour_text, rest = slot.split(":")
    hour = int(hour_text) % 12
    if rest.endswith("PM"):
        hour += 12
    return datetime(day.year, day.month, day.day, hour, int(rest[:2]))


def is_past(day: date, slot: str, now: datetime) -> bool:
    return slot_start(day, slot) <= now


def open_days(start: date, count: int):
    days, day = [], start
    while len(days) < count:
        if is_open_day(day):
            days.append(day)
        day += timedelta(days=1)
    return days
