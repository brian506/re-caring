package com.recaring.notification.implement.feedback;

import com.recaring.location.implement.detection.AnomalyDetectionManager;
import com.recaring.notification.dataaccess.entity.FeedbackAccuracy;
import com.recaring.notification.dataaccess.entity.FeedbackReason;
import com.recaring.notification.dataaccess.entity.NotificationFeedback;
import com.recaring.notification.dataaccess.repository.NotificationFeedbackRepository;
import com.recaring.notification.fixture.NotificationFixture;
import com.recaring.notification.implement.NotificationReader;
import com.recaring.notification.vo.FeedbackTarget;
import com.recaring.notification.vo.NotificationFeedbackAnswer;
import com.recaring.support.exception.AppException;
import com.recaring.support.exception.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
@DisplayName("알림 피드백 Manager 단위 테스트")
class NotificationFeedbackManagerTest {

    private static final Long NOTIFICATION_ID = 30L;
    private static final String NOTIFICATION_KEY = "notification-key-30";
    private static final Long ANOMALY_DETECTION_ID = 77L;
    private static final String COMMENT = "집에 계셨어요";

    @InjectMocks
    private NotificationFeedbackManager notificationFeedbackManager;

    @Mock
    private NotificationReader notificationReader;
    @Mock
    private NotificationFeedbackValidator notificationFeedbackValidator;
    @Mock
    private NotificationFeedbackRepository notificationFeedbackRepository;
    @Mock
    private AnomalyDetectionManager anomalyDetectionManager;

    @Test
    @DisplayName("알림에 담긴 대상자·유형·시각으로 찾은 탐지 기록과 함께 저장한다")
    void submit_saves_feedback_linked_to_matching_detection() {
        given(notificationReader.findFeedbackTarget(NOTIFICATION_KEY)).willReturn(anomalyTarget());
        given(anomalyDetectionManager.findDetectionId(
                NotificationFixture.WARD_KEY,
                NotificationFixture.ANOMALY_TYPE,
                NotificationFixture.ANOMALY_RECORDED_AT))
                .willReturn(Optional.of(ANOMALY_DETECTION_ID));

        notificationFeedbackManager.submit(
                NotificationFixture.GUARDIAN_KEY,
                NOTIFICATION_KEY,
                new NotificationFeedbackAnswer(FeedbackAccuracy.INACCURATE, FeedbackReason.GPS_INACCURATE, COMMENT));

        ArgumentCaptor<NotificationFeedback> saved = ArgumentCaptor.forClass(NotificationFeedback.class);
        then(notificationFeedbackRepository).should().save(saved.capture());
        assertThat(saved.getValue().getNotificationId()).isEqualTo(NOTIFICATION_ID);
        assertThat(saved.getValue().getAnomalyDetectionId()).isEqualTo(ANOMALY_DETECTION_ID);
        assertThat(saved.getValue().getAccuracy()).isEqualTo(FeedbackAccuracy.INACCURATE);
        assertThat(saved.getValue().getReason()).isEqualTo(FeedbackReason.GPS_INACCURATE);
        assertThat(saved.getValue().getComment()).isEqualTo(COMMENT);
    }

    @Test
    @DisplayName("일치하는 탐지 기록이 없으면 저장하지 않고 거부한다")
    void submit_rejects_when_detection_is_gone() {
        given(notificationReader.findFeedbackTarget(NOTIFICATION_KEY)).willReturn(anomalyTarget());
        given(anomalyDetectionManager.findDetectionId(
                NotificationFixture.WARD_KEY,
                NotificationFixture.ANOMALY_TYPE,
                NotificationFixture.ANOMALY_RECORDED_AT))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> notificationFeedbackManager.submit(
                NotificationFixture.GUARDIAN_KEY,
                NOTIFICATION_KEY,
                new NotificationFeedbackAnswer(FeedbackAccuracy.ACCURATE, null, null)))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.NOTIFICATION_FEEDBACK_DETECTION_NOT_FOUND);

        then(notificationFeedbackRepository).should(never()).save(any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"wardKey", "recordedAt"})
    @DisplayName("알림에 탐지를 찾을 키가 빠져 있으면 탐지를 찾지 않고 거부한다")
    void submit_rejects_when_detection_key_is_missing(String missingKey) {
        given(notificationReader.findFeedbackTarget(NOTIFICATION_KEY))
                .willReturn(targetWithPayloadWithout(missingKey));

        assertThatThrownBy(() -> notificationFeedbackManager.submit(
                NotificationFixture.GUARDIAN_KEY,
                NOTIFICATION_KEY,
                new NotificationFeedbackAnswer(FeedbackAccuracy.ACCURATE, null, null)))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.NOTIFICATION_FEEDBACK_DETECTION_NOT_FOUND);

        then(anomalyDetectionManager).shouldHaveNoInteractions();
        then(notificationFeedbackRepository).should(never()).save(any());
    }

    @Test
    @DisplayName("알림의 탐지 시각 형식이 깨져 있으면 거부한다")
    void submit_rejects_when_recorded_at_is_malformed() {
        Map<String, String> payload = new HashMap<>(NotificationFixture.anomalyDataPayload());
        payload.put("recordedAt", "2026/09/24 14:26:03");
        given(notificationReader.findFeedbackTarget(NOTIFICATION_KEY))
                .willReturn(FeedbackTarget.from(NotificationFixture.anomalyNotificationWithId(
                        NOTIFICATION_ID, NotificationFixture.GUARDIAN_KEY, payload)));

        assertThatThrownBy(() -> notificationFeedbackManager.submit(
                NotificationFixture.GUARDIAN_KEY,
                NOTIFICATION_KEY,
                new NotificationFeedbackAnswer(FeedbackAccuracy.ACCURATE, null, null)))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.NOTIFICATION_FEEDBACK_DETECTION_NOT_FOUND);

        then(anomalyDetectionManager).shouldHaveNoInteractions();
        then(notificationFeedbackRepository).should(never()).save(any());
    }

    @Test
    @DisplayName("검증에 실패하면 피드백을 저장하지 않는다")
    void submit_does_not_save_when_validation_fails() {
        FeedbackTarget target = anomalyTarget();
        given(notificationReader.findFeedbackTarget(NOTIFICATION_KEY)).willReturn(target);
        willThrow(new AppException(ErrorType.NOTIFICATION_FEEDBACK_ALREADY_SUBMITTED))
                .given(notificationFeedbackValidator)
                .validateSubmittable(target, NotificationFixture.GUARDIAN_KEY);

        assertThatThrownBy(() -> notificationFeedbackManager.submit(
                NotificationFixture.GUARDIAN_KEY,
                NOTIFICATION_KEY,
                new NotificationFeedbackAnswer(FeedbackAccuracy.ACCURATE, null, null)))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.NOTIFICATION_FEEDBACK_ALREADY_SUBMITTED);

        then(anomalyDetectionManager).shouldHaveNoInteractions();
        then(notificationFeedbackRepository).should(never()).save(any());
    }

    private FeedbackTarget anomalyTarget() {
        return FeedbackTarget.from(
                NotificationFixture.anomalyNotificationWithId(NOTIFICATION_ID, NotificationFixture.GUARDIAN_KEY));
    }

    private FeedbackTarget targetWithPayloadWithout(String key) {
        Map<String, String> payload = new HashMap<>(NotificationFixture.anomalyDataPayload());
        payload.remove(key);
        return FeedbackTarget.from(NotificationFixture.anomalyNotificationWithId(
                NOTIFICATION_ID, NotificationFixture.GUARDIAN_KEY, payload));
    }
}
