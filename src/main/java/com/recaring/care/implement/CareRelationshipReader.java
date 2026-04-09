package com.recaring.care.implement;

import com.recaring.care.business.CaregiverInfo;
import com.recaring.care.business.WardInfo;
import com.recaring.care.dataaccess.entity.CareRelationship;
import com.recaring.care.dataaccess.entity.CareRole;
import com.recaring.care.dataaccess.repository.CareRelationshipRepository;
import com.recaring.member.dataaccess.entity.Member;
import com.recaring.member.implement.MemberReader;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class CareRelationshipReader {

    private final CareRelationshipRepository careRelationshipRepository;
    private final MemberReader memberReader;


    public List<WardInfo> findWardInfos(String caregiverKey) {
        List<CareRelationship> relationships = careRelationshipRepository.findAllByCaregiverMemberKey(caregiverKey);

        List<String> wardKeys = relationships.stream()
                .map(CareRelationship::getWardMemberKey)
                .toList();

        Map<String, Member> memberMap = memberReader.findAllByMemberKeys(wardKeys);

        return relationships.stream()
                .map(r -> {
                    Member ward = memberMap.get(r.getWardMemberKey());
                    return new WardInfo(ward.getMemberKey(), ward.getName(), ward.getPhone(), r.getCareRole());
                })
                .toList();
    }

    public List<CaregiverInfo> findCaregiverInfos(String wardKey) {
        List<CareRelationship> relationships = careRelationshipRepository.findAllByWardMemberKey(wardKey);

        List<String> caregiverKeys = relationships.stream()
                .map(CareRelationship::getCaregiverMemberKey)
                .toList();

        Map<String, Member> memberMap = memberReader.findAllByMemberKeys(caregiverKeys);

        return relationships.stream()
                .map(r -> {
                    Member caregiver = memberMap.get(r.getCaregiverMemberKey());
                    return new CaregiverInfo(caregiver.getMemberKey(), caregiver.getName(), caregiver.getPhone(), r.getCareRole());
                })
                .toList();
    }
}
