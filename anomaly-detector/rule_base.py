import os
import json
import time
import logging
import signal
import sys
from datetime import datetime, timezone
import uuid

import boto3
from botocore.exceptions import ClientError

# ──────────────────────────────────────────────
# 설정
# ──────────────────────────────────────────────
AWS_REGION = os.environ.get("AWS_REGION", "ap-northeast-2")
GPS_QUEUE_URL = os.environ["GPS_QUEUE_URL"]
ALERT_QUEUE_URL = os.environ.get("ALERT_QUEUE_URL")
LOG_LEVEL = os.environ.get("LOG_LEVEL", "INFO")
POLL_WAIT_SECONDS = int(os.environ.get("POLL_WAIT_SECONDS", "20"))
MAX_MESSAGES = int(os.environ.get("MAX_MESSAGES", "10"))

# ──────────────────────────────────────────────
# 로깅
# ──────────────────────────────────────────────
logging.basicConfig(
    level=LOG_LEVEL,
    format="%(asctime)s [%(levelname)s] %(message)s",
)
logger = logging.getLogger(__name__)

# ──────────────────────────────────────────────
# SQS 클라이언트
# ──────────────────────────────────────────────
sqs = boto3.client("sqs", region_name=AWS_REGION)

# ──────────────────────────────────────────────
# graceful shutdown 플래그
# ──────────────────────────────────────────────
shutdown = False


def handle_signal(signum, frame):
    global shutdown
    logger.info(f"Signal {signum} 받음. 정상 종료 시작.")
    shutdown = True


signal.signal(signal.SIGTERM, handle_signal)
signal.signal(signal.SIGINT, handle_signal)


# ──────────────────────────────────────────────
# 더미 룰: 50% 확률로 이상 감지
# 실제 스크립트로 교체될 부분
# ──────────────────────────────────────────────
def analyze(gps_data: dict) -> dict | None:
    """
    이상 감지 시 alert dict 반환, 정상이면 None.
    팀원이 만들 진짜 룰로 이 함수만 교체하면 됨.
    """
    import random

    user_id = gps_data["user_id"]
    lat = gps_data["lat"]
    lng = gps_data["lng"]

    if random.random() < 0.5:
        return {
            "alert_id": f"alert_{uuid.uuid4().hex[:12]}",
            "user_id": user_id,
            "alert_type": "dummy_abnormal",
            "timestamp": datetime.now(timezone.utc).isoformat(),
            "detail": {
                "lat": lat,
                "lng": lng,
                "reason": "더미 룰: 무작위 트리거",
            },
        }
    return None


# ──────────────────────────────────────────────
# 메시지 처리
# ──────────────────────────────────────────────
def process_message(message: dict) -> bool:
    """처리 성공 시 True (메시지 삭제), 실패 시 False (재처리 위해 큐에 남김)."""
    try:
        body = json.loads(message["Body"])
        logger.info(f"수신: user_id={body.get('user_id')} ts={body.get('timestamp')}")

        alert = analyze(body)

        if alert:
            if ALERT_QUEUE_URL:
                sqs.send_message(
                    QueueUrl=ALERT_QUEUE_URL,
                    MessageBody=json.dumps(alert),
                )
                logger.warning(f"이상 감지 발행: alert_id={alert['alert_id']}")
            else:
                logger.warning(f"이상 감지 (ALERT_QUEUE_URL 미설정, 로그만): alert_id={alert['alert_id']}")

        return True
    except json.JSONDecodeError as e:
        logger.error(f"JSON 파싱 실패: {e} / body={message.get('Body')}")
        return True  # 잘못된 메시지는 삭제 (DLQ 재처리 대상 아님)
    except ClientError as e:
        logger.error(f"AWS 호출 실패: {e}")
        return False
    except Exception as e:
        logger.exception(f"예외 발생: {e}")
        return False


# ──────────────────────────────────────────────
# 메인 루프
# ──────────────────────────────────────────────
def main():
    logger.info("Rule-base worker 시작")
    logger.info(f"GPS_QUEUE_URL={GPS_QUEUE_URL}")
    logger.info(f"ALERT_QUEUE_URL={ALERT_QUEUE_URL}")

    while not shutdown:
        try:
            resp = sqs.receive_message(
                QueueUrl=GPS_QUEUE_URL,
                MaxNumberOfMessages=MAX_MESSAGES,
                WaitTimeSeconds=POLL_WAIT_SECONDS,
            )
            messages = resp.get("Messages", [])

            if not messages:
                continue

            for msg in messages:
                if shutdown:
                    break
                success = process_message(msg)
                if success:
                    sqs.delete_message(
                        QueueUrl=GPS_QUEUE_URL,
                        ReceiptHandle=msg["ReceiptHandle"],
                    )
        except ClientError as e:
            logger.error(f"SQS receive 실패: {e}")
            time.sleep(5)
        except Exception as e:
            logger.exception(f"루프 예외: {e}")
            time.sleep(5)

    logger.info("Rule-base worker 종료")


if __name__ == "__main__":
    main()
