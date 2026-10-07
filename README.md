# APK Metadata Report Telegram Bot

A small, private-by-default Telegram bot that accepts an Android `.apk` and returns a **read-only static metadata report**. It does not execute or change APK contents.

## Important: what was and was not ported

The supplied `zayrop.zip` contains the Android **XuanDun/Zayro Protection** app. Its main action is a DEX-encrypting runtime packer with anti-debugging/instrumentation checks. The archive does **not** include the runtime stub (`stub_classes.dex`), `XuanDun.zip`, or the referenced native protection library; the Android project also refers to build-time libraries that are not included. As supplied, it is therefore not enough to produce a working protected APK.

This repository does **not** reproduce the code-encryption loader or anti-analysis behavior, and the bot does not claim to protect/repack APKs. It provides the safe, usable Telegram workflow for inspecting an APK's metadata instead. It never runs an uploaded app and never modifies or signs it.

## Bot features

- Reads package name, app label, version and SDK levels.
- Lists requested permissions, activities, services, receivers and providers.
- Lists root-level DEX files and bundled native ABIs.
- Reports manifest flags such as `debuggable`, `allowBackup` and cleartext traffic.
- Reports detected signing-scheme entries and certificate SHA-256 fingerprints. Scheme presence is **not** a full cryptographic signature verification.
- Includes the APK's SHA-256, size, archive inventory, and review notes.
- Uses upload/archive limits, processes one APK at a time, and deletes temporary files after processing.
- Restricts conversations to private chats. Access is allowlisted by default.

A report is metadata, not a malware verdict or a guarantee that an app is safe.

## Requirements

- Python 3.11+ (or Docker / Docker Compose)
- A Telegram bot token created with Telegram's official **@BotFather**
- Internet access from the machine running the bot (Telegram long polling)

A bot token cannot be created from this repository. Keep the token private; never commit a real `.env` file.

## Run locally

```bash
python -m venv .venv
# Linux/macOS:
source .venv/bin/activate
# Windows PowerShell:
# .venv\Scripts\Activate.ps1

pip install -r requirements.txt
cp .env.example .env
```

Set `BOT_TOKEN` in `.env`, then start the bot:

```bash
python bot.py
```

Open a private chat with your bot and send `/myid`. Add the numeric ID it returns to `ALLOWED_USER_IDS` in `.env`, for example:

```dotenv
ALLOWED_USER_IDS=123456789
```

Restart the bot, send `/start`, then upload an `.apk` **as a document**. The bot replies with an `apk-report-<hash>.txt` file. To allow several users, use comma-separated numeric IDs. To deliberately make the bot public, set `ALLOW_ALL_USERS=true` instead; public use is not recommended for a bot that receives private app files.

## Run with Docker Compose

```bash
cp .env.example .env
# Set BOT_TOKEN in .env (and optionally ALLOWED_USER_IDS).
docker compose up -d --build
docker compose logs -f
```

The container runs as an unprivileged user, has a read-only root filesystem, drops Linux capabilities, and uses a temporary `/tmp` filesystem for uploaded files. Stop it with `docker compose down`.

## Configuration

| Variable | Default | Purpose |
|---|---:|---|
| `BOT_TOKEN` | required | Token from @BotFather. |
| `ALLOWED_USER_IDS` | empty | Comma-separated Telegram user IDs allowed to upload APKs. |
| `ALLOW_ALL_USERS` | `false` | Set to `true` only if you intentionally want a public bot. |
| `MAX_UPLOAD_MB` | `19` | Maximum Telegram download size; cannot exceed 20 MB with Telegram's hosted Bot API. |
| `MAX_UNCOMPRESSED_MB` | `256` | Maximum expanded ZIP size accepted by the inspector. |
| `MAX_ARCHIVE_ENTRIES` | `50000` | Maximum ZIP central-directory entries accepted. |
| `TEMP_DIR` | `/tmp/apk-telegram-bot` | Temporary processing directory; files are removed after the report is sent. |

The bot only accepts private chats and `.apk` documents. It does not keep a database or a permanent copy of uploaded APKs. The temporary upload directory is removed after processing, and the text report is kept in memory only while it is sent. `/privacy` explains this behavior.

## Run tests

```bash
python -m unittest discover -s tests -v
```

## Original archive note

`zayrop.zip` is retained as the supplied Android source archive and is not used by the Telegram bot. It contains sample/test signing-key files; **never use those sample keys to sign a production app**. Use a private release key managed outside the repository for any app you own.
