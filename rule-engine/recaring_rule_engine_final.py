"""
Re;caRing Rule Engine - 기기 상태 감지 (배터리 부족 + 오프라인) + 안심존 감지

detector: rule-v1

입력: GpsDetectionMessage (dict)
출력: DetectionResultMessage (dict) 목록 (run_engine 기준, 0~2건)

── 축 1. 기기 상태 4종 ─────────────────────────────
  ONLINE       : 최근 데이터 있음 + 배터리 정상 구간 (부족 기준 초과 ~ 완충 기준 미만)
  LOW_BATTERY  : 최근 데이터 있음 + 배터리가 부족 기준(사용자 설정) 이하 → 충전 필요 알림
  FULL_BATTERY : 최근 데이터 있음 + 배터리가 완충 기준(사용자 설정) 이상 → 충전 완료 알림
  OFFLINE      : 마지막 데이터가 너무 오래됨 (서버 타이머가 마지막 위치를 다시 던져 판단)

  보호자가 "충전하세요" 연락 → 충전 확인 전화를 반복하는 수고를 덜기 위해
  부족(내려갈 때)과 완충(올라올 때) 양방향 전이를 모두 알림 후보로 반환한다.

── 축 2. 안심존 상태 (기기 상태와 독립) ─────────────
  UNKNOWN → INSIDE(존 안) ⇄ OUTSIDE(존 밖)

  알림 이벤트 2종 (앱 설정 토글과 1:1 대응):
    ZONE_ENTRY : 존 밖 → 존 안 진입
    ZONE_EXIT  : 존 안 → 존 밖 이탈

  미복귀/장기 정체 판정은 두지 않는다 — 안심존이 여러 개(집, 노인정 등)면
  존 밖 장시간 체류가 일상이라 시간 기반 알림은 오탐이 된다.

핵심 규칙:
  - 엔진은 무상태다. 서버가 이전 상태(current_state / zone_state)를 메시지에 실어 보내고,
    엔진은 새 상태와 이벤트를 반환하며, 서버가 새 상태를 저장한다.
  - 상태가 "바뀔 때만" 결과를 반환한다. (전이 = 알림 후보) → 중복 알림 방지
  - 실제 푸시를 보낼지는 서버가 결정한다. (zone_event가 None인 결과 = 상태 저장만,
    예: UNKNOWN에서의 최초 초기화)
  - 존 판정 오탐 방지:
      * accuracy가 나쁜 좌표는 판정 보류 (상태 유지)
      * 이탈은 반경 + 여유 거리(히스테리시스)를 벗어나야 인정 → 경계 왕복 오탐 방지
      * 좌표가 오프라인 기준만큼 오래된 메시지(스케줄러 재평가)는 존 판정 안 함
      * 순서 가드: 좌표 기록 시각이 zone_state.updated_at보다 과거면 판정 안 함
        (SQS 표준 큐의 순서 역전/중복 수신 방어)

입력 스키마 (GpsDetectionMessage):
  member_key, current_state, collection_interval_seconds, evaluation_time,
  battery_low_threshold_percent  — 사용자 설정 부족 기준(%), 없으면 폴백 상수,
  battery_full_threshold_percent — 사용자 설정 완충 기준(%), 없으면 폴백 상수,
  current_location: {latitude, longitude, accuracy, battery, recorded_at},
  recent_locations: [current_location과 동일 형식] — 현재 미사용, 이후 배회/경로 이탈용 예약,
  safe_zones: [{safe_zone_key, latitude, longitude, radius_meters}],
  zone_state: {status, zone_key, outside_since, updated_at} | None

향후 확장: 경로 이탈 / 배회 / 시간대 이상 / 급속도 → 로그만 적재
"""

from __future__ import annotations

import json
import math
from datetime import datetime
from typing import Any, Dict, List, Optional


# ============================================================
# 1. 상수 (rule-v1)
# ============================================================

DETECTOR_NAME = "rule-v1"

# 결과 메시지 종류 (서버 컨슈머가 이 값으로 분기)
DETECTION_TYPE_DEVICE_STATUS = "DEVICE_STATUS"
DETECTION_TYPE_SAFE_ZONE = "SAFE_ZONE"

# 기기 상태값
STATE_ONLINE = "ONLINE"
STATE_LOW_BATTERY = "LOW_BATTERY"
STATE_FULL_BATTERY = "FULL_BATTERY"
STATE_OFFLINE = "OFFLINE"

# 배터리 기준(%) 폴백 — 서버가 사용자 설정값(battery_low/full_threshold_percent)을
# 메시지에 실어 보내는 것이 원칙이고, 이 상수들은 값이 누락됐을 때만 쓴다.
BATTERY_LOW_THRESHOLD_PERCENT = 20.0
BATTERY_FULL_THRESHOLD_PERCENT = 100.0

# 오프라인 기준(분) — 수집 주기에 따라 다르게 적용한다.
#   30초 ~ 2분(<=120초) → 10분
#   3분 ~ 5분(>=180초)  → 15분
OFFLINE_THRESHOLD_SHORT_MINUTES = 10.0
OFFLINE_THRESHOLD_LONG_MINUTES = 15.0
OFFLINE_INTERVAL_BOUNDARY_SECONDS = 120

# ── 안심존 상수 ──────────────────────────────────────

EARTH_RADIUS_METERS = 6_371_000.0

# 존 상태값
ZONE_STATUS_UNKNOWN = "UNKNOWN"
ZONE_STATUS_INSIDE = "INSIDE"
ZONE_STATUS_OUTSIDE = "OUTSIDE"

# 존 알림 이벤트 (앱 설정: 안심존 진입 알림 / 안심존 이탈 알림)
ZONE_EVENT_ENTRY = "ZONE_ENTRY"
ZONE_EVENT_EXIT = "ZONE_EXIT"

# GPS 정확도가 이 값(m)보다 나쁘면 존 판정을 건너뛴다 (오탐 방지)
ZONE_ACCURACY_LIMIT_METERS = 100.0

# 이탈 히스테리시스(m): 반경 + 이 여유를 벗어나야 이탈로 인정 (경계 왕복 오탐 방지)
ZONE_EXIT_MARGIN_METERS = 50.0


# ============================================================
# 2. 공통 함수
# ============================================================

def parse_time(timestamp: str) -> datetime:
    """ISO 형식 timestamp 문자열을 datetime으로 변환한다."""
    return datetime.fromisoformat(timestamp)


def resolve_offline_threshold_minutes(
    collection_interval_seconds: Optional[int],
) -> float:
    """
    수집 주기에 따라 오프라인 판단 기준(분)을 정한다.

    주기가 짧으면(자주 보고) 짧은 공백도 이상하므로 기준을 낮추고,
    주기가 길면 공백이 정상이므로 기준을 높인다.
    """
    if collection_interval_seconds is None:
        return OFFLINE_THRESHOLD_SHORT_MINUTES

    if int(collection_interval_seconds) <= OFFLINE_INTERVAL_BOUNDARY_SECONDS:
        return OFFLINE_THRESHOLD_SHORT_MINUTES

    return OFFLINE_THRESHOLD_LONG_MINUTES


def resolve_battery_low_threshold_percent(message: Dict[str, Any]) -> float:
    """배터리 부족 기준: 사용자 설정값(메시지) 우선, 없으면 폴백 상수."""
    value = message.get("battery_low_threshold_percent")
    return float(value) if value is not None else BATTERY_LOW_THRESHOLD_PERCENT


def resolve_battery_full_threshold_percent(message: Dict[str, Any]) -> float:
    """배터리 완충 기준: 사용자 설정값(메시지) 우선, 없으면 폴백 상수."""
    value = message.get("battery_full_threshold_percent")
    return float(value) if value is not None else BATTERY_FULL_THRESHOLD_PERCENT


def haversine_meters(lat1: float, lng1: float, lat2: float, lng2: float) -> float:
    """두 좌표 사이 거리(m). 안심존 반경(수백 m ~ 수 km) 판정에는 충분한 정밀도."""
    phi1, phi2 = math.radians(lat1), math.radians(lat2)
    d_phi = math.radians(lat2 - lat1)
    d_lambda = math.radians(lng2 - lng1)
    a = math.sin(d_phi / 2) ** 2 + math.cos(phi1) * math.cos(phi2) * math.sin(d_lambda / 2) ** 2
    return 2 * EARTH_RADIUS_METERS * math.asin(math.sqrt(a))


# ============================================================
# 3. 기기 상태 계산
# ============================================================

def compute_device_state(
    current_location: Dict[str, Any],
    evaluation_time: datetime,
    offline_threshold_minutes: float,
    battery_low_threshold_percent: float,
    battery_full_threshold_percent: float,
) -> str:
    """
    기기 상태를 계산한다.

    우선순위: OFFLINE > LOW_BATTERY > FULL_BATTERY > ONLINE
    """
    recorded_at = parse_time(current_location["recorded_at"])
    gap_minutes = (evaluation_time - recorded_at).total_seconds() / 60.0

    # 1) 마지막 데이터가 너무 오래됐으면 오프라인 (배터리 값은 옛날 값이라 무시)
    if gap_minutes >= offline_threshold_minutes:
        return STATE_OFFLINE

    # 2) 최근 데이터가 있으면 그제야 배터리를 본다 (기준값 = 사용자 설정)
    battery = current_location.get("battery")
    if battery is not None and float(battery) <= battery_low_threshold_percent:
        return STATE_LOW_BATTERY
    if battery is not None and float(battery) >= battery_full_threshold_percent:
        return STATE_FULL_BATTERY

    # 3) 정상 구간 (부족 기준 초과 ~ 완충 기준 미만)
    return STATE_ONLINE


def make_result_message(
    member_key: Any,
    previous_state: Optional[str],
    new_state: str,
    detected_at: str,
) -> Dict[str, Any]:
    """기기 상태용 DetectionResultMessage를 만든다."""
    return {
        "detection_type": DETECTION_TYPE_DEVICE_STATUS,
        "member_key": member_key,
        "previous_state": previous_state,
        "new_state": new_state,
        "detected_at": detected_at,
        "detector": DETECTOR_NAME,
    }


def detect_device_status(message: Dict[str, Any]) -> Optional[Dict[str, Any]]:
    """
    GpsDetectionMessage를 받아 기기 상태 전이를 감지한다.

    반환:
      - 상태가 바뀌면 DetectionResultMessage(dict)
      - 안 바뀌면 None (알림 없음)
    """
    current_location = message.get("current_location")

    if current_location is None or current_location.get("recorded_at") is None:
        return None

    # 평가 시각: 서버가 준 evaluation_time 우선, 없으면 좌표 기록 시각으로 폴백
    evaluation_time_raw = message.get("evaluation_time")
    evaluation_time = (
        parse_time(evaluation_time_raw)
        if evaluation_time_raw is not None
        else parse_time(current_location["recorded_at"])
    )

    offline_threshold_minutes = resolve_offline_threshold_minutes(
        message.get("collection_interval_seconds")
    )

    previous_state = message.get("current_state")
    new_state = compute_device_state(
        current_location=current_location,
        evaluation_time=evaluation_time,
        offline_threshold_minutes=offline_threshold_minutes,
        battery_low_threshold_percent=resolve_battery_low_threshold_percent(message),
        battery_full_threshold_percent=resolve_battery_full_threshold_percent(message),
    )

    # 상태가 그대로면 알림 없음 (중복 방지의 핵심)
    if new_state == previous_state:
        return None

    # detected_at: 오프라인은 "알아챈 시각"(평가 시각), 그 외는 좌표 기록 시각
    if new_state == STATE_OFFLINE:
        detected_at = evaluation_time.isoformat()
    else:
        detected_at = current_location["recorded_at"]

    return make_result_message(
        member_key=message.get("member_key"),
        previous_state=previous_state,
        new_state=new_state,
        detected_at=detected_at,
    )


# ============================================================
# 4. 안심존 상태 계산
# ============================================================

def default_zone_state() -> Dict[str, Any]:
    """서버에 저장된 존 상태가 없을 때의 초기 상태."""
    return {
        "status": ZONE_STATUS_UNKNOWN,
        "zone_key": None,
        "outside_since": None,
        "updated_at": None,
    }


def make_inside_state(zone_key: str, updated_at: str) -> Dict[str, Any]:
    """존 안 상태."""
    return {
        "status": ZONE_STATUS_INSIDE,
        "zone_key": zone_key,
        "outside_since": None,
        "updated_at": updated_at,
    }


def make_outside_state(outside_since: str) -> Dict[str, Any]:
    """존 밖 상태. outside_since는 알림에는 안 쓰지만 화면 표시/로그 분석용으로 남긴다."""
    return {
        "status": ZONE_STATUS_OUTSIDE,
        "zone_key": None,
        "outside_since": outside_since,
        "updated_at": outside_since,
    }


def find_current_zone(
    latitude: float,
    longitude: float,
    safe_zones: List[Dict[str, Any]],
    held_zone_key: Optional[str] = None,
) -> Optional[Dict[str, Any]]:
    """
    현재 좌표가 속한 존을 찾는다.

    - 직전까지 속해 있던 존(held)은 반경 + 여유(margin) 안이면 계속 그 존으로 본다
      → 경계에서 GPS가 흔들려도 이탈/진입을 반복하지 않는다 (히스테리시스)
    - 그 외에는 반경 안에 드는 존 중 중심이 가장 가까운 존을 고른다
    """
    if held_zone_key is not None:
        held = next(
            (z for z in safe_zones if z.get("safe_zone_key") == held_zone_key), None)
        if held is not None:
            distance = haversine_meters(
                latitude, longitude, float(held["latitude"]), float(held["longitude"]))
            if distance <= float(held["radius_meters"]) + ZONE_EXIT_MARGIN_METERS:
                return held

    best_zone = None
    best_distance = None
    for zone in safe_zones:
        distance = haversine_meters(
            latitude, longitude, float(zone["latitude"]), float(zone["longitude"]))
        if distance <= float(zone["radius_meters"]) and (
                best_distance is None or distance < best_distance):
            best_zone, best_distance = zone, distance
    return best_zone


def make_zone_result_message(
    member_key: Any,
    zone_event: Optional[str],
    zone_key: Optional[str],
    new_zone_state: Dict[str, Any],
    detected_at: str,
) -> Dict[str, Any]:
    """
    안심존용 DetectionResultMessage를 만든다.

    zone_event가 None이면 "상태 저장만 하고 알림은 없음"이라는 뜻이다.
    (예: UNKNOWN에서의 최초 상태 초기화)
    """
    return {
        "detection_type": DETECTION_TYPE_SAFE_ZONE,
        "member_key": member_key,
        "zone_event": zone_event,
        "zone_key": zone_key,
        "new_zone_state": new_zone_state,
        "detected_at": detected_at,
        "detector": DETECTOR_NAME,
    }


def detect_safe_zone(message: Dict[str, Any]) -> Optional[Dict[str, Any]]:
    """
    GpsDetectionMessage를 받아 안심존 진입/이탈 전이를 감지한다.

    반환:
      - 상태가 바뀌면 결과(dict) — 진입/이탈 전이는 zone_event 포함
      - 아니면 None
    """
    safe_zones = message.get("safe_zones") or []
    if not safe_zones:
        return None

    current_location = message.get("current_location")
    if current_location is None or current_location.get("recorded_at") is None:
        return None

    # 정확도가 나쁜 좌표로는 판정하지 않는다 (상태 유지)
    accuracy = current_location.get("accuracy")
    if accuracy is not None and float(accuracy) > ZONE_ACCURACY_LIMIT_METERS:
        return None

    recorded_at = parse_time(current_location["recorded_at"])
    evaluation_time_raw = message.get("evaluation_time")
    evaluation_time = (
        parse_time(evaluation_time_raw) if evaluation_time_raw is not None else recorded_at
    )

    # 좌표가 오프라인 기준만큼 오래된 메시지(스케줄러 재평가)는 존 판정 보류
    offline_threshold_minutes = resolve_offline_threshold_minutes(
        message.get("collection_interval_seconds")
    )
    gap_minutes = (evaluation_time - recorded_at).total_seconds() / 60.0
    if gap_minutes >= offline_threshold_minutes:
        return None

    previous = message.get("zone_state") or default_zone_state()
    prev_status = previous.get("status", ZONE_STATUS_UNKNOWN)

    # 순서 가드: 이미 반영된 좌표(updated_at)보다 과거/같은 시각의 좌표는 무시
    # (SQS 표준 큐의 순서 역전·중복 수신이 최신 상태를 되돌리는 것을 방지)
    updated_at_raw = previous.get("updated_at")
    if updated_at_raw is not None and recorded_at <= parse_time(updated_at_raw):
        return None

    member_key = message.get("member_key")
    detected_at = current_location["recorded_at"]

    held_zone_key = previous.get("zone_key") if prev_status == ZONE_STATUS_INSIDE else None
    zone = find_current_zone(
        float(current_location["latitude"]),
        float(current_location["longitude"]),
        safe_zones,
        held_zone_key=held_zone_key,
    )

    # 1) 존 안
    if zone is not None:
        zone_key = zone["safe_zone_key"]
        if prev_status == ZONE_STATUS_INSIDE and previous.get("zone_key") == zone_key:
            return None  # 같은 존에 계속 있음

        # UNKNOWN → INSIDE는 최초 초기화라 무음, 그 외(밖→안, 다른 존→이 존)는 진입 이벤트
        zone_event = None if prev_status == ZONE_STATUS_UNKNOWN else ZONE_EVENT_ENTRY
        return make_zone_result_message(
            member_key, zone_event, zone_key,
            make_inside_state(zone_key, detected_at), detected_at)

    # 2) 존 밖
    if prev_status == ZONE_STATUS_INSIDE:
        # 이탈 순간 → 이탈 이벤트 (zone_key = 방금 나온 존)
        return make_zone_result_message(
            member_key, ZONE_EVENT_EXIT, previous.get("zone_key"),
            make_outside_state(detected_at), detected_at)

    if prev_status == ZONE_STATUS_UNKNOWN:
        # 최초 상태가 존 밖 → "이탈"이 아니므로 무음으로 상태만 초기화
        return make_zone_result_message(
            member_key, None, None,
            make_outside_state(detected_at), detected_at)

    # 계속 존 밖 → 변화 없음
    return None


# ============================================================
# 5. 통합 진입점
# ============================================================

def run_engine(message: Dict[str, Any]) -> List[Dict[str, Any]]:
    """
    GpsDetectionMessage 하나를 평가해 결과 목록을 반환한다.

    기기 상태와 안심존은 독립된 축이라 한 메시지에서 0~2건이 나올 수 있다.
    """
    results = []

    device_result = detect_device_status(message)
    if device_result is not None:
        results.append(device_result)

    zone_result = detect_safe_zone(message)
    if zone_result is not None:
        results.append(zone_result)

    return results


# ============================================================
# 6. 직접 실행 테스트
# ============================================================

# 샘플 공용 안심존: 집 (중심 37.5551, 126.9221 / 반경 500m)
_HOME_ZONE = {
    "safe_zone_key": "zone_home",
    "latitude": 37.5551,
    "longitude": 126.9221,
    "radius_meters": 500,
}


def _run_samples() -> None:
    samples = [
        {
            "name": "S1) 배터리 부족 감지 (ONLINE -> LOW_BATTERY)",
            "message": {
                "member_key": "elderly_001",
                "current_state": "ONLINE",
                "collection_interval_seconds": 30,
                "evaluation_time": "2026-07-27T10:15:00",
                "current_location": {
                    "latitude": 37.5551, "longitude": 126.9221,
                    "accuracy": 18, "battery": 15,
                    "recorded_at": "2026-07-27T10:15:00",
                },
                "recent_locations": [],
                "safe_zones": [],
                "zone_state": None,
            },
        },
        {
            "name": "S2) 방전 의심 (LOW_BATTERY -> OFFLINE, 30초 주기 → 기준 10분)",
            "message": {
                "member_key": "elderly_001",
                "current_state": "LOW_BATTERY",
                "collection_interval_seconds": 30,
                "evaluation_time": "2026-07-27T10:15:00",
                "current_location": {
                    "latitude": 37.5551, "longitude": 126.9221,
                    "accuracy": 18, "battery": 15,
                    "recorded_at": "2026-07-27T10:03:00",  # 12분 전
                },
                "recent_locations": [],
                "safe_zones": [],
                "zone_state": None,
            },
        },
        {
            "name": "S3) 일반 오프라인 (ONLINE -> OFFLINE, 배터리 정상이었음)",
            "message": {
                "member_key": "elderly_001",
                "current_state": "ONLINE",
                "collection_interval_seconds": 30,
                "evaluation_time": "2026-07-27T10:15:00",
                "current_location": {
                    "latitude": 37.5551, "longitude": 126.9221,
                    "accuracy": 18, "battery": 80,
                    "recorded_at": "2026-07-27T10:03:00",
                },
                "recent_locations": [],
                "safe_zones": [],
                "zone_state": None,
            },
        },
        {
            "name": "S4) 복귀 (OFFLINE -> ONLINE)",
            "message": {
                "member_key": "elderly_001",
                "current_state": "OFFLINE",
                "collection_interval_seconds": 30,
                "evaluation_time": "2026-07-27T10:20:00",
                "current_location": {
                    "latitude": 37.5551, "longitude": 126.9221,
                    "accuracy": 18, "battery": 80,
                    "recorded_at": "2026-07-27T10:20:00",
                },
                "recent_locations": [],
                "safe_zones": [],
                "zone_state": None,
            },
        },
        {
            "name": "S5) 변화 없음 (ONLINE 유지) → 결과 없음",
            "message": {
                "member_key": "elderly_001",
                "current_state": "ONLINE",
                "collection_interval_seconds": 30,
                "evaluation_time": "2026-07-27T10:15:00",
                "current_location": {
                    "latitude": 37.5551, "longitude": 126.9221,
                    "accuracy": 18, "battery": 80,
                    "recorded_at": "2026-07-27T10:15:00",
                },
                "recent_locations": [],
                "safe_zones": [],
                "zone_state": None,
            },
        },
        {
            "name": "S6) 5분 주기(기준 15분) → 12분 공백은 아직 오프라인 아님 → 결과 없음",
            "message": {
                "member_key": "elderly_002",
                "current_state": "ONLINE",
                "collection_interval_seconds": 300,
                "evaluation_time": "2026-07-27T10:15:00",
                "current_location": {
                    "latitude": 37.5551, "longitude": 126.9221,
                    "accuracy": 18, "battery": 80,
                    "recorded_at": "2026-07-27T10:03:00",  # 12분 전
                },
                "recent_locations": [],
                "safe_zones": [],
                "zone_state": None,
            },
        },
        {
            "name": "S7) 사용자 부족 기준 50% 설정, 배터리 40% → LOW_BATTERY (기본 20%였다면 무음)",
            "message": {
                "member_key": "elderly_003",
                "current_state": "ONLINE",
                "collection_interval_seconds": 30,
                "battery_low_threshold_percent": 50,
                "battery_full_threshold_percent": 90,
                "evaluation_time": "2026-07-27T10:15:00",
                "current_location": {
                    "latitude": 37.5551, "longitude": 126.9221,
                    "accuracy": 18, "battery": 40,
                    "recorded_at": "2026-07-27T10:15:00",
                },
                "recent_locations": [],
                "safe_zones": [],
                "zone_state": None,
            },
        },
        {
            "name": "S8) 충전 완료 (완충 기준 80%, 배터리 85%) → LOW_BATTERY -> FULL_BATTERY",
            "message": {
                "member_key": "elderly_003",
                "current_state": "LOW_BATTERY",
                "collection_interval_seconds": 30,
                "battery_low_threshold_percent": 20,
                "battery_full_threshold_percent": 80,
                "evaluation_time": "2026-07-27T12:00:00",
                "current_location": {
                    "latitude": 37.5551, "longitude": 126.9221,
                    "accuracy": 18, "battery": 85,
                    "recorded_at": "2026-07-27T12:00:00",
                },
                "recent_locations": [],
                "safe_zones": [],
                "zone_state": None,
            },
        },
        {
            "name": "S9) 완충 후 사용으로 감소 (배터리 70%) → FULL_BATTERY -> ONLINE (상태 저장만, 서버는 무음 처리)",
            "message": {
                "member_key": "elderly_003",
                "current_state": "FULL_BATTERY",
                "collection_interval_seconds": 30,
                "battery_low_threshold_percent": 20,
                "battery_full_threshold_percent": 80,
                "evaluation_time": "2026-07-27T15:00:00",
                "current_location": {
                    "latitude": 37.5551, "longitude": 126.9221,
                    "accuracy": 18, "battery": 70,
                    "recorded_at": "2026-07-27T15:00:00",
                },
                "recent_locations": [],
                "safe_zones": [],
                "zone_state": None,
            },
        },
        {
            "name": "Z1) 존 진입 (OUTSIDE -> INSIDE) → ZONE_ENTRY",
            "message": {
                "member_key": "elderly_001",
                "current_state": "ONLINE",
                "collection_interval_seconds": 30,
                "evaluation_time": "2026-07-27T11:00:00",
                "current_location": {
                    "latitude": 37.5551, "longitude": 126.9221,  # 존 중심
                    "accuracy": 18, "battery": 80,
                    "recorded_at": "2026-07-27T11:00:00",
                },
                "recent_locations": [],
                "safe_zones": [_HOME_ZONE],
                "zone_state": {
                    "status": "OUTSIDE", "zone_key": None,
                    "outside_since": "2026-07-27T10:40:00",
                    "updated_at": "2026-07-27T10:59:30",
                },
            },
        },
        {
            "name": "Z2) 존 이탈 (INSIDE -> OUTSIDE) → ZONE_EXIT",
            "message": {
                "member_key": "elderly_001",
                "current_state": "ONLINE",
                "collection_interval_seconds": 30,
                "evaluation_time": "2026-07-27T11:10:00",
                "current_location": {
                    "latitude": 37.5611, "longitude": 126.9221,  # 중심에서 약 668m
                    "accuracy": 18, "battery": 80,
                    "recorded_at": "2026-07-27T11:10:00",
                },
                "recent_locations": [],
                "safe_zones": [_HOME_ZONE],
                "zone_state": {
                    "status": "INSIDE", "zone_key": "zone_home",
                    "outside_since": None,
                    "updated_at": "2026-07-27T11:09:30",
                },
            },
        },
        {
            "name": "Z3) 경계 근처 흔들림 (반경 500m, 약 523m 지점) → 히스테리시스로 INSIDE 유지, 결과 없음",
            "message": {
                "member_key": "elderly_001",
                "current_state": "ONLINE",
                "collection_interval_seconds": 30,
                "evaluation_time": "2026-07-27T11:05:00",
                "current_location": {
                    "latitude": 37.5598, "longitude": 126.9221,  # 중심에서 약 523m (500+50 이내)
                    "accuracy": 30, "battery": 80,
                    "recorded_at": "2026-07-27T11:05:00",
                },
                "recent_locations": [],
                "safe_zones": [_HOME_ZONE],
                "zone_state": {
                    "status": "INSIDE", "zone_key": "zone_home",
                    "outside_since": None,
                    "updated_at": "2026-07-27T11:04:30",
                },
            },
        },
        {
            "name": "Z4) 계속 존 밖 (노인정 갔다가 장시간 외출) → 이탈 알림은 이탈 순간 1회뿐, 결과 없음",
            "message": {
                "member_key": "elderly_001",
                "current_state": "ONLINE",
                "collection_interval_seconds": 30,
                "evaluation_time": "2026-07-27T13:00:00",
                "current_location": {
                    "latitude": 37.5651, "longitude": 126.9221,  # 존 밖
                    "accuracy": 18, "battery": 80,
                    "recorded_at": "2026-07-27T13:00:00",
                },
                "recent_locations": [],
                "safe_zones": [_HOME_ZONE],
                "zone_state": {
                    "status": "OUTSIDE", "zone_key": None,
                    "outside_since": "2026-07-27T11:10:00",  # 2시간 가까이 밖이지만 알림 없음
                    "updated_at": "2026-07-27T12:59:30",
                },
            },
        },
        {
            "name": "Z5) 정확도 나쁜 좌표 (accuracy 250m) → 존 판정 보류, 결과 없음",
            "message": {
                "member_key": "elderly_001",
                "current_state": "ONLINE",
                "collection_interval_seconds": 30,
                "evaluation_time": "2026-07-27T11:20:00",
                "current_location": {
                    "latitude": 37.5551, "longitude": 126.9221,
                    "accuracy": 250, "battery": 80,
                    "recorded_at": "2026-07-27T11:20:00",
                },
                "recent_locations": [],
                "safe_zones": [_HOME_ZONE],
                "zone_state": {
                    "status": "OUTSIDE", "zone_key": None,
                    "outside_since": "2026-07-27T11:10:00",
                    "updated_at": "2026-07-27T11:19:30",
                },
            },
        },
        {
            "name": "Z6) 순서 역전 (11:00 상태가 저장된 뒤 10:58 좌표 도착) → 순서 가드로 무시",
            "message": {
                "member_key": "elderly_001",
                "current_state": "ONLINE",
                "collection_interval_seconds": 30,
                "evaluation_time": "2026-07-27T11:00:30",
                "current_location": {
                    "latitude": 37.5611, "longitude": 126.9221,  # 존 밖 좌표 (뒤늦게 도착)
                    "accuracy": 18, "battery": 80,
                    "recorded_at": "2026-07-27T10:58:00",
                },
                "recent_locations": [],
                "safe_zones": [_HOME_ZONE],
                "zone_state": {
                    "status": "INSIDE", "zone_key": "zone_home",
                    "outside_since": None,
                    "updated_at": "2026-07-27T11:00:00",  # 이미 11:00 좌표까지 반영됨
                },
            },
        },
    ]

    for sample in samples:
        results = run_engine(sample["message"])
        print(f"=== {sample['name']} ===")
        if not results:
            print("  결과: 없음 (상태 유지, 알림 안 보냄)")
        else:
            for result in results:
                print("  결과:", json.dumps(result, ensure_ascii=False))
        print()


if __name__ == "__main__":
    _run_samples()
