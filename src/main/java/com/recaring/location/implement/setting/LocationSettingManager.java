package com.recaring.location.implement.setting;

import com.recaring.location.dataaccess.entity.LocationSetting;
import com.recaring.location.dataaccess.repository.LocationSettingRepository;
import com.recaring.location.vo.LocationCollectionInterval;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class LocationSettingManager {

    private final LocationSettingRepository locationSettingRepository;

    public LocationCollectionInterval findCollectionInterval(String wardMemberKey) {
        return locationSettingRepository.findByWardMemberKey(wardMemberKey)
                .map(setting -> LocationCollectionInterval.fromSeconds(setting.getCollectionIntervalSeconds()))
                .orElse(LocationCollectionInterval.DEFAULT);
    }

    @Transactional
    public void updateCollectionInterval(String wardMemberKey, LocationCollectionInterval interval) {
        LocationSetting setting = locationSettingRepository.findByWardMemberKey(wardMemberKey)
                .orElseGet(() -> LocationSetting.builder()
                        .wardMemberKey(wardMemberKey)
                        .collectionIntervalSeconds(interval.seconds())
                        .build());

        setting.updateCollectionIntervalSeconds(interval.seconds());
        locationSettingRepository.save(setting);
    }
}
