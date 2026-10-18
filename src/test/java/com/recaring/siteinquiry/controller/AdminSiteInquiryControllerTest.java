package com.recaring.siteinquiry.controller;

import com.recaring.siteinquiry.dataaccess.entity.SiteInquiry;
import com.recaring.siteinquiry.dataaccess.entity.SiteInquiryStatus;
import com.recaring.siteinquiry.dataaccess.repository.SiteInquiryRepository;
import com.recaring.siteinquiry.fixture.SiteInquiryFixture;
import com.recaring.support.AbstractIntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("운영자 웹사이트 문의 API HTTP 통합 테스트")
class AdminSiteInquiryControllerTest extends AbstractIntegrationTest {

    private static final String ADMIN_KEY_HEADER = "X-Admin-Key";
    private static final String ADMIN_SITE_INQUIRIES_URI = "/api/v1/admin/site-inquiries";

    @Autowired private SiteInquiryRepository siteInquiryRepository;

    private SiteInquiry waiting;

    @BeforeEach
    void setUp() {
        waiting = siteInquiryRepository.save(SiteInquiryFixture.createPublicInquiry(SiteInquiryFixture.TITLE));
    }

    @AfterEach
    void tearDown() {
        siteInquiryRepository.deleteAllInBatch();
    }

    // ── GET /api/v1/admin/site-inquiries ────────────────────────────────────

    @Test
    @DisplayName("관리자 키 없이 목록을 조회하면 401이다")
    void getInquiries_rejects_missing_admin_key() {
        client.get()
                .uri(ADMIN_SITE_INQUIRIES_URI)
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    @DisplayName("운영자 목록에는 이메일과 연락처가 포함된다")
    void getInquiries_includes_contact() {
        client.get()
                .uri(ADMIN_SITE_INQUIRIES_URI)
                .header(ADMIN_KEY_HEADER, SiteInquiryFixture.ADMIN_API_KEY)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data[0].inquiryKey").isEqualTo(waiting.getInquiryKey())
                .jsonPath("$.data[0].email").isEqualTo(SiteInquiryFixture.EMAIL)
                .jsonPath("$.data[0].phone").isEqualTo(SiteInquiryFixture.PHONE);
    }

    @Test
    @DisplayName("답변 대기 목록은 답변된 문의를 빼고 오래된 순으로 반환된다")
    void getInquiries_returns_waiting_oldest_first() {
        siteInquiryRepository.save(SiteInquiryFixture.createAnsweredInquiry(SiteInquiryFixture.OTHER_TITLE));
        SiteInquiry newerWaiting = siteInquiryRepository.save(
                SiteInquiryFixture.createPublicInquiry(SiteInquiryFixture.OTHER_TITLE));

        client.get()
                .uri(ADMIN_SITE_INQUIRIES_URI + "?status=WAITING")
                .header(ADMIN_KEY_HEADER, SiteInquiryFixture.ADMIN_API_KEY)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.length()").isEqualTo(2)
                .jsonPath("$.data[0].inquiryKey").isEqualTo(waiting.getInquiryKey())
                .jsonPath("$.data[1].inquiryKey").isEqualTo(newerWaiting.getInquiryKey());
    }

    // ── PATCH /api/v1/admin/site-inquiries/{inquiryKey}/answer ──────────────

    @Test
    @DisplayName("답변을 등록하면 답변 완료 상태와 답변·답변 시각이 저장된다")
    void answer_persists_answer_and_answered_at() {
        client.patch()
                .uri(answerUri(waiting.getInquiryKey()))
                .header(ADMIN_KEY_HEADER, SiteInquiryFixture.ADMIN_API_KEY)
                .contentType(MediaType.APPLICATION_JSON)
                .body(answerBody(SiteInquiryFixture.ANSWER))
                .exchange()
                .expectStatus().isOk();

        SiteInquiry answered = reload(waiting);
        assertThat(answered.getStatus()).isEqualTo(SiteInquiryStatus.ANSWERED);
        assertThat(answered.getAnswer()).isEqualTo(SiteInquiryFixture.ANSWER);
        assertThat(answered.getAnsweredAt()).isNotNull();
    }

    @Test
    @DisplayName("답변이 2001자면 E11002로 거부되고 상태가 바뀌지 않는다")
    void answer_rejects_answer_of_2001_chars() {
        client.patch()
                .uri(answerUri(waiting.getInquiryKey()))
                .header(ADMIN_KEY_HEADER, SiteInquiryFixture.ADMIN_API_KEY)
                .contentType(MediaType.APPLICATION_JSON)
                .body(answerBody(SiteInquiryFixture.ANSWER_2001_CHARS))
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.error.errorCode").isEqualTo("E11002");

        assertThat(reload(waiting).getStatus()).isEqualTo(SiteInquiryStatus.WAITING);
        assertThat(reload(waiting).getAnswer()).isNull();
    }

    @Test
    @DisplayName("없는 문의에 답변하면 E11006이 반환된다")
    void answer_rejects_unknown_inquiry() {
        client.patch()
                .uri(answerUri(SiteInquiryFixture.UNKNOWN_INQUIRY_KEY))
                .header(ADMIN_KEY_HEADER, SiteInquiryFixture.ADMIN_API_KEY)
                .contentType(MediaType.APPLICATION_JSON)
                .body(answerBody(SiteInquiryFixture.ANSWER))
                .exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.error.errorCode").isEqualTo("E11006");
    }

    // ── DELETE /api/v1/admin/site-inquiries/{inquiryKey}/answer ─────────────

    @Test
    @DisplayName("답변을 삭제하면 답변 대기로 돌아가고 답변과 답변 시각이 비워진다")
    void clearAnswer_reverts_to_waiting() {
        SiteInquiry answered = siteInquiryRepository.save(
                SiteInquiryFixture.createAnsweredInquiry(SiteInquiryFixture.OTHER_TITLE));

        client.delete()
                .uri(answerUri(answered.getInquiryKey()))
                .header(ADMIN_KEY_HEADER, SiteInquiryFixture.ADMIN_API_KEY)
                .exchange()
                .expectStatus().isOk();

        SiteInquiry reloaded = reload(answered);
        assertThat(reloaded.getStatus()).isEqualTo(SiteInquiryStatus.WAITING);
        assertThat(reloaded.getAnswer()).isNull();
        assertThat(reloaded.getAnsweredAt()).isNull();
    }

    // ── DELETE /api/v1/admin/site-inquiries/{inquiryKey} ────────────────────

    @Test
    @DisplayName("문의를 삭제하면 그 행만 사라지고 다른 문의는 남는다")
    void delete_removes_only_target_row() {
        SiteInquiry other = siteInquiryRepository.save(SiteInquiryFixture.createPublicInquiry(SiteInquiryFixture.OTHER_TITLE));

        client.delete()
                .uri(ADMIN_SITE_INQUIRIES_URI + "/" + waiting.getInquiryKey())
                .header(ADMIN_KEY_HEADER, SiteInquiryFixture.ADMIN_API_KEY)
                .exchange()
                .expectStatus().isOk();

        assertThat(siteInquiryRepository.findByInquiryKey(waiting.getInquiryKey())).isEmpty();
        assertThat(siteInquiryRepository.findByInquiryKey(other.getInquiryKey())).isPresent();
    }

    private String answerUri(String inquiryKey) {
        return ADMIN_SITE_INQUIRIES_URI + "/" + inquiryKey + "/answer";
    }

    private String answerBody(String answer) {
        return """
                {"answer": "%s"}
                """.formatted(answer);
    }

    private SiteInquiry reload(SiteInquiry inquiry) {
        return siteInquiryRepository.findByInquiryKey(inquiry.getInquiryKey()).orElseThrow();
    }
}
