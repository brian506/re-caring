package com.recaring.notification.implement;

import com.recaring.notification.business.NotificationSettingInfo;
import com.recaring.notification.dataaccess.entity.NotificationSetting;
import com.recaring.notification.dataaccess.repository.NotificationSettingRepository;
import com.recaring.notification.vo.BatteryThreshold;
import com.recaring.notification.vo.BatteryThresholdRange;
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

    public boolean isLowBatteryEnabled(String wardKey) {
        return notificationSettingRepository.findByWardMemberKey(wardKey)
                .map(NotificationSetting::isLowBatteryEnabled)
                .orElse(true);
    }

    public BatteryThresholdRange findBatteryThresholds(String wardKey) {
        return notificationSettingRepository.findByWardMemberKey(wardKey)
                .map(setting -> new BatteryThresholdRange(
                        new BatteryThreshold(setting.getBatteryThresholdPercent()),
                        new BatteryThreshold(setting.getFullBatteryThresholdPercent())))
                .orElse(BatteryThresholdRange.DEFAULT);
    }
}
