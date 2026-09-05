package com.recaring.care.implement;

import com.recaring.care.dataaccess.entity.CareRelationship;
import com.recaring.care.dataaccess.entity.CareRole;
import com.recaring.care.dataaccess.repository.CareRelationshipRepository;
import com.recaring.member.implement.MemberValidator;
import com.recaring.support.exception.AppException;
import com.recaring.support.exception.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Function;

@Component
@RequiredArgsConstructor
public class CareRelationshipValidator {

    private static final int MAX_WARD_COUNT = 5;
    private static final int MAX_CAREGIVER_COUNT = 5;

    private final CareRelationshipRepository careRelationshipRepository;
    private final MemberValidator memberValidator;


    /**
     * 한도는 두 축이다.
     * - 보호자 1명이 맡을 수 있는 대상자: 최대 MAX_WARD_COUNT 명
     * - 대상자 1명에 붙을 수 있는 케어 관계: 역할과 무관하게 최대 MAX_CAREGIVER_COUNT 명
     * 주보호자도 관계 하나로 함께 센다. 역할별 정원을 따로 두지 않으므로 역할 변경은 한도에 영향이 없다.
     */

    public void validateCanAddWard(String caregiverMemberKey, String newWardMemberKey) {
        memberValidator.validateSubscription(caregiverMemberKey);

        List<CareRelationship> careRelationships = careRelationshipRepository.findAllByCaregiverMemberKey(caregiverMemberKey);
        checkGuardianLimit(careRelationships, MAX_WARD_COUNT, ErrorType.CARE_WARD_LIMIT_EXCEEDED);
        validateNotDuplicated(careRelationships, CareRelationship::getWardMemberKey,newWardMemberKey);
    }

    /**
     * 대상자를 새로 등록하는 경로에서만 쓴다. 주보호자는 승격으로만 늘려야 하며,
     * 전화번호만 아는 제3자가 대상자 추가 요청으로 주보호자 자리를 차지할 수 없어야 한다.
     */
    public void validateNoPrimaryGuardian(String wardMemberKey) {
        if (careRelationshipRepository.existsCareRelationshipWithRole(wardMemberKey, CareRole.PRIMARY_GUARDIAN)) {
            throw new AppException(ErrorType.WARD_ALREADY_HAS_PRIMARY_GUARDIAN);
        }
    }

    public void validateCanAddCaregiver(String requesterKey, String wardMemberKey, String newCaregiverKey) {
        validatePrimaryGuardianRole(requesterKey, wardMemberKey);

        // memberValidator.validatePremium(requesterKey);
        List<CareRelationship> careRelationships = careRelationshipRepository.findAllByWardMemberKey(wardMemberKey);
        checkCaregiverLimit(careRelationships);
        validateNotDuplicated(careRelationships, CareRelationship::getCaregiverMemberKey, newCaregiverKey);
    }

    public void validateCaregiverViewAccess(String requesterKey, String wardKey) {
        boolean isWardSelf = wardKey.equals(requesterKey);
        boolean isGuardian = careRelationshipRepository.existsCareRelationshipInRoles(wardKey, requesterKey, CareRole.guardianRoles());
        if (!isWardSelf && !isGuardian) {
            throw new AppException(ErrorType.NOT_GUARDIAN_OF_WARD);
        }
    }

    public void validateCaregiver(String requesterKey, String wardKey) {
        boolean isCaregiver = careRelationshipRepository.existsCareRelationship(wardKey, requesterKey);
        if (!isCaregiver) {
            throw new AppException(ErrorType.NOT_FOUND_CARE_RELATIONSHIP);
        }
    }

    public void validatePrimaryGuardianRole(String requesterKey, String wardKey) {
        boolean isPrimaryGuardian = careRelationshipRepository.existsCareRelationship(wardKey, requesterKey, CareRole.PRIMARY_GUARDIAN);
        if (!isPrimaryGuardian) {
            throw new AppException(ErrorType.NOT_PRIMARY_GUARDIAN_ROLE_IN_CARE);
        }
    }

    /**
     * 주보호자로의 승격은 허용하고, 주보호자를 다른 역할로 내리는 것만 막는다.
     * 요청자 본인도 주보호자라 자기 역할을 바꾸려는 시도는 같은 규칙에 걸린다.
     * 역할 변경은 관계 수를 바꾸지 않으므로 인원 한도는 보지 않는다.
     */
    public void validateCareRoleChange(String wardKey, String targetCaregiverKey) {
        CareRole currentCareRole = findCareRole(
                careRelationshipRepository.findAllByWardMemberKey(wardKey), targetCaregiverKey);
        if (currentCareRole == CareRole.PRIMARY_GUARDIAN) {
            throw new AppException(ErrorType.CANNOT_CHANGE_PRIMARY_GUARDIAN_ROLE);
        }
    }

    /**
     * 주보호자는 스스로 떠날 수만 있다. 남이 내보낼 수 있게 하면 승격시켜 준 쪽이
     * 언제든 되물릴 수 있어 주보호자끼리 서로를 밀어내는 상태가 만들어진다.
     */
    public void validateCaregiverRemovable(String wardKey, String targetCaregiverKey) {
        CareRole currentCareRole = findCareRole(
                careRelationshipRepository.findAllByWardMemberKey(wardKey), targetCaregiverKey);
        if (currentCareRole == CareRole.PRIMARY_GUARDIAN) {
            throw new AppException(ErrorType.CANNOT_REMOVE_PRIMARY_GUARDIAN);
        }
    }

    private CareRole findCareRole(List<CareRelationship> careRelationships, String caregiverKey) {
        return careRelationships.stream()
                .filter(relationship -> relationship.getCaregiverMemberKey().equals(caregiverKey))
                .map(CareRelationship::getCareRole)
                .findFirst()
                .orElseThrow(() -> new AppException(ErrorType.NOT_FOUND_CARE_RELATIONSHIP));
    }

    private void checkCaregiverLimit(List<CareRelationship> relationships) {
        if (relationships.size() >= MAX_CAREGIVER_COUNT) {
            throw new AppException(ErrorType.CARE_CAREGIVER_LIMIT_EXCEEDED);
        }
    }

    private void checkGuardianLimit(List<CareRelationship> relationships, int maxCount, ErrorType errorType) {
        long count = relationships.stream()
                .filter(r -> r.getCareRole().isGuardian())
                .count();
        if (count >= maxCount) {
            throw new AppException(errorType);
        }
    }

    private void validateNotDuplicated(List<CareRelationship> careRelationships, Function<CareRelationship, String> key, String targetKey) {
        boolean isDuplicated = careRelationships.stream()
                .map(key)
                .anyMatch(targetKey::equals);

        if (isDuplicated) {
            throw new AppException(ErrorType.ALREADY_CARE_RELATIONSHIP);
        }
    }

}
