package com.recaring.notification.dataaccess.entity;

import com.recaring.common.entity.BaseEntity;
import com.recaring.notification.vo.AnomalySensitivity;
import com.recaring.notification.vo.BatteryThresholds;
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

@Getter
@Entity
@Table(
        name = "notification_settings",
        uniqueConstraints = @UniqueConstraint(columnNames = "ward_member_key")
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NotificationSetting extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notification_setting_id")
    private Long id;

    @Column(name = "ward_member_key", nullable = false)
    private String wardMemberKey;

    // columnDefinition의 DEFAULT는 insertDefaultIfAbsent의 슬림 INSERT(키 컬럼만 지정)를 위한 것이다.
    // 기본값을 DB 스키마에 두어 네이티브 INSERT의 값 나열 중복을 제거한다.
    @Column(name = "safe_zone_entry_enabled", nullable = false, columnDefinition = "boolean not null default true")
    private boolean safeZoneEntryEnabled;

    @Column(name = "safe_zone_exit_enabled", nullable = false, columnDefinition = "boolean not null default true")
    private boolean safeZoneExitEnabled;

    @Column(name = "route_deviation_enabled", nullable = false, columnDefinition = "boolean not null default true")
    private boolean routeDeviationEnabled;

    @Column(name = "speed_anomaly_enabled", nullable = false, columnDefinition = "boolean not null default true")
    private boolean speedAnomalyEnabled;

    @Column(name = "wandering_anomaly_enabled", nullable = false, columnDefinition = "boolean not null default true")
    private boolean wanderingAnomalyEnabled;

    @Enumerated(EnumType.STRING)
    @Column(name = "route_deviation_sensitivity", nullable = false, columnDefinition = "varchar(20) not null default 'NORMAL'")
    private AnomalySensitivity routeDeviationSensitivity;

    @Enumerated(EnumType.STRING)
    @Column(name = "speed_anomaly_sensitivity", nullable = false, columnDefinition = "varchar(20) not null default 'NORMAL'")
    private AnomalySensitivity speedAnomalySensitivity;

    @Enumerated(EnumType.STRING)
    @Column(name = "wandering_anomaly_sensitivity", nullable = false, columnDefinition = "varchar(20) not null default 'NORMAL'")
    private AnomalySensitivity wanderingAnomalySensitivity;

    @Column(name = "emergency_call_enabled", nullable = false, columnDefinition = "boolean not null default true")
    private boolean emergencyCallEnabled;

    @Column(name = "low_battery_enabled", nullable = false, columnDefinition = "boolean not null default true")
    private boolean lowBatteryEnabled;

    // 빈 문자열이 기본이다. 고른 값이 없으면 배터리 알림을 보내지 않는다.
    @Column(name = "battery_threshold_percents", nullable = false, length = 64,
            columnDefinition = "varchar(64) not null default ''")
    private String batteryThresholdPercents;

    @Builder
    public NotificationSetting(
            String wardMemberKey,
            boolean safeZoneEntryEnabled,
            boolean safeZoneExitEnabled,
            boolean routeDeviationEnabled,
            boolean speedAnomalyEnabled,
            boolean wanderingAnomalyEnabled,
            AnomalySensitivity routeDeviationSensitivity,
            AnomalySensitivity speedAnomalySensitivity,
            AnomalySensitivity wanderingAnomalySensitivity,
            boolean emergencyCallEnabled,
            boolean lowBatteryEnabled,
            String batteryThresholdPercents
    ) {
        this.wardMemberKey = wardMemberKey;
        this.safeZoneEntryEnabled = safeZoneEntryEnabled;
        this.safeZoneExitEnabled = safeZoneExitEnabled;
        this.routeDeviationEnabled = routeDeviationEnabled;
        this.speedAnomalyEnabled = speedAnomalyEnabled;
        this.wanderingAnomalyEnabled = wanderingAnomalyEnabled;
        this.routeDeviationSensitivity = routeDeviationSensitivity;
        this.speedAnomalySensitivity = speedAnomalySensitivity;
        this.wanderingAnomalySensitivity = wanderingAnomalySensitivity;
        this.emergencyCallEnabled = emergencyCallEnabled;
        this.lowBatteryEnabled = lowBatteryEnabled;
        this.batteryThresholdPercents = batteryThresholdPercents;
    }

    public static NotificationSetting defaultFor(String wardMemberKey) {
        return NotificationSetting.builder()
                .wardMemberKey(wardMemberKey)
                .safeZoneEntryEnabled(true)
                .safeZoneExitEnabled(true)
                .routeDeviationEnabled(true)
                .speedAnomalyEnabled(true)
                .wanderingAnomalyEnabled(true)
                .routeDeviationSensitivity(AnomalySensitivity.DEFAULT)
                .speedAnomalySensitivity(AnomalySensitivity.DEFAULT)
                .wanderingAnomalySensitivity(AnomalySensitivity.DEFAULT)
                .emergencyCallEnabled(true)
                .lowBatteryEnabled(true)
                .batteryThresholdPercents(BatteryThresholds.NONE.format())
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
            AnomalySensitivity routeDeviationSensitivity,
            AnomalySensitivity speedAnomalySensitivity,
            AnomalySensitivity wanderingAnomalySensitivity
    ) {
        this.routeDeviationEnabled = routeDeviationEnabled;
        this.speedAnomalyEnabled = speedAnomalyEnabled;
        this.wanderingAnomalyEnabled = wanderingAnomalyEnabled;
        this.routeDeviationSensitivity = routeDeviationSensitivity;
        this.speedAnomalySensitivity = speedAnomalySensitivity;
        this.wanderingAnomalySensitivity = wanderingAnomalySensitivity;
        update();
    }

    public void updateEmergencyCall(boolean enabled) {
        this.emergencyCallEnabled = enabled;
        update();
    }

    public void updateBattery(boolean lowBatteryEnabled, BatteryThresholds thresholds) {
        this.lowBatteryEnabled = lowBatteryEnabled;
        this.batteryThresholdPercents = thresholds.format();
        update();
    }
}
