package com.recaring.location.dataaccess.repository;

import com.recaring.location.dataaccess.entity.AnomalyDetection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface AnomalyDetectionRepository extends JpaRepository<AnomalyDetection, Long> {

    // ON CONFLICT는 uk_anomaly_detections_ward_type_detected를 arbiter로 사용하므로 해당 제약이 반드시 존재해야 한다.
    // 재배달된 중복은 0을 돌려주고, 신규 저장만 1이 된다.
    @Modifying
    @Query(
            value = """
                    INSERT INTO anomaly_detections (
                        created_at, updated_at, ward_member_key, detection_type,
                        score, detected_at, latitude, longitude, evidence
                    )
                    VALUES (
                        NOW(), NOW(), :wardMemberKey, :detectionType,
                        :score, :detectedAt, :latitude, :longitude, :evidence
                    )
                    ON CONFLICT (ward_member_key, detection_type, detected_at) DO NOTHING
                    """,
            nativeQuery = true
    )
    int insertIfAbsent(
            @Param("wardMemberKey") String wardMemberKey,
            @Param("detectionType") String detectionType,
            @Param("score") double score,
            @Param("detectedAt") LocalDateTime detectedAt,
            @Param("latitude") double latitude,
            @Param("longitude") double longitude,
            @Param("evidence") String evidence
    );

    void deleteByWardMemberKey(String wardMemberKey);
}
