package com.recaring.notification.implement.feedback;

import com.recaring.notification.dataaccess.repository.NotificationFeedbackRepository;
import com.recaring.notification.fixture.NotificationFixture;
import com.recaring.notification.vo.FeedbackTarget;
import com.recaring.support.exception.AppException;
import com.recaring.support.exception.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("알림 피드백 Validator 단위 테스트")
class NotificationFeedbackValidatorTest {

    private static final Long NOTIFICATION_ID = 30L;

    @InjectMocks
    private NotificationFeedbackValidator notificationFeedbackValidator;

    @Mock
    private NotificationFeedbackRepository notificationFeedbackRepository;

    @Test
    @DisplayName("수신자 본인의 이상탐지 알림이면 통과한다")
    void validateSubmittable_passes_for_own_anomaly_notification() {
        FeedbackTarget target = anomalyTarget();
        given(notificationFeedbackRepository.existsByNotificationId(NOTIFICATION_ID)).willReturn(false);

        assertThatCode(() -> notificationFeedbackValidator.validateSubmittable(target, NotificationFixture.GUARDIAN_KEY))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("다른 회원이 받은 알림은 평가할 수 없다")
    void validateSubmittable_rejects_other_members_notification() {
        FeedbackTarget target = anomalyTarget();

        assertThatThrownBy(() -> notificationFeedbackValidator.validateSubmittable(target, NotificationFixture.OTHER_GUARDIAN_KEY))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.NOTIFICATION_ACCESS_DENIED);
    }

    @Test
    @DisplayName("이상탐지가 아닌 알림은 평가 대상이 아니다")
    void validateSubmittable_rejects_non_anomaly_notification() {
        FeedbackTarget target = FeedbackTarget.from(
                NotificationFixture.notificationWithId(NOTIFICATION_ID, NotificationFixture.GUARDIAN_KEY));

        assertThatThrownBy(() -> notificationFeedbackValidator.validateSubmittable(target, NotificationFixture.GUARDIAN_KEY))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.NOTIFICATION_FEEDBACK_NOT_ELIGIBLE);
    }

    @Test
    @DisplayName("이미 평가한 알림은 다시 평가할 수 없다")
    void validateSubmittable_rejects_already_submitted_notification() {
        FeedbackTarget target = anomalyTarget();
        given(notificationFeedbackRepository.existsByNotificationId(NOTIFICATION_ID)).willReturn(true);

        assertThatThrownBy(() -> notificationFeedbackValidator.validateSubmittable(target, NotificationFixture.GUARDIAN_KEY))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.NOTIFICATION_FEEDBACK_ALREADY_SUBMITTED);
    }

    private FeedbackTarget anomalyTarget() {
        return FeedbackTarget.from(
                NotificationFixture.anomalyNotificationWithId(NOTIFICATION_ID, NotificationFixture.GUARDIAN_KEY));
    }
}
