FROM python:3.12-slim

ENV PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1 \
    PIP_NO_CACHE_DIR=1 \
    HOME=/tmp

WORKDIR /app

COPY requirements.txt ./
RUN pip install --no-cache-dir -r requirements.txt \
    && useradd --system --uid 10001 --user-group --create-home --home-dir /tmp/apkbot apkbot

COPY bot.py apk_inspector.py config.py ./

USER 10001:10001
CMD ["python", "bot.py"]
