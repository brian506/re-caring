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

    public CareRole findCareRole(String wardKey, String caregiverKey) {
        return careRelationshipRepository.findAllByWardMemberKey(wardKey)
                .stream()
                .filter(relationship -> Objects.equals(caregiverKey, relationship.getCaregiverMemberKey()))
                .map(CareRelationship::getCareRole)
                .findFirst()
                .orElseThrow(() -> new AppException(ErrorType.NOT_CARE_RELATED_WARD));
    }
}
