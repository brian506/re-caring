package com.recaring.location.dataaccess.repository;

import com.recaring.location.dataaccess.entity.LocationSetting;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LocationSettingRepository extends JpaRepository<LocationSetting, Long> {
    Optional<LocationSetting> findByWardMemberKey(String wardMemberKey);
}
