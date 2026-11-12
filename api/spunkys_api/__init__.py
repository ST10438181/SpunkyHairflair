"""Spunky's Hairflair REST API (Flask + SQLite)."""
from pathlib import Path

from flask import Flask, jsonify

from .config import Config
from .db import close_db, init_db


def create_app(test_config=None) -> Flask:
    app = Flask(__name__)
    app.config.from_object(Config)
    if test_config:
        app.config.update(test_config)

    db_path = Path(app.config["DATABASE"])
    if not db_path.exists():
        # First start (or an ephemeral host): build the schema, catalogue and demo data.
        init_db(db_path, app.config["PBKDF2_ITERATIONS"],
                with_demo_data=app.config.get("SEED_DEMO_DATA", True))

    app.teardown_appcontext(close_db)

    from .routes import api
    app.register_blueprint(api, url_prefix="/api")

    @app.errorhandler(404)
    def not_found(_e):
        return jsonify(error="Not found"), 404

    @app.errorhandler(405)
    def method_not_allowed(_e):
        return jsonify(error="Method not allowed"), 405

    @app.errorhandler(500)
    def server_error(_e):
        return jsonify(error="Something went wrong on the server"), 500

    return app
