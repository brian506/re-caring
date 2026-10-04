package com.recaring.location.implement.setting;

import com.recaring.care.implement.CareRelationshipReader;
import com.recaring.care.vo.CaregiverInfo;
import com.recaring.location.dataaccess.entity.LocationSetting;
import com.recaring.location.dataaccess.repository.LocationSettingRepository;
import com.recaring.location.vo.LocationCollectionInterval;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class LocationSettingManager {

    private final LocationSettingRepository locationSettingRepository;
    private final CareRelationshipReader careRelationshipReader;

    public LocationCollectionInterval findCollectionInterval(String wardMemberKey, String caregiverMemberKey) {
        return locationSettingRepository.findByWardMemberKeyAndCaregiverMemberKey(wardMemberKey, caregiverMemberKey)
                .map(setting -> LocationCollectionInterval.fromSeconds(setting.getCollectionIntervalSeconds()))
                .orElse(LocationCollectionInterval.DEFAULT);
    }

    // 기기는 보호자들이 고른 주기 중 가장 짧은 주기로 수집한다. 그보다 긴 주기를 고른 보호자에게는 SSE에서 솎아 보낸다.
    // 관계자는 주기를 고를 수 없어 기기 주기를 그대로 따르므로 계산에서 뺀다.
    public LocationCollectionInterval findDeviceCollectionInterval(String wardMemberKey) {
        Map<String, LocationCollectionInterval> intervals = locationSettingRepository
                .findAllByWardMemberKey(wardMemberKey).stream()
                .collect(Collectors.toMap(
                        LocationSetting::getCaregiverMemberKey,
                        setting -> LocationCollectionInterval.fromSeconds(setting.getCollectionIntervalSeconds())));

        return careRelationshipReader.findCaregiverInfos(wardMemberKey).stream()
                .filter(caregiver -> caregiver.careRole().isGuardian())
                .map(CaregiverInfo::memberKey)
                .map(caregiverKey -> intervals.getOrDefault(caregiverKey, LocationCollectionInterval.DEFAULT))
                .reduce(LocationCollectionInterval::shorter)
                .orElse(LocationCollectionInterval.DEFAULT);
    }

    @Transactional
    public void updateCollectionInterval(
            String wardMemberKey,
            String caregiverMemberKey,
            LocationCollectionInterval interval
    ) {
        LocationSetting setting = locationSettingRepository
                .findByWardMemberKeyAndCaregiverMemberKey(wardMemberKey, caregiverMemberKey)
                .orElseGet(() -> LocationSetting.builder()
                        .wardMemberKey(wardMemberKey)
                        .caregiverMemberKey(caregiverMemberKey)
                        .collectionIntervalSeconds(interval.seconds())
                        .build());

        setting.updateCollectionIntervalSeconds(interval.seconds());
        locationSettingRepository.save(setting);
    }
}
