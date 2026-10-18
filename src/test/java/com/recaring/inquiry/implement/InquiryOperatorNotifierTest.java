package com.recaring.inquiry.implement;

import com.recaring.inquiry.fixture.InquiryFixture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
@DisplayName("문의 운영자 알림 단위 테스트")
class InquiryOperatorNotifierTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private RestClient slackWebhookRestClient;

    @Test
    @DisplayName("접수 알림 메시지에 분류 설명·제목·접수 시각·문의 키가 들어간다")
    void message_contains_category_title_submitted_at_and_key() {
        InquiryOperatorNotifier notifier = notifierWith(InquiryFixture.SLACK_WEBHOOK_URL);

        notifier.notifySubmitted(InquiryFixture.createSubmittedInfo(InquiryFixture.TITLE));

        assertThat(sentText())
                .contains(InquiryFixture.CATEGORY_DESCRIPTION)
                .contains(InquiryFixture.TITLE)
                .contains(InquiryFixture.SUBMITTED_AT_TEXT)
                .contains(InquiryFixture.INQUIRY_KEY);
    }

    @Test
    @DisplayName("접수 알림 메시지에 문의 본문은 들어가지 않는다")
    void message_excludes_inquiry_content() {
        InquiryOperatorNotifier notifier = notifierWith(InquiryFixture.SLACK_WEBHOOK_URL);

        notifier.notifySubmitted(InquiryFixture.createSubmittedInfo(InquiryFixture.TITLE));

        assertThat(sentText()).doesNotContain(InquiryFixture.CONTENT);
    }

    @Test
    @DisplayName("제목의 <, >, &는 Slack 이스케이프 문자로 바뀌어 채널 전체 호출이 그대로 전달되지 않는다")
    void message_escapes_slack_control_characters_in_title() {
        InquiryOperatorNotifier notifier = notifierWith(InquiryFixture.SLACK_WEBHOOK_URL);

        notifier.notifySubmitted(InquiryFixture.createSubmittedInfo(InquiryFixture.MENTION_TITLE));

        assertThat(sentText())
                .contains(InquiryFixture.ESCAPED_MENTION_TITLE)
                .doesNotContain("<!channel>");
    }

    @Test
    @DisplayName("웹훅 주소가 비어 있으면 Slack을 호출하지 않는다")
    void skips_slack_when_webhook_url_is_blank() {
        InquiryOperatorNotifier notifier = notifierWith("");

        notifier.notifySubmitted(InquiryFixture.createSubmittedInfo(InquiryFixture.TITLE));

        then(slackWebhookRestClient).should(never()).post();
    }

    @Test
    @DisplayName("Slack이 오류 응답을 줘도 예외가 밖으로 나가지 않는다")
    void does_not_propagate_slack_error_response() {
        InquiryOperatorNotifier notifier = notifierWith(InquiryFixture.SLACK_WEBHOOK_URL);
        given(requestBody().body(any(Map.class)).retrieve().toBodilessEntity())
                .willThrow(HttpClientErrorException.create(HttpStatus.NOT_FOUND, "no_service", null, null, null));

        assertThatCode(() -> notifier.notifySubmitted(InquiryFixture.createSubmittedInfo(InquiryFixture.TITLE)))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Slack 연결에 실패해도 예외가 밖으로 나가지 않는다")
    void does_not_propagate_slack_connection_failure() {
        InquiryOperatorNotifier notifier = notifierWith(InquiryFixture.SLACK_WEBHOOK_URL);
        given(requestBody().body(any(Map.class)).retrieve().toBodilessEntity())
                .willThrow(new ResourceAccessException("Connect timed out"));

        assertThatCode(() -> notifier.notifySubmitted(InquiryFixture.createSubmittedInfo(InquiryFixture.TITLE)))
                .doesNotThrowAnyException();
    }

    private InquiryOperatorNotifier notifierWith(String webhookUrl) {
        return new InquiryOperatorNotifier(slackWebhookRestClient, webhookUrl);
    }

    private RestClient.RequestBodySpec requestBody() {
        return slackWebhookRestClient.post()
                .uri(InquiryFixture.SLACK_WEBHOOK_URL)
                .contentType(MediaType.APPLICATION_JSON);
    }

    @SuppressWarnings("unchecked")
    private String sentText() {
        ArgumentCaptor<Object> body = ArgumentCaptor.forClass(Object.class);
        then(requestBody()).should().body(body.capture());
        return ((Map<String, String>) body.getValue()).get("text");
    }
}
