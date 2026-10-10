package com.recaring.safezone.dataaccess.entity;

import com.recaring.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@Entity
@Table(name = "safe_zones")
// TODO: CREATE INDEX idx_safe_zones_ward_member_key ON safe_zones (ward_member_key);
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SafeZone extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "safe_zone_id")
    private Long id;

    @Column(nullable = false, unique = true)
    private String safeZoneKey;

    @Column(nullable = false)
    private String wardMemberKey;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(nullable = false)
    private String address;

    @Column(nullable = false)
    private double latitude;

    @Column(nullable = false)
    private double longitude;

    @Column(nullable = false)
    private int radiusMeters;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private SafeZoneType type;

    @Builder
    public SafeZone(String wardMemberKey, String name, String address,
                    double latitude, double longitude, int radiusMeters, SafeZoneType type) {
        this.safeZoneKey = UUID.randomUUID().toString();
        this.wardMemberKey = wardMemberKey;
        this.name = name;
        this.address = address;
        this.latitude = latitude;
        this.longitude = longitude;
        this.radiusMeters = radiusMeters;
        this.type = type;
    }

    public void update(String name, String address, double latitude, double longitude,
                       int radiusMeters, SafeZoneType type) {
        this.name = name;
        this.address = address;
        this.latitude = latitude;
        this.longitude = longitude;
        this.radiusMeters = radiusMeters;
        this.type = type;
        update();
    }
}
