package com.recaring.notification.implement.feedback;

import com.recaring.notification.dataaccess.entity.FeedbackReason;
import com.recaring.notification.dataaccess.repository.NotificationFeedbackRepository;
import com.recaring.notification.fixture.NotificationFixture;
import com.recaring.notification.vo.FeedbackTarget;
import com.recaring.support.exception.AppException;
import com.recaring.support.exception.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("알림 피드백 Validator 단위 테스트")
class NotificationFeedbackValidatorTest {

    private static final Long NOTIFICATION_ID = NotificationFixture.FEEDBACK_NOTIFICATION_ID;

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

    @ParameterizedTest
    @EnumSource(value = FeedbackReason.class, names = {"GPS_INACCURATE", "OTHER"})
    @DisplayName("안심존 알림은 위치 부정확·기타 사유를 받는다")
    void validateReason_allows_location_reasons_for_safe_zone(FeedbackReason reason) {
        FeedbackTarget target = safeZoneTarget();

        assertThatCode(() -> notificationFeedbackValidator.validateReason(
                target, NotificationFixture.inaccurateFeedbackAnswer(reason, null)))
                .doesNotThrowAnyException();
    }

    @ParameterizedTest
    @EnumSource(value = FeedbackReason.class, names = {"USUAL_ROUTINE", "BRIEF_STOPOVER"})
    @DisplayName("안심존 알림에 생활 패턴 사유를 주면 거부한다")
    void validateReason_rejects_routine_reasons_for_safe_zone(FeedbackReason reason) {
        FeedbackTarget target = safeZoneTarget();

        assertThatThrownBy(() -> notificationFeedbackValidator.validateReason(
                target, NotificationFixture.inaccurateFeedbackAnswer(reason, null)))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.NOTIFICATION_FEEDBACK_REASON_NOT_ALLOWED);
    }

    @ParameterizedTest
    @EnumSource(FeedbackReason.class)
    @DisplayName("이상탐지 알림은 모든 사유를 받는다")
    void validateReason_allows_every_reason_for_anomaly(FeedbackReason reason) {
        FeedbackTarget target = anomalyTarget();

        assertThatCode(() -> notificationFeedbackValidator.validateReason(
                target, NotificationFixture.inaccurateFeedbackAnswer(reason, null)))
                .doesNotThrowAnyException();
    }

    private FeedbackTarget safeZoneTarget() {
        return FeedbackTarget.from(
                NotificationFixture.safeZoneNotificationWithId(NOTIFICATION_ID, NotificationFixture.GUARDIAN_KEY));
    }

    private FeedbackTarget anomalyTarget() {
        return FeedbackTarget.from(
                NotificationFixture.anomalyNotificationWithId(NOTIFICATION_ID, NotificationFixture.GUARDIAN_KEY));
    }
}
