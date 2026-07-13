package com.recaring.location.dataaccess.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "gps_histories")
// TODO: CREATE INDEX idx_gps_ward_recorded ON gps_histories (ward_member_key, recorded_at);
@EntityListeners(AuditingEntityListener.class)
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

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime recordedAt;

    @Column
    private Double accuracy;

    @Column
    private Integer battery;

    @Builder
    public GpsHistory(String wardMemberKey, double latitude, double longitude, Double accuracy, Integer battery) {
        this.wardMemberKey = wardMemberKey;
        this.latitude = latitude;
        this.longitude = longitude;
        this.accuracy = accuracy;
        this.battery = battery;
    }
}
