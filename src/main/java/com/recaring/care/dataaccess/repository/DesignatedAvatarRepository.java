package com.recaring.care.dataaccess.repository;

import com.recaring.care.dataaccess.entity.DesignatedAvatar;
import com.recaring.care.dataaccess.repository.custom.DesignatedAvatarRepositoryCustom;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DesignatedAvatarRepository extends JpaRepository<DesignatedAvatar, Long>,
        DesignatedAvatarRepositoryCustom {

    List<DesignatedAvatar> findAllByOwnerMemberKey(String ownerMemberKey);

    List<DesignatedAvatar> findAllByOwnerMemberKeyAndWardMemberKey(String ownerMemberKey, String wardMemberKey);

    Optional<DesignatedAvatar> findByOwnerMemberKeyAndWardMemberKeyAndTargetMemberKey(
            String ownerMemberKey, String wardMemberKey, String targetMemberKey);
}
