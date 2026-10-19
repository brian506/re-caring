package com.recaring.siteinquiry.implement;

import com.recaring.siteinquiry.vo.SiteInquiryInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.net.URI;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@Slf4j
@Component
public class SiteInquiryOperatorNotifier {

    private static final DateTimeFormatter CREATED_AT_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final RestClient slackWebhookRestClient;
    private final String webhookUrl;

    public SiteInquiryOperatorNotifier(
            @Qualifier("slackWebhookRestClient") RestClient slackWebhookRestClient,
            @Value("${slack.inquiry-webhook-url:}") String webhookUrl
    ) {
        this.slackWebhookRestClient = slackWebhookRestClient;
        this.webhookUrl = webhookUrl;
    }

    @Async("broadcastExecutor")
    public void notifySubmitted(SiteInquiryInfo inquiry) {
        log.info("[WebInquiry : Received]: inquiryKey={} | category={} | secret={} | createdAt={}",
                inquiry.inquiryKey(), inquiry.category(), inquiry.secret(), inquiry.createdAt());
        if (webhookUrl.isBlank()) {
            log.info("[WebInquiry : SlackSendSkipped]: inquiryKey={} | reason=webhook url not configured", inquiry.inquiryKey());
            return;
        }
        try {
            slackWebhookRestClient.post()
                    .uri(URI.create(webhookUrl))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("text", message(inquiry)))
                    .retrieve()
                    .toBodilessEntity();
            log.info("[WebInquiry : SlackSent]: inquiryKey={}", inquiry.inquiryKey());
        } catch (RestClientResponseException e) {
            log.warn("[WebInquiry : SlackResponseError]: inquiryKey={} | status={} | body={}",
                    inquiry.inquiryKey(), e.getStatusCode(), e.getResponseBodyAsString());
        } catch (ResourceAccessException e) {
            log.warn("[WebInquiry : SlackConnectionFailed]: inquiryKey={} | cause={}",
                    inquiry.inquiryKey(), e.getMostSpecificCause().getClass().getSimpleName());
        }
    }

    private String message(SiteInquiryInfo inquiry) {
        return """
                :globe_with_meridians: *새 웹사이트 문의가 접수되었습니다*
                • 분류: %s
                • 제목: %s%s
                • 접수 시각: %s
                • 문의 키: `%s`""".formatted(
                inquiry.category().getDescription(),
                escape(inquiry.title()),
                inquiry.secret() ? " (비밀글)" : "",
                inquiry.createdAt().format(CREATED_AT_FORMAT),
                inquiry.inquiryKey());
    }

    private String escape(String text) {
        return text.replaceAll("\\R", " ")
                .replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
