"""Placeholder GPS anomaly-detection worker.

This is a throwaway stand-in for the teammate's rule-based detector.
Its only job is to exercise the real runtime contract so the pipeline
(IAM / SQS / network / logging) can be validated before the real image lands:

  1. Long-poll SQS `gps-llm-queue`.
  2. Parse the `GpsPatternSqsMessage` produced by GpsPatternAnalysisScheduler.
  3. Run a trivially dumb "rule" (placeholder for the real analysis).
  4. (Optional) POST the verdict back to the Spring callback URL.
  5. Delete the message.

The real container only has to honour the same env-var contract below;
swapping it in is then just an image reference change.
"""

import json
import logging
import os
import signal
import sys

import boto3
import requests

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s %(levelname)s [anomaly-detector] %(message)s",
)
log = logging.getLogger(__name__)

# --- Runtime contract (keep stable so the real image can drop in) -------------
REGION = os.getenv("AWS_REGION", "ap-northeast-2")
QUEUE_URL = os.getenv("AWS_SQS_GPS_QUEUE_URL")
ENDPOINT_URL = os.getenv("AWS_ENDPOINT_URL")          # set for LocalStack; unset in prod
CALLBACK_URL = os.getenv("ANOMALY_CALLBACK_URL")      # optional until /internal/anomaly exists
WAIT_SECONDS = int(os.getenv("SQS_WAIT_SECONDS", "20"))   # long-poll
MIN_POINTS = int(os.getenv("ANOMALY_MIN_POINTS", "1"))    # placeholder rule threshold

_running = True


def _shutdown(signum, _frame):
    global _running
    log.info("received signal %s, shutting down after current poll", signum)
    _running = False


def detect(message: dict) -> dict | None:
    """Placeholder rule. Replace with real logic in the teammate's image.

    Flags an "anomaly" when fewer than MIN_POINTS GPS points arrived in the
    window -- i.e. the ward's device went quiet. Purely a stand-in.
    """
    ward = message.get("wardMemberKey")
    points = message.get("gpsPoints", [])
    if len(points) < MIN_POINTS:
        return {
            "wardMemberKey": ward,
            "anomaly": True,
            "reason": "INSUFFICIENT_GPS_POINTS",
            "pointCount": len(points),
            "detector": "placeholder",
        }
    return None


def maybe_callback(verdict: dict) -> None:
    if not CALLBACK_URL:
        log.info("anomaly detected (callback disabled): %s", verdict)
        return
    try:
        resp = requests.post(CALLBACK_URL, json=verdict, timeout=5)
        log.info("callback POST %s -> %s", CALLBACK_URL, resp.status_code)
    except requests.RequestException as e:
        log.error("callback POST failed: %s", e)


def main() -> None:
    if not QUEUE_URL:
        log.error("AWS_SQS_GPS_QUEUE_URL is required")
        sys.exit(1)

    signal.signal(signal.SIGTERM, _shutdown)
    signal.signal(signal.SIGINT, _shutdown)

    sqs = boto3.client("sqs", region_name=REGION, endpoint_url=ENDPOINT_URL)
    log.info("polling queue=%s region=%s endpoint=%s", QUEUE_URL, REGION, ENDPOINT_URL or "default")

    while _running:
        resp = sqs.receive_message(
            QueueUrl=QUEUE_URL,
            MaxNumberOfMessages=10,
            WaitTimeSeconds=WAIT_SECONDS,
        )
        for msg in resp.get("Messages", []):
            try:
                body = json.loads(msg["Body"])
                ward = body.get("wardMemberKey")
                points = body.get("gpsPoints", [])
                log.info("received message wardKey=%s points=%d", ward, len(points))

                verdict = detect(body)
                if verdict:
                    maybe_callback(verdict)

                sqs.delete_message(QueueUrl=QUEUE_URL, ReceiptHandle=msg["ReceiptHandle"])
            except (KeyError, json.JSONDecodeError) as e:
                # malformed message: log and leave it for the redrive/DLQ policy
                log.error("skipping malformed message: %s", e)

    log.info("stopped")


if __name__ == "__main__":
    main()
