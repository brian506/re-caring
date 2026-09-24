package com.recaring.notification.controller;

import com.recaring.location.dataaccess.entity.AnomalyDetection;
import com.recaring.location.dataaccess.repository.AnomalyDetectionRepository;
import com.recaring.location.fixture.LocationFixture;
import com.recaring.member.dataaccess.entity.Member;
import com.recaring.member.dataaccess.repository.MemberRepository;
import com.recaring.notification.dataaccess.entity.FeedbackAccuracy;
import com.recaring.notification.dataaccess.entity.FeedbackReason;
import com.recaring.notification.dataaccess.entity.Notification;
import com.recaring.notification.dataaccess.entity.NotificationFeedback;
import com.recaring.notification.dataaccess.repository.NotificationFeedbackRepository;
import com.recaring.notification.dataaccess.repository.NotificationRepository;
import com.recaring.notification.fixture.NotificationFixture;
import com.recaring.support.AbstractIntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.client.RestTestClient;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("알림 피드백 컨트롤러 HTTP 통합 테스트")
class NotificationFeedbackControllerTest extends AbstractIntegrationTest {

    private static final String COMMENT = "집에 계셨어요";

    @Autowired
    private MemberRepository memberRepository;
    @Autowired
    private NotificationRepository notificationRepository;
    @Autowired
    private NotificationFeedbackRepository notificationFeedbackRepository;
    @Autowired
    private AnomalyDetectionRepository anomalyDetectionRepository;

    private Member guardian;
    private AnomalyDetection detection;

    @BeforeEach
    void setUp() {
        guardian = memberRepository.save(NotificationFixture.createGuardian());
        detection = anomalyDetectionRepository.saveAndFlush(LocationFixture.createAnomalyDetection(
                NotificationFixture.ANOMALY_TYPE, NotificationFixture.ANOMALY_EVIDENCE));
    }

    @AfterEach
    void tearDown() {
        notificationFeedbackRepository.deleteAllInBatch();
        notificationRepository.deleteAllInBatch();
        anomalyDetectionRepository.deleteAllInBatch();
        memberRepository.deleteAllInBatch();
    }

    @Test
    @DisplayName("부정확 평가를 사유·의견과 함께 제출하면 탐지 기록과 연결해 저장한다")
    void submitFeedback_saves_inaccurate_answer_with_reason_and_comment() {
        Notification notification = saveAnomalyNotification(guardian.getMemberKey());

        submit(notification.getNotificationKey(), """
                {"accuracy": "INACCURATE", "reason": "GPS_INACCURATE", "comment": "%s"}
                """.formatted(COMMENT))
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.resultType").isEqualTo("SUCCESS");

        NotificationFeedback saved = onlyFeedback();
        assertThat(saved.getNotificationId()).isEqualTo(notification.getId());
        assertThat(saved.getAnomalyDetectionId()).isEqualTo(detection.getId());
        assertThat(saved.getAccuracy()).isEqualTo(FeedbackAccuracy.INACCURATE);
        assertThat(saved.getReason()).isEqualTo(FeedbackReason.GPS_INACCURATE);
        assertThat(saved.getComment()).isEqualTo(COMMENT);
    }

    @Test
    @DisplayName("정확 평가는 사유 없이 제출할 수 있다")
    void submitFeedback_accepts_accurate_answer_without_reason() {
        Notification notification = saveAnomalyNotification(guardian.getMemberKey());

        submit(notification.getNotificationKey(), """
                {"accuracy": "ACCURATE"}
                """)
                .expectStatus().isCreated();

        NotificationFeedback saved = onlyFeedback();
        assertThat(saved.getAccuracy()).isEqualTo(FeedbackAccuracy.ACCURATE);
        assertThat(saved.getReason()).isNull();
        assertThat(saved.getComment()).isNull();
    }

    @Test
    @DisplayName("부정확 평가에 사유가 없으면 거부한다")
    void submitFeedback_rejects_inaccurate_answer_without_reason() {
        Notification notification = saveAnomalyNotification(guardian.getMemberKey());

        submit(notification.getNotificationKey(), """
                {"accuracy": "INACCURATE"}
                """)
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.error.errorCode").isEqualTo("E9008");

        assertThat(notificationFeedbackRepository.findAll()).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"ACCURATE", "UNSURE"})
    @DisplayName("사유를 받지 않는 평가에 사유를 담아 보내면 거부한다")
    void submitFeedback_rejects_reason_on_answers_that_take_none(String accuracy) {
        Notification notification = saveAnomalyNotification(guardian.getMemberKey());

        submit(notification.getNotificationKey(), """
                {"accuracy": "%s", "reason": "USUAL_ROUTINE"}
                """.formatted(accuracy))
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.error.errorCode").isEqualTo("E9008");

        assertThat(notificationFeedbackRepository.findAll()).isEmpty();
    }

    @Test
    @DisplayName("같은 알림을 두 번 평가할 수 없다")
    void submitFeedback_rejects_second_submission_for_same_notification() {
        Notification notification = saveAnomalyNotification(guardian.getMemberKey());
        submit(notification.getNotificationKey(), """
                {"accuracy": "ACCURATE"}
                """).expectStatus().isCreated();

        submit(notification.getNotificationKey(), """
                {"accuracy": "UNSURE"}
                """)
                .expectStatus().isEqualTo(409)
                .expectBody()
                .jsonPath("$.error.errorCode").isEqualTo("E9007");

        assertThat(notificationFeedbackRepository.findAll()).hasSize(1);
    }

    @Test
    @DisplayName("이상탐지가 아닌 알림은 평가할 수 없다")
    void submitFeedback_rejects_non_anomaly_notification() {
        Notification notification = notificationRepository.saveAndFlush(
                NotificationFixture.batteryLowNotification(guardian.getMemberKey()));

        submit(notification.getNotificationKey(), """
                {"accuracy": "ACCURATE"}
                """)
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.error.errorCode").isEqualTo("E9006");

        assertThat(notificationFeedbackRepository.findAll()).isEmpty();
    }

    @Test
    @DisplayName("연결된 탐지 기록이 사라진 알림은 평가할 수 없다")
    void submitFeedback_rejects_when_detection_record_is_gone() {
        Notification notification = saveAnomalyNotification(guardian.getMemberKey());
        anomalyDetectionRepository.deleteAllInBatch();

        submit(notification.getNotificationKey(), """
                {"accuracy": "ACCURATE"}
                """)
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.error.errorCode").isEqualTo("E9009");

        assertThat(notificationFeedbackRepository.findAll()).isEmpty();
    }

    @Test
    @DisplayName("다른 회원이 받은 알림은 평가할 수 없다")
    void submitFeedback_rejects_other_members_notification() {
        Member otherGuardian = memberRepository.save(NotificationFixture.createOtherGuardian());
        Notification notification = saveAnomalyNotification(otherGuardian.getMemberKey());

        submit(notification.getNotificationKey(), """
                {"accuracy": "ACCURATE"}
                """)
                .expectStatus().isForbidden()
                .expectBody()
                .jsonPath("$.error.errorCode").isEqualTo("E9005");

        assertThat(notificationFeedbackRepository.findAll()).isEmpty();
    }

    @Test
    @DisplayName("존재하지 않는 알림은 평가할 수 없다")
    void submitFeedback_rejects_unknown_notification_key() {
        submit("00000000-0000-0000-0000-000000000000", """
                {"accuracy": "ACCURATE"}
                """)
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.error.errorCode").isEqualTo("E9004");
    }

    @Test
    @DisplayName("의견이 100자면 제출할 수 있다")
    void submitFeedback_accepts_comment_of_max_length() {
        Notification notification = saveAnomalyNotification(guardian.getMemberKey());

        submit(notification.getNotificationKey(), """
                {"accuracy": "UNSURE", "comment": "%s"}
                """.formatted("가".repeat(100)))
                .expectStatus().isCreated();

        assertThat(onlyFeedback().getComment()).hasSize(100);
    }

    @Test
    @DisplayName("의견이 100자를 넘으면 거부한다")
    void submitFeedback_rejects_comment_over_max_length() {
        Notification notification = saveAnomalyNotification(guardian.getMemberKey());

        submit(notification.getNotificationKey(), """
                {"accuracy": "UNSURE", "comment": "%s"}
                """.formatted("가".repeat(101)))
                .expectStatus().isBadRequest();

        assertThat(notificationFeedbackRepository.findAll()).isEmpty();
    }

    @Test
    @DisplayName("공백만 담긴 의견은 의견 없음으로 저장한다")
    void submitFeedback_stores_blank_comment_as_absent() {
        Notification notification = saveAnomalyNotification(guardian.getMemberKey());

        submit(notification.getNotificationKey(), """
                {"accuracy": "UNSURE", "comment": "   "}
                """)
                .expectStatus().isCreated();

        assertThat(onlyFeedback().getComment()).isNull();
    }

    @Test
    @DisplayName("평가한 알림은 목록에서 제출 완료로 내려온다")
    void getMyNotifications_marks_evaluated_notification_as_submitted() {
        Notification evaluated = saveAnomalyNotification(guardian.getMemberKey());
        Notification untouched = saveAnomalyNotification(guardian.getMemberKey());
        submit(evaluated.getNotificationKey(), """
                {"accuracy": "ACCURATE"}
                """).expectStatus().isCreated();

        client.get()
                .uri("/api/v1/notifications?size=2")
                .header(HttpHeaders.AUTHORIZATION, bearerToken(guardian.getMemberKey(), guardian.getRole()))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.items[0].notificationKey").isEqualTo(untouched.getNotificationKey())
                .jsonPath("$.data.items[0].feedbackEligible").isEqualTo(true)
                .jsonPath("$.data.items[0].feedbackSubmitted").isEqualTo(false)
                .jsonPath("$.data.items[1].notificationKey").isEqualTo(evaluated.getNotificationKey())
                .jsonPath("$.data.items[1].feedbackEligible").isEqualTo(true)
                .jsonPath("$.data.items[1].feedbackSubmitted").isEqualTo(true);
    }

    private RestTestClient.ResponseSpec submit(String notificationKey, String body) {
        return client.post()
                .uri("/api/v1/notifications/" + notificationKey + "/feedback")
                .header(HttpHeaders.AUTHORIZATION, bearerToken(guardian.getMemberKey(), guardian.getRole()))
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .exchange();
    }

    private Notification saveAnomalyNotification(String recipientMemberKey) {
        return notificationRepository.saveAndFlush(NotificationFixture.anomalyNotification(recipientMemberKey));
    }

    private NotificationFeedback onlyFeedback() {
        List<NotificationFeedback> feedbacks = notificationFeedbackRepository.findAll();
        assertThat(feedbacks).hasSize(1);
        return feedbacks.getFirst();
    }
}
