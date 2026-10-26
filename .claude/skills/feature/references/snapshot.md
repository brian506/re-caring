# 프로젝트 스냅샷

> 마지막 업데이트: 2026-10-26 (패키지별 코드 대조 + AWS 인프라 조회로 전면 검증). 기능 추가·수정 시 해당 섹션을 갱신한다.

> 도메인 enum은 `vo/`에 둔다(`MemberRole`·`Gender`, `CareRole`·`CarePartyRole`, `SafeZoneType`·`SafeZoneAlertType`, `OAuthProvider`, `DetectionType`·`AnomalyAlertKind`·`AnomalyReasonCode`, `LocationCollectionInterval`, `FeedbackCategory`). 나머지 enum(`SignUpType`, `CareInvitationStatus`, `InquiryStatus/Category`, `SiteInquiryStatus/Category`, `FcmDevicePlatform`, `FeedbackAccuracy/Reason`)은 아직 `dataaccess/entity`에 남아 있다 (#284)

## 도메인별 패키지 현황

| 도메인 | 주요 Service | 주요 Implement |
|--------|-------------|---------------|
| `auth` | LocalAuthService, OAuthService, TokenRefreshService, CookieService | LocalAuthAuthenticator(미가입 번호도 더미 해시 비교), LocalAuthManager(가입·비밀번호 변경), LocalAuthReader(`EncodedPassword` 반환), TokenIssuer(`MemberInfo`로 발급), RefreshTokenReader/Writer, OAuthAuthenticator(Kakao/Naver), OAuthReader(`memberKey` 반환)/OAuthManager/OAuthLinkValidator. 로그아웃의 FCM 토큰 삭제는 `FcmDeviceTokenManager` 직접 호출 |
| `care` | CareInvitationService, CareRelationshipService | CareInvitationManager, CareInvitationReader/Writer(`CareInvitationInfo` VO 반환), CareInvitationResendLimiter, CareRelationshipManager(leaveCare/leaveAllCare)/Reader/Writer(register·별명), CareRelationshipValidator, DesignatedAvatarManager |
| `device` | DeviceTokenService | WardDeviceTokenManager(발급·재발급, 구 토큰 캐시 evict), WardDeviceTokenReader(`deviceToken:` Redis 캐시 → 없으면 DB) |
| `location` | LocationService, LocationSettingService | GpsHistoryManager, GpsLatestCacheManager/Listener, LocationSignalMonitor/AlertManager(signal), SseEmitterManager, LocationValidator, CareRelationshipCacheReader, BatteryThresholdEvaluator/BatteryDetectionListener/BatteryAlertStateManager/AnomalyAcknowledgementManager/DetectionPublisher/DetectionListener/AnomalyDetectionConsumer/AnomalyDetectionParser/AnomalyDetectionManager/AnomalyPendingReclaimer/AnomalyStreamErrorHandler/AnomalyStreamMetrics/AnomalyStreamProperties(detection), LocationSettingManager. 배터리는 고른 잔량 중 도달한 최저값을 알리고, 그 값보다 5%p 이상 회복해야 다시 알린다(충전 중 중간 임계값은 알림 없이 기준만 이동) |
| `member` | MemberService | MemberReader(`MemberInfo` VO 반환)/Writer, MembersTermsAgreementReader(`TermsAgreementInfo` 반환)/Writer, MemberWithdrawalManager |
| `notification` | NotificationService, NotificationSettingService, FcmDeviceTokenService | NotificationReader/Writer, NotificationSendManager, NotificationFeedbackManager/Validator(feedback), NotificationAcknowledgementManager(acknowledgement), NotificationSettingReader/Manager(setting — 케어 관계 검증은 care의 `CareRelationshipValidator`), FcmDeviceTokenReader/Manager(`FcmDeviceTokenInfo` VO 반환), FcmClient(Firebase/NoOp)(fcm), CareInvitationNotificationListener, BatteryThresholdNotificationListener, SafeZoneNotificationListener, AnomalyNotificationListener, LocationSignalNotificationListener, InquiryNotificationListener(listener). FCM은 500토큰 단위 발송, UNAVAILABLE·INTERNAL·DEADLINE_EXCEEDED면 1회 재시도(소진 시 `[FcmSend : RetryExhausted]` ERROR), UNREGISTERED·INVALID_ARGUMENT 토큰은 발송 직후 삭제 |
| `inquiry` | InquiryService | InquiryReader, InquiryManager(접수·답변·탈퇴 삭제, 답변 시 InquiryAnsweredEvent), InquiryOperatorNotifier(신규 접수 운영자 Slack 알림) |
| `siteinquiry` | SiteInquiryService | SiteInquiryReader, SiteInquiryManager(등록·답변·답변 삭제·삭제), SiteInquiryAuthenticator(비밀글 비밀번호 BCrypt 인코딩·검증), SiteInquiryRateLimitValidator(등록 IP 한도·비밀번호 실패 잠금), SiteInquiryOperatorNotifier(신규 접수 Slack). 소개 웹사이트 비회원 문의 게시판 (#271). 길이 VO는 `inquiry.vo`의 InquiryTitle/Content/Answer 재사용 |
| `place` | PlaceService | KakaoPlaceSearchClient (카카오 로컬 키워드 검색 프록시, 엔티티 없음). VO `PlaceKeyword`(검색어·일치 판정)·`PlaceSearchCondition`(편향 조건) |
| `safezone` | SafeZoneService | SafeZoneReader, SafeZoneWriter, SafeZoneStateManager, VO `SafeZoneRadius`(100~2000)·`SafeZoneCreation/Update`, SafeZoneDetectionListener(`GpsSavedEvent` 구독 → `SafeZoneEntered/ExitedEvent` 발행). 안심존 상태(`SafeZoneState`)·이벤트·`SafeZoneObservation/Transition` VO도 이 도메인 소유 (#284에서 location에서 이동) |
| `sms` | PhoneVerificationService | SmsClient, SmsCodeGenerator, PhoneVerificationReader/Writer, SmsRateLimitValidator |

> 공통 필터: `common/filter/RequestIdFilter` — 최우선 순위 서블릿 필터. 요청의 `X-Request-Id`(영숫자·하이픈 8~64자)를 재사용하거나 UUID를 생성해 MDC `requestId`에 넣고 응답 헤더로 반환. `AsyncConfig`의 executor 3개는 모두 가상 스레드 `SimpleAsyncTaskExecutor`(종료 대기 20초)이고 `MdcTaskDecorator`로 MDC를 복사해 비동기 스레드에도 `requestId`가 남는다. broadcast = 배터리·안심존·케어 초대 리스너와 `InquiryOperatorNotifier`, detection-retry = 이상탐지 컨슈머 재시도·`AnomalyPendingReclaimer`, sse-poll = `SseEmitterManager`. 로그 출력: dev는 ECS JSON(`requestId` 필드), local은 평문 `[requestId]` (#259)

> 스케줄 작업(`SchedulingConfig`): `LocationSignalMonitor`(10분)·`AnomalyStreamMetrics`(30초, 초기 10초)·`AnomalyPendingReclaimer`(1시간, detection-retry executor). 분산 락이 없어 태스크가 여러 개면 각자 실행된다

> 보안 라우팅(`SecurityConfig`): 필터 순서 AuthExceptionTranslation → DeviceTokenAuthFilter → JwtAuthenticationFilter → (UsernamePassword) → AdminApiKeyFilter. Device Token 인증 경로는 `POST /location/gps`·`GET /location/settings/collection-interval/me` 두 곳이고 JWT 필터는 이 두 경로와 `/api/v1/admin/**`를 건너뛴다. permitAll: 인증(가입·로그인·갱신·로그아웃·SMS·비밀번호 재설정), 웹 문의 등록·목록·상세, Swagger, `/actuator/**`(nginx가 외부 `/actuator`를 403으로 막음), `/error`, ASYNC·ERROR 디스패치(SSE). `/api/v1/admin/**`는 `ROLE_ADMIN`(`X-Admin-Key`). 나머지는 인증 필요, 역할 제한은 API 표에 표기

> CORS: 기본 `/**`는 `client.url`만 허용(credentials true). `/api/v1/site/**`·`/api/v1/admin/inquiries/**`·`/api/v1/admin/site-inquiries/**`는 `client.site-url`을 추가로 허용(credentials false)

> 예외 응답(`ApiControllerAdvice`): AppException → ErrorType 상태, 낙관적 락·DB 제약 위반 409, 요청 검증·바인딩·본문 해석 실패 400, SSE 비동기 요청 종료는 debug 로그만, 클라이언트 연결 끊김은 `ResponseWrite` warn, 그 외 `UnhandledException` 500. 캐시는 `careRelationship` 하나(TTL 10분, JSON 직렬화). `/actuator` 요청은 `http.server.requests` 지표에서 제외

> 로그 필드(dev ECS JSON): `config/logging/LogFieldsJsonMembersCustomizer`(`logging.structured.json.customizer`)가 `domain`(앱 로거는 `com.recaring.` 다음 패키지, 외부는 라이브러리명·`springframework`→`spring`), `category`·`detail`(메시지 선두 `[카테고리 : 상세]`)을 추가. `service.version`은 deploy.yml이 `LOGGING_STRUCTURED_ECS_SERVICE_VERSION=SHORT_SHA`로 주입 (#281)

> AppException 로그: 두 advice 모두 `common/controller/AppExceptionLogger`로 찍는다. SLF4J key-value로 `errorCode`·`errorType`·`httpStatus`·`domain`을 별도 필드로 남기고(ECS 포맷이 key-value를 최상위 필드로 출력), `domain`은 스택에서 `common`·`support`·`config`를 제외한 첫 `com.recaring.` 프레임의 패키지다. key-value로 `domain`이 있으면 customizer는 로거 기반 `domain`을 생략해 키 중복을 막는다. ERROR 레벨만 예외 객체를 넘겨 원인 체인을 남기고 WARN/INFO는 스택 없이 남긴다 (#293)

> 외부 API 로그 마스킹: 외부 응답 body·SDK 예외 메시지는 `MaskingUtils.maskExternalText`로 남긴다 (32자 이상 토큰형 문자열 `[REDACTED]`, 휴대폰 번호 가운데 마스킹, 300자 절단). 적용: 카카오 장소 검색·앱/웹 문의 Slack 응답 body, FCM 전송 실패 메시지. CoolSMS 실패는 요청에 인증번호가 담기므로 예외 메시지 없이 타입만 남긴다 (#293)

> 외부 API 지표: 카카오·Slack·OAuth RestClient는 `ObservationRegistry`를 연결해 `http_client_requests`(client.name·status·outcome)를 남긴다. `uri(String)`으로 호출하면 경로 전체가 uri 태그에 들어가므로 비밀값이 담긴 URL(Slack 웹훅)은 `URI` 객체로 호출한다. CoolSMS는 `sms.send` Timer(outcome·exception). FCM은 Firebase 콘솔 지표를 쓴다 (#259)

> 활성 회원(DAU/MAU): 별도 수집 없이 `refresh_token.created_at`으로 SQL 집계한다. 로그인·토큰 갱신마다 새 행이 저장되므로 `count(DISTINCT member_key) WHERE created_at >= current_date`(DAU)·`>= now() - 30 days`(MAU). 근사치이며 과거 일자는 재계산 불가. Grafana Postgres 데이터소스는 리포지토리 프로비저닝에 없다(콘솔에서 수동 추가한 경우에만 조회 가능) (#259)

> 로그인·연동·갱신 응답은 `{ accessToken }`이고 Refresh Token은 `Set-Cookie: refresh_token`(HttpOnly, Secure, SameSite=Lax, Path=`/api/v1/auth`). 회원가입은 200

## API 엔드포인트

| 도메인 | Method | Path | 설명 |
|--------|--------|------|------|
| Auth | POST | `/api/v1/auth/sign-up` | 로컬 회원가입. 이메일 없음 — SMS 인증된 전화번호가 로그인 ID. 전화번호 중복 E3006, 선검사를 통과한 동시 요청의 UNIQUE 위반 E409 — 모두 409. `verificationToken`은 1회용(성공·실패 무관하게 소비) |
| Auth | POST | `/api/v1/auth/sign-in/local` | 로컬 로그인. Request `{ phone, password }` (이메일 로그인 제거). 미가입 번호·비밀번호 불일치 모두 E2022(400) 하나로 응답해 가입 여부를 숨기고, 미가입 번호도 더미 해시로 비교 1회를 수행해 응답 시간을 맞춘다 |
| Auth | POST | `/api/v1/auth/sign-in/{kakao\|naver}` | OAuth 로그인 (미연동 계정은 OAUTH_NOT_LINKED) |
| Auth | POST | `/api/v1/auth/oauth/link/{kakao\|naver}` | OAuth 사후 연동 (JWT 인증). 같은 provider 재연동 E2019, 다른 회원이 이미 연동한 계정은 UNIQUE 위반 E409. local_auth 존재 검사는 없다(현재 가입 경로가 로컬뿐이라 실질 차이 없음) |
| Auth | POST | `/api/v1/auth/refresh` | 토큰 갱신. 쿠키 `refresh_token`을 읽어 기존 행을 지우고 새 쌍 발급(rotation). 쿠키 없음 E401, DB에 없음 E2021 |
| Auth | POST | `/api/v1/auth/sign-out` | 로그아웃 (permitAll). 쿠키의 Refresh Token 삭제 + 쿠키 만료. 바디 `{ fcmToken? }`(≤512)이 있으면 그 FCM 토큰도 삭제. 쿠키 없음 E401 |
| Auth | POST | `/api/v1/auth/password` | [비로그인] 비밀번호 재설정 `{ smsToken, password }`. `smsToken` = `/phone/verify`의 `verificationToken`, 1회용(토큰 소비 이후 단계의 실패와 무관하게 소비. 요청 형식 검증 400이면 미소비). 미가입 번호면 토큰 소비 후 실패. 기존 Refresh Token은 폐기하지 않는다 |
| Care | POST | `/api/v1/care/requests/ward` | 보호대상자 추가 요청 (GUARDIAN). 이미 보호자가 있는 대상자에게도 가능, 수락 시 GUARDIAN. 같은 (요청자, 대상자) 대기 요청이 있으면 E5009 |
| Care | POST | `/api/v1/care/requests/guardian` | 보호자 추가 요청 (그 대상자의 보호자 누구나, 아니면 E5011). 같은 (받는 회원, 대상자) 대기 요청이 있으면 E5009 |
| Care | GET | `/api/v1/care/requests/received` | 받은 대기(PENDING) 요청 목록, 최신순 |
| Care | GET | `/api/v1/care/requests/sent` | 보낸 케어 요청 목록 (GUARDIAN). `?status=` 기본 PENDING. `phoneNumber`는 받는 회원의 현재 번호, 대상자 추가 요청은 수락 후에만 `wardKey`·`wardName` 공개 |
| Care | POST | `/api/v1/care/requests/{key}/resend` | 보낸 대기 요청 푸시 재전송 (GUARDIAN). 요청당 5분 간격, 위반 시 429 E5019 + `error.data.seconds` |
| Care | DELETE | `/api/v1/care/requests/{key}` | 보낸 대기 요청 취소 (GUARDIAN, hard-delete) |
| Care | PATCH | `/api/v1/care/requests/{key}/accept` | 케어 요청 수락 |
| Care | PATCH | `/api/v1/care/requests/{key}/reject` | 케어 요청 거절 |
| Care | GET | `/api/v1/care/wards` | 내 보호대상자 목록. `wardJoinedAt`(대상자 가입 시각 = members.created_at, 위치기록 달력의 하한. 연동일이 아님 — 보호자는 연동 이전 기록도 조회 가능) |
| Care | GET | `/api/v1/care/wards/{wardKey}/caregivers` | 보호자 목록 |
| Care | DELETE | `/api/v1/care/wards/{wardKey}` | 보호 대상자 케어 관계 삭제 (GUARDIAN, 본인 이탈. 내가 보낸 해당 대상자의 대기 초대도 삭제). 대상자 본인은 관계를 끊을 수 없다(탈퇴만 가능) |
| Care | PATCH | `/api/v1/care/wards/{wardKey}/nickname` | 보호 대상자 별명 수정 (GUARDIAN, 보호자별로 따로 보임. 빈 값이면 해제, 최대 20자) |
| Care | PATCH | `/api/v1/care/wards/{wardKey}/avatar` | 내가 보는 보호 대상자 아바타 지정 (케어 관계가 있는 회원 전원, 호출자에게만 보임. 빈 값이면 해제, 허용 코드 외 E3008) |
| Care | PATCH | `/api/v1/care/wards/{wardKey}/caregivers/{caregiverKey}/avatar` | 내가 보는 보호자 아바타 지정 (대상자 본인 또는 그 대상자의 보호자, 호출자에게만 보임. 빈 값이면 해제) |
| Device | POST | `/api/v1/device/token` | Device Token 발급 (WARD, JWT 인증) |
| Location | POST | `/api/v1/location/gps` | GPS 좌표 전송 (WARD, Device Token 인증). `{ latitude, longitude(필수), accuracy?, battery?(0~100), speed?(m/s), measuredAt?(ISO-8601 오프셋 포함 → KST 저장) }`. `recordedAt`은 서버 수신 시각 |
| Location | GET | `/api/v1/location/stream/{wardKey}` | SSE 실시간 위치 스트림 (역할 matcher 없음, 케어 관계 필수 — 없으면 E5011). 요청 보호자 본인의 수신 주기로 솎아서 전송. `gps:display`만 읽어 부정확(accuracy > 100m)·튀는 좌표(직전 표시 좌표 대비 50m/s 초과)는 보내지 않는다(#243). 속도는 이동 거리에서 양쪽 accuracy를 빼고 `measuredAt`(없으면 `recordedAt`) 차이로 재며, 시간이 역행하면 제외. 연결마다 가상 스레드 폴링 루프(10초, 빈 주석 heartbeat), 타임아웃 5분. 첫 전송은 `gps:display`가 비면 DB 최근 20건 중 마지막 표시 가능 좌표로 대체. 지표 `sse_active_connections`·`sse_emitter_removed{reason}`·`sse_emitter_send_failures` |
| Location | GET | `/api/v1/location/history/{wardKey}` | 날짜별 이동 경로 히스토리 (WARD 본인 또는 케어 관계 보호자). 시간순으로 훑으며 부정확·튀는 좌표를 제외해 응답(#243, `Gps.isDisplayableAfter`). 앞(마지막 표시 좌표)·뒤(다음 좌표)와 대조해 혼자 튀었다 돌아온 좌표도 제외(#266, `Gps.isSpikeBetween` — 양쪽 200m 이상·앞뒤 100m 이내·왕복 속도 3m/s 초과). 마지막 좌표는 다음 좌표가 없어 이 판정을 받지 않는다. DB 원본은 그대로 |
| Location | GET | `/api/v1/location/anomaly-events/{wardKey}/route?eventId=` | 이상탐지 사건 경로 (#252, GUARDIAN — 케어 관계 없으면 E5011). 엔진이 경로 좌표를 보내지 않으므로 `anomaly_detections`에서 같은 `eventId` 행의 `min(event_started_at)`~EVENT_END 행의 `recorded_at`(엔진 `detected_at`)을 구간으로 잡아 `gps_histories`를 조회한다(날짜 경계 무관, 날짜별 이력과 같은 튐 좌표 필터). 종료 행(EVENT_END)이 없으면 진행 중으로 보고 그 사건의 마지막 알림 `recorded_at`까지(#257) — 앱이 이상 구간만 색칠하므로 마지막 알림 이후는 넣지 않는다. 응답 `{ eventId, startedAt, endedAt(진행 중이면 null), ongoing, route: [{latitude, longitude, recordedAt}] }` — 시각은 KST를 Instant(UTC `Z`)로 바꿔 응답. 사건 없음 E6003(404) |
| Location | GET | `/api/v1/location/settings/{wardKey}/collection-interval` | 요청 보호자 본인의 위치 수신 주기 조회 (GUARDIAN, 옵션 30/60/180초 포함, 미설정 시 30) |
| Location | PATCH | `/api/v1/location/settings/{wardKey}/collection-interval` | 요청 보호자 본인의 위치 수신 주기 수정 (GUARDIAN only, 다른 보호자에 영향 없음) |
| Location | GET | `/api/v1/location/settings/collection-interval/me` | 기기 수집 주기 조회 (WARD, Device Token 인증). 보호자 계열 설정 중 최솟값(미설정 보호자는 30) |
| Notification | GET | `/api/v1/notifications?cursor&size` | 내 알림함 목록 조회 (WARD, GUARDIAN — recipient 기준). notification_id DESC 커서 페이징. `cursor`=직전 응답 `nextCursor`(첫 페이지는 생략), `size`=1~50(기본 10). 응답 `{ items, nextCursor, hasNext }`. 이상탐지 알림의 `dataPayload`는 `type/wardKey/score/recordedAt/latitude/longitude` + 사건 형식 메시지면 `eventId/alertKind/reasonCode`(#248 — 앱은 `eventId`로 한 사건의 알림을 묶는다), 안심존·위험존 진입·이탈 알림은 `type/wardKey/safeZoneKey/recordedAt/latitude/longitude`(type: SAFE_ZONE_ENTERED/EXITED, DANGER_ZONE_ENTERED/EXITED), 위치 수집 중단 알림(`LOCATION_SIGNAL_LOST`)은 `type/wardKey/lastReceivedAt/recordedAt/latitude/longitude`(마지막 위치, 값 전부 문자열) — 앱은 이걸로 위치 이력 탭에 핀을 찍는다. 이 알림은 FCM을 **data 전용**으로 보낸다(#246, notification 블록 없음, Android priority HIGH, data에 `title`/`body` 추가) — 백그라운드에서도 앱이 직접 알림을 그려 전화 버튼을 붙이기 위함. iOS 앱은 아직 없어 APNs는 고려하지 않았다. 그 외 알림 유형에는 좌표가 없다(#231 이전 안심존 알림도 없음): 배터리 `DEVICE_BATTERY_THRESHOLD`는 `type/wardKey/thresholdPercent`, 케어 초대 `CARE_INVITATION_SENT/ACCEPTED`는 `type/requestKey/role`, 문의 답변 `INQUIRY_ANSWERED`는 `type/inquiryKey`. 보호자 대상 알림은 `recipient_type=GUARDIAN`으로 등록된 토큰에만, 케어 초대·문의 답변은 회원의 모든 토큰에 보낸다. 각 item에 `feedbackEligible`(이상탐지 5종·안심존/위험존 진입/이탈이면 true)·`feedbackSubmitted`가 붙는다 — 평가 UI는 `eligible && !submitted`일 때만 노출 |
| Notification | POST | `/api/v1/notifications/{notificationKey}/feedback` | 이상탐지·안심존 알림 정확도 피드백 제출 (WARD, GUARDIAN — 수신자 본인만). `{ accuracy: ACCURATE\|INACCURATE\|UNSURE, reason?, comment? }`. `reason`(USUAL_ROUTINE/BRIEF_STOPOVER/GPS_INACCURATE/OTHER)은 `INACCURATE`일 때만 필수이자 허용(위반 E9008), 안심존·위험존 알림은 GPS_INACCURATE/OTHER만 허용(그 외 E9010), `comment`는 선택·최대 100자. 알림 1건당 1회(E9007). 대상 아님 E9006, 타인 알림 E9005, 없음 E9004, 연결된 탐지 기록 소실 E9009 |
| Notification | POST | `/api/v1/notifications/anomaly-events/acknowledge` | 이상탐지 사건 보호자 확인 (#248). `{ wardKey, eventId }` — 푸시 data·알림 목록 dataPayload의 값을 그대로 보낸다(FCM은 수신자별 행을 만들기 전 한 번에 발송해 `notificationKey`가 푸시에 없으므로 사건 키로 받는다). 요청자-대상자 케어 관계 필수(보호자, 없으면 E5011). 한 명이라도 확인하면 그 사건은 끝날 때까지 재알림(REMIND)이 가지 않는다. `eventId`가 현재 사건과 다르면(지난 사건) 200이지만 아무것도 바뀌지 않는다 |
| Notification | PUT | `/api/v1/notifications/device-tokens` | FCM 토큰 등록·갱신 (GUARDIAN·WARD). `{ token(≤512), careRole: GUARDIAN\|WARD, platform: ANDROID\|IOS\|WEB }` — token 기준 upsert(기존 토큰이면 회원·역할·플랫폼 재할당). 응답 `{ memberKey, careRole, platform }`. 로그아웃 시 삭제 |
| Notification | GET | `/api/v1/notifications/settings/{wardKey}` | 요청 보호자 본인의 알림 설정 조회 (안심존·위험존·이상탐지·응급호출·배터리). 케어 관계 필수(없으면 E5011) — WARD 본인 접근 불가 |
| Notification | PATCH | `/api/v1/notifications/settings/{wardKey}/safe-zone` | 안심존 진입·이탈 알림 토글 |
| Notification | PATCH | `/api/v1/notifications/settings/{wardKey}/danger-zone` | 위험존 진입·이탈 알림 토글 `{ entryEnabled, exitEnabled }` (#238) |
| Notification | PATCH | `/api/v1/notifications/settings/{wardKey}/anomaly` | 이상탐지 알림 토글 수정 (5종 각각 on/off. **민감도 제거됨**) |
| Notification | PATCH | `/api/v1/notifications/settings/{wardKey}/emergency-call` | 응급호출 알림 토글 |
| Notification | PATCH | `/api/v1/notifications/settings/{wardKey}/battery` | 배터리 알림 토글 + 알림 받을 잔량(%) 다중 선택 (10~100, 10 단위, 개수 제한 없음, 위반 E9001). 기본값 없음 — 빈 배열이면 알림 안 감. 조회 응답에 `thresholdOptions` 포함 |
| Member | GET | `/api/v1/members/me` | 내 정보 조회 (JWT 인증, Member+약관+안심존 통합). `joinedAt` = 가입 시각, 약관 3종 동의 시각(`termsServiceAgreedAt/PrivacyAgreedAt/LocationAgreedAt`), `signUpType`, `safeZones`. `subscriptionType`은 #254에서 제거(구독제 폐지) |
| Member | PATCH | `/api/v1/members/me` | 내 정보 수정 (이름·생년월일·비밀번호·profileAvatarCode 부분 수정, JWT 인증. 아바타는 빈 문자열이면 기본 코드로 재배정, 허용 코드 외 E3008) |
| Member | POST | `/api/v1/members/phones` | 연락처 기반 가입 회원 조회 (GUARDIAN) |
| Member | PATCH | `/api/v1/members/me/phone` | 전화번호(로그인 ID) 변경. Request `{ smsToken, password }` — 새 번호 SMS 인증 토큰 + 현재 비밀번호. 비밀번호 불일치 시 토큰 미소비. 새 번호 중복 E3006(409) |
| Member | DELETE | `/api/v1/members/me` | 회원 탈퇴. Request `{ password }` — 현재 비밀번호 확인 후 `member_withdrawal`에 기록하고 연관 데이터 hard-delete (아래 "회원 탈퇴 정리" 참고) |
| Inquiry | POST | `/api/v1/inquiries` | 1:1 문의 접수 (GUARDIAN·WARD). `{ category: LOCATION_SAFE_ZONE\|ALERT_EMERGENCY\|CONNECTION_ACCOUNT\|ERROR_OTHER, title, content }`. 앞뒤 공백 제거 후 title 1~50자(E11000), content 1~1000자(E11001). 201 + InquiryResponse. 연속 접수 제한 없음. 접수 후 운영자 Slack 채널에 비동기 알림(#264, `InquiryOperatorNotifier` — 분류·제목·접수 시각·문의 키만, 본문 제외. 웹훅은 `slack.inquiry-webhook-url`=SSM `/recaring/SLACK_WEBHOOK_SECRET`, 미설정이면 생략. Slack 실패는 로그만 남기고 접수는 성공) |
| Inquiry | GET | `/api/v1/inquiries/me` | 내 문의 전체 최신순 (페이징 없음). `inquiryKey, category, title, content, status(WAITING/ANSWERED), createdAt, answer, answeredAt` |
| Admin | GET | `/api/v1/admin/inquiries?status=` | [운영] 문의 목록 (`X-Admin-Key` = `ADMIN_API_KEY`). status를 지정하면 그 상태만 오래된 순, 생략하면 전체 최신순. 응답에 memberKey 포함 |
| Admin | PATCH | `/api/v1/admin/inquiries/{inquiryKey}/answer` | [운영] 답변 등록·덮어쓰기 `{ answer }` (1~2000자, E11002, 없음 E11003). ANSWERED + answeredAt 갱신, 커밋 후 작성자에게 `INQUIRY_ANSWERED` 푸시+알림함(`dataPayload: type/inquiryKey`). 재등록 시 푸시 재발송 |
| SiteInquiry | POST | `/api/v1/site/inquiries` | [비로그인] 웹사이트 문의 등록 `{ category: FEATURE\|USAGE\|ACCOUNT_DELETION\|OTHER, title, content, email, phone?, isSecret, password? }`. title 1~50(E11000), content 1~1000(E11001), 비밀글 password 숫자 4~6자리(E11004, BCrypt 저장), email ≤100, phone 숫자·하이픈 ≤13자(빈 값은 null). X-Real-IP당 10분 5건(E11007, 429 + Retry-After). 201 + 요약. 접수 시 Slack 알림(앱 문의와 같은 웹훅, 비밀글이면 "(비밀글)" 표기) |
| SiteInquiry | GET | `/api/v1/site/inquiries` | [비로그인] 전체 최신순 (페이징 없음). `inquiryKey, category, title, status, isSecret, createdAt`만 — 내용·연락처 제외 |
| SiteInquiry | POST | `/api/v1/site/inquiries/{inquiryKey}/view` | [비로그인] 상세 `{ password? }`. 비밀글이면 불일치 E11005(403), 문의당 10분 5회 실패 시 잠금 E11008(429). 내용·답변 포함, 연락처 제외. 없음 E11006 |
| Admin | GET | `/api/v1/admin/site-inquiries?status=` | [운영] 웹사이트 문의 목록 (X-Admin-Key). email·phone 포함. status를 지정하면 그 상태만 오래된 순, 생략하면 전체 최신순 |
| Admin | PATCH | `/api/v1/admin/site-inquiries/{inquiryKey}/answer` | [운영] 답변 등록·덮어쓰기 `{ answer }` (1~2000자, E11002). ANSWERED |
| Admin | DELETE | `/api/v1/admin/site-inquiries/{inquiryKey}/answer` | [운영] 답변 삭제 → WAITING |
| Admin | DELETE | `/api/v1/admin/site-inquiries/{inquiryKey}` | [운영] 문의 hard-delete |
| Place | GET | `/api/v1/places/search?query=&latitude=&longitude=&radiusMeters=` | 장소 검색 (GUARDIAN·WARD, 카카오 로컬 키워드 검색 프록시, 최대 5건, 편향 결과가 3건 미만이거나 키워드 불일치면 전국 재검색 후 전국 결과를 앞에 두고 placeId로 병합, 결과 없음은 빈 배열 200). 파라미터는 모두 선택: query는 trim 후 1~100자(E10000), radiusMeters는 0~20000으로 클램프(미지정 시 20000), 위경도 둘 다 유효할 때만 편향. 키워드 일치 = 공백으로 나눈 2자 이상 토큰 중 하나라도 장소명(공백 제거)에 포함. 카카오 장애 E10001(502), 쿼터 초과 E10002(429). 연결 타임아웃 1초·읽기 3초 |
| SafeZone | POST | `/api/v1/care/wards/{wardKey}/safe-zones` | 안심존·위험존 추가 (GUARDIAN only). `type`(SAFE/DANGER) 필수, `radiusMeters`(정수 100~2000 — #260에서 300으로 올렸다가 #266에서 진입·이탈 히스테리시스를 넣고 100으로 되돌렸다. 범위 밖 E8003). 좌표 범위(위도 ±90, 경도 ±180) 밖 E8002 |
| SafeZone | GET | `/api/v1/care/wards/{wardKey}/safe-zones` | 안심존·위험존 목록 조회 (GUARDIAN). 평면 리스트, 항목별 `type`으로 구분, `created_at` 오름차순. 응답에 `updatedAt` 없음 |
| SafeZone | GET | `/api/v1/care/wards/{wardKey}/safe-zones/{safeZoneKey}` | 안심존 상세 조회 (GUARDIAN). 없거나 다른 대상자 존이면 E8000(400) |
| SafeZone | PATCH | `/api/v1/care/wards/{wardKey}/safe-zones/{safeZoneKey}` | 안심존·위험존 수정 (GUARDIAN only). `type` 변경 가능, `radiusMeters`·좌표 규칙은 추가와 동일. 없으면 E8000(400) |
| SafeZone | DELETE | `/api/v1/care/wards/{wardKey}/safe-zones/{safeZoneKey}` | 안심존 삭제 (GUARDIAN only). 없으면 E8000(400) |
| SMS | POST | `/api/v1/auth/phone/send-code` | SMS 인증코드 발송. 번호당 10분 5회(고정 창). 초과 시 429(E4005) + `error.data.seconds`(재요청 가능까지 남은 초) |
| SMS | POST | `/api/v1/auth/phone/verify` | SMS 인증코드 검증. 응답 `{ verificationToken, registered }` — `registered`=해당 번호로 이미 가입된 회원 존재 여부. 가입 화면은 true일 때, 비밀번호 재설정 화면은 false일 때 진행을 막는다 |

## 엔티티 목록

| Entity | Table | 주요 필드 |
|--------|-------|---------|
| Member | members | memberKey(UUID), phone(11, UNIQUE, 로그인 ID), name(20), birth, gender(MALE/FEMALE), role(GUARDIAN/WARD), signUpType(LOCAL), profileAvatarCode(30, NOT NULL, 앱 번들 일러스트 16종 코드. 가입 시 역할(WARD→senior, GUARDIAN→adult)+성별+memberKey 해시로 기본 배정) |
| LocalAuth | local_auth | memberKey, password (로그인 ID는 `members.phone`) |
| OAuth | oauth | provider(KAKAO/NAVER), providerMemberId, memberKey. `(provider, provider_member_id)` UNIQUE |
| RefreshToken | refresh_token | memberKey, token(UNIQUE), created_at. 로그인·갱신마다 새 행(DAU/MAU 집계 근거). jti 없음 |
| LoginHistory | login_history | memberKey, ipAddress, userAgent, isSuccess. **미사용** — Repository·저장 코드 없음 |
| CareRelationship | care_relationships | wardMemberKey, caregiverMemberKey, careRole(GUARDIAN 단일 값 — MANAGER는 #254에서 제거), wardNickname(nullable, 최대 20자, 보호자별 별명. null이면 대상자 실명 사용). `(ward, caregiver)` UNIQUE |
| DesignatedAvatar | designated_avatars | ownerMemberKey(지정한 사람), wardMemberKey(케어 범위), targetMemberKey(얼굴이 붙는 사람), profileAvatarCode. `(owner, ward, target)` UNIQUE. 관계 삭제·탈퇴 시 함께 hard-delete |
| CareInvitation | care_invitation | requestKey(UUID, UNIQUE), requesterMemberKey, targetMemberKey(받는 회원 — 대상자 추가 요청이면 wardMemberKey와 같음), wardMemberKey, careRole(항상 GUARDIAN), status(PENDING/ACCEPTED/REJECTED), createdAt. 만료 상태·컬럼 없음 |
| GpsHistory | gps_histories | wardMemberKey, latitude, longitude, recordedAt(서버 수신 시각), accuracy, battery, speed(m/s, nullable), measuredAt(기기 측정 시각, nullable — null이면 시간 간격 신뢰 불가). 시각 컬럼은 모두 KST 저장 |
| LocationSetting | location_settings | (wardMemberKey, caregiverMemberKey) UNIQUE — 보호자별 수신 주기, collectionIntervalSeconds(30/60/180, 기본 30) |
| WardDeviceToken | ward_device_tokens | wardKey(UUID, UNIQUE), token(UUID, UNIQUE), createdAt, expiresAt(nullable, 현재 미사용 — 항상 NULL, 토큰은 만료되지 않음) |
| MembersTermsAgreement | members_terms_agreements | memberKey(UNIQUE), termsServiceAgreedAt, termsPrivacyAgreedAt, termsLocationAgreedAt (가입 시 now()로 일괄 기록) |
| MemberWithdrawal | member_withdrawal | memberKey, role, withdrawnAt. 탈퇴 이력(회원 행 삭제 후에도 남음). 인덱스 `idx_member_withdrawal_member_key`(V1) |
| FcmDeviceToken | fcm_device_tokens | memberKey, careRole(컬럼명 `recipient_type`, GUARDIAN/WARD), token(512, UNIQUE), platform(ANDROID/IOS/WEB, nullable). 한 회원이 여러 기기 가능 |
| SafeZone | safe_zones | safeZoneKey(UUID), wardMemberKey, name, address, latitude, longitude, radiusMeters(int, 100~2000m — 앱 슬라이더 값을 그대로 저장. #260 배포 때 300 미만 값을 300으로 보정했고, #266에서 하한을 100으로 되돌렸지만 보정된 값은 복원하지 않았다), type(SAFE/DANGER — 위험존도 같은 테이블, #238) |
| SafeZoneState | safe_zone_states | wardMemberKey(UNIQUE), safeZoneKeys(CSV, 현재 속한 안심존). 행 없음=최초 관측(알림 안 함), 빈 문자열=존 밖. zoneVersions(TEXT, nullable, `key@updatedAt` CSV) — 판정에 쓴 존 버전. 버전이 같은 존끼리만 진입·이탈을 비교하고, 신규·수정 존은 알림 없이 기준선만 세운다 (#289). 엔티티는 `safezone.dataaccess.entity` (#284에서 location에서 이동, 테이블명 변경 없음) |
| Notification | notifications | notificationKey(UUID, UNIQUE), recipientMemberKey, eventType, title, body, dataPayload(jsonb, 리다이렉트용), createdAt. 수신자별 개별 row. 읽음 필드 없음. 목록 조회 정렬·커서는 notification_id DESC (created_at은 fan-out 시 동시각 행이 생겨 정렬 불안정) |
| AnomalyDetection | anomaly_detections | wardMemberKey, detectionType(5종), score, recordedAt, latitude, longitude, evidence(1000자, nullable — 엔진 2차 배포 후 안 옴), alertKind/eventId/eventStartedAt/eventEndedAt/categories/reasonCode/durationMin/usualMin/speedKmh/eventHour/remindSeq(#248, 전부 nullable — 이전 형식 메시지는 비어 있음). 사건(eventId) 하나에 알림 여러 행(START·CATEGORY_ADDED·REMIND·END). `(wardMemberKey, detectionType, recordedAt, alertKind)` UNIQUE NULLS NOT DISTINCT = 재배달 멱등 키(#255). 알림 토글과 무관하게 항상 저장되는 탐지 사건 원본 (1건 = 1 row) |
| NotificationFeedback | notification_feedbacks | notificationId(UNIQUE — 1알림 1회), anomalyDetectionId(이상탐지 알림만 채움, 안심존 알림 평가는 NULL — 안심존 키·좌표는 notification_id로 알림 payload에서 찾는다), accuracy(ACCURATE/INACCURATE/UNSURE), reason(nullable, INACCURATE 전용), comment(100자, nullable). FK 제약은 걸지 않는다(이 프로젝트는 JPA 연관관계를 쓰지 않음). 이상탐지 알림의 `anomalyDetectionId`는 제출 시 `dataPayload`의 wardKey+type+recordedAt으로 `anomaly_detections`를 역조회해 채우며, **찾지 못하면 저장하지 않고 E9009로 거부한다** — 탐지 행과 조인되지 않는 라벨은 학습에 쓸 수 없고, 키 불일치 버그가 로그가 아니라 에러율로 드러나야 한다 |
| NotificationSetting | notification_settings | (wardMemberKey, caregiverMemberKey) UNIQUE — 보호자별 설정, 안심존·위험존(진입/이탈)·응급호출 토글, 이상탐지 토글 5종(speed/wandering/abnormalDwelling/routeDeviation/timeAnomaly), lowBatteryEnabled, batteryThresholdPercents(CSV, 기본 '' = 선택 없음 → 알림 없음). 수정은 `INSERT ... ON CONFLICT DO NOTHING` 후 벌크 UPDATE(`@Version` 없음). DB에는 제거된 민감도 컬럼 3개(`*_sensitivity`, NOT NULL DEFAULT 'NORMAL')가 남아 있다 |
| Inquiry | inquiries | inquiryKey(UUID, UNIQUE), memberKey, category(4종), title(50), content(1000), status(WAITING/ANSWERED), answer(2000, nullable), answeredAt(nullable). 탈퇴 시 hard-delete. 인덱스 TODO `(member_key, inquiry_id DESC)`·`(status, inquiry_id)` (#20) |
| SiteInquiry | site_inquiries | inquiryKey(UUID, UNIQUE), category(4종), title(50), content(1000), email(100), phone(20, nullable), isSecret, passwordHash(BCrypt, nullable), status(WAITING/ANSWERED), answer(2000, nullable), answeredAt(nullable). 회원과 무관. 인덱스 TODO `(status, site_inquiry_id)` |

**회원 탈퇴 정리** (`MemberWithdrawalManager`, @Transactional): 비밀번호 확인 → `member_withdrawal` 기록 → 한 트랜잭션에서 hard-delete: `refresh_token`, `local_auth`, `oauth`, `members_terms_agreements`, `fcm_device_tokens`, `notification_settings`, 케어 관계·`designated_avatars`(`leaveAllCare`), `care_invitation`, `gps_histories`, `anomaly_detections`, `location_settings`, `safe_zones`, `safe_zone_states`, `ward_device_tokens`, `inquiries`, `members`. Redis는 `gps:latest`·`gps:display`·`device:battery`·`anomaly:ack`를 지운다. **남는 것**: `notifications`(#149), `login_history`(미사용), Redis `deviceToken:{token}` 캐시(기술 부채 #24)

## 케어 역할(CareRole) — 보호자-대상자 (#242)

주보호자(`PRIMARY_GUARDIAN`) 개념은 #242에서 없앴다. MVP에서는 **보호자(`GUARDIAN`)-대상자** 관계만 쓴다.
`MANAGER`(관계자) 역할은 #254에서 코드·API·데이터까지 제거했다(`CareRole`·`CarePartyRole`에 값 없음, `POST /requests/manager` 삭제).
보호자끼리는 권한이 같으므로 역할 검증도 없다 — "보호자 전용" API도 **케어 관계 존재 여부만** 본다(`CareRelationshipValidator.validateCaregiver`, 없으면 E5011).

관계는 두 방향으로만 맺어진다. 신규 보호자가 기존 보호자에게 합류를 요청하거나 기존 보호자가 승인하는 절차는 없다.
1. **대상자 추가**(`POST /requests/ward`): 보호자 → 대상자, 대상자가 수락. **이미 보호자가 있는 대상자에게도 보낼 수 있다.**
   대기 요청 중복(E5009)은 **(요청자, 대상자)** 기준이다(`existsPendingWardRequest`) — 여러 보호자가 각자 보낼 수 있어야 한다.
2. **보호자 추가**(`POST /requests/guardian`): 그 대상자의 기존 보호자 누구나 → 신규 보호자, 신규 보호자가 수락.
   대기 요청 중복은 **(받는 회원, 대상자)** 기준 — 두 보호자가 같은 사람을 초대하면 하나만 남는다.


**타인 내보내기·역할 변경 API는 없다**(#242에서 제거). 동등한 보호자끼리 서로 내보낼 수 있게 하면 분쟁이 생긴다.
관계를 끊는 방법은 본인 이탈(`DELETE /wards/{wardKey}`)과 탈퇴뿐이다.

한도는 두 축이다. 대상자 1명에 붙는 보호자는 **총 5명**(초과 E5000), 보호자 1명이 맡을 수 있는 대상자는 5명(초과 E5013). 이미 연결된 관계는 E5001.
대상자 추가 요청은 **두 축 모두 발급과 수락 양쪽에서 검사한다**(`validateCanAddWard`). 보호자 추가 요청은 발급 시 대상자 쪽 인원·중복만 보고(`validateCanAddCaregiver`), 신규 보호자의 대상자 한도는 수락 때 처음 검사한다. 그 밖의 코드: 처리된 요청을 수락·거절·취소·재전송하면 E5006, 요청을 못 찾거나 남의 요청이면 E5003(400), 대상자 추가 대상이 WARD가 아니면 E5007, 보호자 추가 대상이 GUARDIAN이 아니면 E5008. 수락(`CareRelationshipWriter.register`)은
대상자·보호자 두 회원 행을 **memberKey 정렬 순서로** `SELECT ... FOR UPDATE` 한 뒤 검사한다 — 대상자 수락과 보호자 초대 수락이 동시에
들어와도 한도를 넘지 않고, 잠금 순서가 같아 교착이 생기지 않는다.

**이탈하면 그 사람이 그 대상자에 대해 보낸 대기 초대를 지운다**(`deletePendingSentBy`). 초대에는 만료가 없고(`expiredAt` 컬럼·`EXPIRED` 상태 모두 없음.
`EXPIRED_CARE_REQUEST`(E5005)만 미사용으로 남아 있음) 수락 시점에 발신자의 관계를 다시 보지 않아, 남겨두면 이탈 후 뒤늦은 수락으로 남은 보호자가 모르는 관계가 생긴다.
"마지막 관계가 끊기면 대상자의 초대를 전부 지운다"는 규칙은 **없앴다** — 이제 무관한 보호자들이 보낸 대상자 추가 요청도
그 대상자에 걸려 있고, 그건 기존 보호자와 상관없는 정상 요청이다. 탈퇴는 `CareInvitationWriter.deleteAllByMemberKey`가 정리한다.
JPA 연관관계·cascade는 쓰지 않는다.
관계를 바꾸는 세 곳(`register`·`leaveCare`·`leaveAllCare`)은 `careRelationship` 캐시를 `allEntries`로 전부 비운다.

**알림 설정·위치 수신 주기는 보호자별로 독립이다**(#237). `notification_settings`·`location_settings`는
`(ward_member_key, caregiver_member_key)` 키라 한 보호자가 바꾼 값이 다른 보호자에게 퍼지지 않는다.
알림 리스너는 대상자의 설정 행을 한 번에 읽고 `NotificationSettingReader.filterRecipients`로 수신자를 거른다(행 없으면 전부 켜짐).
배터리는 판정 자체가 보호자별이다 — 고른 잔량·억제 상태가 다르므로 도달 잔량마다 `BatteryThresholdAlertEvent(recipientKeys)`를 따로 낸다.
기기는 하나라 수집 주기는 하나뿐이다: 기기는 **보호자 설정의 최솟값**으로 수집하고, 더 긴 주기를 고른 보호자에게는
SSE에서 `recordedAt` 간격으로 솎아 보낸다(허용 오차 10초).
설정은 관계에 딸린 데이터라 **관계가 끊기면 함께 지운다** — 본인 이탈(`CareRelationshipManager.leaveCare`)이
`deleteByRelationship`으로 설정 행과 배터리 억제 필드를 지우고, 그 대상자 범위에서 이탈자가 owner 또는 target인 `designated_avatars` 행도 지운다.
탈퇴 시에는 대상자·보호자 어느 쪽 키든 `deleteByMemberKey`로 지운다.

**케어 관계가 끊겨도 대상자 데이터는 남는다** — `safe_zones`·
`ward_device_tokens`·`gps_histories`·`anomaly_detections`. **의도한 동작이다.** 새 보호자가 등록되면 이전 안심존과
이력을 그대로 물려받는다. 대상자를 기준으로 쌓인 데이터라 보호자가 바뀐다고 초기화할 이유가 없다는 판단.

안심존 단건 조회·수정·삭제는 `(safeZoneKey, wardMemberKey)` 쌍으로 스코프한다. `safeZoneKey`만으로 찾으면
경로의 `wardKey`로 인가를 통과한 뒤 **다른 대상자의 안심존**을 읽고 고치고 지울 수 있다 —
관계가 끊긴 전 보호자가 키를 기억하고 있으면 그대로 악용된다.

별명은 `(보호자, 대상자)` 쌍마다 하나라 `care_relationships`의 UNIQUE 키와 카디널리티가 같다.
그래서 별도 테이블을 만들지 않고 `ward_nickname` 컬럼을 뒀다. 관계가 삭제되면 별명도 같이 사라진다.
보호자 알림(이상탐지·안심존·배터리·위치 수집 중단) 본문은 수신자가 붙인 별명을 쓰고, 별명이 없으면 실명을 쓴다.
수신자 조회(`findCaregiverInfos`)가 이미 관계 행을 읽으므로 `CaregiverInfo.wardNickname`으로 실어 추가 조회가 없다.
본문이 수신자마다 다를 수 있어 표시 이름별로 수신자를 묶어(`CaregiverInfo.groupKeysByWardDisplayName`) 그룹마다 한 번씩 발송한다.

프로필 아바타는 이미지가 아니라 앱 번들 일러스트 16종의 코드 문자열(`ProfileAvatarCode` VO, 예: `senior_female_1`)만 저장한다.
본인이 고른 얼굴은 `members.profile_avatar_code`(전역), 보호자가 대상자·다른 보호자에게 붙인 얼굴은 `designated_avatars`(호출자 범위)로 나눈다.
별명과 달리 별도 테이블인 이유는 "보호자가 다른 보호자에게 붙인 얼굴"이 `(보는 보호자, 보이는 보호자)` 쌍이라 `care_relationships` 행에 자리가 없기 때문이다.
응답은 본인 값과 내가 지정한 값(`designatedProfileAvatarCode`)을 둘 다 내리고, 앱이 지정 > 본인 순으로 고른다. 본인 값은 가입 시 서버가 기본 배정하므로 항상 채워져 있다(#235).
서버는 기본값을 채우지 않는다 — null이어야 앱이 "직접 고른 값 없음"을 구분해 자동 배정한다. 빈 문자열은 해제 신호다
(앱 JSON 직렬화가 null 필드를 생략해 null로는 해제를 못 보낸다).

## Redis 키 구조

```
phone:verify:{phone}           SMS 인증코드          TTL: 5분
phone:token:{token}            인증 완료 토큰        TTL: 10분, 가입·재설정 시 GETDEL로 1회 소비
sms:quota:{phone}              번호별 발송 횟수      TTL: 10분 (고정 창, 한도 5). INCR=1일 때 EXPIRE, 초과 시 TTL 없으면 재설정
gps:latest:{memberKey}         GPS 최신 위치(원본)   TTL: 25시간  Gps JSON — 위치 수집 중단 판정(recordedAt). 부정확 좌표도 갱신해야 실내에서 오알림이 안 난다. 탈퇴 시 삭제
gps:display:{memberKey}        GPS 표시용 최신 위치  TTL: 25시간  Gps JSON — SSE 폴링. 정확도·속도 판정 통과분만 저장, 다음 판정의 기준점. 기준점이 10분(서버 수신 시각) 넘게 지나면 속도 판정 없이 재설정. 탈퇴 시 삭제
location:signal-lost:{memberKey}:{lastReceivedAt}  수집 중단 알림 중복 방지(SET NX)  TTL: 25시간
deviceToken:{token}            Device Token 인증 캐시(값 wardKey)  TTL: 30일, 재발급 시 구 토큰 삭제. 탈퇴 시에는 지우지 않는다(#24)
device:battery:{wardKey}       배터리 재알림 억제(hash) TTL: 1일, 억제 해제·탈퇴 시 삭제  { caregiverKey: lastNotifiedThreshold }
anomaly:ack:{wardKey}          이상탐지 사건 보호자 확인(hash) TTL: 1일(알림마다 갱신), EVENT_END·탈퇴 시 삭제  { eventId, acknowledged }
careRelationship::*            케어관계 캐시           TTL: 10분
care:invitation:resend:{requestKey}  케어 요청 재전송 간격 제한(SETNX)  TTL: 5분
site-inquiry:submit:{ip}       웹 문의 등록 IP 한도   TTL: 10분 (고정 창, 한도 5). IP는 X-Real-IP
site-inquiry:view-failure:{inquiryKey}  비밀글 비밀번호 시도 횟수  TTL: 10분, 비교 전에 INCR(원자적 예약), 6번째부터 잠금, 성공 시 DECR
site-inquiry:view-failure-daily:{inquiryKey}  비밀글 24시간 누적 시도  TTL: 24시간, 21번째부터 잠금
```

안심존 상태는 Redis(`safezone:state:{wardKey}`)에서 `safe_zone_states` 테이블로 이전했다. 유실되면 복구 불가능한
알림 이력이라 TTL이 있는 저장소와 성격이 맞지 않았고, 저장 실패 시 알림만 나가는 경로가 있었다.
안심존 목록은 캐싱하지 않고 GPS 지점마다 DB를 조회한다(핫패스 아님 + 존 수정 시 오알림 방지).

진입·이탈은 직전 상태 기준 여유 폭(히스테리시스)으로 판정한다(#266, `SafeZoneObservation`). 여유 폭 `m = max(30m, accuracy)`. 직전에 존 안이면 `r + m`을 넘어야 이탈, 밖이면 `max(r - m, r/2)` 이내로 들어와야 진입, 그 사이는 직전 상태 유지. 최초 관측(행 없음)은 여유 폭 없이 `r` 기준으로 기준선만 세운다. 정확도 100m 초과 좌표는 판정·상태 갱신 없이 건너뛰고(그래서 `m`은 최대 100m), accuracy가 null이면 `m = 30m`. 대상자에게 존이 하나도 없으면 상태 행을 지운다 — 이후 첫 존 등록은 다시 최초 관측(알림 없음). 반경 하한은 이 판정을 전제로 300m → 100m로 되돌렸다. 대가: 이탈 알림이 최대 `m`(실내 기준 최대 100m)만큼 늦게 울린다

## 인프라 현황 (2026-10-26 조회)

AWS CLI·SSM 읽기 전용 조회와 콘솔 확인(ElastiCache·EventBridge Scheduler·EFS) 결과다. 공인 IP·인스턴스 ID·엔드포인트는 적지 않는다(인스턴스는 `Name` 태그로 조회).
로컬 CLI 사용자에는 ElastiCache·Scheduler·EFS 조회 권한이 없어 이 항목들은 콘솔로 확인한다.

### 네트워크

| 항목 | 값 |
|---|---|
| VPC | `recaring-vpc` 10.0.0.0/16, 리전 ap-northeast-2 |
| 퍼블릭 서브넷 | 10.0.1.0/24 (2a) — `nginx-server`, `recaring-nat-instance` |
| 프라이빗 서브넷 | 10.0.2.0/24 (2a, 앱 서버), 10.0.3.0/24 (2c, RDS 서브넷 그룹용) |
| 외부 송신 | 프라이빗 라우트 테이블 0.0.0.0/0 → NAT 인스턴스(t4g.nano, src/dst check off). NAT Gateway 아님 |
| 진입 경로 | 클라이언트 → `nginx-server`(EIP, `re-caring.com`) → `http://spring-app.recaring.local:8080`(CloudMap) |

### EC2

| 인스턴스 | 타입 | 위치 | 역할 |
|---|---|---|---|
| `recaring-app-server` | t3.medium | 프라이빗(공인 IP 없음) | ECS 컨테이너 인스턴스 — 앱·탐지 엔진·모니터링. 메모리 3.8GB 중 약 1.5GB 사용, 디스크 30GB 중 7.7GB, ECS agent 1.103.2 |
| `nginx-server` | t3.nano | 퍼블릭(EIP) | ECS `nginx-service`만 실행. EFS(인증서 보관용)로 letsencrypt·webroot 마운트 |
| `recaring-nat-instance` | t4g.nano | 퍼블릭(EIP) | 프라이빗 서브넷 NAT |

앱 서버 접근은 SSM Session Manager만 쓴다. Grafana(3000)도 프라이빗이라 SSM 포트 포워딩으로 연다.

### ECS (`recaring-cluster`)

| 서비스 | 태스크 정의 | 네트워크 | 리소스 | 이미지 | 비고 |
|---|---|---|---|---|---|
| `recaring-service` | `recaring-task` | awsvpc, CloudMap 등록 | cpu 512 / mem 1024 (실사용 약 600MiB) | `re-caring-api:{SHORT_SHA}` | 포트 8080·9010, 헬스체크 `wget /actuator/health`, min 100%/max 200%, circuit breaker 롤백, 로그 `/ecs/recaring` |
| `recaring-detection-service` | `detection-detect-task` | — | cpu 256 / mem 768 hard·321 soft (실사용 약 165MiB) | `recaring-detection-detect:latest` | 로그 `/ecs/recaring-detection`, 역할 `detection-task-role` |
| `recaring-monitoring-service` | `monitoring-task` | bridge | 512 / 512 | `prometheus:latest`, `grafana:latest`, node-exporter | 설정은 호스트 `/opt/recaring/monitoring`, min 0%/max 100% |
| `nginx-service` | `nginx` | — | mem 128 | `recaring-nginx:latest` | `nginx-server`에만 배치 |

- 앱 비밀값은 SSM Parameter Store `/recaring/*`에서 태스크 정의 `secrets`로 주입한다(DB·JWT·CoolSMS·Firebase·Kakao·Slack 웹훅·Admin 키 등). 일반 env는 `REDIS_HOST`·`REDIS_PORT`·`SPRING_PROFILES_ACTIVE`·`JAVA_OPTS`·`LOGGING_STRUCTURED_ECS_SERVICE_VERSION` 등
- 엔진 이미지는 `:latest`라 새 이미지 푸시 후 `recaring-detection-service`를 강제 재배포해야 반영된다(`flyway-conventions.md` 엔진 영향 테이블 절차)

### 배포

- `deploy.yml`: develop에 PR 머지 시 `clean test`(단위만) → 빌드 → ECR `re-caring-api:{SHORT_SHA}` 푸시 → 현재 `recaring-task` 리비전을 렌더링 → 서비스 롤링 배포(wait-for-stability)
- nginx: `nginx/**` 변경 시 `:latest` 푸시 + force-new-deployment
- `ci-check.yml`: PR 검사. **통합 테스트(`integrationTest`)는 CI에서 돌지 않는다** — 로컬에서 확인 필요

### 데이터 저장소

| 항목 | 값 |
|---|---|
| RDS `recaring-db` | PostgreSQL 17.9, db.t4g.micro, gp3 20GB(자동 확장 상한 100GB), Single-AZ(2c), 퍼블릭 접근 없음, `default.postgres17` 파라미터 그룹 |
| RDS 백업·관측 | 자동 백업 보관 **1일**, Performance Insights **꺼짐** |
| ElastiCache `recaring-redis` | Valkey, cache.t3.micro **노드 1개**(레플리카·자동 장애조치 없음). 전송 암호화(TLS), AUTH 없음. 노드 장애 시 캐시·GPS 최신 위치·Stream이 함께 멈춘다 |
| EFS | 인증서(letsencrypt·certbot webroot) 보관 전용 |
| Redis 연결 설정 | dev: `ssl.enabled: true`, `REDIS_PASSWORD` 기본값 빈 문자열이고 태스크 정의에 주입하지 않으므로 AUTH 없이 연결. prod yml에는 ssl·password 설정이 없다 |
| S3 | 탐지 모델(`recaring-detection-models-…`), 학습 데이터(`recaring-isolation-learning-…`), 소개 사이트(`recaring-site-deploy`), Terraform state, 옛 Elastic Beanstalk 버킷 |

Redis TODO: AUTH 토큰 적용 검토(prod 전 SEC-001), `maxmemory-policy`가 Stream(TTL 없는 키)을 임의 삭제하지 않는 값(`volatile-lru`/`noeviction`)인지 확인.

### 스케줄 (EventBridge Scheduler, default 그룹)

| 일정 | Cron (KST) | 대상 |
|---|---|---|
| `recaring-learning` | `0 3 * * ? *` — 매일 03:00 | 학습 컨테이너 `recaring-detection-train` 기동 |
| `recaring-certbot-renewal` | `0 2 15 * ? *` — 매월 15일 02:00 | 인증서 갱신(certbot) |

앱 내부 `@Scheduled` 작업은 위 "스케줄 작업" 노트 참고.

### 로그·모니터링

| 로그 그룹 | 보관 |
|---|---|
| `/ecs/recaring` | 120일 |
| `/ecs/recaring-detection` | 30일 |
| `/ecs/nginx` | 7일 |
| `/ecs/certbot` | 무기한 |
| `/ecs/redis` | 30일 (비어 있음, 잔재) |

- Prometheus 잡: 리포는 `recaring`(`spring-app.recaring.local:8080`)·`node`. **EC2 호스트 설정에는 제거된 `redis`(redis-exporter:9121) 잡이 아직 남아 있다** — 호스트 설정이 리포와 어긋남
- Grafana 알림 규칙은 `recaring-error-log` 하나(`logback_events_total{level="error"}` 5분). 이상탐지 스트림 지표에는 규칙이 없다
- ECR 리포: `re-caring-api`, `recaring-detection-detect`, `recaring-detection-train`, `recaring-nginx`, `recaring-certbot`, `recaring-redis`(잔재)

### 보안 그룹 관찰

- 앱 SG(`recaring-recaring-app-sg`): 8080 ← nginx SG·self·k6 SG, 6379·443 ← 10.0.0.0/16
- `nginx-sg`: 22 ← CIDR 3개(EC2 Instance Connect 대역 포함). SSM 전용 규칙과 어긋남
- `k6-sg`: 22 ← 0.0.0.0/0, 그리고 앱 8080·RDS 5432에 허용돼 있음 — 부하 테스트 잔재
- `recaring-app-sg`(이름 다른 옛 SG): 22 ← 0.0.0.0/0. 실행 중 인스턴스에는 붙어 있지 않음
- 앱 태스크 정의에 `GRAFANA_DB_PASSWORD`가 `secrets`가 아닌 일반 env로 들어 있다

### 범위 밖·미사용

- CloudFront: 소개 사이트(www) 전용이라 이 문서에서 다루지 않는다
- SQS: 사용하지 않는다
- CloudMap 네임스페이스 `recaring.local`은 nginx 설정의 `spring-app.recaring.local`에서 확인

## 탐지 파이프라인

결정론 판정(배터리·안심존)은 룰엔진 왕복 없이 **서버 인라인**으로 처리한다. GPS 수신 → `GpsSavedEvent` →
`BatteryDetectionListener` / `SafeZoneDetectionListener`가 각각 판정하고, 도달·전이 시에만 알림 이벤트를 발행한다.

**위치 수집 중단**은 GPS가 *안 오는* 것을 봐야 해서 이벤트가 아니라 `LocationSignalMonitor`(10분 `@Scheduled`)가 판정한다.
케어 관계의 대상자 키 목록 → `gps:latest` MGET → 서버 수신 시각(`recordedAt`)이 20분~24시간 전이면
`location:signal-lost:{wardKey}:{recordedAt}` SET NX로 선점한 쪽만 `LocationSignalLostEvent` 발행(끊김 구간당 1회, 토글 없음).
발송 실패 시 키를 지워 다음 주기에 재시도. 별도 테이블·GPS 경로 쓰기 없음. Redis 유실 시 그 시점에 이미 끊겨 있던 대상자는 누락된다(수용).

이상이동 탐지(경로 이탈·배회·속도·시간대)는 **Redis Stream**으로 별도 탐지 엔진 컨테이너에 넘긴다.
탐지 컨테이너는 상시 기동해 스트림을 계속 소비한다. SQS는 쓰지 않는다(코드·의존성 모두 제거 완료).

**탐지와 학습은 다른 트리거로 돈다.** 아래 스트림 경로는 *탐지* 전용이다.
학습 컨테이너(`recaring-detection-train`)는 실시간 좌표가 아니라 RDS에 쌓인 이력을 배치로 읽어
사용자별 모델을 S3에 쓰며, **EventBridge Scheduler `recaring-learning`(매일 03:00 KST)으로 기동**한다. 두 경로를 섞지 않는다.

```
GpsSavedEvent → DetectionListener → DetectionPublisher → XADD gps-detection → 탐지 엔진
                                                          ← XADD anomaly-alerts ← AnomalyDetectionConsumer (RedisStreamConfig 구독)
```

**`gps-detection` 발행 필드** (연동 명세 v2 기준. Redis Stream이라 값은 전부 문자열)

> 스트림 이름은 명세서의 `gps-coordinates`가 아니라 **`gps-detection`**이다(의도적 변경).
> 이름이 어긋나면 에러 없이 조용히 전달만 끊기므로, 엔진 쪽 `XREADGROUP` 대상도 반드시 같아야 한다.

| 필드 | 필수 | 출처 |
|------|------|------|
| `ward_member_key` | O | `GpsSavedEvent.memberKey` |
| `latitude` / `longitude` | O | `Gps` (WGS84 십진도) |
| `recorded_at` | O | `Gps.recordedAt` — `yyyy-MM-dd HH:mm:ss` (KST, 오프셋 없음) |
| `accuracy` | X | 미터. 100m 초과분 제외는 엔진 책임 |
| `speed` | X | 기기 순간 속도, m/s (#255) |
| `measured_at` | X | 기기 측정 시각, `recorded_at`과 같은 형식 (#255) |
| `acknowledged` | O | `"true"`/`"false"` (#248). `anomaly:ack:{wardKey}`의 값, 없으면 false. 푸시되는 알림 수신 시 false로 리셋, 확인 API(`POST /api/v1/notifications/anomaly-events/acknowledge`)로 true. 엔진은 마지막 알림 이후 false→true 전환만 확인으로 인정하고, 확인되면 사건이 끝날 때까지 재알림하지 않는다 |

- `recorded_at`은 **서버 수신 시각**이다. 엔진이 학습에 RDS `gps_histories`를, 탐지에 이 스트림을 쓰므로 두 경로의
  시각이 같은 의미여야 한다. `measured_at`·`speed`는 엔진이 판정 보조로 쓸 수 있게 추가로만 싣는다(#255).
  엔진은 지연 분포를 확인한 뒤 필요할 때만 판정 시간축에 반영하기로 했다(기기 시계 오류 위험). 출력 시각은 계속 `recorded_at` 에코.
  같은 이유로 시각에 오프셋을 붙이지 않는다 — RDS `timestamp` 표기와 통일.
- 정확도가 낮은 좌표도 거르지 않고 그대로 발행한다. 판정 제외는 엔진이 `accuracy`를 보고 정한다.
  (인라인 판정인 `SafeZoneDetectionListener`는 거르므로 기준이 다르다)
- null인 선택 필드는 빈 문자열이 아니라 맵에서 제외한다.
**전달 보장은 구간별로 다르다. 뭉뚱그리면 안 된다.**

| 구간 | 보장 | 근거 |
|------|------|------|
| 백엔드 → Redis (`XADD`) | **at-most-once** | 우리 선택. Redis 장애가 GPS 수신 API를 실패시키면 안 되므로 `@Async` 리스너에서 `DataAccessException`을 삼키고 warn만 남긴다. 재시도하지 않으므로 유실 가능 |
| Redis → 엔진 (`XREADGROUP`+`XACK`) | **at-least-once** | Redis Stream 기본 동작. 미ACK 메시지가 PEL(Pending Entries List)에 남아 `XAUTOCLAIM`으로 회수된다 |

- 발행 유실분은 RDS `gps_histories`에 남아 있으므로 학습·재계산으로 메운다. 실시간 판정만 그 좌표를 못 본다.
- **trim은 시간 기준(`MINID ~`, 1시간)이다.** 길이 기준(`MAXLEN`)이 아니다.
  이상탐지 알림은 실시간성이 목적이라 1시간 지나 판정된 좌표는 값이 없다 — 보관할 이유 자체가 없다.
  길이 기준이면 WARD 수가 늘수록 보존 창이 반비례해 줄지만(100명이면 8시간, 1,000명이면 50분),
  시간 기준은 WARD 수와 무관하게 1시간으로 고정된다. 그만큼 메모리 상한은 없어지지만 창이 1시간이라
  WARD 1,000명에서도 약 24MB 수준이라 t3.micro(568MB)에 부담이 안 된다.
- **trim은 PEL을 존중하지 않는다.** 아직 ACK되지 않은 메시지도 잘려나간다.
  MINID를 쓰면 이게 결함이 아니라 **의도한 동작**이다 — 1시간 넘게 미처리된 좌표는 버리는 게 맞다.
  단, 그래서 **at-least-once 보장 창도 정확히 1시간**이다. 엔진이 1시간 넘게 죽어 있으면 그 구간은 복구되지 않는다.
- **발행 실패는 재시도·DLQ 없이 삼킨다.** 좌표 원본은 이미 RDS에 저장된 뒤라 유실되는 건 데이터가 아니라
  실시간 판정 기회 한 건뿐이고, 30초 뒤 다음 좌표가 거의 같은 정보를 준다. 늦게 밀어넣는 건 1시간 보존 정책과도 모순.
  Redis가 죽어 XADD가 실패한 상황이라 DLQ를 Redis에 둘 수도 없다.
- 지금은 **`DataAccessException`을 한 덩어리로 잡아 warn 로그만 남긴다.** 원인별 예외 분리와 실패 메트릭은
  검토했으나 **의도적으로 보류**했다 — 운영 로그를 먼저 쌓아 실제로 어떤 실패가 나는지 본 뒤 필요한 것만 넣는다.
  (검토 내용은 이슈 #187 코멘트 참고)
- MINID는 앱 서버 시계로 계산하지만 Stream ID는 Redis 서버 시계로 찍힌다. 같은 VPC에서 둘 다 NTP 동기화되므로
  실무상 무시할 수준이나, 시계가 크게 어긋나면 trim 창이 함께 어긋난다.
- 소비자 그룹(`XGROUP CREATE`) 생성은 엔진 컨테이너 책임. 백엔드는 producer만 담당한다.
- **파티션 개념이 없다 — 단, 컨슈머를 늘릴 때만 문제가 된다.** Kafka처럼 키로 파티셔닝할 수 없어서
  consumer group에 컨슈머가 2개 이상이면 같은 ward의 좌표가 흩어지고, 한쪽이 잠깐 밀리면 **처리 순서가 뒤집힌다**
  (쌓이는 순서는 멀쩡하다 — 뒤집히는 건 처리 순서다). 속도 계산의 시간차가 음수가 되는 식으로 없는 이상을 만들어낸다.
  유실이 아니라서 다음 좌표가 와도 교정되지 않는다.

  **컨슈머 1개면 발생하지 않으며, 당분간 늘릴 이유도 없다.** 명세의 1.65ms/건으로 계산하면 초당 약 606건,
  WARD 1명이 30초당 1건이므로 컨슈머 1개로 **약 18,000명**까지 소화한다.

  여기서 "컨슈머"는 컨테이너가 아니라 `XREADGROUP GROUP <group> <consumer-name>`에 넘기는 **이름**이다.
  컨테이너와 1:1이 되는 건 그렇게 짜기 때문이고, 한 컨테이너가 워커를 여러 개 띄우면(파이썬 멀티프로세싱 등)
  컨테이너 1개여도 컨슈머는 N개다. Kafka와 달리 파티션 상한이 없어 이름만 다르면 얼마든지 늘어난다.
  (스케일 아웃 시 이름은 반드시 달라야 한다 — 같은 이름을 두 프로세스가 쓰면 PEL이 섞인다)
  → 처리량 때문에 컨슈머를 늘리는 시점에 ward 단위 재정렬 또는 `recorded_at` 기준 정렬이 필요해진다. 그전엔 무관.
- `is_charging`은 수집하지 않으므로 전송하지 않는다.

### `anomaly-alerts` — 엔진 → 백엔드 (백엔드가 컨슈머)

엔진이 이상을 판정하면 사건 안에서 **카테고리가 처음 성립할 때마다 1건**(동시 성립은 1건으로 합침), 미확인이면 재알림을 발행한다.
출력 형식은 엔진 문서 「탐지 알림 출력 형식 변경 안내 (백엔드용)」(2026-10-10)가 기준이다(#248). 백엔드는 `XREADGROUP`으로 소비하고
알림을 저장한 뒤 `XACK`한다. 그룹 `recaring-backend`.

| 필드 | 설명 |
|------|------|
| `ward_member_key` | 입력의 `ward_member_key` 그대로 (필드명도 동일 — `user_id` 아님) |
| `detection_type` | 5종 — `SPEED_ANOMALY` / `WANDERING` / `ABNORMAL_DWELLING` / `ROUTE_DEVIATION` / `TIME_ANOMALY` |
| `score` | 항상 0.5000~1.0000, 소수 4자리 |
| `detected_at` | 입력의 `recorded_at`을 **그대로 에코**(필드명은 `detected_at`, 파서는 `recorded_at`도 받는다). EVENT_END는 `event_ended_at`과 같은 값. 엔진 자기 시계 금지. 구간을 보는 탐지(배회 등)는 판정을 트리거한 GPS point의 값 |
| `latitude` / `longitude` | 소수 6자리 |
| `evidence` | **전환 기간에만** 옴. 백엔드 배포 후 엔진이 `EMIT_EVIDENCE=0`으로 끈다. 저장만 하고 푸시 문구에는 쓰지 않는다(#299) |
| `alert_kind` | `EVENT_START` / `CATEGORY_ADDED` / `REMIND` / `EVENT_END`. 없으면 이전 형식 메시지로 보고 바로 푸시한다. 모르는 값이면 버린다 |
| `event_id` / `event_started_at` | 사건 식별자(`{key}:{사건 시작 시각}`)·시작 시각. `alert_kind`가 있는데 `event_id`가 없으면 버린다 |
| `categories` | 이번에 성립한 카테고리 전부(쉼표 구분). `detection_type`은 그중 최상위 |
| `reason_code` | 걸린 규칙 12종(`AnomalyReasonCode`). 모르는 값이면 null로 두고 기본 문구("이상 징후가 감지되었어요")로 보낸다 |
| `duration_min` / `usual_min` / `speed_kmh` / `hour` | 해당 시에만. 문구 조립용 숫자 |
| `remind_seq` / `event_ended_at` | REMIND / EVENT_END에만 |

**푸시 정책**(#248): 문구는 `AnomalyNotificationBody`가 `reason_code`별 템플릿으로 조립한다(분 → "1시간 30분"). `duration_min`이 오면 괄호 안에 "지속 3시간 20분"으로 붙인다(속도·평소 체류와 함께면 쉼표로 이음, 시각 유형 NIGHT_OUTING·RARE_HOUR 제외).
REMIND의 numbers는 직전 알림 값 재전송이라 본문에서 지속 시간·속도를 뺀다(#299, 멈춘 시간처럼 보임). `NO_ARRIVAL`은 고정 문구. 낯선 곳 체류(`DWELL_UNKNOWN`·`DWELL_AFTER_VEHICLE`)는 "낯선 곳에 머물고 계십니다" 한 문구로 통일한다. EVENT_END는 `anomaly_detections`에만 저장하고 푸시하지 않으며 `anomaly:ack` 키를 지운다.
제목은 `DetectionType` 기준이되, `DWELL_KNOWN_LONG`·`DWELL_SAFEZONE_LONG`만 "오래 머무름 확인"이다(#255, `AnomalyReasonCode.notificationTitle`).
이 두 사유(`DWELL_KNOWN_LONG`·`DWELL_SAFEZONE_LONG`)의 REMIND는 저장만 하고 푸시하지 않으며 `anomaly:ack`도 리셋하지 않는다 — 같은 자리에 있는 한 사건이 닫히지 않아 확인 전까지 매시간 울린다.
`VEHICLE`의 REMIND는 푸시하며 제목만 "차량 이동 계속 중"이다(#257). 엔진이 확인 전까지 `VEHICLE_REMIND_EVERY_MINUTES`(기본 30분) 간격으로 보낸다.
엔진은 REMIND에 그 사건의 가장 최근 `reason_code`를 싣는다. REMIND는 새 좌표가 올 때만 나간다.
값이 없는 필드는 `"null"`이 아니라 필드 자체가 생략된다. 파서가 Map으로 읽으므로 모르는 필드가 늘어도 버려지지 않는다.

**명세서 v2와 다른 점**: 명세의 `SIGNAL_LOST`는 채택하지 않았다(5종만). 다만 엔진이 보낼 가능성이 남아 있어
파서가 모르는 유형을 만나면 예외 대신 빈 결과를 주고, 컨슈머가 그 메시지를 버린 뒤 ACK한다.

**흐름**
```
AnomalyDetectionConsumer(수동 ACK)
  → AnomalyDetectionParser         파싱 실패 시 Optional.empty
  → AnomalyDetectionManager.record(@Transactional)
       INSERT ... ON CONFLICT DO NOTHING   중복(0)이면 여기서 종료
       → anomaly:ack 갱신                  EVENT_END면 삭제, 푸시 대상이면 false로 리셋
       → AnomalyDetectedEvent 동기 발행 (푸시 대상일 때만. @Async 아님 — 저장 후 ACK해야 하므로)
            → AnomalyNotificationListener  토글 확인 → 보호자 조회 → 저장 + FCM
  → XACK
  실패 시 컨슈머 안에서 5초·10초 뒤 재시도(총 3회), 그래도 실패하면 ACK 없이 PEL에 남겨 AnomalyPendingReclaimer가 회수
```

**탐지 기록과 알림은 분리돼 있다.** `anomaly_detections`는 알림 토글과 무관하게 항상 쌓이고,
토글은 `AnomalyNotificationListener`에서만 본다. 예전엔 토글이 꺼져 있으면 탐지 사실 자체가
로그 외에 아무데도 남지 않았다 — 모델 평가·튜닝의 근거가 사용자 설정에 좌우되면 안 된다.

- **ACK는 알림 저장 뒤에 한다.** 저장 전에 ACK하면 처리 중 죽었을 때 알림이 통째로 사라진다.
  `FirebaseFcmClient`는 예외를 내부에서 삼키므로(무효 토큰 목록만 반환) FCM 실패가 ACK를 막지 않는다.
- **알림 리스너에 `@Async`를 붙이지 않는다.** 컨슈머 스레드에서 동기로 끝나야 실패가 전파되어 ACK를 건너뛸 수 있다.
  (`@Async` 알림 리스너는 SafeZone·Battery·CareInvitation·Inquiry. `LocationSignalNotificationListener`도 동기 — 실패 시 모니터가 선점 키를 지워야 하므로)
- **PEL 회수는 `AnomalyPendingReclaimer`가 한다.** 기동 시(idle 10초 이상)와 1시간마다(idle 1분 이상) `XPENDING` → `XCLAIM`(20건 × 최대 5회).
  2회 이상 배달된 메시지는 다시 처리하지 않고 `[AnomalyStream : DeliveryLimitDiscarded]` ERROR를 남긴 뒤 ACK한다. trim으로 본문이 사라진 항목은 ACK만 한다.
- **처리 못 하는 메시지는 버리고 ACK한다.** 알 수 없는 유형, 숫자·시각 형식 오류, 필수 필드 누락 모두.
  ACK하지 않으면 PEL에 남아 영원히 재배달되며 뒤가 밀린다(poison message).
- **`evidence`는 1000자로 자른다.** `anomaly_detections.evidence`가 `varchar(1000)`이라 초과 시 INSERT가 실패하고,
  실패하면 ACK되지 않아 같은 poison 경로로 들어간다.
- **컨슈머 그룹은 백엔드가 만든다**(시작 ID `$` — 그룹 생성 이전 메시지는 받지 않는다). `createGroup`이 스트림까지 함께 만들어(MKSTREAM) 엔진이 첫 결과를 넣기 전에
  기동해도 된다. **`BUSYGROUP`은 정상으로 취급해야 한다** — 재기동·다중 태스크마다 발생하므로 던지면
  두 번째 배포부터 기동이 막힌다. (`RedisSystemException`으로 오며 `InvalidDataAccessApiUsageException`이 아니다)
- 컨슈머 이름은 hostname이다. 태스크마다 달라야 PEL이 섞이지 않는다.
- **시각 값은 양방향 모두 `recorded_at`이다.** 백엔드가 `gps-detection`에 발행한 GPS 수신 시각(KST,
  `yyyy-MM-dd HH:mm:ss`)을 엔진이 그대로 에코해 `anomaly-alerts`의 `detected_at`으로 돌려준다. 엔진 자기 시계를 쓰면 안 된다 —
  컨테이너 TZ에 따라 값이 밀리고, 알림을 눌러 위치 이력으로 이동할 때 `GET /api/v1/location/history`의 `date`가
  엉뚱한 날짜가 된다(이 조회는 `gps_histories.recorded_at`을 KST LocalDate로 자른다). 재발행 때마다 값이
  달라지면 UNIQUE 멱등 키도 무력해진다. 구간을 보는 탐지(배회 등)는 **판정을 트리거한 GPS point의 값**을 보낸다.
  파서는 오프셋·`Z`를 받지 못하므로 포맷도 에코 대상이다.
- **중복 재배달은 `anomaly_detections`의 UNIQUE 제약으로 막는다.** ACK 전에 죽어 재배달되면 INSERT가 0을 돌려주고
  이벤트를 발행하지 않아 알림도 다시 나가지 않는다. 0이면 `[AnomalyResult : DuplicateSkipped]` WARN을 남긴다.
  키는 `(ward_member_key, detection_type, recorded_at, alert_kind)` `NULLS NOT DISTINCT`다(#255). EVENT_END의 `detected_at`은
  종료 시각(두절이면 마지막 좌표, 복귀면 도착 시각)이라 같은 좌표에서 나간 알림과 3컬럼 키가 겹쳐 버려졌다.
  `NULLS NOT DISTINCT` 제약은 Flyway `V1__baseline.sql`에 있어 로컬·테스트에도 동일하게 적용된다(#287). 피드백 역조회(`findIdByDetectionKeys`)는 EVENT_END 행을 제외한다.
- **저장과 알림은 반드시 한 트랜잭션이어야 한다.** 알림 저장이 실패했는데 탐지 저장만 커밋되면,
  재배달분이 중복으로 걸러져 알림이 영영 발송되지 않는다. `AnomalyDetectionManager.record`의 `@Transactional`이
  그 경계이며, 실패 시 통째로 롤백되고 ACK도 건너뛰어 다음 재배달에서 처음부터 다시 처리된다.
- FCM 발송은 롤백되지 않는다. 다만 `NotificationSendManager`가 DB 저장을 먼저, FCM을 나중에 하므로
  DB가 실패하면 FCM 이전에 멈춘다.
- **구독은 폴링 에러로 취소되지 않는다.** `StreamReadRequest`의 `cancelOnError` 기본값이 "항상 true"라
  Redis 연결이 한 번만 끊겨도 구독이 영구 취소된다(2026-09-16 DNS 타임아웃 1회로 5일간 알림 전면 중단).
  `cancelOnError(throwable -> false)`로 폴링 루프를 유지하고, 재시도 간격과 로그 억제는
  `AnomalyStreamErrorHandler`가 맡는다(1s→30s 지수 백오프, 첫 실패 WARN·이후 5분에 최대 1회 WARN. ERROR는 남기지 않으므로 지속 장애는 `anomaly_stream_poll_consecutive_errors`로 감시).
- **스트림 상태는 Prometheus로 노출한다.** `AnomalyStreamMetrics`가 30초마다 `XINFO GROUPS`를 읽어 갱신한다.

| 메트릭 | 의미 |
|--------|------|
| `anomaly_stream_subscription_active` | 구독 생존 여부 (0이면 알림 전면 중단) |
| `anomaly_stream_lag` | 미배달 백로그. `-1`은 Redis가 `lag`을 주지 않아 측정 불가 |
| `anomaly_stream_pending` | PEL(배달됐으나 미ACK) 크기 |
| `anomaly_stream_delivery_delay_seconds` | 마지막 발행 ID − 마지막 배달 ID의 시각 차. `lag` 없이도 동작 |
| `anomaly_stream_poll_errors_total` / `anomaly_stream_poll_consecutive_errors` | 폴링 실패 누적 / 연속 실패 수 |

- **컨슈머가 멈춘 뒤 재기동하면 백로그가 한 번에 배달된다.** 오프셋이 `lastConsumed`라 그룹의
  `last-delivered-id` 이후분이 전부 나간다. 오래된 알림을 보내고 싶지 않으면 기동 전에
  `XGROUP SETID anomaly-alerts recaring-backend $`로 오프셋을 넘긴다.

---

## 알려진 기술 부채

> 개선 구현 시 해당 항목을 제거한다.

| # | 항목 | 위치 | 설명 |
|---|------|------|------|
| 2 | GPS 후속 처리 실패 무시 | `LocationService.receiveGps` | DB 저장 뒤 `GpsSavedEvent`를 `@Async` 리스너(캐시·탐지 발행·배터리·안심존)로 넘긴다(AFTER_COMMIT 아님). Redis 실패는 WARN만 남기고 재시도하지 않는다. Outbox 미적용 |
| 3 | GPS 히스토리 보관 정책 부재 | `GpsHistory` entity | `(ward_member_key, recorded_at)` 인덱스는 dev에 있다(V1 baseline 포함). `(ward_member_key, measured_at)`은 TODO로만 남아 있다. 보관 정리 수단도 없어 gp3 20GB(자동 확장 상한 100GB)가 실질 한도(WARD 1,000명이면 약 36일) |
| 4 | SSE 구독자 수 무제한 | `SseEmitterManager` | 연결마다 폴링 루프를 따로 돌려 broadcast 누수 문제(구 #1)는 사라졌지만, wardKey당·회원당 emitter 수 제한이 없다 |
| 5 | 대기 초대가 무기한 남는다 | `CareInvitation` | 만료 개념(`expiredAt`·`EXPIRED`)이 없어 PENDING 요청은 수락·거절·취소되기 전까지 계속 남는다. 정리 배치 없음 |
| 8 | 겹친 안심존 동시 알림 | `SafeZoneDetectionListener` | 반경 100~2000m라 도심에서 존이 겹치면 교집합 진입·이탈 시 알림이 존 개수만큼 발송된다. 해결안: 판정 1회당 이벤트 1건으로 병합("집, 병원에 도착했어요"). FCM data payload의 `safeZoneKey` 복수화가 필요해 앱 팀 확인 선행 |
| 9 | 안심존 상태 동시 갱신 미방어 | `SafeZoneStateManager` | `findByWardMemberKey` → `replace`가 원자적이지 않다. GPS 최소 주기 30초라 정상 흐름에선 겹치지 않으나, 앱이 오프라인 버퍼링 후 일괄 전송하거나 재시도하면 중복 알림 가능. 실제 도착 간격 확인 후 필요 시 비관적 락 + `observed_at` 도입 |
| 10 | 장소 검색 캐시·호출 제한 없음 | `KakaoPlaceSearchClient` | 프록시에 캐시도 회원별 제한도 없다. 앱이 타이핑마다 호출하면 카카오 일일 쿼터가 소진되고 그 시점부터 429 → `PLACE_SEARCH_RATE_LIMITED`로 검색 기능 전체 정지. 개선안: `query` + 반올림 좌표 Redis 캐시(10분) → 회원별 분당 호출 제한 |
| 11 | 편향 폴백이 카카오 호출을 2배로 만든다 | `PlaceService` | 카카오의 `x/y/radius`는 필터가 아니라 정렬 가중치라, 편향 위치 근처의 엉뚱한 결과가 상위에 섞여 온다(`해운대역` + 서울 편향 → '해운대연탄생갈비 부평갈산역점'). 그래서 "결과 0건"이 아니라 "검색어 토큰이 결과 이름에 하나도 안 걸림"을 폴백 조건으로 쓴다. 대신 편향 밖 검색마다 호출 2회 + 지연 최대 2배(읽기 타임아웃 3초 × 2). 이름 기반 휴리스틱이라 주소 문자열 검색(`서울 마포구 …`)은 항상 폴백을 탄다 |
| 12 | 해석 불가 알림의 무음 폐기 | `AnomalyDetectionConsumer` | 파싱 실패 메시지를 WARN 한 줄만 남기고 ACK한다. 데드레터도 지표도 없어 계약이 어긋나도 스트림·PEL은 정상으로 보인다. 실제로 `detected_at` 포맷 불일치로 알림 8건이 전량 유실된 뒤에야 DB 0행으로 발견됐다(#208). 개선안: 폐기 전용 스트림 적재 또는 폐기 건수 메트릭 노출 |
| 13 | 컨슈머 그룹에 죽은 컨슈머 누적 | `AnomalyStreamProperties` | 컨슈머 이름이 호스트명이라 배포마다 새 이름이 등록되고 옛 항목은 남는다(dev 기준 7개). 각 컨슈머의 PEL이 비어 있어 유실은 없지만 `XINFO CONSUMERS` 판독이 어려워진다. 개선안: 기동 시 idle이 충분히 큰 컨슈머 `XGROUP DELCONSUMER` |
| 14 | 탈퇴한 대상자의 알림에 평가 UI가 보인다 | `NotificationReader` | 목록의 `feedbackEligible`은 `eventType`만 본다. 대상자가 탈퇴하면 `anomaly_detections`는 지워지는데 `notifications`는 남아(#149) 평가 UI는 뜨고 제출하면 E9009로 거부된다. 해소하려면 목록 쿼리에서도 탐지 행 존재를 확인해야 하는데 페이지당 조회가 하나 더 는다. #149(탈퇴 시 notifications 미삭제)를 해결하면 함께 사라진다 |
| 15 | 번호 재할당 계정 탈취 | `LocalAuthService.resetPassword` | 로그인 ID·비밀번호 재설정이 모두 전화번호 기반이라, 해지 후 재할당된 번호의 새 주인이 SMS 재설정으로 이전 계정에 접근 가능. 새 주인은 가입도 E3006으로 막힘. 장기 미접속 계정 정리나 추가 본인확인 필요 |
| 16 | 응급호출 알림 토글이 아무 효과 없음 | `NotificationSetting.emergencyCallEnabled` | 보호자별로 저장·조회만 되고 발송 경로 어디에서도 읽지 않는다. 응급호출 발송 기능이 붙을 때 `filterRecipients(settings -> settings.emergencyCall().enabled())`로 연결해야 한다 |
| 17 | 운영 API 인증이 공유 키 하나 | `AdminApiKeyFilter` | `/api/v1/admin/**`는 `X-Admin-Key` == `ADMIN_API_KEY` 상수 시간 비교만 한다. 운영자 식별·감사 로그·키 회전이 없고, 키가 비어 있으면 전부 401. 운영자 계정·권한이 필요해지면 교체 |
| 18 | `NetworkUtils.getClientIp`가 조작 가능한 IP를 믿음 | `NetworkUtils` | X-Forwarded-For 첫 값을 우선한다. nginx는 `$proxy_add_x_forwarded_for`로 덧붙이기만 해서 첫 값은 클라이언트가 정한다. IP 기반 제한에는 `getProxiedClientIp`(X-Real-IP)를 쓴다. `AuthenticationExceptionHandler`에도 같은 로직이 중복돼 있다 |
| 19 | 웹 문의 목록 페이징 없음 | `GET /api/v1/site/inquiries` | 전체를 내려주고 프론트가 6개씩 나눈다. 건수가 늘면 커서 페이징으로 바꿔야 한다 |
| 20 | 엔티티 TODO 인덱스는 후보 목록 | `Inquiry`, `DesignatedAvatar`, `RefreshToken`, `Notification`, `AnomalyDetection`, `GpsHistory`, `SafeZone`, `SiteInquiry` | 데이터가 적어 seq scan으로 충분하므로 미리 만들지 않는다. dev 보조 인덱스는 `idx_gps_ward_recorded`·`idx_member_withdrawal_member_key`뿐이고, UNIQUE 키(`token`, `notification_key`, `inquiry_key` 등)는 제약 인덱스가 있다. 느린 쿼리(`log_slow_query` 500ms·Performance Insights)가 확인되면 해당 TODO를 마이그레이션으로 만든다(큰 테이블은 `CONCURRENTLY` + `executeInTransaction=false`). 먼저 지켜볼 후보: 알림함 커서 조회 `notifications(recipient_member_key, notification_id DESC)`. 단, `safe_zones(ward_member_key)`는 GPS 1건마다 조회되므로 우선 후보. `WardDeviceToken`·`MemberWithdrawal`의 TODO는 이미 UNIQUE·보조 인덱스가 있어 지워도 된다 |
| 22 | prod Flyway 미적용 | `application-prod.yml` | `spring.flyway.enabled: false`. prod 전환 시 dev처럼 스키마를 대조하고 `baseline-on-migrate`·`baseline-version: 1`을 켠다 |
| 23 | **`gps_histories` 월 파티션 수동 생성 (긴급)** | dev RDS | `recorded_at` 월별 RANGE 파티션이 2026_08~2026_11 + default까지만 있다. **2026-12-01부터 모든 GPS가 default 파티션으로 들어간다.** default에 행이 쌓인 뒤에는 해당 월 파티션을 만들 때 행 이동이 필요해진다. 월 파티션 생성 수단(마이그레이션·배치·pg_partman) 결정 필요 |
| 24 | **탈퇴한 대상자의 Device Token이 최대 30일 유효** | `MemberWithdrawalManager` | 탈퇴 시 `ward_device_tokens` 행은 지우지만 `deviceToken:{token}` Redis 캐시는 비우지 않는다. `DeviceTokenAuthFilter`가 캐시를 먼저 보므로 TTL(30일) 동안 탈퇴 회원 키로 GPS 수신이 통과한다 |
| 25 | SMS 인증코드 오답 횟수 제한 없음 | `PhoneVerificationService.verifyCode` | 발송은 번호당 10분 5회(`SmsRateLimitValidator`)로 막지만, 검증은 틀려도 `phone:verify:{phone}` 키를 지우거나 횟수를 세지 않는다. 6자리 코드 하나를 TTL 5분 동안 무제한으로 맞혀 볼 수 있다. 개선안: 오답 N회 시 코드 삭제 |
| 26 | 로컬 로그인 시도 제한 없음 | `LocalAuthService` | 계정·IP별 실패 횟수 제한·잠금이 없다 |
| 27 | Refresh Token에 jti 없음 | `JwtGenerator` | 같은 회원·같은 초에 두 번 발급하면 토큰 문자열이 같아 `refresh_token.token` UNIQUE 위반 가능(코드상 추정, 재현 안 함) |
| 28 | 비밀번호 재설정이 기존 세션을 끊지 않는다 | `LocalAuthService.resetPassword` | 재설정 후에도 기존 refresh token이 유효하다. #15(번호 재할당)와 겹치면 위험이 커진다 |
| 29 | 미사용 ErrorType | `ErrorType` | E5004, E5005(`EXPIRED_CARE_REQUEST`), E5010, E5012, E9002(`@Version` 없음), E9003(`FcmToken` VO 미사용) |
| 30 | `notification_settings`의 `*_sensitivity` 컬럼 잔존 | V{n} 마이그레이션 | 엔티티에서 제거된 민감도 컬럼 3개가 DB에 남아 있다. expand → contract 2단계 중 DROP COLUMN 단계가 남음 |
| 31 | 설정 파일 잔재·불일치 | `application-dev.yml`, `application-local.yml`, `application-prod.yml` | dev의 `alert:` 블록은 읽는 코드가 없다. local의 최상위 `redis.host`는 `spring.data.redis`가 아니라 효과가 없다. prod yml은 dev보다 크게 뒤처져 있다(#22 포함) |
| 32 | 이상탐지 스트림 지표에 알림 규칙 없음 | `monitoring/grafana/provisioning/alerting` | `anomaly_stream_subscription_active`·`anomaly_stream_poll_consecutive_errors` 등을 노출하지만 Grafana 규칙은 ERROR 로그 하나뿐이다. 폴링 에러 핸들러가 ERROR를 남기지 않으므로 구독 중단은 규칙 없이는 감지되지 않는다 |
| 33 | 인프라 잔재 | AWS | `k6-sg`(22 ← 0.0.0.0/0, 앱·RDS 허용), 옛 `recaring-app-sg`(22 ← 0.0.0.0/0), `nginx-sg` 22 규칙, `/ecs/redis` 로그 그룹, `recaring-redis` ECR, EC2 Prometheus의 `redis` 잡, 엔진 이미지 `:latest`, `GRAFANA_DB_PASSWORD` 평문 env. RDS 백업 1일·Single-AZ·PI 꺼짐. ElastiCache 노드 1개(장애조치 없음) |
