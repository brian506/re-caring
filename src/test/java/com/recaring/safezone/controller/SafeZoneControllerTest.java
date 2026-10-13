package com.recaring.safezone.controller;

import com.recaring.care.dataaccess.repository.CareRelationshipRepository;
import com.recaring.care.fixture.CareFixture;
import com.recaring.member.dataaccess.entity.Member;
import com.recaring.member.dataaccess.repository.MemberRepository;
import com.recaring.safezone.dataaccess.entity.SafeZone;
import com.recaring.safezone.vo.SafeZoneType;
import com.recaring.safezone.dataaccess.repository.SafeZoneRepository;
import com.recaring.safezone.fixture.SafeZoneFixture;
import com.recaring.support.AbstractIntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("안심존 API HTTP 통합 테스트")
class SafeZoneControllerTest extends AbstractIntegrationTest {

    private static final String STRANGER_PHONE = "01077778888";
    private static final String CO_GUARDIAN_PHONE = "01066665555";

    private static final String NEW_NAME = "학교";
    private static final String NEW_ADDRESS = "서울시 마포구 1";
    private static final double NEW_LATITUDE = 37.55;
    private static final double NEW_LONGITUDE = 126.92;
    private static final int NEW_RADIUS_METERS = 350;
    private static final int TOO_SMALL_RADIUS_METERS = 99;

    private static final String CREATE_BODY = """
            {"name": "%s", "address": "%s", "latitude": %s, "longitude": %s, "radiusMeters": %s, "type": "SAFE"}
            """.formatted(NEW_NAME, NEW_ADDRESS, NEW_LATITUDE, NEW_LONGITUDE, NEW_RADIUS_METERS);

    private static final String DANGER_CREATE_BODY = """
            {"name": "%s", "address": "%s", "latitude": %s, "longitude": %s, "radiusMeters": %s, "type": "DANGER"}
            """.formatted(NEW_NAME, NEW_ADDRESS, NEW_LATITUDE, NEW_LONGITUDE, NEW_RADIUS_METERS);

    private static final String CREATE_BODY_WITHOUT_TYPE = """
            {"name": "%s", "address": "%s", "latitude": %s, "longitude": %s, "radiusMeters": %s}
            """.formatted(NEW_NAME, NEW_ADDRESS, NEW_LATITUDE, NEW_LONGITUDE, NEW_RADIUS_METERS);

    private static final String TOO_SMALL_RADIUS_BODY = """
            {"name": "%s", "address": "%s", "latitude": %s, "longitude": %s, "radiusMeters": %s, "type": "SAFE"}
            """.formatted(NEW_NAME, NEW_ADDRESS, NEW_LATITUDE, NEW_LONGITUDE, TOO_SMALL_RADIUS_METERS);

    private static final String UPDATE_BODY = """
            {"name": "%s", "address": "%s", "latitude": %s, "longitude": %s, "radiusMeters": %s, "type": "SAFE"}
            """.formatted(SafeZoneFixture.UPDATED_NAME, SafeZoneFixture.UPDATED_ADDRESS,
            SafeZoneFixture.UPDATED_LATITUDE, SafeZoneFixture.UPDATED_LONGITUDE, SafeZoneFixture.UPDATED_RADIUS_METERS);

    private static final String UPDATE_TO_DANGER_BODY = """
            {"name": "%s", "address": "%s", "latitude": %s, "longitude": %s, "radiusMeters": %s, "type": "DANGER"}
            """.formatted(SafeZoneFixture.UPDATED_NAME, SafeZoneFixture.UPDATED_ADDRESS,
            SafeZoneFixture.UPDATED_LATITUDE, SafeZoneFixture.UPDATED_LONGITUDE, SafeZoneFixture.UPDATED_RADIUS_METERS);

    @Autowired private MemberRepository memberRepository;
    @Autowired private CareRelationshipRepository careRelationshipRepository;
    @Autowired private SafeZoneRepository safeZoneRepository;
    @Autowired private JdbcTemplate jdbcTemplate;

    private Member ward;
    private Member victimWard;
    private String guardianAuth;
    private String coGuardianAuth;
    private String managerAuth;
    private String strangerAuth;
    private SafeZone savedZone;

    @BeforeEach
    void setUp() {
        Member guardian = memberRepository.save(CareFixture.createGuardianMember());
        Member coGuardian = memberRepository.save(CareFixture.createGuardianMember(CO_GUARDIAN_PHONE));
        Member manager = memberRepository.save(CareFixture.createGuardianMember(CareFixture.MANAGER_PHONE));
        Member stranger = memberRepository.save(CareFixture.createGuardianMember(STRANGER_PHONE));
        ward = memberRepository.save(CareFixture.createWardMember());
        victimWard = memberRepository.save(CareFixture.createWardMember("01044445555"));

        careRelationshipRepository.save(
                CareFixture.createGuardianRelationship(ward.getMemberKey(), guardian.getMemberKey()));
        careRelationshipRepository.save(
                CareFixture.createGuardianRelationship(ward.getMemberKey(), coGuardian.getMemberKey()));
        careRelationshipRepository.save(
                CareFixture.createManagerRelationship(ward.getMemberKey(), manager.getMemberKey()));

        guardianAuth = bearerToken(guardian.getMemberKey(), guardian.getRole());
        coGuardianAuth = bearerToken(coGuardian.getMemberKey(), coGuardian.getRole());
        managerAuth = bearerToken(manager.getMemberKey(), manager.getRole());
        strangerAuth = bearerToken(stranger.getMemberKey(), stranger.getRole());

        savedZone = safeZoneRepository.save(SafeZoneFixture.createSafeZone(ward.getMemberKey()));
    }

    @AfterEach
    void tearDown() {
        safeZoneRepository.deleteAllInBatch();
        careRelationshipRepository.deleteAllInBatch();
        memberRepository.deleteAllInBatch();
    }

    // ── POST /api/v1/care/wards/{wardKey}/safe-zones ─────────────────────────

    @Test
    @DisplayName("보호자가 안심존을 추가하면 요청한 좌표·반경 그대로 저장된다")
    void addSafeZone_persists_zone_when_guardian() {
        client.post()
                .uri(zonesUri())
                .header(HttpHeaders.AUTHORIZATION, guardianAuth)
                .contentType(MediaType.APPLICATION_JSON)
                .body(CREATE_BODY)
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.resultType").isEqualTo("SUCCESS");

        SafeZone added = findAddedZone();
        assertThat(added.getName()).isEqualTo(NEW_NAME);
        assertThat(added.getAddress()).isEqualTo(NEW_ADDRESS);
        assertThat(added.getLatitude()).isEqualTo(NEW_LATITUDE);
        assertThat(added.getLongitude()).isEqualTo(NEW_LONGITUDE);
        assertThat(added.getRadiusMeters()).isEqualTo(NEW_RADIUS_METERS);
    }

    @Test
    @DisplayName("주보호자가 아닌 보호자도 안심존을 추가할 수 있다")
    void addSafeZone_persists_zone_when_co_guardian() {
        client.post()
                .uri(zonesUri())
                .header(HttpHeaders.AUTHORIZATION, coGuardianAuth)
                .contentType(MediaType.APPLICATION_JSON)
                .body(CREATE_BODY)
                .exchange()
                .expectStatus().isCreated();

        assertThat(findAddedZone().getName()).isEqualTo(NEW_NAME);
    }

    @Test
    @DisplayName("보호자가 위험존을 추가하면 위험존 타입으로 저장된다")
    void addSafeZone_persists_danger_type() {
        client.post()
                .uri(zonesUri())
                .header(HttpHeaders.AUTHORIZATION, guardianAuth)
                .contentType(MediaType.APPLICATION_JSON)
                .body(DANGER_CREATE_BODY)
                .exchange()
                .expectStatus().isCreated();

        SafeZone added = findAddedZone();
        assertThat(added.getType()).isEqualTo(SafeZoneType.DANGER);
        assertThat(added.getName()).isEqualTo(NEW_NAME);
    }

    @Test
    @DisplayName("안심/위험 타입 없이 추가하면 400이 반환되고 저장되지 않는다")
    void addSafeZone_returns_400_without_type() {
        client.post()
                .uri(zonesUri())
                .header(HttpHeaders.AUTHORIZATION, guardianAuth)
                .contentType(MediaType.APPLICATION_JSON)
                .body(CREATE_BODY_WITHOUT_TYPE)
                .exchange()
                .expectStatus().isBadRequest();

        assertThat(zonesOfWard()).hasSize(1);
    }

    @Test
    @DisplayName("반경이 100m 미만이면 400 E8003이 반환되고 저장되지 않는다")
    void addSafeZone_returns_400_when_radius_too_small() {
        client.post()
                .uri(zonesUri())
                .header(HttpHeaders.AUTHORIZATION, guardianAuth)
                .contentType(MediaType.APPLICATION_JSON)
                .body(TOO_SMALL_RADIUS_BODY)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.error.errorCode").isEqualTo("E8003");

        assertThat(zonesOfWard()).hasSize(1);
    }

    @Test
    @DisplayName("관계자가 안심존을 추가하면 403 E5002가 반환되고 저장되지 않는다")
    void addSafeZone_returns_403_when_manager() {
        client.post()
                .uri(zonesUri())
                .header(HttpHeaders.AUTHORIZATION, managerAuth)
                .contentType(MediaType.APPLICATION_JSON)
                .body(CREATE_BODY)
                .exchange()
                .expectStatus().isForbidden()
                .expectBody()
                .jsonPath("$.error.errorCode").isEqualTo("E5002");

        assertThat(zonesOfWard()).hasSize(1);
    }

    @Test
    @DisplayName("케어 관계가 없는 보호자가 안심존을 추가하면 보호자 전용 작업이므로 403 E5002가 반환되고 저장되지 않는다")
    void addSafeZone_returns_403_when_not_caregiver() {
        client.post()
                .uri(zonesUri())
                .header(HttpHeaders.AUTHORIZATION, strangerAuth)
                .contentType(MediaType.APPLICATION_JSON)
                .body(CREATE_BODY)
                .exchange()
                .expectStatus().isForbidden()
                .expectBody()
                .jsonPath("$.error.errorCode").isEqualTo("E5002");

        assertThat(zonesOfWard()).hasSize(1);
    }

    @Test
    @DisplayName("인증 없이 안심존을 추가하면 401이 반환된다")
    void addSafeZone_returns_401_without_auth() {
        client.post()
                .uri(zonesUri())
                .contentType(MediaType.APPLICATION_JSON)
                .body(CREATE_BODY)
                .exchange()
                .expectStatus().isUnauthorized();
    }

    // ── GET /api/v1/care/wards/{wardKey}/safe-zones ──────────────────────────

    @Test
    @DisplayName("보호자가 조회하면 보호 대상자의 안심존 목록이 반환된다")
    void getSafeZones_returns_list_when_guardian() {
        client.get()
                .uri(zonesUri())
                .header(HttpHeaders.AUTHORIZATION, guardianAuth)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.resultType").isEqualTo("SUCCESS")
                .jsonPath("$.data.length()").isEqualTo(1)
                .jsonPath("$.data[0].safeZoneKey").isEqualTo(savedZone.getSafeZoneKey())
                .jsonPath("$.data[0].name").isEqualTo(SafeZoneFixture.NAME);
    }

    @Test
    @DisplayName("목록에는 안심존과 위험존이 각자의 타입과 함께 반환된다")
    void getSafeZones_returns_safe_and_danger_zones_with_type() {
        SafeZone dangerZone = safeZoneRepository.save(SafeZoneFixture.createDangerZone(ward.getMemberKey()));
        overwriteCreatedAt(savedZone, LocalDateTime.of(2026, 10, 5, 9, 0));
        overwriteCreatedAt(dangerZone, LocalDateTime.of(2026, 10, 5, 10, 0));

        client.get()
                .uri(zonesUri())
                .header(HttpHeaders.AUTHORIZATION, guardianAuth)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.length()").isEqualTo(2)
                .jsonPath("$.data[0].safeZoneKey").isEqualTo(savedZone.getSafeZoneKey())
                .jsonPath("$.data[0].type").isEqualTo("SAFE")
                .jsonPath("$.data[1].safeZoneKey").isEqualTo(dangerZone.getSafeZoneKey())
                .jsonPath("$.data[1].type").isEqualTo("DANGER");
    }

    @Test
    @DisplayName("관계자도 보호 대상자의 안심존 목록을 조회할 수 있다")
    void getSafeZones_returns_list_when_manager() {
        client.get()
                .uri(zonesUri())
                .header(HttpHeaders.AUTHORIZATION, managerAuth)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.length()").isEqualTo(1)
                .jsonPath("$.data[0].name").isEqualTo(SafeZoneFixture.NAME);
    }

    @Test
    @DisplayName("케어 관계가 없는 보호자가 목록을 조회하면 400 E5011이 반환된다")
    void getSafeZones_returns_400_when_not_caregiver() {
        client.get()
                .uri(zonesUri())
                .header(HttpHeaders.AUTHORIZATION, strangerAuth)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.error.errorCode").isEqualTo("E5011");
    }

    @Test
    @DisplayName("인증 없이 목록을 조회하면 401이 반환된다")
    void getSafeZones_returns_401_without_auth() {
        client.get()
                .uri(zonesUri())
                .exchange()
                .expectStatus().isUnauthorized();
    }

    // ── GET /api/v1/care/wards/{wardKey}/safe-zones/{safeZoneKey} ────────────

    @Test
    @DisplayName("보호자가 조회하면 안심존 상세가 반경(m) 단위로 반환된다")
    void getSafeZone_returns_detail_when_guardian() {
        client.get()
                .uri(zoneUri(savedZone.getSafeZoneKey()))
                .header(HttpHeaders.AUTHORIZATION, guardianAuth)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.resultType").isEqualTo("SUCCESS")
                .jsonPath("$.data.name").isEqualTo(SafeZoneFixture.NAME)
                .jsonPath("$.data.address").isEqualTo(SafeZoneFixture.ADDRESS)
                .jsonPath("$.data.radiusMeters").isEqualTo(SafeZoneFixture.RADIUS_METERS);
    }

    @Test
    @DisplayName("관계자도 안심존 상세를 조회할 수 있다")
    void getSafeZone_returns_detail_when_manager() {
        client.get()
                .uri(zoneUri(savedZone.getSafeZoneKey()))
                .header(HttpHeaders.AUTHORIZATION, managerAuth)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.name").isEqualTo(SafeZoneFixture.NAME);
    }

    @Test
    @DisplayName("존재하지 않는 안심존을 조회하면 NOT_FOUND_SAFE_ZONE이 반환된다")
    void getSafeZone_returns_not_found_safe_zone() {
        client.get()
                .uri(zoneUri(SafeZoneFixture.UNKNOWN_SAFE_ZONE_KEY))
                .header(HttpHeaders.AUTHORIZATION, guardianAuth)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.error.errorCode").isEqualTo("E8000");
    }

    // ── PATCH /api/v1/care/wards/{wardKey}/safe-zones/{safeZoneKey} ──────────

    @Test
    @DisplayName("보호자가 안심존을 수정하면 요청한 좌표·반경이 반영된다")
    void updateSafeZone_persists_changes_when_guardian() {
        client.patch()
                .uri(zoneUri(savedZone.getSafeZoneKey()))
                .header(HttpHeaders.AUTHORIZATION, guardianAuth)
                .contentType(MediaType.APPLICATION_JSON)
                .body(UPDATE_BODY)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.resultType").isEqualTo("SUCCESS");

        SafeZone updated = reloadSavedZone();
        assertThat(updated.getName()).isEqualTo(SafeZoneFixture.UPDATED_NAME);
        assertThat(updated.getAddress()).isEqualTo(SafeZoneFixture.UPDATED_ADDRESS);
        assertThat(updated.getLatitude()).isEqualTo(SafeZoneFixture.UPDATED_LATITUDE);
        assertThat(updated.getLongitude()).isEqualTo(SafeZoneFixture.UPDATED_LONGITUDE);
        assertThat(updated.getRadiusMeters()).isEqualTo(SafeZoneFixture.UPDATED_RADIUS_METERS);
    }

    @Test
    @DisplayName("안심존을 위험존으로 수정하면 저장된 타입이 위험존으로 바뀐다")
    void updateSafeZone_changes_type_to_danger() {
        client.patch()
                .uri(zoneUri(savedZone.getSafeZoneKey()))
                .header(HttpHeaders.AUTHORIZATION, guardianAuth)
                .contentType(MediaType.APPLICATION_JSON)
                .body(UPDATE_TO_DANGER_BODY)
                .exchange()
                .expectStatus().isOk();

        assertThat(reloadSavedZone().getType()).isEqualTo(SafeZoneType.DANGER);
    }

    @Test
    @DisplayName("관계자가 안심존을 수정하면 403 E5002가 반환되고 값이 그대로 남는다")
    void updateSafeZone_returns_403_when_manager() {
        client.patch()
                .uri(zoneUri(savedZone.getSafeZoneKey()))
                .header(HttpHeaders.AUTHORIZATION, managerAuth)
                .contentType(MediaType.APPLICATION_JSON)
                .body(UPDATE_BODY)
                .exchange()
                .expectStatus().isForbidden()
                .expectBody()
                .jsonPath("$.error.errorCode").isEqualTo("E5002");

        assertThat(reloadSavedZone().getName()).isEqualTo(SafeZoneFixture.NAME);
    }

    @Test
    @DisplayName("반경 100m 미만으로 수정하면 400 E8003이 반환되고 값이 그대로 남는다")
    void updateSafeZone_returns_400_when_radius_too_small() {
        client.patch()
                .uri(zoneUri(savedZone.getSafeZoneKey()))
                .header(HttpHeaders.AUTHORIZATION, guardianAuth)
                .contentType(MediaType.APPLICATION_JSON)
                .body(TOO_SMALL_RADIUS_BODY)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.error.errorCode").isEqualTo("E8003");

        assertThat(reloadSavedZone().getRadiusMeters()).isEqualTo(SafeZoneFixture.RADIUS_METERS);
    }

    @Test
    @DisplayName("케어 관계가 없는 보호자가 안심존을 수정하면 보호자 전용 작업이므로 403 E5002가 반환되고 값이 그대로 남는다")
    void updateSafeZone_returns_403_when_not_caregiver() {
        client.patch()
                .uri(zoneUri(savedZone.getSafeZoneKey()))
                .header(HttpHeaders.AUTHORIZATION, strangerAuth)
                .contentType(MediaType.APPLICATION_JSON)
                .body(UPDATE_BODY)
                .exchange()
                .expectStatus().isForbidden()
                .expectBody()
                .jsonPath("$.error.errorCode").isEqualTo("E5002");

        assertThat(reloadSavedZone().getName()).isEqualTo(SafeZoneFixture.NAME);
    }

    // ── DELETE /api/v1/care/wards/{wardKey}/safe-zones/{safeZoneKey} ─────────

    @Test
    @DisplayName("보호자가 안심존을 삭제하면 행이 실제로 제거된다")
    void deleteSafeZone_removes_row_when_guardian() {
        client.delete()
                .uri(zoneUri(savedZone.getSafeZoneKey()))
                .header(HttpHeaders.AUTHORIZATION, guardianAuth)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.resultType").isEqualTo("SUCCESS");

        assertThat(safeZoneRepository.findBySafeZoneKeyAndWardMemberKey(savedZone.getSafeZoneKey(), ward.getMemberKey())).isEmpty();
    }

    @Test
    @DisplayName("관계자가 안심존을 삭제하면 403 E5002가 반환되고 행이 남아 있다")
    void deleteSafeZone_returns_403_when_manager() {
        client.delete()
                .uri(zoneUri(savedZone.getSafeZoneKey()))
                .header(HttpHeaders.AUTHORIZATION, managerAuth)
                .exchange()
                .expectStatus().isForbidden()
                .expectBody()
                .jsonPath("$.error.errorCode").isEqualTo("E5002");

        assertThat(safeZoneRepository.findBySafeZoneKeyAndWardMemberKey(savedZone.getSafeZoneKey(), ward.getMemberKey())).isPresent();
    }

    @Test
    @DisplayName("케어 관계가 없는 보호자가 안심존을 삭제하면 보호자 전용 작업이므로 403 E5002가 반환되고 행이 남아 있다")
    void deleteSafeZone_returns_403_when_not_caregiver() {
        client.delete()
                .uri(zoneUri(savedZone.getSafeZoneKey()))
                .header(HttpHeaders.AUTHORIZATION, strangerAuth)
                .exchange()
                .expectStatus().isForbidden()
                .expectBody()
                .jsonPath("$.error.errorCode").isEqualTo("E5002");

        assertThat(safeZoneRepository.findBySafeZoneKeyAndWardMemberKey(savedZone.getSafeZoneKey(), ward.getMemberKey())).isPresent();
    }

    private String zonesUri() {
        return "/api/v1/care/wards/" + ward.getMemberKey() + "/safe-zones";
    }

    private String zoneUri(String safeZoneKey) {
        return zonesUri() + "/" + safeZoneKey;
    }

    private List<SafeZone> zonesOfWard() {
        return safeZoneRepository.findAllByWardMemberKey(ward.getMemberKey());
    }

    private SafeZone findAddedZone() {
        return zonesOfWard().stream()
                .filter(zone -> !zone.getSafeZoneKey().equals(savedZone.getSafeZoneKey()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("추가된 안심존이 저장되지 않았다"));
    }

    private SafeZone reloadSavedZone() {
        return safeZoneRepository.findBySafeZoneKeyAndWardMemberKey(savedZone.getSafeZoneKey(), ward.getMemberKey())
                .orElseThrow(() -> new AssertionError("안심존이 존재하지 않는다"));
    }

    // ── 다른 대상자의 안심존 키로 접근 (IDOR) ────────────────────────────────

    @Test
    @DisplayName("자기 대상자 경로에 남의 안심존 키를 붙여 조회하면 400 E8000이 반환된다")
    void getSafeZone_returns_400_for_other_wards_zone_key() {
        SafeZone victimZone = safeZoneRepository.save(SafeZoneFixture.createSafeZone(victimWard.getMemberKey()));

        client.get()
                .uri("/api/v1/care/wards/" + ward.getMemberKey() + "/safe-zones/" + victimZone.getSafeZoneKey())
                .header(HttpHeaders.AUTHORIZATION, guardianAuth)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.error.errorCode").isEqualTo("E8000");
    }

    @Test
    @DisplayName("자기 대상자 경로에 남의 안심존 키를 붙여 삭제하면 400 E8000이고 행이 남아 있다")
    void deleteSafeZone_returns_400_for_other_wards_zone_key() {
        SafeZone victimZone = safeZoneRepository.save(SafeZoneFixture.createSafeZone(victimWard.getMemberKey()));

        client.delete()
                .uri("/api/v1/care/wards/" + ward.getMemberKey() + "/safe-zones/" + victimZone.getSafeZoneKey())
                .header(HttpHeaders.AUTHORIZATION, guardianAuth)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.error.errorCode").isEqualTo("E8000");

        assertThat(safeZoneRepository.findBySafeZoneKeyAndWardMemberKey(
                victimZone.getSafeZoneKey(), victimWard.getMemberKey())).isPresent();
    }

    @Test
    @DisplayName("자기 대상자 경로에 남의 안심존 키를 붙여 수정하면 400 E8000이고 값이 그대로다")
    void updateSafeZone_returns_400_for_other_wards_zone_key() {
        SafeZone victimZone = safeZoneRepository.save(SafeZoneFixture.createSafeZone(victimWard.getMemberKey()));

        client.patch()
                .uri("/api/v1/care/wards/" + ward.getMemberKey() + "/safe-zones/" + victimZone.getSafeZoneKey())
                .header(HttpHeaders.AUTHORIZATION, guardianAuth)
                .contentType(MediaType.APPLICATION_JSON)
                .body(UPDATE_BODY)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.error.errorCode").isEqualTo("E8000");

        assertThat(safeZoneRepository.findBySafeZoneKeyAndWardMemberKey(
                victimZone.getSafeZoneKey(), victimWard.getMemberKey())
        ).get().extracting(SafeZone::getName).isEqualTo(SafeZoneFixture.NAME);
    }

    private void overwriteCreatedAt(SafeZone zone, LocalDateTime createdAt) {
        jdbcTemplate.update(
                "UPDATE safe_zones SET created_at = ? WHERE safe_zone_id = ?",
                createdAt, zone.getId()
        );
    }
}
