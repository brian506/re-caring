package com.recaring.location.business;

import com.recaring.location.implement.LocationValidator;
import com.recaring.location.implement.setting.LocationSettingManager;
import com.recaring.location.vo.LocationCollectionInterval;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LocationSettingService {

    private final LocationSettingManager locationSettingManager;
    private final LocationValidator locationValidator;

    public LocationCollectionInterval getCollectionInterval(String requesterKey, String wardKey) {
        locationValidator.validateCaregiverAccess(requesterKey, wardKey);

        return locationSettingManager.findCollectionInterval(wardKey, requesterKey);
    }

    public void updateCollectionInterval(String requesterKey, String wardKey, LocationCollectionInterval interval) {
        locationValidator.validateCaregiverAccess(requesterKey, wardKey);

        locationSettingManager.updateCollectionInterval(wardKey, requesterKey, interval);
    }

    public LocationCollectionInterval getMyCollectionInterval(String wardKey) {
        return locationSettingManager.findDeviceCollectionInterval(wardKey);
    }
}
