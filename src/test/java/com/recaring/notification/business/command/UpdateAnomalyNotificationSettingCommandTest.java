package com.recaring.notification.business.command;

import com.recaring.support.exception.AppException;
import com.recaring.support.exception.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("이상탐지 알림 설정 변경 명령 단위 테스트")
class UpdateAnomalyNotificationSettingCommandTest {

    @ParameterizedTest(name = "대상자 식별자가 [{0}]이면 거부한다")
    @NullAndEmptySource
    @ValueSource(strings = {" "})
    @DisplayName("대상자 식별자가 비어 있으면 명령을 만들 수 없다")
    void rejects_blank_ward_key(String wardKey) {
        assertThatThrownBy(() -> new UpdateAnomalyNotificationSettingCommand(
                wardKey, true, true, true, true, true))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.INVALID_MEMBER_KEY);
    }
}
