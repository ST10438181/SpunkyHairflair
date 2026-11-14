# Hosting the API so every user can reach it

The app cannot use your laptop in production: other people's phones need a public **HTTPS** address.
These steps use **PythonAnywhere's free plan**, which keeps its files (so the SQLite database survives),
gives you `https://<username>.pythonanywhere.com` and needs no credit card. (Render's free plan also works,
but its disk is wiped on every restart, so new accounts and bookings would be lost.)

Free-plan limits to know about: one web app, 100 CPU-seconds per day, and the Web tab may ask you to press an
"extend" button every so often to keep the app running. That is plenty for a demo. Check pythonanywhere.com for the
current limits.

## 1. Create the account and get the code

1. Sign up at <https://www.pythonanywhere.com> (free **Beginner** account). Your username becomes the web address.
2. Open **Consoles ▸ Bash** and run (replace the URL with your repository if it differs):
   ```bash
   git clone https://github.com/ST10438181/SpunkyHairflair.git
   cd SpunkyHairflair/api
   mkvirtualenv spunky-venv --python=/usr/bin/python3.12
   pip install -r requirements.txt
   python -m spunkys_api.init_db
   ```
   If `python3.12` is not offered, pick any version from 3.10 up that PythonAnywhere lists.
   `init_db` creates the database (schema, services, stylists, demo data) at `api/data/spunkys.db`.

## 2. Create the web app

1. Open the **Web** tab ▸ **Add a new web app** ▸ **Manual configuration** ▸ the same Python version as above.
2. Under **Virtualenv**, enter `/home/<username>/.virtualenvs/spunky-venv`.
3. Under **Code**, click the link to the **WSGI configuration file** and replace everything in it with:
   ```python
   import sys

   path = '/home/<username>/SpunkyHairflair/api'
   if path not in sys.path:
       sys.path.insert(0, path)

   from spunkys_api import create_app
   application = create_app()
   ```
   (replace `<username>` with yours), save, go back to the Web tab and press **Reload**.
4. Test it in a browser: `https://<username>.pythonanywhere.com/api/services` should show the services list.

## 3. Point the app at it

Add this to `gradle.properties` in the project root (note the trailing slash and `/api/`):

```
apiBaseUrl=https://<username>.pythonanywhere.com/api/
releaseApiBaseUrl=https://<username>.pythonanywhere.com/api/
```

Rebuild and run. The app now works on any phone, with no USB cable and no `adb reverse`.
Debug builds still allow plain HTTP for local development; release builds only allow HTTPS.

## Updating the API later

```bash
cd ~/SpunkyHairflair && git pull
```
then press **Reload** on the Web tab. The database file is not in Git, so `git pull` never overwrites your data.
To reset it to the demo data: `cd api && python -m spunkys_api.init_db --reset`.

## Notes

* Passwords use PBKDF2 with 600 000 iterations. If the free CPU allowance runs out you can lower it with the
  environment variable `SPUNKYS_PBKDF2_ITERATIONS` (set it in the WSGI file with `os.environ` before the import).
* The demo accounts all use the password `Curls2026`. Delete them (or change the seed) before real customers use the app.
* Logs are on the Web tab (error log) if something fails after a reload.
