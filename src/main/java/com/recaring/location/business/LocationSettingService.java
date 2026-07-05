package com.recaring.location.business;

import com.recaring.location.implement.LocationSettingManager;
import com.recaring.location.implement.LocationSettingReader;
import com.recaring.location.implement.LocationValidator;
import com.recaring.location.vo.LocationCollectionInterval;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LocationSettingService {

    private final LocationSettingReader locationSettingReader;
    private final LocationSettingManager locationSettingManager;
    private final LocationValidator locationValidator;

    public LocationCollectionIntervalSettingInfo getCollectionInterval(String requesterKey, String wardKey) {
        locationValidator.validateGuardianAccess(requesterKey, wardKey);

        LocationCollectionInterval interval = locationSettingReader.findCollectionInterval(wardKey);
        return LocationCollectionIntervalSettingInfo.from(interval);
    }

    public void updateCollectionInterval(String requesterKey, String wardKey, LocationCollectionInterval interval) {
        locationValidator.validateGuardianAccess(requesterKey, wardKey);

        locationSettingManager.updateCollectionInterval(wardKey, interval);
    }

    public WardLocationCollectionIntervalInfo getMyCollectionInterval(String wardKey) {
        LocationCollectionInterval interval = locationSettingReader.findCollectionInterval(wardKey);
        return WardLocationCollectionIntervalInfo.from(interval);
    }
}
