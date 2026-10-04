package com.recaring.notification.implement.setting;

import com.recaring.care.implement.CareRelationshipReader;
import com.recaring.support.exception.AppException;
import com.recaring.support.exception.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NotificationSettingValidator {

    private final CareRelationshipReader careRelationshipReader;

    // 설정은 알림을 받는 보호자 본인의 것이다. 대상자는 수신자가 아니므로 설정할 대상이 없다.
    public void validateSettingAccess(String caregiverKey, String wardKey) {
        if (!careRelationshipReader.exists(wardKey, caregiverKey)) {
            throw new AppException(ErrorType.NOT_CARE_RELATED_WARD);
        }
    }
}
