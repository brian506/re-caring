package com.recaring.location.dataaccess.repository;

import com.recaring.location.dataaccess.entity.LocationSetting;
import com.recaring.location.dataaccess.repository.custom.LocationSettingRepositoryCustom;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LocationSettingRepository extends JpaRepository<LocationSetting, Long>,
        LocationSettingRepositoryCustom {
    Optional<LocationSetting> findByWardMemberKey(String wardMemberKey);
}
