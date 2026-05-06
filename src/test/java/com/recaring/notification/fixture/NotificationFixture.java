package com.recaring.notification.fixture;

import com.recaring.member.dataaccess.entity.Gender;
import com.recaring.member.dataaccess.entity.Member;
import com.recaring.member.dataaccess.entity.MemberRole;
import com.recaring.member.dataaccess.entity.SignUpType;
import com.recaring.notification.dataaccess.entity.NotificationSetting;
import com.recaring.notification.vo.AnomalySensitivity;

import java.time.LocalDate;

public class NotificationFixture {

    public static final String WARD_KEY = "ward-member-key-001";
    public static final String GUARDIAN_KEY = "guardian-member-key-001";
    public static final String MANAGER_KEY = "manager-member-key-001";
    public static final String OTHER_GUARDIAN_KEY = "other-guardian-member-key-001";

    public static Member createWard() {
        return Member.builder()
                .phone("01011112222")
                .name("Ward")
                .birth(LocalDate.of(1950, 1, 1))
                .gender(Gender.MALE)
                .role(MemberRole.WARD)
                .signUpType(SignUpType.LOCAL)
                .build();
    }

    public static Member createGuardian() {
        return Member.builder()
                .phone("01033334444")
                .name("Guardian")
                .birth(LocalDate.of(1980, 1, 1))
                .gender(Gender.FEMALE)
                .role(MemberRole.GUARDIAN)
                .signUpType(SignUpType.LOCAL)
                .build();
    }

    public static Member createManager() {
        return Member.builder()
                .phone("01055556666")
                .name("Manager")
                .birth(LocalDate.of(1975, 1, 1))
                .gender(Gender.MALE)
                .role(MemberRole.GUARDIAN)
                .signUpType(SignUpType.LOCAL)
                .build();
    }

    public static Member createOtherGuardian() {
        return Member.builder()
                .phone("01077778888")
                .name("Other Guardian")
                .birth(LocalDate.of(1985, 1, 1))
                .gender(Gender.FEMALE)
                .role(MemberRole.GUARDIAN)
                .signUpType(SignUpType.LOCAL)
                .build();
    }

    public static NotificationSetting createSetting(String wardKey) {
        return NotificationSetting.builder()
                .wardMemberKey(wardKey)
                .safeZoneEntryEnabled(true)
                .safeZoneExitEnabled(false)
                .routeDeviationEnabled(true)
                .speedAnomalyEnabled(false)
                .wanderingAnomalyEnabled(true)
                .anomalySensitivity(AnomalySensitivity.HIGH)
                .emergencyCallEnabled(true)
                .lowBatteryEnabled(false)
                .batteryThresholdPercent(40)
                .build();
    }
}
