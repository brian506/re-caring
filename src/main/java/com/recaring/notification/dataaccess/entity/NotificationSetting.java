package com.recaring.notification.dataaccess.entity;

import com.recaring.common.entity.BaseEntity;
import com.recaring.notification.vo.AnomalySensitivity;
import com.recaring.notification.vo.BatteryThreshold;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@Getter
@Entity
@Table(
        name = "notification_settings",
        uniqueConstraints = @UniqueConstraint(columnNames = "ward_member_key")
)
@SQLDelete(sql = "UPDATE notification_settings SET deleted_at = NOW() WHERE notification_setting_id = ?")
@SQLRestriction("deleted_at IS NULL")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NotificationSetting extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notification_setting_id")
    private Long id;

    @Column(name = "ward_member_key", nullable = false)
    private String wardMemberKey;

    @Column(name = "safe_zone_entry_enabled", nullable = false)
    private boolean safeZoneEntryEnabled;

    @Column(name = "safe_zone_exit_enabled", nullable = false)
    private boolean safeZoneExitEnabled;

    @Column(name = "route_deviation_enabled", nullable = false)
    private boolean routeDeviationEnabled;

    @Column(name = "speed_anomaly_enabled", nullable = false)
    private boolean speedAnomalyEnabled;

    @Column(name = "wandering_anomaly_enabled", nullable = false)
    private boolean wanderingAnomalyEnabled;

    @Enumerated(EnumType.STRING)
    @Column(name = "anomaly_sensitivity", nullable = false, length = 20)
    private AnomalySensitivity anomalySensitivity;

    @Column(name = "emergency_call_enabled", nullable = false)
    private boolean emergencyCallEnabled;

    @Column(name = "low_battery_enabled", nullable = false)
    private boolean lowBatteryEnabled;

    @Column(name = "battery_threshold_percent", nullable = false)
    private int batteryThresholdPercent;

    @Builder
    public NotificationSetting(
            String wardMemberKey,
            boolean safeZoneEntryEnabled,
            boolean safeZoneExitEnabled,
            boolean routeDeviationEnabled,
            boolean speedAnomalyEnabled,
            boolean wanderingAnomalyEnabled,
            AnomalySensitivity anomalySensitivity,
            boolean emergencyCallEnabled,
            boolean lowBatteryEnabled,
            int batteryThresholdPercent
    ) {
        this.wardMemberKey = wardMemberKey;
        this.safeZoneEntryEnabled = safeZoneEntryEnabled;
        this.safeZoneExitEnabled = safeZoneExitEnabled;
        this.routeDeviationEnabled = routeDeviationEnabled;
        this.speedAnomalyEnabled = speedAnomalyEnabled;
        this.wanderingAnomalyEnabled = wanderingAnomalyEnabled;
        this.anomalySensitivity = anomalySensitivity;
        this.emergencyCallEnabled = emergencyCallEnabled;
        this.lowBatteryEnabled = lowBatteryEnabled;
        this.batteryThresholdPercent = batteryThresholdPercent;
    }

    public static NotificationSetting defaultFor(String wardMemberKey) {
        return NotificationSetting.builder()
                .wardMemberKey(wardMemberKey)
                .safeZoneEntryEnabled(true)
                .safeZoneExitEnabled(true)
                .routeDeviationEnabled(true)
                .speedAnomalyEnabled(true)
                .wanderingAnomalyEnabled(true)
                .anomalySensitivity(AnomalySensitivity.DEFAULT)
                .emergencyCallEnabled(true)
                .lowBatteryEnabled(true)
                .batteryThresholdPercent(BatteryThreshold.DEFAULT.percent())
                .build();
    }

    public void updateSafeZone(boolean entryEnabled, boolean exitEnabled) {
        this.safeZoneEntryEnabled = entryEnabled;
        this.safeZoneExitEnabled = exitEnabled;
        update();
    }

    public void updateAnomaly(
            boolean routeDeviationEnabled,
            boolean speedAnomalyEnabled,
            boolean wanderingAnomalyEnabled,
            AnomalySensitivity sensitivity
    ) {
        this.routeDeviationEnabled = routeDeviationEnabled;
        this.speedAnomalyEnabled = speedAnomalyEnabled;
        this.wanderingAnomalyEnabled = wanderingAnomalyEnabled;
        this.anomalySensitivity = sensitivity;
        update();
    }

    public void updateEmergencyCall(boolean enabled) {
        this.emergencyCallEnabled = enabled;
        update();
    }

    public void updateBattery(boolean lowBatteryEnabled, BatteryThreshold threshold) {
        this.lowBatteryEnabled = lowBatteryEnabled;
        this.batteryThresholdPercent = threshold.percent();
        update();
    }
}
