package com.recaring.care.implement;

import com.recaring.care.vo.CaregiverInfo;
import com.recaring.care.vo.WardInfo;
import com.recaring.care.dataaccess.entity.CareRelationship;
import com.recaring.care.dataaccess.repository.CareRelationshipRepository;
import com.recaring.member.implement.MemberReader;
import com.recaring.member.vo.MemberInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

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

        Map<String, MemberInfo> memberMap = memberReader.findAllByMemberKeys(wardKeys);
        Map<String, String> designatedAvatarCodes = designatedAvatarManager.findWardAvatarCodes(caregiverKey);

        return relationships.stream()
                .map(r -> {
                    MemberInfo ward = memberMap.get(r.getWardMemberKey());
                    return new WardInfo(ward.memberKey(), ward.name(), r.getWardNickname(), ward.phone(),
                            ward.gender(), r.getCareRole(), ward.profileAvatarCode(),
                            designatedAvatarCodes.get(ward.memberKey()), ward.joinedAt());
                })
                .toList();
    }

    public List<String> findAllWardMemberKeys() {
        return careRelationshipRepository.findAllWardMemberKeys();
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

        Map<String, MemberInfo> memberMap = memberReader.findAllByMemberKeys(caregiverKeys);

        return relationships.stream()
                .map(r -> {
                    MemberInfo caregiver = memberMap.get(r.getCaregiverMemberKey());
                    return new CaregiverInfo(caregiver.memberKey(), caregiver.name(), caregiver.phone(),
                            r.getCareRole(), caregiver.profileAvatarCode(),
                            designatedAvatarCodes.get(caregiver.memberKey()), r.getWardNickname());
                })
                .toList();
    }
}
