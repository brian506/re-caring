package com.recaring.location.dataaccess.repository;

import com.recaring.location.dataaccess.entity.AnomalyDetection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.recaring.location.vo.DetectionType;

import java.time.LocalDateTime;
import java.util.Optional;

public interface AnomalyDetectionRepository extends JpaRepository<AnomalyDetection, Long> {

    // ON CONFLICT는 uk_anomaly_detections_ward_type_recorded를 arbiter로 사용하므로 해당 제약이 반드시 존재해야 한다.
    // 재배달된 중복은 0을 돌려주고, 신규 저장만 1이 된다.
    @Modifying
    @Query(
            value = """
                    INSERT INTO anomaly_detections (
                        created_at, updated_at, ward_member_key, detection_type,
                        score, recorded_at, latitude, longitude, evidence,
                        alert_kind, event_id, event_started_at, event_ended_at, categories, reason_code,
                        duration_min, usual_min, speed_kmh, event_hour, remind_seq
                    )
                    VALUES (
                        NOW(), NOW(), :wardMemberKey, :detectionType,
                        :score, :recordedAt, :latitude, :longitude, :evidence,
                        :alertKind, :eventId, :eventStartedAt, :eventEndedAt, :categories, :reasonCode,
                        :durationMin, :usualMin, :speedKmh, :eventHour, :remindSeq
                    )
                    ON CONFLICT (ward_member_key, detection_type, recorded_at) DO NOTHING
                    """,
            nativeQuery = true
    )
    int insertIfAbsent(
            @Param("wardMemberKey") String wardMemberKey,
            @Param("detectionType") String detectionType,
            @Param("score") double score,
            @Param("recordedAt") LocalDateTime recordedAt,
            @Param("latitude") double latitude,
            @Param("longitude") double longitude,
            @Param("evidence") String evidence,
            @Param("alertKind") String alertKind,
            @Param("eventId") String eventId,
            @Param("eventStartedAt") LocalDateTime eventStartedAt,
            @Param("eventEndedAt") LocalDateTime eventEndedAt,
            @Param("categories") String categories,
            @Param("reasonCode") String reasonCode,
            @Param("durationMin") Integer durationMin,
            @Param("usualMin") Integer usualMin,
            @Param("speedKmh") Integer speedKmh,
            @Param("eventHour") Integer eventHour,
            @Param("remindSeq") Integer remindSeq
    );

    @Query("""
            select a.id from AnomalyDetection a
            where a.wardMemberKey = :wardMemberKey
              and a.detectionType = :detectionType
              and a.recordedAt = :recordedAt
            """)
    Optional<Long> findIdByDetectionKeys(
            @Param("wardMemberKey") String wardMemberKey,
            @Param("detectionType") DetectionType detectionType,
            @Param("recordedAt") LocalDateTime recordedAt
    );

    void deleteByWardMemberKey(String wardMemberKey);
}
