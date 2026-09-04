package com.recaring.safezone.controller;

import com.recaring.care.dataaccess.repository.CareRelationshipRepository;
import com.recaring.care.fixture.CareFixture;
import com.recaring.member.dataaccess.entity.Member;
import com.recaring.member.dataaccess.repository.MemberRepository;
import com.recaring.safezone.dataaccess.entity.SafeZone;
import com.recaring.safezone.dataaccess.entity.SafeZoneRadius;
import com.recaring.safezone.dataaccess.repository.SafeZoneRepository;
import com.recaring.safezone.fixture.SafeZoneFixture;
import com.recaring.support.AbstractIntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

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
    private static final SafeZoneRadius NEW_RADIUS = SafeZoneRadius.SMALL;

    private static final String CREATE_BODY = """
            {"name": "%s", "address": "%s", "latitude": %s, "longitude": %s, "radius": "%s"}
            """.formatted(NEW_NAME, NEW_ADDRESS, NEW_LATITUDE, NEW_LONGITUDE, NEW_RADIUS);

    private static final String UPDATE_BODY = """
            {"name": "%s", "address": "%s", "latitude": %s, "longitude": %s, "radius": "%s"}
            """.formatted(SafeZoneFixture.UPDATED_NAME, SafeZoneFixture.UPDATED_ADDRESS,
            SafeZoneFixture.UPDATED_LATITUDE, SafeZoneFixture.UPDATED_LONGITUDE, SafeZoneFixture.UPDATED_RADIUS);

    @Autowired private MemberRepository memberRepository;
    @Autowired private CareRelationshipRepository careRelationshipRepository;
    @Autowired private SafeZoneRepository safeZoneRepository;

    private Member ward;
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

        careRelationshipRepository.save(
                CareFixture.createPrimaryGuardianRelationship(ward.getMemberKey(), guardian.getMemberKey()));
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
        assertThat(added.getRadius()).isEqualTo(NEW_RADIUS);
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
    @DisplayName("관계자가 안심존을 추가하면 403이 반환되고 저장되지 않는다")
    void addSafeZone_returns_403_when_manager() {
        client.post()
                .uri(zonesUri())
                .header(HttpHeaders.AUTHORIZATION, managerAuth)
                .contentType(MediaType.APPLICATION_JSON)
                .body(CREATE_BODY)
                .exchange()
                .expectStatus().isForbidden();

        assertThat(zonesOfWard()).hasSize(1);
    }

    @Test
    @DisplayName("케어 관계가 없는 보호자가 안심존을 추가하면 403이 반환되고 저장되지 않는다")
    void addSafeZone_returns_403_when_not_caregiver() {
        client.post()
                .uri(zonesUri())
                .header(HttpHeaders.AUTHORIZATION, strangerAuth)
                .contentType(MediaType.APPLICATION_JSON)
                .body(CREATE_BODY)
                .exchange()
                .expectStatus().isForbidden();

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
    @DisplayName("케어 관계가 없는 보호자가 목록을 조회하면 403이 반환된다")
    void getSafeZones_returns_403_when_not_caregiver() {
        client.get()
                .uri(zonesUri())
                .header(HttpHeaders.AUTHORIZATION, strangerAuth)
                .exchange()
                .expectStatus().isForbidden();
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
                .jsonPath("$.data.radiusMeters").isEqualTo(SafeZoneFixture.RADIUS.getMeters());
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
        assertThat(updated.getRadius()).isEqualTo(SafeZoneFixture.UPDATED_RADIUS);
    }

    @Test
    @DisplayName("관계자가 안심존을 수정하면 403이 반환되고 값이 그대로 남는다")
    void updateSafeZone_returns_403_when_manager() {
        client.patch()
                .uri(zoneUri(savedZone.getSafeZoneKey()))
                .header(HttpHeaders.AUTHORIZATION, managerAuth)
                .contentType(MediaType.APPLICATION_JSON)
                .body(UPDATE_BODY)
                .exchange()
                .expectStatus().isForbidden();

        assertThat(reloadSavedZone().getName()).isEqualTo(SafeZoneFixture.NAME);
    }

    @Test
    @DisplayName("케어 관계가 없는 보호자가 안심존을 수정하면 403이 반환되고 값이 그대로 남는다")
    void updateSafeZone_returns_403_when_not_caregiver() {
        client.patch()
                .uri(zoneUri(savedZone.getSafeZoneKey()))
                .header(HttpHeaders.AUTHORIZATION, strangerAuth)
                .contentType(MediaType.APPLICATION_JSON)
                .body(UPDATE_BODY)
                .exchange()
                .expectStatus().isForbidden();

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

        assertThat(safeZoneRepository.findBySafeZoneKey(savedZone.getSafeZoneKey())).isEmpty();
    }

    @Test
    @DisplayName("관계자가 안심존을 삭제하면 403이 반환되고 행이 남아 있다")
    void deleteSafeZone_returns_403_when_manager() {
        client.delete()
                .uri(zoneUri(savedZone.getSafeZoneKey()))
                .header(HttpHeaders.AUTHORIZATION, managerAuth)
                .exchange()
                .expectStatus().isForbidden();

        assertThat(safeZoneRepository.findBySafeZoneKey(savedZone.getSafeZoneKey())).isPresent();
    }

    @Test
    @DisplayName("케어 관계가 없는 보호자가 안심존을 삭제하면 403이 반환되고 행이 남아 있다")
    void deleteSafeZone_returns_403_when_not_caregiver() {
        client.delete()
                .uri(zoneUri(savedZone.getSafeZoneKey()))
                .header(HttpHeaders.AUTHORIZATION, strangerAuth)
                .exchange()
                .expectStatus().isForbidden();

        assertThat(safeZoneRepository.findBySafeZoneKey(savedZone.getSafeZoneKey())).isPresent();
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
        return safeZoneRepository.findBySafeZoneKey(savedZone.getSafeZoneKey())
                .orElseThrow(() -> new AssertionError("안심존이 존재하지 않는다"));
    }
}
