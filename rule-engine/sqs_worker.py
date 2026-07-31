"""
Re;caRing Rule Engine SQS Worker

Bridges the two queues around the rule engine:

  detection-request queue  --(GpsDetectionMessage)-->  run_engine()
                                                            |
                            <--(DetectionResultMessage×0~2)-- device status + safe zone
  detection-result queue

Flow per message:
  1. Long-poll the request queue (matches the Java consumer: max 10, wait 20s).
  2. Deserialize the body and run run_engine() — evaluates both axes
     (device status, safe zone) and returns 0~2 results.
  3. Publish every result to the result queue.
  4. Delete (ack) the request message only after all results are safely published.
     If publishing fails midway, the whole request is redelivered; the Java
     consumer deduplicates by comparing stored state, so re-published results
     are harmless.

Failure policy:
  - Malformed message (bad JSON / missing or invalid fields): a retry can never
    succeed, so log and delete to avoid infinite redelivery. A DLQ redrive
    policy on the request queue is still recommended as a safety net.
  - AWS/network error while publishing the result: keep the message (no delete)
    so SQS redelivers it after the visibility timeout.

Environment variables (same names the Java app uses in application-dev.yml):
  AWS_SQS_DETECTION_REQUEST_QUEUE_URL   (required)
  AWS_SQS_DETECTION_RESULT_QUEUE_URL    (required)
  AWS_REGION                            (standard boto3 resolution applies)
"""

from __future__ import annotations

import json
import logging
import os
import signal
import sys
import time
from typing import Any, Dict, Optional

import boto3
from botocore.exceptions import BotoCoreError, ClientError

from recaring_rule_engine_final import run_engine

REQUEST_QUEUE_URL_ENV = "AWS_SQS_DETECTION_REQUEST_QUEUE_URL"
RESULT_QUEUE_URL_ENV = "AWS_SQS_DETECTION_RESULT_QUEUE_URL"

MAX_MESSAGES = 10
WAIT_TIME_SECONDS = 20
ERROR_BACKOFF_SECONDS = 5

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s %(levelname)s %(message)s",
)
log = logging.getLogger("rule-engine-worker")

_shutdown_requested = False


def _request_shutdown(signum: int, frame: Any) -> None:
    global _shutdown_requested
    _shutdown_requested = True
    log.info("[워커 종료 : 시그널 수신]: signum=%s", signum)


def load_queue_urls() -> tuple[str, str]:
    request_queue_url = os.environ.get(REQUEST_QUEUE_URL_ENV, "").strip()
    result_queue_url = os.environ.get(RESULT_QUEUE_URL_ENV, "").strip()

    missing = [
        name
        for name, value in [
            (REQUEST_QUEUE_URL_ENV, request_queue_url),
            (RESULT_QUEUE_URL_ENV, result_queue_url),
        ]
        if not value
    ]
    if missing:
        log.error("[워커 기동 : 환경변수 누락]: missing=%s", ",".join(missing))
        sys.exit(1)

    return request_queue_url, result_queue_url


def parse_request(body: str) -> Dict[str, Any]:
    message = json.loads(body)
    if not isinstance(message, dict):
        raise ValueError(f"message body must be a JSON object, got {type(message).__name__}")
    return message


def handle_message(
    sqs: Any,
    result_queue_url: str,
    message_id: str,
    body: str,
) -> bool:
    """
    Process one request message.

    Returns True if the message should be deleted (acked) from the request
    queue, False if it should be kept for redelivery.
    """
    try:
        request = parse_request(body)
    except (json.JSONDecodeError, ValueError) as e:
        # Poison message: retrying will never help, drop it.
        log.error("[기기상태 판정 : 역직렬화 실패]: messageId=%s | error=%s", message_id, e)
        return True

    try:
        results = run_engine(request)
    except (KeyError, TypeError, ValueError) as e:
        # Field present but invalid (e.g. unparsable timestamp) — also poison.
        log.error("[룰 판정 : 판정 실패]: messageId=%s | error=%s", message_id, e)
        return True

    member_key = request.get("member_key")

    if not results:
        log.info("[룰 판정 : 전이 없음]: member_key=%s", member_key)
        return True

    for result in results:
        try:
            sqs.send_message(
                QueueUrl=result_queue_url,
                MessageBody=json.dumps(result, ensure_ascii=False),
            )
        except (ClientError, BotoCoreError) as e:
            # Transient AWS failure: keep the request message so SQS redelivers it.
            log.error("[룰 결과 : 발행 실패]: member_key=%s | error=%s", member_key, e)
            return False

        log.info("[룰 결과 : 발행 완료]: member_key=%s | %s", member_key, describe_result(result))

    return True


def describe_result(result: Dict[str, Any]) -> str:
    """결과 종류별 로그용 한 줄 요약."""
    if result.get("detection_type") == "SAFE_ZONE":
        return (
            f"type=SAFE_ZONE | event={result.get('zone_event')}"
            f" | zone_key={result.get('zone_key')}"
        )
    return (
        f"type=DEVICE_STATUS | {result.get('previous_state')}"
        f" -> {result.get('new_state')}"
    )


def poll_once(sqs: Any, request_queue_url: str, result_queue_url: str) -> None:
    response = sqs.receive_message(
        QueueUrl=request_queue_url,
        MaxNumberOfMessages=MAX_MESSAGES,
        WaitTimeSeconds=WAIT_TIME_SECONDS,
    )

    for message in response.get("Messages", []):
        should_delete = handle_message(
            sqs=sqs,
            result_queue_url=result_queue_url,
            message_id=message.get("MessageId", "unknown"),
            body=message.get("Body", ""),
        )
        if should_delete:
            sqs.delete_message(
                QueueUrl=request_queue_url,
                ReceiptHandle=message["ReceiptHandle"],
            )


def run() -> None:
    request_queue_url, result_queue_url = load_queue_urls()

    signal.signal(signal.SIGINT, _request_shutdown)
    signal.signal(signal.SIGTERM, _request_shutdown)

    sqs = boto3.client("sqs")
    log.info("[워커 기동 : 폴링 시작]: requestQueue=%s", request_queue_url)

    while not _shutdown_requested:
        try:
            poll_once(sqs, request_queue_url, result_queue_url)
        except (ClientError, BotoCoreError) as e:
            log.error("[SQS 수신 : 실패]: error=%s", e)
            time.sleep(ERROR_BACKOFF_SECONDS)

    log.info("[워커 종료 : 폴링 중단]")


if __name__ == "__main__":
    run()
