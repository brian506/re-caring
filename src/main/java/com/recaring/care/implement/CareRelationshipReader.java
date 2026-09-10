package com.recaring.care.implement;

import com.recaring.care.vo.CaregiverInfo;
import com.recaring.care.vo.WardInfo;
import com.recaring.care.dataaccess.entity.CareRelationship;
import com.recaring.care.dataaccess.entity.CareRole;
import com.recaring.care.dataaccess.repository.CareRelationshipRepository;
import com.recaring.member.dataaccess.entity.Member;
import com.recaring.member.implement.MemberReader;
import com.recaring.support.exception.AppException;
import com.recaring.support.exception.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@Component
@RequiredArgsConstructor
public class CareRelationshipReader {

    private final CareRelationshipRepository careRelationshipRepository;
    private final MemberReader memberReader;
    private final DesignatedAvatarManager designatedAvatarManager;

    public List<WardInfo> findWardInfos(String caregiverKey) {
        List<CareRelationship> relationships = careRelationshipRepository.findAllByCaregiverMemberKey(caregiverKey);

        List<String> wardKeys = relationships.stream()
                .map(CareRelationship::getWardMemberKey)
                .toList();

        Map<String, Member> memberMap = memberReader.findAllByMemberKeys(wardKeys);
        Map<String, String> designatedAvatarCodes = designatedAvatarManager.findWardAvatarCodes(caregiverKey);

        return relationships.stream()
                .map(r -> {
                    Member ward = memberMap.get(r.getWardMemberKey());
                    return new WardInfo(ward.getMemberKey(), ward.getName(), r.getWardNickname(), ward.getPhone(),
                            ward.getGender(), r.getCareRole(), ward.getProfileAvatarCode(),
                            designatedAvatarCodes.get(ward.getMemberKey()));
                })
                .toList();
    }

    /**
     * 알림 수신자 판정 등 보는 사람이 없는 경로. 지정 아바타는 채우지 않는다.
     */
    public List<CaregiverInfo> findCaregiverInfos(String wardKey) {
        return findCaregiverInfos(wardKey, Map.of());
    }

    public List<CaregiverInfo> findCaregiverInfos(String wardKey, String requesterKey) {
        return findCaregiverInfos(wardKey, designatedAvatarManager.findAvatarCodesInWard(requesterKey, wardKey));
    }

    private List<CaregiverInfo> findCaregiverInfos(String wardKey, Map<String, String> designatedAvatarCodes) {
        List<CareRelationship> relationships = careRelationshipRepository.findAllByWardMemberKey(wardKey);

        List<String> caregiverKeys = relationships.stream()
                .map(CareRelationship::getCaregiverMemberKey)
                .toList();

        Map<String, Member> memberMap = memberReader.findAllByMemberKeys(caregiverKeys);

        return relationships.stream()
                .map(r -> {
                    Member caregiver = memberMap.get(r.getCaregiverMemberKey());
                    return new CaregiverInfo(caregiver.getMemberKey(), caregiver.getName(), caregiver.getPhone(),
                            r.getCareRole(), caregiver.getProfileAvatarCode(),
                            designatedAvatarCodes.get(caregiver.getMemberKey()));
                })
                .toList();
    }

    public CareRole findCareRole(String wardKey, String caregiverKey) {
        return careRelationshipRepository.findAllByWardMemberKey(wardKey)
                .stream()
                .filter(relationship -> Objects.equals(caregiverKey, relationship.getCaregiverMemberKey()))
                .map(CareRelationship::getCareRole)
                .findFirst()
                .orElseThrow(() -> new AppException(ErrorType.NOT_CARE_RELATED_WARD));
    }

    public boolean existsWithGuardianRole(String wardKey, String caregiverKey) {
        return careRelationshipRepository.existsCareRelationshipInRoles(wardKey, caregiverKey, CareRole.guardianRoles());
    }

    public boolean exists(String wardKey, String caregiverKey) {
        return careRelationshipRepository.existsCareRelationship(wardKey, caregiverKey);
    }
}
