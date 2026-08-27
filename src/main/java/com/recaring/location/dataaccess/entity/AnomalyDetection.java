package com.recaring.location.dataaccess.entity;

import com.recaring.common.entity.BaseEntity;
import com.recaring.location.vo.DetectionType;
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

import java.time.LocalDateTime;

@Getter
@Entity
@Table(
        name = "anomaly_detections",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_anomaly_detections_ward_type_detected",
                columnNames = {"ward_member_key", "detection_type", "detected_at"})
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AnomalyDetection extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "anomaly_detection_id")
    private Long id;

    @Column(name = "ward_member_key", nullable = false)
    private String wardMemberKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "detection_type", nullable = false, length = 50)
    private DetectionType detectionType;

    @Column(nullable = false)
    private double score;

    @Column(nullable = false, updatable = false)
    private LocalDateTime detectedAt;

    @Column(nullable = false)
    private double latitude;

    @Column(nullable = false)
    private double longitude;

    @Column(nullable = false, length = 1000)
    private String evidence;

    @Builder
    public AnomalyDetection(String wardMemberKey, DetectionType detectionType, double score,
                            LocalDateTime detectedAt, double latitude, double longitude, String evidence) {
        this.wardMemberKey = wardMemberKey;
        this.detectionType = detectionType;
        this.score = score;
        this.detectedAt = detectedAt;
        this.latitude = latitude;
        this.longitude = longitude;
        this.evidence = evidence;
    }
}
