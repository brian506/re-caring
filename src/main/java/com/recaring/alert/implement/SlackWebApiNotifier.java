package com.recaring.alert.implement;

import com.fasterxml.jackson.databind.JsonNode;
import com.recaring.alert.vo.AlertItem;
import com.recaring.alert.vo.GpsRecoveryResult;
import com.recaring.alert.vo.GpsVerdict;
import com.recaring.support.exception.AppException;
import com.recaring.support.exception.ErrorType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
@Profile("dev")
@RequiredArgsConstructor
public class SlackWebApiNotifier implements SlackAlertNotifier {

    private static final String SLACK_POST_MESSAGE_URL = "https://slack.com/api/chat.postMessage";

    private final RestClient restClient;

    @Value("${alert.slack.bot-token}")
    private String botToken;

    @Value("${alert.slack.channel-id}")
    private String channelId;

    @Override
    public String sendInitialAlert(AlertItem alert) {
        log.info("[Slack 알림 : 초기 발송]: alertName={} | fingerprint={}", alert.alertName(), alert.fingerprint());
        try {
            Map<String, Object> body = Map.of(
                    "channel", channelId,
                    "blocks", List.of(
                            Map.of(
                                    "type", "section",
                                    "text", Map.of(
                                            "type", "mrkdwn",
                                            "text", String.format(
                                                    "*[%s] %s*\n%s\nStarted: %s\n\n_3개 에이전트가 병렬로 조사 중입니다..._",
                                                    alert.severity(),
                                                    alert.alertName(),
                                                    alert.message(),
                                                    alert.startsAt()
                                            )
                                    )
                            )
                    )
            );

            JsonNode response = restClient.post()
                    .uri(SLACK_POST_MESSAGE_URL)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + botToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(JsonNode.class);

            if (response == null || !response.path("ok").asBoolean()) {
                String slackError = response != null ? response.path("error").asText() : "null response";
                log.warn("[Slack 알림 : 전송 실패]: alertName={} | error={}", alert.alertName(), slackError);
                throw new AppException(ErrorType.ALERT_SLACK_SEND_FAILED);
            }

            String threadTs = response.path("ts").asText();
            log.info("[Slack 알림 : 발송 완료]: threadTs={}", threadTs);
            return threadTs;

        } catch (AppException e) {
            throw e;
        } catch (Exception e) {
            log.error("[Slack 알림 : 전송 오류]: alertName={} | error={}", alert.alertName(), e.getMessage());
            throw new AppException(ErrorType.ALERT_SLACK_SEND_FAILED);
        }
    }

    @Override
    public void sendGpsResolution(String threadTs, GpsVerdict verdict, GpsRecoveryResult recovery) {
        log.info("[Slack 알림 : GPS 해결 발송]: threadTs={} | verdictType={}", threadTs, verdict.verdictType());

        String wardKeysText = verdict.affectedWardKeys().isEmpty()
                ? "확인 필요"
                : String.join(", ", verdict.affectedWardKeys());

        String recoveryText = recovery.recovered()
                ? String.format("*자동 복구 완료*: %s", recovery.actionTaken())
                : String.format("*수동 조치 필요*: %s", recovery.actionTaken());

        String message = String.format(
                "*GPS 조사 완료* — `%s`\n\n*판정*: %s\n*영향 피보호자*: %s\n%s\n\n*보호자 안내*: %s",
                verdict.verdictType(),
                verdict.rootCause(),
                wardKeysText,
                recoveryText,
                verdict.caregiverMessage()
        );

        sendThreadReply(threadTs, message);
    }

    @Override
    public void sendError(String threadTs, String message) {
        log.warn("[Slack 알림 : 오류 발송]: threadTs={}", threadTs);
        sendThreadReply(threadTs, "*GPS 조사 오류*\n" + message);
    }

    private void sendThreadReply(String threadTs, String text) {
        try {
            Map<String, Object> body = Map.of(
                    "channel", channelId,
                    "thread_ts", threadTs,
                    "text", text
            );

            restClient.post()
                    .uri(SLACK_POST_MESSAGE_URL)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + botToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();

        } catch (Exception e) {
            log.warn("[Slack 알림 : 스레드 전송 실패]: threadTs={} | error={}", threadTs, e.getMessage());
        }
    }
}
