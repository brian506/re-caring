package com.recaring.notification.implement.setting;

import com.recaring.care.implement.CareRelationshipReader;
import com.recaring.notification.fixture.NotificationFixture;
import com.recaring.support.exception.AppException;
import com.recaring.support.exception.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("알림 설정 접근 검증 단위 테스트")
class NotificationSettingValidatorTest {

    @InjectMocks
    private NotificationSettingValidator notificationSettingValidator;

    @Mock
    private CareRelationshipReader careRelationshipReader;

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {NotificationFixture.GUARDIAN_KEY, NotificationFixture.MANAGER_KEY})
    @DisplayName("케어 관계가 있는 보호자·관계자는 자기 설정에 접근할 수 있다")
    void validateSettingAccess_passes_for_related_caregiver(String caregiverKey) {
        given(careRelationshipReader.exists(NotificationFixture.WARD_KEY, caregiverKey)).willReturn(true);

        assertThatNoException().isThrownBy(() ->
                notificationSettingValidator.validateSettingAccess(caregiverKey, NotificationFixture.WARD_KEY));
    }

    @Test
    @DisplayName("케어 관계가 없는 사람은 설정에 접근할 수 없다")
    void validateSettingAccess_throws_for_unrelated_member() {
        given(careRelationshipReader.exists(NotificationFixture.WARD_KEY, NotificationFixture.OTHER_GUARDIAN_KEY))
                .willReturn(false);

        assertThatThrownBy(() -> notificationSettingValidator.validateSettingAccess(
                NotificationFixture.OTHER_GUARDIAN_KEY, NotificationFixture.WARD_KEY))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.NOT_CARE_RELATED_WARD);
    }
}
