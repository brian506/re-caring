# 로그 Category 고정 어휘

`[Category : Detail]`의 Category는 아래 값만 쓴다. 새 기능에 필요하면 이 표에 먼저 추가한다 (CLAUDE.md 로깅 규칙).
도메인은 logger 패키지에서 따로 뽑히므로 도메인명만으로 된 Category(`Location`, `Care`)나 도메인 접두어(`LocationGpsCache`)는 쓰지 않는다. `CareInvitationAlert`, `MemberWithdrawal`처럼 기능 자체의 이름은 허용한다.

| Category | 의미 | 주요 위치 |
|---|---|---|
| `RequestApi` | API 요청 진입 | `SignAspect` |
| `ResponseApi` | API 응답 상태 | `SignAspect` |
| `SignIn` | 로그인 요청 진입 | `SignAspect` |
| `Authentication` | JWT/Device 인증 실패 401 | `AuthenticationExceptionHandler` |
| `AppException` | 비즈니스 예외 처리 (Detail `Handled`) | `AppExceptionLogger` (`ApiControllerAdvice`, `LocationControllerAdvice`에서 호출) |
| `UnhandledException` | 미처리 예외 500 | `ApiControllerAdvice` |
| `RequestValidation` | 요청 검증·바인딩·본문 해석 실패 400 | `ApiControllerAdvice` |
| `DataIntegrity` | DB 제약 위반 409 | `ApiControllerAdvice` |
| `OptimisticLock` | 낙관적 락 충돌 409 | `ApiControllerAdvice` |
| `ResponseWrite` | 응답 쓰기 중 클라이언트 연결 끊김 | `ApiControllerAdvice` |
| `SseEvent` | 위치 SSE 스트림 전송·종료 | `SseEmitterManager`, `ApiControllerAdvice` |
| `GpsCache` | 최신 GPS Redis 캐시 | `GpsLatestCacheManager`, `GpsLatestCacheListener` |
| `DetectionStream` | 엔진 입력 스트림(`gps-detection`) 발행 | `DetectionPublisher` |
| `AnomalyStream` | 이상탐지 결과 스트림(`anomaly-alerts`) 컨슈머 그룹·폴링·PEL·지표 | `RedisStreamConfig`, `AnomalyStreamErrorHandler`, `AnomalyPendingReclaimer`, `AnomalyStreamMetrics` |
| `AnomalyResult` | 이상탐지 결과 레코드 파싱·저장 | `AnomalyDetectionConsumer`, `AnomalyDetectionParser`, `AnomalyDetectionManager` |
| `AnomalyAlert` | 이상탐지 보호자 알림 발송·확인 | `AnomalyNotificationListener`, `NotificationAcknowledgementManager` |
| `SafeZoneAlert` | 안심존 진입·이탈 판정과 알림 | `SafeZoneDetectionListener`, `SafeZoneNotificationListener` |
| `BatteryAlert` | 배터리 임계값 도달 판정·상태·알림 | `BatteryDetectionListener`, `BatteryAlertStateManager`, `BatteryThresholdNotificationListener` |
| `LocationSignalLost` | 위치 수집 중단 감지와 알림 | `LocationSignalMonitor`, `LocationSignalNotificationListener` |
| `CareInvitationAlert` | 케어 초대 발송·수락 알림 | `CareInvitationNotificationListener` |
| `InquiryAnswerAlert` | 1:1 문의 답변 알림 | `InquiryNotificationListener` |
| `NotificationFeedback` | 알림 정확도 피드백 | `NotificationFeedbackManager` |
| `FcmSend` | FCM 푸시 전송 | `FirebaseFcmClient` |
| `CareInvitationResend` | 케어 초대 재전송 간격 제한 | `CareInvitationResendLimiter` |
| `CareRelationship` | 케어 관계 변경 | `CareRelationshipManager` |
| `MemberWithdrawal` | 회원 탈퇴 | `MemberWithdrawalManager` |
| `MemberProfile` | 회원 정보 변경 | `MemberWriter` |
| `SmsSend` | SMS 발송·발송 한도 | `SmsClient`, `SmsRateLimitValidator` |
| `PlaceSearch` | 카카오 장소 검색 호출 | `KakaoPlaceSearchClient` |
| `Inquiry` | 앱 1:1 문의 접수·운영자 Slack 알림 | `InquiryOperatorNotifier` |
| `WebInquiry` | 소개 사이트 문의 접수·Slack·한도·비밀글 | `SiteInquiry*` |

Detail은 결과를 나타내는 PascalCase 구로 자유롭게 쓰되, 같은 의미는 같은 단어로 맞춘다: `Received`, `Sent`, `Saved`, `Failed`, `SendFailed`, `ConnectionFailed`, `ResponseError`, `RateLimitExceeded`, `RetryExhausted`, `NoRecipients`, `Missing*`, `Unknown*`, `Invalid*`, `*Skipped`.
