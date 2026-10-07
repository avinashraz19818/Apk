# Zayro APK Inspector Telegram Bot

A ready-to-run Telegram bot based on the supplied APK project. It accepts APK documents and performs safe, non-executing ZIP-level inspection: file size, SHA-256, manifest presence, DEX/native-library counts, and signing entries.

The Android source is an Android GUI application and cannot be directly imported into a Telegram bot. This bot provides the useful server-side workflow without pretending to run Android code on a server. It never installs or executes uploaded APKs.

## Run locally

1. Create a bot with [@BotFather](https://t.me/BotFather).
2. `cd telegram_bot && python -m venv .venv && . .venv/bin/activate`
3. `pip install -r requirements.txt`
4. `cp .env.example .env` and set `BOT_TOKEN`.
5. `export $(grep -v '^#' .env | xargs)` then `python bot.py`.

Or with Docker:

```bash
docker build -t zayro-apk-bot .
docker run --rm -e BOT_TOKEN='your-token' zayro-apk-bot
```

For production, add persistent logging, rate limiting, and an external malware scanner before accepting untrusted files at scale.
