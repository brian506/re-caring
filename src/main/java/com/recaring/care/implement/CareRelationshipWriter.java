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
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
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
        //todo 보호 대상자의 보호자 수 제한?
        careRelationshipRepository.save(
                CareRelationship.of(registration.wardMemberKey(), registration.caregiverKey(),
                        resolveCareRole(registration))
        );
    }

    /**
     * 주보호자가 없는 대상자에 맺어지는 관계는 역할과 무관하게 주보호자가 된다.
     * 관계자로 들어오게 두면 그를 내보낼 주체가 없는 대상자가 만들어진다.
     */
    private CareRole resolveCareRole(CareRelationshipRegistration registration) {
        if (registration.careRole() == CareRole.PRIMARY_GUARDIAN) {
            relationshipValidator.validateNoPrimaryGuardian(registration.wardMemberKey());
            return CareRole.PRIMARY_GUARDIAN;
        }
        boolean hasPrimaryGuardian = careRelationshipRepository
                .existsCareRelationshipWithRole(registration.wardMemberKey(), CareRole.PRIMARY_GUARDIAN);
        if (hasPrimaryGuardian) {
            return registration.careRole();
        }
        log.info("[케어 관계 : 주보호자 부재로 승격]: wardKey={} | caregiverKey={} | requestedRole={}",
                registration.wardMemberKey(), registration.caregiverKey(), registration.careRole());
        return CareRole.PRIMARY_GUARDIAN;
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

    private CareRelationship findRelationship(String wardKey, String caregiverKey) {
        return careRelationshipRepository
                .findCareRelationship(wardKey, caregiverKey)
                .orElseThrow(() -> new AppException(ErrorType.NOT_FOUND_CARE_RELATIONSHIP));
    }
}
