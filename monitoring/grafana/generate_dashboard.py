"""Generates the Grafana dashboard provisioned from dashboards/recaring-dashboard.json.

Edit this file instead of the JSON, then run:
    python3 monitoring/grafana/generate_dashboard.py monitoring/grafana/provisioning/dashboards/recaring-dashboard.json
"""
import json
import sys

PROM = {"type": "prometheus", "uid": "prometheus"}
CW = {"type": "cloudwatch", "uid": "cloudwatch"}
APP = 'job="recaring"'
API = APP + ', uri!~"/actuator.*|/api/v1/location/stream/.*|UNKNOWN"'
LOG_GROUP = [{"name": "/ecs/recaring"}]
APP_LOGS = "filter @logStream like /^spring-app/"

panels = []
_id = [0]
_y = [0]


def nid():
    _id[0] += 1
    return _id[0]


def row(title, collapsed=False):
    p = {"type": "row", "id": nid(), "title": title, "collapsed": collapsed,
         "gridPos": {"h": 1, "w": 24, "x": 0, "y": _y[0]}, "panels": []}
    _y[0] += 1
    panels.append(p)
    return p


class Line:
    """Lays out panels left to right on one grid line."""

    def __init__(self, h, target=None):
        self.h = h
        self.x = 0
        self.target = target

    def add(self, w, panel):
        panel["id"] = nid()
        panel["gridPos"] = {"h": self.h, "w": w, "x": self.x, "y": _y[0]}
        self.x += w
        (self.target["panels"] if self.target is not None else panels).append(panel)
        return self

    def done(self):
        _y[0] += self.h


def prom(expr, legend="", instant=False, ref="A"):
    t = {"datasource": PROM, "expr": expr, "legendFormat": legend, "refId": ref}
    if instant:
        t["instant"] = True
        t["range"] = False
    return t


def steps(*pairs):
    return {"mode": "absolute",
            "steps": [{"color": c, "value": v} for v, c in pairs]}


def stat(title, expr, unit="short", thresholds=None, mappings=None, desc="", decimals=None):
    defaults = {"unit": unit, "thresholds": thresholds or steps((None, "green")),
                "mappings": mappings or [], "color": {"mode": "thresholds"}}
    if decimals is not None:
        defaults["decimals"] = decimals
    return {"type": "stat", "title": title, "description": desc, "datasource": PROM,
            "targets": [prom(expr, instant=True)],
            "fieldConfig": {"defaults": defaults, "overrides": []},
            "options": {"colorMode": "background", "graphMode": "none", "textMode": "value",
                        "justifyMode": "center", "reduceOptions": {"calcs": ["lastNotNull"]}}}


def gauge(title, expr, thresholds, desc=""):
    return {"type": "gauge", "title": title, "description": desc, "datasource": PROM,
            "targets": [prom(expr, instant=True)],
            "fieldConfig": {"defaults": {"unit": "percentunit", "min": 0, "max": 1,
                                         "thresholds": thresholds}, "overrides": []},
            "options": {"reduceOptions": {"calcs": ["lastNotNull"]}, "showThresholdMarkers": True}}


def ts(title, targets, unit="short", desc="", threshold_line=None, stack=False, bars=False, minv=0):
    custom = {"drawStyle": "bars" if bars else "line", "lineWidth": 2,
              "fillOpacity": 60 if bars else 10, "showPoints": "never", "spanNulls": True}
    if stack:
        custom["stacking"] = {"mode": "normal"}
    defaults = {"unit": unit, "custom": custom, "min": minv}
    if threshold_line is not None:
        custom["thresholdsStyle"] = {"mode": "line"}
        defaults["thresholds"] = steps((None, "transparent"), (threshold_line, "red"))
    return {"type": "timeseries", "title": title, "description": desc,
            "datasource": targets[0]["datasource"], "targets": targets,
            "fieldConfig": {"defaults": defaults, "overrides": []},
            "options": {"legend": {"displayMode": "table", "placement": "right",
                                   "calcs": ["lastNotNull", "max"], "sortBy": "Max", "sortDesc": True},
                        "tooltip": {"mode": "multi", "sort": "desc"}}}


def bargauge(title, expr, unit, thresholds, desc="", legend="{{uri}}"):
    return {"type": "bargauge", "title": title, "description": desc, "datasource": PROM,
            "targets": [prom(expr, legend, instant=True)],
            "fieldConfig": {"defaults": {"unit": unit, "min": 0, "thresholds": thresholds,
                                         "color": {"mode": "thresholds"}}, "overrides": []},
            "options": {"orientation": "horizontal", "displayMode": "gradient", "showUnfilled": True,
                        "reduceOptions": {"calcs": ["lastNotNull"]}}}


def cw_metric(namespace, metric, dims, legend, stat_="Average", ref="A", period=""):
    return {"datasource": CW, "queryMode": "Metrics", "metricQueryType": 0, "metricEditorMode": 0,
            "namespace": namespace, "metricName": metric, "dimensions": dims, "matchExact": True,
            "statistic": stat_, "period": period, "region": "default", "legendFormat": legend,
            "refId": ref}


def logs_table(title, query, desc=""):
    return {"type": "table", "title": title, "description": desc, "datasource": CW,
            "targets": [{"datasource": CW, "queryMode": "Logs", "region": "default",
                         "logGroups": LOG_GROUP, "expression": query, "refId": "A"}],
            "fieldConfig": {"defaults": {"custom": {"cellOptions": {"type": "auto"}, "inspect": True}},
                            "overrides": [{"matcher": {"id": "byName", "options": "last_seen"},
                                           "properties": [{"id": "unit", "value": "dateTimeAsLocal"}]}]},
            "options": {"showHeader": True, "cellHeight": "sm"}}


HEALTH = [{"type": "value", "options": {"0": {"text": "다운", "color": "red"},
                                        "1": {"text": "정상", "color": "green"}}}]
CONSUMER = [{"type": "value", "options": {"0": {"text": "중단", "color": "red"},
                                          "1": {"text": "정상", "color": "green"}}}]

# ── 1. Current status ────────────────────
row("🚦 지금 상태 — 초록이면 정상, 빨강이면 확인")
(Line(4)
 .add(3, stat("앱", f'max(up{{{APP}}}) or vector(0)', mappings=HEALTH,
              thresholds=steps((None, "red"), (1, "green")), desc="Prometheus가 앱 지표를 수집할 수 있는지"))
 .add(3, stat("ERROR 로그 (1시간)", f'round(sum(increase(logback_events_total{{{APP}, level="error"}}[1h]))) or vector(0)',
              thresholds=steps((None, "green"), (1, "red")),
              desc="사람이 바로 조치해야 하는 오류. 0이 아니면 아래 📋 로그에서 내용 확인"))
 .add(3, stat("WARN 로그 (1시간)", f'round(sum(increase(logback_events_total{{{APP}, level="warn"}}[1h]))) or vector(0)',
              thresholds=steps((None, "green"), (20, "yellow"), (100, "orange")),
              desc="재시도·일시 장애 등 복구 가능한 실패. 급증할 때만 확인"))
 .add(3, stat("5xx 비율 (5분)",
              f'(sum(rate(http_server_requests_seconds_count{{{API}, status=~"5.."}}[5m])) or vector(0))'
              f' / clamp_min(sum(rate(http_server_requests_seconds_count{{{API}}}[5m])), 1e-9)',
              unit="percentunit", decimals=1,
              thresholds=steps((None, "green"), (0.01, "yellow"), (0.05, "red")),
              desc="서버 오류로 끝난 요청 비율 (SSE 스트림 제외)"))
 .add(3, stat("API 응답 P95 (5분)",
              f'histogram_quantile(0.95, sum by (le) (rate(http_server_requests_seconds_bucket{{{API}}}[5m])))',
              unit="s", thresholds=steps((None, "green"), (0.5, "yellow"), (1, "red")),
              desc="요청 95%가 이 시간 안에 끝남. 1초 넘으면 빨강"))
 .add(3, stat("이상탐지 컨슈머", f'max(anomaly_stream_subscription_active{{{APP}}}) or vector(0)',
              mappings=CONSUMER, thresholds=steps((None, "red"), (1, "green")),
              desc="이상탐지 결과를 받아 보호자 알림을 보내는 컨슈머가 동작 중인지"))
 .add(3, stat("SSE 연결 수", f'sum(sse_active_connections{{{APP}}}) or vector(0)',
              thresholds=steps((None, "blue")), desc="지금 실시간 위치 화면을 보고 있는 연결 수"))
 .add(3, stat("DB 커넥션 대기", f'sum(hikaricp_connections_pending{{{APP}}}) or vector(0)',
              thresholds=steps((None, "green"), (1, "red")),
              desc="커넥션을 못 얻고 기다리는 스레드 수. 0이 아니면 풀 고갈 의심"))
 .done())
(Line(5)
 .add(6, gauge("JVM Heap 사용률",
               f'sum(jvm_memory_used_bytes{{{APP}, area="heap"}}) / sum(jvm_memory_max_bytes{{{APP}, area="heap"}})',
               steps((None, "green"), (0.7, "yellow"), (0.85, "red"))))
 .add(6, gauge("EC2 메모리 사용률",
               '1 - node_memory_MemAvailable_bytes{job="node"} / node_memory_MemTotal_bytes{job="node"}',
               steps((None, "green"), (0.8, "yellow"), (0.9, "red")),
               desc="앱·탐지 엔진·모니터링이 한 대(t3.medium)를 같이 씀"))
 .add(6, gauge("EC2 디스크 사용률",
               '1 - node_filesystem_avail_bytes{job="node", mountpoint="/"} / node_filesystem_size_bytes{job="node", mountpoint="/"}',
               steps((None, "green"), (0.75, "yellow"), (0.9, "red"))))
 .add(6, stat("앱 가동 시간", f'max(process_uptime_seconds{{{APP}}})', unit="s",
              thresholds=steps((None, "orange"), (600, "green")),
              desc="재시작(배포·크래시) 후 경과 시간. 배포하지 않았는데 짧으면 재시작 의심"))
 .done())

# ── 2. API ────────────────────
row("🌐 API")
(Line(8)
 .add(12, ts("초당 요청 수 (API별)",
             [prom(f'sum by (method, uri) (rate(http_server_requests_seconds_count{{{API}}}[1m]))', "{{method}} {{uri}}")],
             unit="reqps"))
 .add(12, ts("에러 응답 (API·상태코드별, 분당)",
             [prom(f'sum by (status, uri) (rate(http_server_requests_seconds_count{{{API}, status=~"4..|5.."}}[5m])) * 60',
                   "{{status}} {{uri}}")],
             desc="4xx는 대부분 클라이언트 원인(401 만료 토큰 등). 5xx가 보이면 확인"))
 .done())
(Line(8)
 .add(12, ts("응답시간 P95 (API별)",
             [prom(f'histogram_quantile(0.95, sum by (uri, le) (rate(http_server_requests_seconds_bucket{{{API}}}[5m])))', "{{uri}}")],
             unit="s", threshold_line=1, desc="빨간 선 = 1초 기준"))
 .add(12, bargauge("느린 API 순위 (최근 1시간 P95)",
                   f'sort_desc(histogram_quantile(0.95, sum by (uri, le) (rate(http_server_requests_seconds_bucket{{{API}}}[1h]))))',
                   "s", steps((None, "green"), (0.5, "yellow"), (1, "red"))))
 .done())

# ── 3. GPS ingest ────────────────────
row("📍 GPS 수신")
GPS = APP + ', uri="/api/v1/location/gps"'
(Line(7)
 .add(8, stat("GPS 수신 (분당)", f'sum(rate(http_server_requests_seconds_count{{{GPS}}}[5m])) * 60 or vector(0)',
              decimals=1, thresholds=steps((None, "blue")), desc="보호 대상자 앱이 보내는 위치 수신 건수"))
 .add(16, ts("GPS 수신 추이 (상태코드별, 분당)",
             [prom(f'sum by (status) (rate(http_server_requests_seconds_count{{{GPS}}}[5m])) * 60', "{{status}}")],
             stack=True))
 .done())

# ── 4. External APIs ────────────────────
row("🔌 외부 API (Kakao·Slack·SMS)")
(Line(8)
 .add(8, ts("호출 결과 (분당)",
            [prom('sum by (client_name, outcome) (rate(http_client_requests_seconds_count[5m])) * 60',
                  "{{client_name}} {{outcome}}"),
             prom('sum by (outcome) (rate(sms_send_seconds_count[5m])) * 60', "CoolSMS {{outcome}}", ref="B")],
            desc="첫 호출 전에는 지표가 없어 No data로 보임"))
 .add(8, ts("실패율 (15분)",
            [prom('sum by (client_name) (rate(http_client_requests_seconds_count{outcome!="SUCCESS"}[15m]))'
                  ' / sum by (client_name) (rate(http_client_requests_seconds_count[15m]))', "{{client_name}}"),
             prom('sum(rate(sms_send_seconds_count{outcome="FAILURE"}[15m])) / sum(rate(sms_send_seconds_count[15m]))',
                  "CoolSMS", ref="B")],
            unit="percentunit", threshold_line=0.1, desc="빨간 선 = 10%"))
 .add(8, ts("응답시간 P95",
            [prom('histogram_quantile(0.95, sum by (client_name, le) (rate(http_client_requests_seconds_bucket[5m])))',
                  "{{client_name}}"),
             prom('histogram_quantile(0.95, sum by (le) (rate(sms_send_seconds_bucket[5m])))', "CoolSMS", ref="B")],
            unit="s", threshold_line=2))
 .done())

# ── 5. Anomaly detection and SSE ────────────────────
row("🚨 이상탐지 · 실시간 위치(SSE)")
(Line(4)
 .add(6, stat("미배달 백로그", f'max(anomaly_stream_lag{{{APP}}})',
              thresholds=steps((None, "green"), (10, "yellow"), (100, "red")),
              desc="엔진이 발행했지만 아직 앱이 가져가지 않은 결과 수"))
 .add(6, stat("처리 중(미확인) 메시지", f'max(anomaly_stream_pending{{{APP}}})',
              thresholds=steps((None, "green"), (5, "yellow"), (50, "red")),
              desc="가져갔지만 ACK 못 한 메시지. 계속 쌓이면 처리 실패 반복"))
 .add(6, stat("배달 지연", f'max(anomaly_stream_delivery_delay_seconds{{{APP}}})', unit="s",
              thresholds=steps((None, "green"), (30, "yellow"), (120, "red"))))
 .add(6, stat("폴링 연속 실패", f'max(anomaly_stream_poll_consecutive_errors{{{APP}}})',
              thresholds=steps((None, "green"), (1, "yellow"), (5, "red"))))
 .done())
(Line(7)
 .add(8, ts("이상탐지 스트림 추이",
            [prom(f'max(anomaly_stream_lag{{{APP}}})', "백로그"),
             prom(f'max(anomaly_stream_pending{{{APP}}})', "미확인", ref="B")]))
 .add(8, ts("SSE 연결 수",
            [prom(f'sum(sse_active_connections{{{APP}}})', "연결")]))
 .add(8, ts("SSE 종료·전송 실패 (분당)",
            [prom(f'sum by (reason) (rate(sse_emitter_removed_total{{{APP}}}[5m])) * 60', "종료: {{reason}}"),
             prom(f'sum(rate(sse_emitter_send_failures_total{{{APP}}}[5m])) * 60', "전송 실패", ref="B")]))
 .done())

# ── 5-1. Nginx ────────────────────
NGINX_GROUP = [{"name": "/ecs/nginx"}]
ACCESS = 'parse @message /\\] "(?<method>[A-Z]+) (?<path>[^ ?"]+)[^"]*" (?<status>\\d{3}) (?<bytes>\\d+)/ | filter ispresent(status)'


def nginx_logs(panel_type, title, query, desc="", unit="short"):
    p = logs_table(title, query, desc)
    p["type"] = panel_type
    p["targets"][0]["logGroups"] = NGINX_GROUP
    if panel_type == "timeseries":
        p["fieldConfig"] = {"defaults": {"unit": unit, "min": 0,
                                         "custom": {"drawStyle": "bars", "fillOpacity": 60, "lineWidth": 1,
                                                    "stacking": {"mode": "normal"}}},
                            "overrides": []}
        p["options"] = {"legend": {"displayMode": "list", "placement": "bottom"},
                        "tooltip": {"mode": "multi"}}
    return p


row("🚪 Nginx (외부 진입점)")
(Line(8)
 .add(12, nginx_logs("timeseries", "Nginx 오류 응답 (5분 단위)",
                     f'{ACCESS} | filter status >= 499 | stats count(*) as count by bin(5m), status',
                     desc="499 = 응답 전에 앱이 요청을 끊음, 502/504 = 앱(upstream)에 연결 실패·시간 초과"))
 .add(12, nginx_logs("table", "Nginx 오류 로그 (upstream 연결 실패 등)",
                     'filter @message like /\\[(error|crit|alert|emerg)\\]/ '
                     '| parse @message /\\[(?<severity>[a-z]+)\\] [^:]*: \\*\\d+ (?<reason>[^,]+), client: [^,]+, server: [^,]+, request: "(?<request>[^"]+)"/ '
                     '| sort @timestamp desc | limit 100 | display @timestamp, severity, reason, request',
                     desc="배포 중 새 컨테이너가 뜨기 전 connect() failed가 몇 건 찍히는 것은 알려진 현상"))
 .done())
(Line(8)
 .add(12, nginx_logs("table", "응답 크기 큰 API (평균, 바이트)",
                     f'{ACCESS} | filter status = 200 and path like /^\\/api\\// '
                     '| stats count(*) as count, avg(bytes) as avg_bytes, max(bytes) as max_bytes by method, path '
                     '| sort avg_bytes desc | limit 15',
                     desc="gzip 적용 후에는 압축된 전송 크기. path의 UUID는 그대로 보임"))
 .add(12, nginx_logs("table", "Nginx 상태코드 분포",
                     f'{ACCESS} | filter path like /^\\/api\\// | stats count(*) as count by status | sort count desc'))
 .done())

# ── 5-2. DB slow queries ────────────────────
row("🗄 DB 슬로우 쿼리 (500ms 이상)")
(Line(9)
 .add(24, logs_table("슬로우 쿼리 집계",
                     f'{APP_LOGS} and `log.logger` = "org.hibernate.SQL_SLOW" '
                     '| parse message /took (?<ms>\\d+) milliseconds \\[(?<sql>.*)\\]/ '
                     '| stats count(*) as count, max(ms) as max_ms, avg(ms) as avg_ms, latest(@timestamp) as last_seen by sql '
                     '| sort max_ms desc | limit 30',
                     desc="앱(Hibernate)이 실행한 쿼리만 집계. 이상탐지 엔진이 직접 읽는 쿼리는 포함되지 않음. SQL은 ? 자리표시자로만 기록"))
 .done())

# ── 6. Logs ────────────────────
row("📋 로그 (CloudWatch)")
(Line(7)
 .add(24, ts("ERROR · WARN 발생 추이 (5분 단위)",
             [prom(f'sum by (level) (increase(logback_events_total{{{APP}, level=~"error|warn"}}[5m]))', "{{level}}")],
             bars=True, stack=True))
 .done())
CATEGORY = "parse message /^(?<category>\\[[^\\]]+\\])/"
(Line(8)
 .add(12, logs_table("ERROR 종류별 집계",
                     f'{APP_LOGS} and `log.level` = "ERROR" | {CATEGORY} '
                     '| fields coalesce(category, substr(message, 0, 80)) as kind '
                     '| stats count(*) as count, latest(@timestamp) as last_seen by kind, `log.logger` '
                     '| sort count desc | limit 20',
                     desc="같은 [카테고리 : 상세] 메시지끼리 묶은 건수. 대시보드 시간 범위 기준"))
 .add(12, logs_table("WARN 종류별 집계",
                     f'{APP_LOGS} and `log.level` = "WARN" | {CATEGORY} '
                     '| fields coalesce(category, substr(message, 0, 80)) as kind '
                     '| stats count(*) as count, latest(@timestamp) as last_seen by kind, `log.logger` '
                     '| sort count desc | limit 20'))
 .done())
(Line(10)
 .add(24, logs_table("최근 ERROR · WARN",
                     f'{APP_LOGS} and `log.level` in ["ERROR", "WARN"] '
                     '| fields @timestamp, `log.level` as level, message, requestId, `error.type` as error_type, '
                     '`error.message` as error_message, `process.thread.name` as thread '
                     '| sort @timestamp desc | limit 100',
                     desc="requestId를 복사해 위쪽 'requestId' 입력칸에 넣으면 그 요청의 전체 로그를 볼 수 있음"))
 .done())
(Line(10)
 .add(24, logs_table("요청 추적 (상단 requestId 입력)",
                     f'{APP_LOGS} and requestId = "${{requestId}}" '
                     '| fields @timestamp, `log.level` as level, message, `process.thread.name` as thread, '
                     '`error.type` as error_type, `error.stack_trace` as stack_trace '
                     '| sort @timestamp asc | limit 200',
                     desc="앱 응답 헤더 X-Request-Id 값으로 한 요청의 로그(비동기 스레드 포함)를 시간순으로 표시"))
 .done())

# ── 7. Infrastructure details (collapsed) ────────────────────
infra = row("⚙️ 인프라 상세 — JVM · DB 커넥션 · ECS · ElastiCache", collapsed=True)
ECS = {"ClusterName": ["recaring-cluster"]}
REDIS = {"CacheClusterId": ["recaring-redis-001", "recaring-redis-002", "recaring-redis-003"]}
saved_y = _y[0]
(Line(7, infra)
 .add(8, ts("JVM Heap", [prom(f'sum(jvm_memory_used_bytes{{{APP}, area="heap"}})', "사용"),
                         prom(f'sum(jvm_memory_max_bytes{{{APP}, area="heap"}})', "최대", ref="B")], unit="bytes"))
 .add(8, ts("GC 일시정지 (초당 누적)", [prom(f'sum by (gc) (rate(jvm_gc_pause_seconds_sum{{{APP}}}[5m]))', "{{gc}}")], unit="s"))
 .add(8, ts("CPU", [prom(f'process_cpu_usage{{{APP}}}', "앱 프로세스"),
                    prom(f'system_cpu_usage{{{APP}}}', "호스트 전체", ref="B")], unit="percentunit"))
 .done())
(Line(7, infra)
 .add(8, ts("DB 커넥션 풀", [prom(f'sum(hikaricp_connections_active{{{APP}}})', "사용 중"),
                          prom(f'sum(hikaricp_connections_idle{{{APP}}})', "유휴", ref="B"),
                          prom(f'sum(hikaricp_connections_pending{{{APP}}})', "대기", ref="C"),
                          prom(f'sum(hikaricp_connections_max{{{APP}}})', "최대", ref="D")]))
 .add(8, ts("DB 커넥션 획득 시간 (평균)",
            [prom(f'sum(rate(hikaricp_connections_acquire_seconds_sum{{{APP}}}[5m])) / sum(rate(hikaricp_connections_acquire_seconds_count{{{APP}}}[5m]))', "평균")],
            unit="s"))
 .add(8, ts("DB 커넥션 타임아웃 (5분)",
            [prom(f'sum(increase(hikaricp_connections_timeout_total{{{APP}}}[5m]))', "타임아웃")], bars=True))
 .done())
(Line(7, infra)
 .add(8, ts("ECS CPU (%)", [cw_metric("AWS/ECS", "CPUUtilization", {**ECS, "ServiceName": [s]}, s, ref=r)
                            for s, r in [("recaring-service", "A"), ("recaring-detection-service", "B"),
                                         ("recaring-monitoring-service", "C"), ("nginx-service", "D")]],
            unit="percent"))
 .add(8, ts("ECS 메모리 (%)", [cw_metric("AWS/ECS", "MemoryUtilization", {**ECS, "ServiceName": [s]}, s, ref=r)
                             for s, r in [("recaring-service", "A"), ("recaring-detection-service", "B"),
                                          ("recaring-monitoring-service", "C"), ("nginx-service", "D")]],
            unit="percent"))
 .add(8, ts("ElastiCache 메모리 · 연결",
            [cw_metric("AWS/ElastiCache", "DatabaseMemoryUsagePercentage", REDIS, "{{CacheClusterId}} 메모리 %"),
             cw_metric("AWS/ElastiCache", "CurrConnections", REDIS, "{{CacheClusterId}} 연결", ref="B")]))
 .done())
targets_up = stat("수집 대상 상태 (Prometheus up)", "up", mappings=HEALTH,
                  thresholds=steps((None, "red"), (1, "green")),
                  desc="recaring=앱, node=EC2 호스트, redis=redis-exporter. ECS 서비스별 실행 태스크 수는 Container Insights가 꺼져 있어 수집되지 않음")
targets_up["targets"][0]["legendFormat"] = "{{job}}"
targets_up["options"]["textMode"] = "value_and_name"
(Line(4, infra)
 .add(24, targets_up)
 .done())
_y[0] = saved_y

dashboard = {
    "uid": "recaring-obs-v1",
    "title": "re-caring 운영",
    "tags": ["recaring", "dev"],
    "timezone": "browser",
    "schemaVersion": 39,
    "version": 1,
    "refresh": "1m",
    "time": {"from": "now-6h", "to": "now"},
    "editable": True,
    "graphTooltip": 1,
    "templating": {"list": [{
        "type": "textbox", "name": "requestId", "label": "requestId", "query": "",
        "current": {"text": "", "value": ""}, "hide": 0,
        "description": "응답 헤더 X-Request-Id 값"}]},
    "annotations": {"list": [{
        "name": "앱 재시작", "enable": True, "iconColor": "orange", "datasource": PROM,
        "expr": f'changes(process_start_time_seconds{{{APP}}}[2m]) > 0', "step": "60s",
        "titleFormat": "앱 재시작(배포)"}]},
    "panels": panels,
}

json.dump(dashboard, open(sys.argv[1], "w"), ensure_ascii=False, indent=2)
print("panels:", sum(1 + len(p.get("panels", [])) for p in panels))
