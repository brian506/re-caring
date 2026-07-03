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

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(
            value = """
                    INSERT INTO notification_settings (
                        created_at,
                        updated_at,
                        version,
                        ward_member_key,
                        safe_zone_entry_enabled,
                        safe_zone_exit_enabled,
                        route_deviation_enabled,
                        speed_anomaly_enabled,
                        wandering_anomaly_enabled,
                        anomaly_sensitivity,
                        emergency_call_enabled,
                        low_battery_enabled,
                        battery_threshold_percent
                    )
                    VALUES (
                        NOW(),
                        NOW(),
                        0,
                        :wardMemberKey,
                        TRUE,
                        TRUE,
                        TRUE,
                        TRUE,
                        TRUE,
                        'NORMAL',
                        TRUE,
                        TRUE,
                        25
                    )
                    ON CONFLICT (ward_member_key) DO NOTHING
                    """,
            nativeQuery = true
    )
    int insertDefaultIfAbsent(@Param("wardMemberKey") String wardMemberKey);
}
