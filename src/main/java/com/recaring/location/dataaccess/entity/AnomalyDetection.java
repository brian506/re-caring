package com.recaring.location.dataaccess.entity;

import com.recaring.common.entity.BaseEntity;
import com.recaring.location.vo.AnomalyAlertKind;
import com.recaring.location.vo.AnomalyReasonCode;
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
                name = "uk_anomaly_detections_ward_type_recorded",
                columnNames = {"ward_member_key", "detection_type", "recorded_at"})
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
    private LocalDateTime recordedAt;

    @Column(nullable = false)
    private double latitude;

    @Column(nullable = false)
    private double longitude;

    @Column(length = 1000)
    private String evidence;

    @Enumerated(EnumType.STRING)
    @Column(name = "alert_kind", length = 30)
    private AnomalyAlertKind alertKind;

    // TODO: CREATE INDEX idx_anomaly_detections_event_id ON anomaly_detections(event_id);
    @Column(name = "event_id", length = 100)
    private String eventId;

    @Column(name = "event_started_at")
    private LocalDateTime eventStartedAt;

    @Column(name = "event_ended_at")
    private LocalDateTime eventEndedAt;

    @Column(name = "categories", length = 200)
    private String categories;

    @Enumerated(EnumType.STRING)
    @Column(name = "reason_code", length = 50)
    private AnomalyReasonCode reasonCode;

    @Column(name = "duration_min")
    private Integer durationMin;

    @Column(name = "usual_min")
    private Integer usualMin;

    @Column(name = "speed_kmh")
    private Integer speedKmh;

    @Column(name = "event_hour")
    private Integer eventHour;

    @Column(name = "remind_seq")
    private Integer remindSeq;

    @Builder
    public AnomalyDetection(String wardMemberKey, DetectionType detectionType, double score,
                            LocalDateTime recordedAt, double latitude, double longitude, String evidence,
                            AnomalyAlertKind alertKind, String eventId, LocalDateTime eventStartedAt,
                            LocalDateTime eventEndedAt, String categories, AnomalyReasonCode reasonCode,
                            Integer durationMin, Integer usualMin, Integer speedKmh, Integer eventHour,
                            Integer remindSeq) {
        this.wardMemberKey = wardMemberKey;
        this.detectionType = detectionType;
        this.score = score;
        this.recordedAt = recordedAt;
        this.latitude = latitude;
        this.longitude = longitude;
        this.evidence = evidence;
        this.alertKind = alertKind;
        this.eventId = eventId;
        this.eventStartedAt = eventStartedAt;
        this.eventEndedAt = eventEndedAt;
        this.categories = categories;
        this.reasonCode = reasonCode;
        this.durationMin = durationMin;
        this.usualMin = usualMin;
        this.speedKmh = speedKmh;
        this.eventHour = eventHour;
        this.remindSeq = remindSeq;
    }
}
