package com.recaring.safezone.business;

import com.recaring.care.implement.CareRelationshipValidator;
import com.recaring.safezone.vo.SafeZoneCreation;
import com.recaring.safezone.vo.SafeZoneUpdate;
import com.recaring.safezone.implement.SafeZoneReader;
import com.recaring.safezone.implement.SafeZoneWriter;
import com.recaring.safezone.vo.SafeZoneInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SafeZoneService {

    private final SafeZoneReader safeZoneReader;
    private final SafeZoneWriter safeZoneWriter;
    private final CareRelationshipValidator careRelationshipValidator;

    @Transactional
    public void addSafeZone(String requesterKey, SafeZoneCreation command) {
        careRelationshipValidator.validateGuardian(requesterKey, command.wardMemberKey());
        safeZoneWriter.register(command);
    }

    @Transactional(readOnly = true)
    public List<SafeZoneInfo> getSafeZones(String requesterKey, String wardKey) {
        careRelationshipValidator.validateCaregiver(requesterKey, wardKey);
        return safeZoneReader.findAllByWardMemberKey(wardKey);
    }

    @Transactional(readOnly = true)
    public SafeZoneInfo getSafeZone(String requesterKey,  String wardKey, String safeZoneKey) {
        careRelationshipValidator.validateCaregiver(requesterKey, wardKey);
        return safeZoneReader.findBySafeZoneKey(safeZoneKey, wardKey);
    }

    @Transactional
    public void updateSafeZone(String requesterKey, String wardKey, String safeZoneKey, SafeZoneUpdate command) {
        careRelationshipValidator.validateGuardian(requesterKey, wardKey);
        safeZoneWriter.update(safeZoneKey, wardKey, command);
    }

    @Transactional
    public void deleteSafeZone(String requesterKey, String wardKey, String safeZoneKey) {
        careRelationshipValidator.validateGuardian(requesterKey, wardKey);
        safeZoneWriter.delete(safeZoneKey, wardKey);
    }
}
