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
    private static final int MAX_MANAGER_COUNT = 3;
    private static final int MAX_GUARDIAN_COUNT = 1;

    private final CareRelationshipRepository careRelationshipRepository;
    private final MemberValidator memberValidator;


    /**
     *  베이식 - 보호 대상자 1명만 추가 가능
     *  프리미엄 -  보호자1 -> 대상자 1명 추가
     * and 추가한 대상자의 보호자2 추가 - 최대 1명
     * and 관리자 3명 추가 가능 - 최대 3명
     */

    public void validateCanAddWard(String caregiverMemberKey, String newWardMemberKey) {
        memberValidator.validateSubscription(caregiverMemberKey);
        validateNoPrimaryGuardian(newWardMemberKey);

        List<CareRelationship> careRelationships = careRelationshipRepository.findAllByCaregiverMemberKey(caregiverMemberKey);
        checkGuardianLimit(careRelationships, MAX_WARD_COUNT, ErrorType.CARE_WARD_LIMIT_EXCEEDED);
        validateNotDuplicated(careRelationships, CareRelationship::getWardMemberKey,newWardMemberKey);
    }

    /**
     * 주보호자는 대상자당 1명이어야 한다. 이 불변식이 깨지면 서로 삭제도 강등도 못 하는 주보호자가 공존해,
     * 잘못 맺어진 관계를 끊을 주체가 사라진다.
     */
    public void validateNoPrimaryGuardian(String wardMemberKey) {
        if (careRelationshipRepository.existsCareRelationshipWithRole(wardMemberKey, CareRole.PRIMARY_GUARDIAN)) {
            throw new AppException(ErrorType.WARD_ALREADY_HAS_PRIMARY_GUARDIAN);
        }
    }

    public void validateCanAddManager(String requesterKey, String wardMemberKey, String newManagerKey) {
        validatePrimaryGuardianRole(requesterKey, wardMemberKey);

        // memberValidator.validatePremium(requesterKey);
        List<CareRelationship> careRelationships = careRelationshipRepository.findAllByWardMemberKey(wardMemberKey);
        checkRoleLimit(careRelationships, CareRole.MANAGER, MAX_MANAGER_COUNT, ErrorType.CARE_CAREGIVER_LIMIT_EXCEEDED);
        validateNotDuplicated(careRelationships, CareRelationship::getCaregiverMemberKey, newManagerKey);
    }

    public void validateCanAddGuardian(String requesterKey, String wardMemberKey, String newGuardianKey) {
        validatePrimaryGuardianRole(requesterKey, wardMemberKey);

        // memberValidator.validatePremium(requesterKey);
        List<CareRelationship> careRelationships = careRelationshipRepository.findAllByWardMemberKey(wardMemberKey);
        checkRoleLimit(careRelationships, CareRole.GUARDIAN, MAX_GUARDIAN_COUNT, ErrorType.CARE_CAREGIVER_LIMIT_EXCEEDED);
        validateNotDuplicated(careRelationships, CareRelationship::getCaregiverMemberKey, newGuardianKey);
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

    public void validateGuardianRole(String requesterKey, String wardKey) {
        boolean isGuardian = careRelationshipRepository.existsCareRelationshipInRoles(wardKey, requesterKey, CareRole.guardianRoles());
        if (!isGuardian) {
            throw new AppException(ErrorType.NOT_GUARDIAN_ROLE_IN_CARE);
        }
    }

    public void validatePrimaryGuardianRole(String requesterKey, String wardKey) {
        boolean isPrimaryGuardian = careRelationshipRepository.existsCareRelationship(wardKey, requesterKey, CareRole.PRIMARY_GUARDIAN);
        if (!isPrimaryGuardian) {
            throw new AppException(ErrorType.NOT_PRIMARY_GUARDIAN_ROLE_IN_CARE);
        }
    }

    /**
     * 주보호자는 대상자를 등록한 본인이라 역할을 넘겨줄 수 없고, 다른 사람을 주보호자로 올릴 수도 없다.
     * 넘길 수 있게 하면 주보호자가 0명이 되는 순간 보호자·관계자를 추가할 주체가 사라진다.
     */
    public void validateCareRoleChange(String wardKey, String targetCaregiverKey, CareRole newCareRole) {
        if (newCareRole == CareRole.PRIMARY_GUARDIAN) {
            throw new AppException(ErrorType.INVALID_TARGET_CARE_ROLE);
        }

        List<CareRelationship> careRelationships = careRelationshipRepository.findAllByWardMemberKey(wardKey);
        CareRole currentCareRole = findCareRole(careRelationships, targetCaregiverKey);

        if (currentCareRole == CareRole.PRIMARY_GUARDIAN) {
            throw new AppException(ErrorType.CANNOT_CHANGE_PRIMARY_GUARDIAN_ROLE);
        }
        if (currentCareRole == newCareRole) {
            return;
        }

        int maxCount = newCareRole == CareRole.GUARDIAN ? MAX_GUARDIAN_COUNT : MAX_MANAGER_COUNT;
        checkRoleLimit(careRelationships, newCareRole, maxCount, ErrorType.CARE_CAREGIVER_LIMIT_EXCEEDED);
    }

    /**
     * 주보호자를 내보낼 수 있으면 관리 주체가 없는 대상자가 생긴다. 역할 변경과 같은 불변식을 삭제에도 건다.
     * 주보호자 본인이 관계를 끊는 것은 removeWard 경로라 여기서 막히지 않는다.
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

    private void checkRoleLimit(List<CareRelationship> relationships, CareRole role, int maxCount, ErrorType errorType) {
        long count = relationships.stream()
                .filter(r -> r.getCareRole() == role)
                .count();
        if(count >= maxCount) {
            throw new AppException(errorType);
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
