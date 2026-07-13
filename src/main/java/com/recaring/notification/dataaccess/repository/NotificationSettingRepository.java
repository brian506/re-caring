package com.recaring.notification.dataaccess.repository;

import com.recaring.notification.dataaccess.entity.NotificationSetting;
import com.recaring.notification.dataaccess.repository.custom.NotificationSettingRepositoryCustom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface NotificationSettingRepository extends JpaRepository<NotificationSetting, Long>,
        NotificationSettingRepositoryCustom {
    Optional<NotificationSetting> findByWardMemberKey(String wardMemberKey);

    // 나머지 컬럼은 스키마 DEFAULT(엔티티 columnDefinition / pending-ddl의 SET DEFAULT)로 채워진다.
    // ON CONFLICT는 ward_member_key UNIQUE 제약을 arbiter로 사용하므로 해당 제약이 반드시 존재해야 한다.
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(
            value = """
                    INSERT INTO notification_settings (created_at, updated_at, ward_member_key)
                    VALUES (NOW(), NOW(), :wardMemberKey)
                    ON CONFLICT (ward_member_key) DO NOTHING
                    """,
            nativeQuery = true
    )
    int insertDefaultIfAbsent(@Param("wardMemberKey") String wardMemberKey);
}
