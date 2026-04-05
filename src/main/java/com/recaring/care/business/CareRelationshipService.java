package com.recaring.care.business;

import com.recaring.care.dataaccess.entity.CareRelationship;
import com.recaring.care.implement.CareRelationshipReader;
import com.recaring.member.dataaccess.entity.Member;
import com.recaring.member.implement.MemberReader;
import com.recaring.support.exception.AppException;
import com.recaring.support.exception.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CareRelationshipService {

    private final CareRelationshipReader careRelationshipReader;
    private final MemberReader memberReader;

    /**
     * 내가 보호자/관리자인 보호 대상자 목록 조회
     */
    public List<WardInfo> getMyWards(String memberKey) {
        List<CareRelationship> relationships = careRelationshipReader.findAllByCaregiverKey(memberKey);

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

    /**
     * 특정 보호 대상자의 보호자/관리자 목록 조회
     * 본인(보호 대상자) 또는 해당 대상자의 보호자만 조회 가능
     */
    public List<CaregiverInfo> getCaregivers(String wardKey, String requesterKey) {
        boolean isWardSelf = wardKey.equals(requesterKey);
        boolean isGuardian = careRelationshipReader.isGuardianOf(requesterKey, wardKey);

        if (!isWardSelf && !isGuardian) {
            throw new AppException(ErrorType.NOT_GUARDIAN_OF_WARD);
        }

        List<CareRelationship> relationships = careRelationshipReader.findAllByWardKey(wardKey);

        List<String> caregiverKeys = relationships.stream()
                .map(CareRelationship::getCaregiverKey)
                .toList();

        Map<String, Member> memberMap = memberReader.findAllByMemberKeys(caregiverKeys);

        return relationships.stream()
                .map(r -> {
                    Member caregiver = memberMap.get(r.getCaregiverKey());
                    return new CaregiverInfo(caregiver.getMemberKey(), caregiver.getName(), caregiver.getPhone(), r.getCareRole());
                })
                .toList();
    }
}
