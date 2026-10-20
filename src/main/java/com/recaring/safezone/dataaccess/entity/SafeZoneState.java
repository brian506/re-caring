package com.recaring.safezone.dataaccess.entity;

import com.recaring.common.entity.BaseEntity;
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

/**
 * WARD가 현재 어느 안심존 안에 있는지를 저장한다. 진입·이탈은 이 직전 상태와의 차이로만 판정하므로,
 * 존 안에 머무는 동안 같은 알림이 반복되지 않는다.
 * 행이 없는 경우(최초 관측)와 존 밖에 있는 경우(빈 문자열)를 구분해야 한다.
 * zoneVersions는 판정에 쓴 존별 버전(updatedAt)이다. 버전이 같은 존끼리만 진입·이탈을 비교한다.
 */
@Getter
@Entity
@Table(name = "safe_zone_states")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SafeZoneState extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "safe_zone_state_id")
    private Long id;

    @Column(nullable = false, unique = true)
    private String wardMemberKey;

    @Column(nullable = false, length = 2000)
    private String safeZoneKeys;

    @Column(columnDefinition = "text")
    private String zoneVersions;

    @Builder
    public SafeZoneState(String wardMemberKey, String safeZoneKeys, String zoneVersions) {
        this.wardMemberKey = wardMemberKey;
        this.safeZoneKeys = safeZoneKeys;
        this.zoneVersions = zoneVersions;
    }

    public void replace(String safeZoneKeys, String zoneVersions) {
        this.safeZoneKeys = safeZoneKeys;
        this.zoneVersions = zoneVersions;
        update();
    }
}
