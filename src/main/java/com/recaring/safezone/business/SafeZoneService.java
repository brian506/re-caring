package com.recaring.safezone.business;

import com.recaring.care.dataaccess.entity.CareRole;
import com.recaring.care.dataaccess.repository.CareRelationshipRepository;
import com.recaring.safezone.controller.request.CreateSafeZoneCommand;
import com.recaring.safezone.controller.request.UpdateSafeZoneCommand;
import com.recaring.safezone.dataaccess.entity.SafeZone;
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
    private final CareRelationshipRepository careRelationshipRepository;

    @Transactional
    public void addSafeZone(String requesterKey, CreateSafeZoneCommand command) {
        validateGuardianAccess(requesterKey, command.wardMemberKey());
        safeZoneWriter.register(command);
    }

    @Transactional(readOnly = true)
    public List<SafeZoneInfo> getSafeZones(String requesterKey, String wardKey) {
        validateCareAccess(requesterKey, wardKey);
        return safeZoneReader.findAllByWardMemberKey(wardKey);
    }

    @Transactional(readOnly = true)
    public SafeZoneInfo getSafeZone(String requesterKey, String wardKey, String safeZoneKey) {
        validateCareAccess(requesterKey, wardKey);
        return safeZoneReader.findBySafeZoneKey(safeZoneKey);
    }

    @Transactional
    public void updateSafeZone(String requesterKey, String wardKey, String safeZoneKey, UpdateSafeZoneCommand command) {
        validateGuardianAccess(requesterKey, wardKey);
        SafeZone zone = safeZoneReader.getEntity(safeZoneKey);
        safeZoneWriter.update(zone, command);
    }

    @Transactional
    public void deleteSafeZone(String requesterKey, String wardKey, String safeZoneKey) {
        validateGuardianAccess(requesterKey, wardKey);
        SafeZone zone = safeZoneReader.getEntity(safeZoneKey);
        safeZoneWriter.delete(zone);
    }

    private void validateCareAccess(String requesterKey, String wardKey) {
        if (!careRelationshipRepository.existsByWardKeyAndCaregiverKey(wardKey, requesterKey)) {
            throw new AppException(ErrorType.NOT_CAREGIVER_OF_WARD);
        }
    }

    private void validateGuardianAccess(String requesterKey, String wardKey) {
        if (!careRelationshipRepository.existsByWardKeyAndCaregiverKeyAndCareRole(wardKey, requesterKey, CareRole.GUARDIAN)) {
            throw new AppException(ErrorType.NOT_GUARDIAN_OF_WARD);
        }
    }
}
