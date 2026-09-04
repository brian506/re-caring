package com.recaring.safezone.business;

import com.recaring.care.implement.CareRelationshipReader;
import com.recaring.safezone.vo.SafeZoneCreation;
import com.recaring.safezone.vo.SafeZoneUpdate;
import com.recaring.safezone.implement.SafeZoneReader;
import com.recaring.safezone.implement.SafeZoneWriter;
import com.recaring.safezone.vo.SafeZoneInfo;
import com.recaring.support.exception.AppException;
import com.recaring.support.exception.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SafeZoneService {

    private final SafeZoneReader safeZoneReader;
    private final SafeZoneWriter safeZoneWriter;
    private final CareRelationshipReader careRelationshipReader;

    @Transactional
    public void addSafeZone(String requesterKey, SafeZoneCreation command) {
        validateGuardianAccess(requesterKey, command.wardMemberKey());
        safeZoneWriter.register(command);
    }

    @Transactional(readOnly = true)
    public List<SafeZoneInfo> getSafeZones(String requesterKey, String wardKey) {
        validateCareAccess(requesterKey, wardKey);
        return safeZoneReader.findAllByWardMemberKey(wardKey);
    }

    @Transactional(readOnly = true)
    public SafeZoneInfo getSafeZone(String requesterKey,  String wardKey, String safeZoneKey) {
        validateCareAccess(requesterKey, wardKey);
        return safeZoneReader.findBySafeZoneKey(safeZoneKey, wardKey);
    }

    @Transactional
    public void updateSafeZone(String requesterKey, String wardKey, String safeZoneKey, SafeZoneUpdate command) {
        validateGuardianAccess(requesterKey, wardKey);
        safeZoneWriter.update(safeZoneKey, wardKey, command);
    }

    @Transactional
    public void deleteSafeZone(String requesterKey, String wardKey, String safeZoneKey) {
        validateGuardianAccess(requesterKey, wardKey);
        safeZoneWriter.delete(safeZoneKey, wardKey);
    }

    private void validateCareAccess(String requesterKey, String wardKey) {
        if (!careRelationshipReader.exists(wardKey, requesterKey)) {
            throw new AppException(ErrorType.NOT_CAREGIVER_OF_WARD);
        }
    }

    private void validateGuardianAccess(String requesterKey, String wardKey) {
        if (!careRelationshipReader.existsWithGuardianRole(wardKey, requesterKey)) {
            throw new AppException(ErrorType.NOT_GUARDIAN_OF_WARD);
        }
    }
}
