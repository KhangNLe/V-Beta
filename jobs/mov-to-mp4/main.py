"""Convert uploaded .mov betas to .mp4 and delete the original object."""

from __future__ import annotations

import json
import os
import subprocess
import tempfile
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path
from urllib.parse import urlparse

from google.cloud import storage

PORT = int(os.environ.get("PORT", "8080"))


def _event_object(payload: dict) -> tuple[str, str]:
    data = payload.get("data") if isinstance(payload.get("data"), dict) else payload
    bucket = data.get("bucket") or os.environ.get("STORAGE_PUBLIC_BUCKET_NAME", "")
    name = data.get("name") or data.get("object") or ""
    if not bucket or not name:
        raise ValueError("Cloud event is missing bucket or object name")
    return bucket, name


def _is_mov(name: str) -> bool:
    return name.lower().endswith(".mov")


def _run_ffmpeg(src: Path, dest: Path) -> None:
    copy = subprocess.run(
        [
            "ffmpeg",
            "-y",
            "-i",
            str(src),
            "-c",
            "copy",
            "-movflags",
            "+faststart",
            str(dest),
        ],
        capture_output=True,
        text=True,
        check=False,
    )
    if copy.returncode == 0 and dest.exists() and dest.stat().st_size > 0:
        return
    encode = subprocess.run(
        [
            "ffmpeg",
            "-y",
            "-i",
            str(src),
            "-c:v",
            "libx264",

            "-preset",
            "superfast",
            "-crf",
            "26",

            "-vf",
            "scale=-2:720",

            "-r",
            "30",

            "-c:a",
            "aac",
            "-b:a",
            "96k",
            "-movflags",
            "+faststart",
            str(dest),
        ],
        capture_output=True,
        text=True,
        check=False,
    )
    if encode.returncode != 0 or not dest.exists():
        raise RuntimeError(encode.stderr or copy.stderr or "ffmpeg failed")


def convert_mov(bucket_name: str, object_name: str) -> str:
    client = storage.Client()
    bucket = client.bucket(bucket_name)
    source = bucket.blob(object_name)
    mp4_name = object_name.rsplit(".", 1)[0] + ".mp4"
    dest_blob = bucket.blob(mp4_name)

    with tempfile.TemporaryDirectory() as tmp:
        src_path = Path(tmp) / "input.mov"
        dest_path = Path(tmp) / "output.mp4"
        source.download_to_filename(src_path)
        _run_ffmpeg(src_path, dest_path)
        dest_blob.upload_from_filename(dest_path, content_type="video/mp4")

    source.delete()
    return mp4_name


class Handler(BaseHTTPRequestHandler):
    def _write(self, status: int, body: str = "") -> None:
        encoded = body.encode("utf-8")
        self.send_response(status)
        self.send_header("Content-Type", "text/plain; charset=utf-8")
        self.send_header("Content-Length", str(len(encoded)))
        self.end_headers()
        if encoded:
            self.wfile.write(encoded)

    def do_GET(self) -> None:  # noqa: N802
        if urlparse(self.path).path in {"/", "/health"}:
            self._write(200, "ok")
            return
        self._write(404, "not found")

    def do_POST(self) -> None:  # noqa: N802
        length = int(self.headers.get("Content-Length", "0"))
        raw = self.rfile.read(length) if length else b"{}"
        try:
            payload = json.loads(raw.decode("utf-8") or "{}")
            bucket, name = _event_object(payload if isinstance(payload, dict) else {})
        except (ValueError, json.JSONDecodeError) as exc:
            self._write(400, str(exc))
            return

        if not _is_mov(name):
            self._write(204)
            return

        try:
            mp4_name = convert_mov(bucket, name)
        except Exception as exc:  # noqa: BLE001
            self._write(500, str(exc))
            return
        self._write(200, mp4_name)

    def log_message(self, format: str, *args) -> None:  # noqa: A003
        print(format % args)


if __name__ == "__main__":
    ThreadingHTTPServer(("0.0.0.0", PORT), Handler).serve_forever()
