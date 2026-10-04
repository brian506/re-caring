package com.recaring.notification.implement.setting;

import com.recaring.care.vo.CaregiverInfo;
import com.recaring.notification.dataaccess.entity.NotificationSetting;
import com.recaring.notification.dataaccess.repository.NotificationSettingRepository;
import com.recaring.notification.vo.BatteryNotificationSetting;
import com.recaring.notification.vo.BatteryThresholds;
import com.recaring.notification.vo.NotificationSettings;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class NotificationSettingReader {

    private final NotificationSettingRepository notificationSettingRepository;

    public NotificationSettings findSetting(String wardKey, String caregiverKey) {
        NotificationSetting setting = notificationSettingRepository
                .findByWardMemberKeyAndCaregiverMemberKey(wardKey, caregiverKey)
                .orElseGet(() -> NotificationSetting.defaultFor(wardKey, caregiverKey));
        return NotificationSettings.from(setting);
    }

    // 설정 행이 없는 보호자는 기본값(전부 켜짐)으로 본다. 첫 알림을 놓치지 않기 위함이다.
    public List<CaregiverInfo> filterRecipients(
            String wardKey,
            List<CaregiverInfo> caregivers,
            Predicate<NotificationSettings> enabled
    ) {
        Map<String, NotificationSettings> settings = findAllByCaregiver(wardKey);
        NotificationSettings defaults = NotificationSettings.from(NotificationSetting.defaultFor(wardKey, null));
        return caregivers.stream()
                .filter(caregiver -> enabled.test(settings.getOrDefault(caregiver.memberKey(), defaults)))
                .toList();
    }

    // 설정 행이 없는 보호자는 고른 잔량이 없어 판정할 것이 없으므로 결과에서 빠진다.
    public Map<String, BatteryNotificationSetting> findBatterySettings(String wardKey) {
        return notificationSettingRepository.findAllByWardMemberKey(wardKey).stream()
                .collect(Collectors.toMap(
                        NotificationSetting::getCaregiverMemberKey,
                        setting -> new BatteryNotificationSetting(
                                setting.isLowBatteryEnabled(),
                                BatteryThresholds.parse(setting.getBatteryThresholdPercents()))));
    }

    private Map<String, NotificationSettings> findAllByCaregiver(String wardKey) {
        return notificationSettingRepository.findAllByWardMemberKey(wardKey).stream()
                .collect(Collectors.toMap(NotificationSetting::getCaregiverMemberKey, NotificationSettings::from));
    }
}
