package com.recaring.location.dataaccess.entity;

import com.recaring.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
        name = "location_settings",
        uniqueConstraints = @UniqueConstraint(columnNames = "ward_member_key")
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LocationSetting extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "location_setting_id")
    private Long id;

    @Column(name = "ward_member_key", nullable = false)
    private String wardMemberKey;

    @Column(name = "collection_interval_seconds", nullable = false)
    private int collectionIntervalSeconds;

    @Builder
    public LocationSetting(String wardMemberKey, int collectionIntervalSeconds) {
        this.wardMemberKey = wardMemberKey;
        this.collectionIntervalSeconds = collectionIntervalSeconds;
    }

    public void updateCollectionIntervalSeconds(int collectionIntervalSeconds) {
        this.collectionIntervalSeconds = collectionIntervalSeconds;
        update();
    }
}
