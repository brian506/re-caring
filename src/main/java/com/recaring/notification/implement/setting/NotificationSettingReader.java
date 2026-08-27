package com.recaring.notification.implement.setting;

import com.recaring.location.vo.DetectionType;
import com.recaring.notification.vo.NotificationSettings;
import com.recaring.notification.dataaccess.entity.NotificationSetting;
import com.recaring.notification.dataaccess.repository.NotificationSettingRepository;
import com.recaring.notification.vo.BatteryNotificationSetting;
import com.recaring.notification.vo.BatteryThresholds;
import com.recaring.support.exception.AppException;
import com.recaring.support.exception.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NotificationSettingReader {

    private final NotificationSettingRepository notificationSettingRepository;

    public NotificationSettings findSetting(String wardKey) {
        NotificationSetting setting = notificationSettingRepository.findByWardMemberKey(wardKey)
                .orElseGet(() -> NotificationSetting.defaultFor(wardKey));
        return NotificationSettings.from(setting);
    }

    public NotificationSetting findExistingSetting(String wardKey) {
        return notificationSettingRepository.findByWardMemberKey(wardKey)
                .orElseThrow(() -> new AppException(ErrorType.NOT_FOUND_DATA));
    }

    public boolean isSafeZoneEntryEnabled(String wardKey) {
        return notificationSettingRepository.findByWardMemberKey(wardKey)
                .map(NotificationSetting::isSafeZoneEntryEnabled)
                .orElse(true);
    }

    public boolean isSafeZoneExitEnabled(String wardKey) {
        return notificationSettingRepository.findByWardMemberKey(wardKey)
                .map(NotificationSetting::isSafeZoneExitEnabled)
                .orElse(true);
    }

    // 설정 행이 없으면 켜져 있는 것으로 본다. 기본값이 전부 true라 첫 알림을 놓치지 않기 위함이다.
    public boolean isAnomalyEnabled(String wardKey, DetectionType detectionType) {
        return notificationSettingRepository.findByWardMemberKey(wardKey)
                .map(setting -> isEnabled(setting, detectionType))
                .orElse(true);
    }

    private boolean isEnabled(NotificationSetting setting, DetectionType detectionType) {
        return switch (detectionType) {
            case SPEED_ANOMALY -> setting.isSpeedAnomalyEnabled();
            case WANDERING -> setting.isWanderingAnomalyEnabled();
            case ABNORMAL_DWELLING -> setting.isAbnormalDwellingEnabled();
            case ROUTE_DEVIATION -> setting.isRouteDeviationEnabled();
            case TIME_ANOMALY -> setting.isTimeAnomalyEnabled();
        };
    }

    public BatteryNotificationSetting findBatterySetting(String wardKey) {
        return notificationSettingRepository.findByWardMemberKey(wardKey)
                .map(setting -> new BatteryNotificationSetting(
                        setting.isLowBatteryEnabled(),
                        BatteryThresholds.parse(setting.getBatteryThresholdPercents())))
                .orElse(BatteryNotificationSetting.DEFAULT);
    }
}
