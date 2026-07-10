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

    private static final int MAX_WARD_COUNT = 1;
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

        List<CareRelationship> careRelationships = careRelationshipRepository.findAllByCaregiverMemberKey(caregiverMemberKey);
        checkRoleLimit(careRelationships, CareRole.GUARDIAN, MAX_WARD_COUNT);
        validateNotDuplicated(careRelationships, CareRelationship::getWardMemberKey,newWardMemberKey);
    }

    public void validateCanAddManager(String requesterKey, String wardMemberKey, String newManagerKey) {
        validateGuardianRole(requesterKey, wardMemberKey);

        // memberValidator.validatePremium(requesterKey);
        List<CareRelationship> careRelationships = careRelationshipRepository.findAllByWardMemberKey(wardMemberKey);
        checkRoleLimit(careRelationships, CareRole.MANAGER, MAX_MANAGER_COUNT);
        validateNotDuplicated(careRelationships, CareRelationship::getCaregiverMemberKey, newManagerKey);
    }

    public void validateCanAddGuardian(String requesterKey, String wardMemberKey, String newGuardianKey) {
        validateGuardianRole(requesterKey, wardMemberKey);

        // memberValidator.validatePremium(requesterKey);
        List<CareRelationship> careRelationships = careRelationshipRepository.findAllByWardMemberKey(wardMemberKey);
        checkRoleLimit(careRelationships, CareRole.GUARDIAN, MAX_GUARDIAN_COUNT);
        validateNotDuplicated(careRelationships, CareRelationship::getCaregiverMemberKey, newGuardianKey);
    }

    public void validateCaregiverViewAccess(String requesterKey, String wardKey) {
        boolean isWardSelf = wardKey.equals(requesterKey);
        boolean isGuardian = careRelationshipRepository.existsCareRelationship(wardKey, requesterKey, CareRole.GUARDIAN);
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
        boolean isGuardian = careRelationshipRepository.existsCareRelationship(wardKey, requesterKey, CareRole.GUARDIAN);
        if (!isGuardian) {
            throw new AppException(ErrorType.NOT_GUARDIAN_ROLE_IN_CARE);
        }
    }

    private void checkRoleLimit(List<CareRelationship> relationships, CareRole role, int maxCount) {
        long count = relationships.stream()
                .filter(r -> r.getCareRole() == role)
                .count();
        if(count >= maxCount) {
            throw new AppException(ErrorType.CARE_CAREGIVER_LIMIT_EXCEEDED);
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
