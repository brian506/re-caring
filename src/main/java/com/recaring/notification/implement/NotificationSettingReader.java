package com.recaring.notification.implement;

import com.recaring.notification.business.NotificationSettingInfo;
import com.recaring.notification.dataaccess.entity.NotificationSetting;
import com.recaring.notification.dataaccess.repository.NotificationSettingRepository;
import com.recaring.support.exception.AppException;
import com.recaring.support.exception.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NotificationSettingReader {

    private final NotificationSettingRepository notificationSettingRepository;

    public NotificationSettingInfo findSetting(String wardKey) {
        NotificationSetting setting = notificationSettingRepository.findByWardMemberKey(wardKey)
                .orElseGet(() -> NotificationSetting.defaultFor(wardKey));
        return NotificationSettingInfo.from(setting);
    }

    public NotificationSetting findExistingSetting(String wardKey) {
        return notificationSettingRepository.findByWardMemberKey(wardKey)
                .orElseThrow(() -> new AppException(ErrorType.NOT_FOUND_DATA));
    }
}
