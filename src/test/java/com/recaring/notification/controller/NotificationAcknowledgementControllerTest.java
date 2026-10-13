package com.recaring.notification.controller;

import com.recaring.care.dataaccess.repository.CareRelationshipRepository;
import com.recaring.care.fixture.CareFixture;
import com.recaring.location.fixture.LocationFixture;
import com.recaring.location.implement.detection.AnomalyAcknowledgementManager;
import com.recaring.member.dataaccess.entity.Member;
import com.recaring.member.dataaccess.repository.MemberRepository;
import com.recaring.notification.fixture.NotificationFixture;
import com.recaring.support.AbstractIntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.client.RestTestClient;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("이상탐지 사건 확인 컨트롤러 HTTP 통합 테스트")
class NotificationAcknowledgementControllerTest extends AbstractIntegrationTest {

    @Autowired
    private MemberRepository memberRepository;
    @Autowired
    private CareRelationshipRepository careRelationshipRepository;
    @Autowired
    private AnomalyAcknowledgementManager anomalyAcknowledgementManager;

    private Member ward;
    private Member guardian;

    @BeforeEach
    void setUp() {
        ward = memberRepository.save(NotificationFixture.createWard());
        guardian = memberRepository.save(NotificationFixture.createGuardian());
        careRelationshipRepository.save(
                CareFixture.createGuardianRelationship(ward.getMemberKey(), guardian.getMemberKey()));
        anomalyAcknowledgementManager.reset(ward.getMemberKey(), LocationFixture.EVENT_ID);
    }

    @AfterEach
    void tearDown() {
        anomalyAcknowledgementManager.delete(ward.getMemberKey());
        careRelationshipRepository.deleteAllInBatch();
        memberRepository.deleteAllInBatch();
    }

    @Test
    @DisplayName("보호자가 진행 중인 사건을 확인하면 그 사건이 확인 상태가 된다")
    void acknowledge_marks_current_event_as_acknowledged() {
        acknowledge(guardian, ward.getMemberKey(), LocationFixture.EVENT_ID)
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.resultType").isEqualTo("SUCCESS");

        assertThat(anomalyAcknowledgementManager.isAcknowledged(ward.getMemberKey())).isTrue();
    }

    @Test
    @DisplayName("지난 사건을 확인하면 성공하지만 진행 중인 사건은 확인되지 않는다")
    void acknowledge_of_previous_event_keeps_current_event_unacknowledged() {
        acknowledge(guardian, ward.getMemberKey(), LocationFixture.PREVIOUS_EVENT_ID)
                .expectStatus().isOk();

        assertThat(anomalyAcknowledgementManager.isAcknowledged(ward.getMemberKey())).isFalse();
    }

    @Test
    @DisplayName("케어 관계가 없는 대상자의 사건은 확인할 수 없다")
    void acknowledge_rejects_unrelated_member() {
        Member otherGuardian = memberRepository.save(NotificationFixture.createOtherGuardian());

        acknowledge(otherGuardian, ward.getMemberKey(), LocationFixture.EVENT_ID)
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.error.errorCode").isEqualTo("E5011");

        assertThat(anomalyAcknowledgementManager.isAcknowledged(ward.getMemberKey())).isFalse();
    }

    @Test
    @DisplayName("사건 식별자 없이 확인하면 요청을 거부한다")
    void acknowledge_rejects_blank_event_id() {
        acknowledge(guardian, ward.getMemberKey(), " ")
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.error.errorCode").isEqualTo("E400");

        assertThat(anomalyAcknowledgementManager.isAcknowledged(ward.getMemberKey())).isFalse();
    }

    private RestTestClient.ResponseSpec acknowledge(Member member, String wardKey, String eventId) {
        return client.post()
                .uri("/api/v1/notifications/anomaly-events/acknowledge")
                .header(HttpHeaders.AUTHORIZATION, bearerToken(member.getMemberKey(), member.getRole()))
                .contentType(MediaType.APPLICATION_JSON)
                .body("""
                        {"wardKey": "%s", "eventId": "%s"}
                        """.formatted(wardKey, eventId))
                .exchange();
    }
}
