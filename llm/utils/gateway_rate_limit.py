import json
import os
import re
import sys
import time
from contextlib import contextmanager
from datetime import datetime, timezone
from pathlib import Path
from typing import Callable, List, TypeVar


DEFAULT_GATEWAY_RPM = 10
WINDOW_SECONDS = 60.0

PROJECT_ROOT = Path(__file__).resolve().parents[2]
STATE_DIR = PROJECT_ROOT / "llm"
STATE_FILE = STATE_DIR / ".gateway_rate_limit_state.json"
LOCK_FILE = STATE_DIR / ".gateway_rate_limit_state.lock"


def _resolve_gateway_rpm() -> int:
    """Read gateway RPM from env, falling back to the default on invalid input."""
    raw = os.getenv("GATEWAY_RPM", "").strip()
    if not raw:
        return DEFAULT_GATEWAY_RPM
    try:
        rpm = int(raw)
    except ValueError:
        return DEFAULT_GATEWAY_RPM
    return rpm if rpm > 0 else DEFAULT_GATEWAY_RPM


if os.name == "nt":
    import msvcrt

    @contextmanager
    def _exclusive_lock():
        """Acquire an inter-process lock on Windows using msvcrt."""
        STATE_DIR.mkdir(parents=True, exist_ok=True)
        with open(LOCK_FILE, "a+b") as lock_f:
            lock_f.seek(0, os.SEEK_END)
            if lock_f.tell() == 0:
                lock_f.write(b"0")
                lock_f.flush()
            lock_f.seek(0)
            while True:
                try:
                    msvcrt.locking(lock_f.fileno(), msvcrt.LK_NBLCK, 1)
                    break
                except OSError:
                    time.sleep(0.05)
            try:
                yield
            finally:
                lock_f.seek(0)
                msvcrt.locking(lock_f.fileno(), msvcrt.LK_UNLCK, 1)

else:
    import fcntl

    @contextmanager
    def _exclusive_lock():
        """Acquire an inter-process lock on POSIX systems."""
        STATE_DIR.mkdir(parents=True, exist_ok=True)
        with open(LOCK_FILE, "a+", encoding="utf-8") as lock_f:
            fcntl.flock(lock_f.fileno(), fcntl.LOCK_EX)
            try:
                yield
            finally:
                fcntl.flock(lock_f.fileno(), fcntl.LOCK_UN)


def _read_state() -> List[float]:
    if not STATE_FILE.exists():
        return []
    try:
        payload = json.loads(STATE_FILE.read_text(encoding="utf-8"))
    except Exception:
        return []
    if not isinstance(payload, list):
        return []
    out: List[float] = []
    for item in payload:
        try:
            out.append(float(item))
        except (TypeError, ValueError):
            continue
    return out


def _write_state(timestamps: List[float]) -> None:
    STATE_DIR.mkdir(parents=True, exist_ok=True)
    temp_file = STATE_FILE.with_suffix(".tmp")
    temp_file.write_text(json.dumps(timestamps), encoding="utf-8")
    temp_file.replace(STATE_FILE)


def wait_for_gateway_slot(operation: str = "request") -> None:
    """
    Block until a request slot is available under the configured gateway RPM.

    Uses a cross-process sliding-window counter stored in `llm/.gateway_rate_limit_state.json`
    and guarded by a filesystem lock.
    """
    rpm = _resolve_gateway_rpm()
    while True:
        now = time.time()
        with _exclusive_lock():
            timestamps = sorted(ts for ts in _read_state() if now - ts < WINDOW_SECONDS)
            if len(timestamps) < rpm:
                timestamps.append(now)
                _write_state(timestamps)
                return
            wait_seconds = max(0.01, WINDOW_SECONDS - (now - timestamps[0]))

        print(
            f"[INFO] Gateway throttle ({rpm} RPM): sleeping {wait_seconds:.2f}s before {operation}.",
            file=sys.stderr,
        )
        time.sleep(wait_seconds)


_RESET_AT_RE = re.compile(r"Limit resets at:\s*(\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2})\s*UTC")

T = TypeVar("T")


def _seconds_until_reset(message: str, max_wait_seconds: float) -> "float | None":
    """Parse the gateway's own '... Limit resets at: <UTC time> ...' out of a 429 body."""
    match = _RESET_AT_RE.search(message or "")
    if not match:
        return None
    try:
        reset_at = datetime.strptime(match.group(1), "%Y-%m-%d %H:%M:%S").replace(tzinfo=timezone.utc)
    except ValueError:
        return None
    wait_seconds = (reset_at - datetime.now(timezone.utc)).total_seconds() + 3.0  # small buffer
    if wait_seconds <= 0:
        return None
    return min(wait_seconds, max_wait_seconds)


def call_with_concurrency_backoff(fn: Callable[[], T], operation: str = "request",
                                    max_wait_seconds: float = 180.0) -> T:
    """
    Call `fn()` (a zero-arg gateway request). `wait_for_gateway_slot` paces how many NEW
    requests we start per minute, but the UPB gateway separately caps concurrent in-flight
    requests per API key ("max_parallel_requests"), and under load that cap can be reached
    even with our own pacing respected - the shared account has other traffic on it too.
    The 429 body names exactly when a slot frees ("Limit resets at: <UTC time>"), but the
    OpenAI SDK's own retry backoff is a few seconds - far short of that. Parse it and wait
    the real amount once, then retry; a second failure is left to the caller's existing
    per-call handling (log + skip), same as every other gateway failure in this pipeline.
    """
    try:
        return fn()
    except Exception as exc:
        if getattr(exc, "status_code", None) != 429:
            raise
        wait_seconds = _seconds_until_reset(str(exc), max_wait_seconds)
        if wait_seconds is None:
            raise
        print(
            f"[INFO] Gateway concurrency limit (max_parallel_requests) hit before {operation}; "
            f"waiting {wait_seconds:.0f}s for a slot to free up.",
            file=sys.stderr,
        )
        time.sleep(wait_seconds)
        return fn()
