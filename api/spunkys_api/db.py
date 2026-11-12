"""SQLite connection handling and database initialisation."""
import sqlite3
from pathlib import Path

from flask import current_app, g

DB_DIR = Path(__file__).resolve().parent.parent / "db"


def connect(path) -> sqlite3.Connection:
    conn = sqlite3.connect(str(path))
    conn.row_factory = sqlite3.Row
    conn.execute("PRAGMA foreign_keys = ON")
    return conn


def get_db() -> sqlite3.Connection:
    if "db" not in g:
        g.db = connect(current_app.config["DATABASE"])
    return g.db


def close_db(_exc=None):
    conn = g.pop("db", None)
    if conn is not None:
        conn.close()


def init_db(path, pbkdf2_iterations: int, with_demo_data: bool = True) -> None:
    """Create tables and load the catalogue. Safe to run repeatedly (idempotent)."""
    Path(path).parent.mkdir(parents=True, exist_ok=True)
    conn = connect(path)
    try:
        conn.executescript((DB_DIR / "schema.sql").read_text(encoding="utf-8"))
        conn.executescript((DB_DIR / "seed_catalog.sql").read_text(encoding="utf-8"))
        conn.commit()
        if with_demo_data:
            from .seed import seed_demo_data
            seed_demo_data(conn, pbkdf2_iterations)
    finally:
        conn.close()
