package com.recaring.location.implement;

import com.recaring.location.dataaccess.repository.LocationSettingRepository;
import com.recaring.location.vo.LocationCollectionInterval;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class LocationSettingReader {

    private final LocationSettingRepository locationSettingRepository;

    public LocationCollectionInterval findCollectionInterval(String wardMemberKey) {
        return locationSettingRepository.findByWardMemberKey(wardMemberKey)
                .map(setting -> LocationCollectionInterval.fromSeconds(setting.getCollectionIntervalSeconds()))
                .orElse(LocationCollectionInterval.DEFAULT);
    }
}
