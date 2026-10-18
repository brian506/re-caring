package com.recaring.inquiry.implement;

import com.recaring.inquiry.vo.InquiryInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.time.format.DateTimeFormatter;
import java.util.Map;

@Slf4j
@Component
public class InquiryOperatorNotifier {

    private static final DateTimeFormatter CREATED_AT_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final RestClient slackWebhookRestClient;
    private final String webhookUrl;

    public InquiryOperatorNotifier(
            @Qualifier("slackWebhookRestClient") RestClient slackWebhookRestClient,
            @Value("${slack.inquiry-webhook-url:}") String webhookUrl
    ) {
        this.slackWebhookRestClient = slackWebhookRestClient;
        this.webhookUrl = webhookUrl;
    }

    @Async("broadcastExecutor")
    public void notifySubmitted(InquiryInfo inquiry) {
        log.info("[1:1 문의 : 신규 접수]: inquiryKey={} | memberKey={} | category={} | createdAt={}",
                inquiry.inquiryKey(), inquiry.memberKey(), inquiry.category(), inquiry.createdAt());
        if (webhookUrl.isBlank()) {
            log.info("[1:1 문의 : Slack 발송 생략]: inquiryKey={} | reason=webhook url not configured", inquiry.inquiryKey());
            return;
        }
        try {
            slackWebhookRestClient.post()
                    .uri(webhookUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("text", message(inquiry)))
                    .retrieve()
                    .toBodilessEntity();
            log.info("[1:1 문의 : Slack 발송 완료]: inquiryKey={}", inquiry.inquiryKey());
        } catch (RestClientResponseException e) {
            log.error("[1:1 문의 : Slack 응답 오류]: inquiryKey={} | status={} | body={}",
                    inquiry.inquiryKey(), e.getStatusCode(), e.getResponseBodyAsString());
        } catch (ResourceAccessException e) {
            log.error("[1:1 문의 : Slack 연결 실패]: inquiryKey={} | error={}", inquiry.inquiryKey(), e.getMessage());
        }
    }

    private String message(InquiryInfo inquiry) {
        return """
                :envelope: *새 1:1 문의가 접수되었습니다*
                • 분류: %s
                • 제목: %s
                • 접수 시각: %s
                • 문의 키: `%s`""".formatted(
                inquiry.category().getDescription(),
                escape(inquiry.title()),
                inquiry.createdAt().format(CREATED_AT_FORMAT),
                inquiry.inquiryKey());
    }

    private String escape(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
