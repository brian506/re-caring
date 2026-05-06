package com.recaring.location.business;

import com.recaring.location.business.command.UpdateLocationCollectionIntervalCommand;
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

    public void updateCollectionInterval(String requesterKey, UpdateLocationCollectionIntervalCommand command) {
        locationValidator.validateGuardianAccess(requesterKey, command.wardKey());

        locationSettingManager.updateCollectionInterval(command.wardKey(), command.interval());
    }

    public WardLocationCollectionIntervalInfo getMyCollectionInterval(String wardKey) {
        locationValidator.validateWardRole(wardKey);

        LocationCollectionInterval interval = locationSettingReader.findCollectionInterval(wardKey);
        return WardLocationCollectionIntervalInfo.from(interval);
    }
}
