package com.recaring.care.implement;

import com.recaring.care.dataaccess.entity.DesignatedAvatar;
import com.recaring.care.dataaccess.repository.DesignatedAvatarRepository;
import com.recaring.member.vo.ProfileAvatarCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class DesignatedAvatarManager {

    private final DesignatedAvatarRepository designatedAvatarRepository;

    /**
     * owner가 여러 대상자 화면에서 지정한 얼굴을 대상자 memberKey로 묶는다. 대상자 목록 조회용.
     */
    public Map<String, String> findWardAvatarCodes(String ownerMemberKey) {
        return designatedAvatarRepository.findAllByOwnerMemberKey(ownerMemberKey).stream()
                .filter(avatar -> avatar.getWardMemberKey().equals(avatar.getTargetMemberKey()))
                .collect(Collectors.toMap(
                        DesignatedAvatar::getWardMemberKey, DesignatedAvatar::getProfileAvatarCode));
    }

    /**
     * owner가 한 대상자 범위 안에서 지정한 얼굴을 대상 memberKey로 묶는다. 보호자·관계자 목록 조회용.
     */
    public Map<String, String> findAvatarCodesInWard(String ownerMemberKey, String wardMemberKey) {
        return designatedAvatarRepository.findAllByOwnerMemberKeyAndWardMemberKey(ownerMemberKey, wardMemberKey)
                .stream()
                .collect(Collectors.toMap(
                        DesignatedAvatar::getTargetMemberKey, DesignatedAvatar::getProfileAvatarCode));
    }

    @Transactional
    public void designate(String ownerMemberKey, String wardMemberKey, String targetMemberKey, ProfileAvatarCode code) {
        designatedAvatarRepository
                .findByOwnerMemberKeyAndWardMemberKeyAndTargetMemberKey(ownerMemberKey, wardMemberKey, targetMemberKey)
                .ifPresentOrElse(
                        existing -> applyToExisting(existing, code),
                        () -> saveIfPresent(ownerMemberKey, wardMemberKey, targetMemberKey, code)
                );
    }

    @Transactional
    public void deleteAllByRelationship(String wardMemberKey, String caregiverMemberKey) {
        designatedAvatarRepository.deleteAllByRelationship(wardMemberKey, caregiverMemberKey);
    }

    @Transactional
    public void deleteAllByMemberKey(String memberKey) {
        designatedAvatarRepository.deleteAllByMemberKey(memberKey);
    }

    private void applyToExisting(DesignatedAvatar existing, ProfileAvatarCode code) {
        if (code.isCleared()) {
            designatedAvatarRepository.delete(existing);
            return;
        }
        existing.changeProfileAvatarCode(code.value());
    }

    private void saveIfPresent(String ownerMemberKey, String wardMemberKey, String targetMemberKey, ProfileAvatarCode code) {
        if (code.isCleared()) {
            return;
        }
        designatedAvatarRepository.save(DesignatedAvatar.builder()
                .ownerMemberKey(ownerMemberKey)
                .wardMemberKey(wardMemberKey)
                .targetMemberKey(targetMemberKey)
                .profileAvatarCode(code.value())
                .build());
    }
}
