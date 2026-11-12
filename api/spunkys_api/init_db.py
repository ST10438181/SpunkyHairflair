"""Command line: create / refresh the database.

    python -m spunkys_api.init_db            # schema + catalogue + demo data
    python -m spunkys_api.init_db --reset    # delete the file first
    python -m spunkys_api.init_db --no-demo  # schema + catalogue only
"""
import argparse
from pathlib import Path

from .config import Config
from .db import init_db


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--reset", action="store_true", help="delete the database file first")
    parser.add_argument("--no-demo", action="store_true", help="skip demo customers and bookings")
    args = parser.parse_args()

    path = Path(Config.DATABASE)
    if args.reset and path.exists():
        path.unlink()
    init_db(path, Config.PBKDF2_ITERATIONS, with_demo_data=not args.no_demo)
    print(f"Database ready: {path}")


if __name__ == "__main__":
    main()
