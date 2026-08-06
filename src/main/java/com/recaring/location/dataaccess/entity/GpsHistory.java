package com.recaring.location.dataaccess.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "gps_histories")
// TODO: CREATE INDEX idx_gps_ward_recorded ON gps_histories (ward_member_key, recorded_at);
// TODO: CREATE INDEX idx_gps_ward_measured ON gps_histories (ward_member_key, measured_at);
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GpsHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "gps_history_id")
    private Long id;

    @Column(nullable = false)
    private String wardMemberKey;

    @Column(nullable = false)
    private double latitude;

    @Column(nullable = false)
    private double longitude;

    // Server-side receive time. Not the device measurement time — use measuredAt for that.
    @Column(nullable = false, updatable = false)
    private LocalDateTime recordedAt;

    @Column
    private Double accuracy;

    @Column
    private Integer battery;

    @Column
    private Double speed;

    // Device measurement time in UTC. Null means the device did not report one,
    // so any time-interval based analysis must exclude this row.
    @Column
    private LocalDateTime measuredAt;

    @Builder
    public GpsHistory(String wardMemberKey, double latitude, double longitude, LocalDateTime recordedAt,
                      Double accuracy, Integer battery, Double speed, LocalDateTime measuredAt) {
        this.wardMemberKey = wardMemberKey;
        this.latitude = latitude;
        this.longitude = longitude;
        this.recordedAt = recordedAt;
        this.accuracy = accuracy;
        this.battery = battery;
        this.speed = speed;
        this.measuredAt = measuredAt;
    }
}
