package com.recaring.safezone.dataaccess.repository.custom;

import com.recaring.safezone.dataaccess.entity.SafeZone;

import java.util.List;
import java.util.Optional;

public interface SafeZoneRepositoryCustom {

    List<SafeZone> findAllByWardMemberKey(String wardMemberKey);

    Optional<SafeZone> findBySafeZoneKey(String safeZoneKey);
}
