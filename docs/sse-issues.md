# SSE 실시간 위치 스트림 — 주제별 이슈 정리

> 실시간 위치(`GET /api/v1/location/stream/{wardKey}`)에서 겪은 문제들을 원인 계층별로 나눠 정리한다.
> 개별 장애의 조사 과정은 [troubleshooting-sse-realtime-location.md](./troubleshooting-sse-realtime-location.md) 참고.

## 0. 왜 SSE에서만 문제가 반복되는가

REST 응답은 **시작과 끝이 있고, 끝난 뒤에 응답이 확정된다.** 그래서 예외가 나면 상태 코드를 바꿀 수 있고, 프록시는 다 읽어서 한 번에 내보내면 되고, 어느 인스턴스가 처리하든 상관없다.

SSE는 이 전제가 전부 깨진다.

| 전제 | REST | SSE |
|---|---|---|
| 응답이 언제 확정되나 | 핸들러가 끝난 뒤 | **첫 이벤트를 쓰는 순간 committed** |
| 예외 발생 시 | 상태 코드·바디 교체 가능 | 이미 `200 text/event-stream` → 교체 불가 |
| 요청 스레드 | 응답 끝날 때까지 유지 | 컨트롤러 리턴 즉시 반납, 이후는 별도 스레드 |
| 프록시 | 다 읽고 내보내는 게 최적 | 다 읽으려 하면 **영원히 안 끝남** |
| 서버 인스턴스 | 요청 단위라 무관 | 연결이 특정 인스턴스에 **5분간 고정** |

아래 1~5번은 전부 이 표의 한 줄씩이 원인이다.

## 2. 예외 처리 — 서로 다른 두 문제

**이 둘은 자주 헷갈리는데 원인 계층이 완전히 다르다.**

| | 2-1. 연결 수립 **전** | 2-2. 연결 수립 **후** |
|---|---|---|
| 시점 | `SseEmitter` 생성 전 | emitter 반환 후, 폴링 루프 안 |
| 응답 상태 | 아직 동기 REST 응답 (미확정) | 이미 committed (`200 text/event-stream`) |
| `@ControllerAdvice` | **도달함** | **절대 도달 못 함** |
| 원인 | `produces` 때문에 콘텐츠 협상 실패 | 비동기라 예외를 올릴 스택이 없음 |
| 해결 | `@Order` + Content-Type 고정 | 루프에서 직접 try/catch, 완료 방식 주의 |

### 2-1. 연결 수립 전 — `@Order` 문제 (콘텐츠 협상)

`validateCaregiverAccess()`는 `SseEmitter`를 만들기 **전에** 실행된다. 이 시점의 응답은 아직 평범한 동기 REST 응답이고 스트림은 시작조차 안 했다. 즉 **"연결됐으니 REST가 아니라서" 생기는 문제가 아니다.**

진짜 원인은 `produces = TEXT_EVENT_STREAM_VALUE` 선언이다.

```
1. 엔드포인트가 text/event-stream만 생산한다고 선언 → 클라이언트도 Accept: text/event-stream 전송
2. AppException(403) 발생 → 전역 ApiControllerAdvice가 ApiResponse(JSON) 반환
3. Content-Type 미지정 → Spring이 콘텐츠 협상 수행 → 후보가 text/event-stream뿐
4. ApiResponse를 text/event-stream으로 쓸 컨버터 없음
   → HttpMediaTypeNotAcceptableException: No acceptable representation
5. 예외 핸들러 자체가 실패 → 컨테이너 에러 경로 → 500 + Content-Length: 0
```

`LocationControllerAdvice`는 정확히 이걸 막으려고 `.contentType(APPLICATION_JSON)`으로 타입을 못 박아 협상을 건너뛰게 만들어져 있었다. 그런데 **선택되지를 않았다.**

`ExceptionHandlerExceptionResolver`는 모든 advice 빈을 정렬해두고, 예외가 나면 **(a) 해당 컨트롤러에 적용되고 (b) 그 예외 타입을 처리하는 첫 번째** advice를 쓴다. 첫 매치에서 끝이다. 여기서 함정은 **`assignableTypes`가 "후보 여부"만 정하고 "우선순위"는 올려주지 않는다**는 점이다. 범위가 좁다고 먼저 뽑히지 않는다. 둘 다 `@Order`가 없어 나란히 `LOWEST_PRECEDENCE`였고, 동률이라 빈 스캔 순서로 갈려 전역 쪽이 이겼다.

프로덕션 로그가 그대로 보여줬다.

```
WARN ExceptionHandlerExceptionResolver : Failure in @ExceptionHandler ApiControllerAdvice#handleAppException
org.springframework.web.HttpMediaTypeNotAcceptableException: No acceptable representation
```

**해결**: `@Order(Ordered.HIGHEST_PRECEDENCE)`. `assignableTypes` 덕분에 다른 컨트롤러에는 영향이 없다.

**실제 피해**: 케어 관계가 끊긴 보호자가 스트림을 열면 이유를 알 수 없는 500 빈 응답을 받는다. 앱이 "서버 고장"과 "권한 없음"을 구분할 수 없다. 고치면 `403 + E6001` JSON이 정상적으로 내려간다.

> 교훈: `produces`로 미디어 타입을 좁힌 엔드포인트는 **에러 응답까지 그 타입으로 협상된다.** SSE·파일 다운로드 등 특수 타입 엔드포인트는 전용 advice + 명시적 Content-Type이 필요하고, 그 advice에는 반드시 `@Order`를 준다.

### 2-2. 연결 수립 후 — 진짜 "비동기라서" 생기는 문제

emitter를 반환하는 순간 `startAsync()`가 걸리고, 첫 이벤트를 쓰면 응답 헤더가 나가며 committed 된다. 컨트롤러 메서드는 즉시 리턴하고 요청 스레드는 반납된다. **이후 폴링 스레드에서 나는 예외는 올려보낼 컨트롤러 호출 스택 자체가 없어서 `@ControllerAdvice`가 절대 잡지 못한다.** 그래서 `pollLoop`가 직접 try/catch 한다.

여기서 세 가지 함정이 연달아 터졌다.

**① `completeWithError()`가 에러 캐스케이드를 만든다** (#127, #128)

클라이언트가 끊으면 `send()`가 IOException을 던진다. 여기서 `completeWithError(e)`를 부르면 Tomcat이 error dispatch를 돌리는데, 응답이 이미 committed(200 + SSE 헤더)라 에러 페이지를 쓸 수 없다.

```
"Unable to handle Spring Security Exception (response committed)"
AuthorizationDeniedException: Access Denied
```

정상적인 클라이언트 이탈에는 `complete()`를 써서 error dispatch를 아예 유발하지 않는 게 맞다. `onError` 콜백에서도 마찬가지다 — 이미 에러가 신호된 뒤에 호출되므로 거기서 다시 `completeWithError()`를 부르면 재유발된다.

**② async 재디스패치에서 인증이 다시 평가된다** (#129)

SSE 요청이 완료·타임아웃·에러로 끝나면 컨테이너가 필터 체인으로 **ASYNC 디스패치**를 다시 돌린다. Spring Security의 `AuthorizationFilter`는 기본적으로 모든 디스패치 타입을 필터링하는데, 이 앱은 STATELESS라 재디스패치 시 SecurityContext가 비어 있다. 그래서 원래 경로(`/api/v1/location/stream/{wardKey}`)에 대해 인증 없이 인가가 재평가되고 `Access Denied`가 난다. 응답은 이미 committed라 에러 페이지도 못 쓴다.

```java
.dispatcherTypeMatchers(DispatcherType.ASYNC, DispatcherType.ERROR).permitAll()
```

최초 REQUEST 디스패치는 여전히 전체 JWT 인증·인가를 거치고, **컨테이너 내부 재디스패치만** 면제된다.

**③ `AsyncRequestNotUsableException`은 advice까지 올라온다**

클라이언트가 이미 끊긴 상태에서 쓰기를 시도하면 Spring async 인프라가 이 예외를 error dispatch로 advice까지 올려보낸다. 이때도 응답은 이미 `text/event-stream`이라 JSON을 쓸 수 없다. 정상적인 이탈이므로 **바디 없이 debug 로그만 남기는 전용 핸들러**가 필요하다.

```java
@ExceptionHandler(AsyncRequestNotUsableException.class)
public void handleAsyncRequestNotUsable(AsyncRequestNotUsableException e) {
    log.debug("[SSE 이벤트 : 연결 종료]: message={}", e.getMessage());
}
```

> 정리하면 "예외 처리가 씹힌다"는 표현에는 두 의미가 섞여 있다. **advice에 도달조차 못 하는 것**(2-2)과, **도달했는데 응답을 쓸 수 없어 500으로 깨지는 것**(2-1). @Order는 후자다.

---

---

## 4. 멀티 인스턴스에서는 어떻게 되는가

### 현재 구성

`recaring-service`는 `desiredCount = 1`이다. 하지만 **"지금은 1대니까 상관없다"는 완전히 참이 아니다.**

```
maximumPercent = 200, minimumHealthyPercent = 100
```

롤링 배포 중에는 신·구 태스크가 **동시에 2개** 뜬다. 그동안 nginx는 CloudMap DNS(`spring-app.recaring.local`)로 두 IP를 라운드로빈한다. 즉 **배포할 때마다 짧은 멀티 인스턴스 구간이 생긴다.**

### 현재 폴링 모델은 멀티 인스턴스에 강하다

상태(최신 GPS)가 **Redis에 있고 각 인스턴스가 각자 읽기** 때문이다. 구독자가 어느 인스턴스에 붙든, GPS 업로드가 어느 인스턴스로 가든 동작한다. 인스턴스 간 통신이 전혀 필요 없다.

### 반면 push(map) 모델은 그대로는 깨진다

emitter map은 **JVM 로컬 메모리**다. GPS POST가 인스턴스 A로 가고 구독자가 인스턴스 B에 붙어 있으면, A의 `broadcast()`는 B의 구독자를 알지 못해 **이벤트가 영영 전달되지 않는다.** 인스턴스가 늘수록 전달 실패 확률이 올라간다(2대면 절반).

해결하려면 인스턴스 간 팬아웃 계층이 필요하다 — Redis Pub/Sub이 가장 가볍고, 그다음이 별도 브로커다.

### 그 외 멀티 인스턴스 고려사항

- **sticky session은 불필요하다.** SSE는 하나의 연결을 계속 유지하는 것이라 연결만 끊기지 않으면 어느 인스턴스든 상관없다. 단 재연결 시 다른 인스턴스로 붙을 수 있으므로 **인스턴스 로컬 상태에 의존하면 안 된다.**
- **배포하면 모든 SSE 연결이 끊긴다.** `graceful shutdown 25초`가 있어도 결국 끊긴다. 클라이언트 재연결(3번)이 없으면 배포할 때마다 실시간 기능이 죽은 채로 남는다.
- **연결 수 상한**은 인스턴스가 아니라 nginx에서 먼저 걸린다. `worker_connections 1024` × 워커 2개, SSE는 연결당 클라이언트+업스트림 2슬롯을 점유하므로 동시 스트림 상한이 대략 1,000개다.

---

## 5. polling과 push(map 구조)의 차이

이 프로젝트는 `hotfix[#121]`에서 broadcast(push) → polling으로 바꿨다. 두 모델의 성질이 정확히 반대라 상황에 따라 선택이 갈린다.

| 항목 | **polling** (현재) | **push / emitter map** (이전) |
|---|---|---|
| 전달 방식 | 연결마다 10초 주기 Redis GET, 변경 시 전송 | GPS 수신 시 map을 돌며 즉시 전송 |
| 지연 | 최대 폴링 주기(10초) | 즉시 |
| Redis 부하 | **연결 수에 선형** (1000 연결 = 초당 100 GET) | GPS 수신 수에만 비례 |
| 서버 상태 | 없음 (Redis가 유일한 진실) | JVM 메모리에 emitter map |
| 멀티 인스턴스 | **그대로 동작** | 인스턴스 간 팬아웃 필요 |
| 스레드 | 연결당 1개(가상 스레드)를 5분 점유 | 연결당 0개, 이벤트 스레드에서 팬아웃 |
| 실패 격리 | 연결마다 독립 — 한 연결이 죽어도 무관 | 한 emitter의 느린 `send()`가 뒤 구독자를 지연 |
| heartbeat | 폴링 루프에 자연스럽게 포함 | 별도 스케줄러 필요 |
| 구현 복잡도 | 낮음 | 중간 (동시성·정리 로직 필요) |

### 현재 폴링 모델의 실제 낭비

GPS는 30초 주기인데 폴링은 10초 주기다. **3번 중 2번은 변경이 없어 heartbeat만 보낸다.** Redis GET을 3배로 하고 있는 셈이다. 폴링 주기를 GPS 주기에 맞추면 낭비는 줄지만 지연이 커진다.

### 절충안: Redis Pub/Sub + 로컬 emitter map

두 모델의 장점을 합칠 수 있다.

```
GPS 저장 → Redis 채널에 publish
         → 모든 인스턴스가 구독 중
         → 각 인스턴스가 자기 로컬 구독자에게 즉시 push
```

- 지연 0, Redis 부하는 **이벤트 수에만** 비례(연결 수와 무관), 멀티 인스턴스 안전.
- **단점**: Pub/Sub은 at-most-once라 순간 유실 가능(단 "최신 위치"라 다음 이벤트가 곧 덮어쓴다). Lettuce pub/sub 연결의 생명주기 관리가 필요하고, emitter map 동시성 처리가 다시 들어온다. 무엇보다 **#121에서 push를 걷어낸 이유**(팬아웃 중 블로킹·스레드 문제, #111/#113의 가상 스레드 병렬화·fire-and-forget 시도가 #114에서 롤백된 이력)를 먼저 정확히 파악하지 않으면 같은 장애를 반복한다.

### 판단 기준

| 상황 | 선택 |
|---|---|
| 연결 수가 적고(수백) 지연 10초가 허용됨 | **polling 유지** — 가장 단순하고 멀티 인스턴스에 안전 |
| 지연이 중요하거나 연결 수가 Redis 부하로 이어짐 | **Pub/Sub + 로컬 map** |
| 단일 인스턴스가 확정이고 지연이 중요 | 순수 push(map) — 단 배포 중 2태스크 구간 주의 |

---

## 부록: 배포 파이프라인의 공백

이번 nginx 수정은 **6월 19일에 이미 커밋돼 있었는데 6주간 배포되지 않았다.**

- 해당 커밋(`7b64fda`)이 PR이 아니라 develop에 **직접 푸시**됐고, 워크플로에 `push` 트리거가 없어 아무것도 실행되지 않았다.
- 이후 PR 머지에서는 워크플로가 돌았지만 `dorny/paths-filter`가 **그 PR의 diff**만 보므로 매번 "nginx 변경 없음"으로 판정해 건너뛰었다.

즉 이 파이프라인은 "배포된 이미지와 저장소의 차이"가 아니라 "이번 PR의 변경분"을 기준으로 판단한다. **PR 밖으로 들어온 인프라 변경은 영원히 배포되지 않으며, 조용히 실패한다.**

`workflow_dispatch`(수동 실행)는 조건상 nginx를 무조건 빌드하므로 즉시 조치는 수동 실행 한 번이다. 재발 방지는 `push: branches: [develop]` 트리거 추가, paths-filter 기준을 마지막 배포 커밋으로 변경, 또는 nginx는 변경 감지 없이 매 배포 빌드 중 택일한다.
