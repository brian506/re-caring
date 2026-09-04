package com.recaring.care.implement;

import com.recaring.care.dataaccess.entity.CareRelationship;
import com.recaring.care.dataaccess.entity.CareRole;
import com.recaring.care.dataaccess.repository.CareRelationshipRepository;
import com.recaring.care.vo.CareRelationshipRegistration;
import com.recaring.member.dataaccess.entity.Member;
import com.recaring.member.dataaccess.entity.MemberRole;
import com.recaring.member.implement.MemberReader;
import com.recaring.support.exception.AppException;
import com.recaring.support.exception.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class CareRelationshipWriter {

    private final CareRelationshipRepository careRelationshipRepository;
    private final MemberReader memberReader;
    private final CareRelationshipValidator relationshipValidator;

    @CacheEvict(value = "careRelationship", allEntries = true)
    @Transactional
    public void register(CareRelationshipRegistration registration, String memberKey) {
        Member member = memberReader.findForUpdate(memberKey);
        if (member.getRole() == MemberRole.GUARDIAN) {
            relationshipValidator.validateCanAddWard(memberKey, registration.wardMemberKey());
        }
        if (registration.careRole() == CareRole.PRIMARY_GUARDIAN) {
            relationshipValidator.validateNoPrimaryGuardian(registration.wardMemberKey());
        }
        //todo 보호 대상자의 보호자 수 제한?
        careRelationshipRepository.save(
                CareRelationship.of(registration.wardMemberKey(), registration.caregiverKey(), registration.careRole())
        );
    }

    @CacheEvict(value = "careRelationship", allEntries = true)
    @Transactional
    public void delete(String wardKey, String caregiverKey) {
        careRelationshipRepository.delete(findRelationship(wardKey, caregiverKey));
    }

    @Transactional
    public void updateWardNickname(String wardKey, String caregiverKey, String wardNickname) {
        findRelationship(wardKey, caregiverKey).changeWardNickname(wardNickname);
    }

    @CacheEvict(value = "careRelationship", allEntries = true)
    @Transactional
    public void updateCareRole(String wardKey, String caregiverKey, CareRole careRole) {
        findRelationship(wardKey, caregiverKey).changeCareRole(careRole);
    }

    /**
     * 주보호자가 떠나면 남은 보호자·관계자를 정리할 주체가 사라진다 — 주보호자 자리는 넘길 수도 없고
     * 대상자 본인에게 회수 수단도 없다. 그래서 주보호자의 이탈은 그 대상자의 케어 관계 전체를 끝낸다.
     * 남은 사람들은 모두 주보호자가 초대해 들어온 사람들이다.
     */
    @CacheEvict(value = "careRelationship", allEntries = true)
    @Transactional
    public void deleteWithRemainingCaregivers(String wardKey, String caregiverKey) {
        CareRelationship relationship = findRelationship(wardKey, caregiverKey);
        if (relationship.getCareRole() != CareRole.PRIMARY_GUARDIAN) {
            careRelationshipRepository.delete(relationship);
            return;
        }
        careRelationshipRepository.deleteAllByWardMemberKey(wardKey);
    }

    @CacheEvict(value = "careRelationship", allEntries = true)
    @Transactional
    public void deleteAllByMemberKey(String memberKey) {
        careRelationshipRepository.deleteAllByMemberKey(memberKey);
    }

    private CareRelationship findRelationship(String wardKey, String caregiverKey) {
        return careRelationshipRepository
                .findCareRelationship(wardKey, caregiverKey)
                .orElseThrow(() -> new AppException(ErrorType.NOT_FOUND_CARE_RELATIONSHIP));
    }
}
