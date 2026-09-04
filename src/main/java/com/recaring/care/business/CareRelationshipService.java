package com.recaring.care.business;

import com.recaring.care.dataaccess.entity.CareRole;
import com.recaring.care.implement.CareRelationshipReader;
import com.recaring.care.implement.CareRelationshipValidator;
import com.recaring.care.implement.CareRelationshipWriter;
import com.recaring.care.vo.CaregiverInfo;
import com.recaring.care.vo.WardInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CareRelationshipService {

    private final CareRelationshipReader careRelationshipReader;
    private final CareRelationshipWriter careRelationshipWriter;
    private final CareRelationshipValidator careRelationshipValidator;

    public List<WardInfo> getMyWards(String memberKey) {
        return careRelationshipReader.findWardInfos(memberKey);
    }

    public List<CaregiverInfo> getCaregivers(String wardKey, String requesterKey) {
        careRelationshipValidator.validateCaregiverViewAccess(requesterKey, wardKey);
        return careRelationshipReader.findCaregiverInfos(wardKey);
    }

    public void removeWard(String guardianKey, String wardKey) {
        careRelationshipValidator.validateCaregiver(guardianKey, wardKey);
        careRelationshipValidator.validateWardRemovable(guardianKey, wardKey);
        careRelationshipWriter.delete(wardKey, guardianKey);
    }

    public void removeCaregiver(String guardianKey, String wardKey, String caregiverKey) {
        careRelationshipValidator.validatePrimaryGuardianRole(guardianKey, wardKey);
        careRelationshipValidator.validateCaregiverRemovable(wardKey, caregiverKey);
        careRelationshipWriter.delete(wardKey, caregiverKey);
    }

    public void updateWardNickname(String caregiverKey, String wardKey, String nickname) {
        careRelationshipValidator.validateCaregiver(caregiverKey, wardKey);
        careRelationshipWriter.updateWardNickname(wardKey, caregiverKey, normalize(nickname));
    }

    public void updateCaregiverRole(String requesterKey, String wardKey, String caregiverKey, CareRole careRole) {
        careRelationshipValidator.validatePrimaryGuardianRole(requesterKey, wardKey);
        careRelationshipValidator.validateCareRoleChange(wardKey, caregiverKey, careRole);
        careRelationshipWriter.updateCareRole(wardKey, caregiverKey, careRole);
    }

    // 빈 값은 별명 해제로 본다. null이면 대상자 실명이 그대로 표시된다.
    private String normalize(String nickname) {
        if (nickname == null || nickname.isBlank()) {
            return null;
        }
        return nickname.trim();
    }
}
