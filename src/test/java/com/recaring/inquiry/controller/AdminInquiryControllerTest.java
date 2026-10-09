package com.recaring.inquiry.controller;

import com.recaring.care.fixture.CareFixture;
import com.recaring.inquiry.dataaccess.entity.Inquiry;
import com.recaring.inquiry.dataaccess.entity.InquiryStatus;
import com.recaring.inquiry.dataaccess.repository.InquiryRepository;
import com.recaring.inquiry.fixture.InquiryFixture;
import com.recaring.member.dataaccess.entity.Member;
import com.recaring.member.dataaccess.repository.MemberRepository;
import com.recaring.support.AbstractIntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("운영자 문의 API HTTP 통합 테스트")
class AdminInquiryControllerTest extends AbstractIntegrationTest {

    private static final String ADMIN_KEY_HEADER = "X-Admin-Key";
    private static final String ADMIN_INQUIRIES_URI = "/api/v1/admin/inquiries";

    @Autowired private MemberRepository memberRepository;
    @Autowired private InquiryRepository inquiryRepository;

    private Member guardian;
    private Inquiry waiting;

    @BeforeEach
    void setUp() {
        guardian = memberRepository.save(CareFixture.createGuardianMember());
        waiting = inquiryRepository.save(InquiryFixture.createInquiry(guardian.getMemberKey()));
    }

    @AfterEach
    void tearDown() {
        inquiryRepository.deleteAllInBatch();
        memberRepository.deleteAllInBatch();
    }

    // ── PATCH /api/v1/admin/inquiries/{inquiryKey}/answer ───────────────────

    @Test
    @DisplayName("올바른 관리자 키로 답변하면 답변 완료 상태와 답변 시각이 저장된다")
    void answer_persists_answer_and_answered_at() {
        client.patch()
                .uri(answerUri(waiting.getInquiryKey()))
                .header(ADMIN_KEY_HEADER, InquiryFixture.ADMIN_API_KEY)
                .contentType(MediaType.APPLICATION_JSON)
                .body(answerBody(InquiryFixture.ANSWER))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.status").isEqualTo("ANSWERED")
                .jsonPath("$.data.answer").isEqualTo(InquiryFixture.ANSWER);

        Inquiry answered = reload(waiting);
        assertThat(answered.getStatus()).isEqualTo(InquiryStatus.ANSWERED);
        assertThat(answered.getAnswer()).isEqualTo(InquiryFixture.ANSWER);
        assertThat(answered.getAnsweredAt()).isNotNull();
    }

    @Test
    @DisplayName("이미 답변한 문의에 다시 답변하면 새 답변으로 덮어쓴다")
    void answer_overwrites_previous_answer() {
        Inquiry answered = inquiryRepository.save(
                InquiryFixture.createAnsweredInquiry(guardian.getMemberKey(), InquiryFixture.OTHER_TITLE));

        client.patch()
                .uri(answerUri(answered.getInquiryKey()))
                .header(ADMIN_KEY_HEADER, InquiryFixture.ADMIN_API_KEY)
                .contentType(MediaType.APPLICATION_JSON)
                .body(answerBody(InquiryFixture.UPDATED_ANSWER))
                .exchange()
                .expectStatus().isOk();

        Inquiry reloaded = reload(answered);
        assertThat(reloaded.getAnswer()).isEqualTo(InquiryFixture.UPDATED_ANSWER);
        assertThat(reloaded.getAnsweredAt()).isAfter(InquiryFixture.ANSWERED_AT);
    }

    @Test
    @DisplayName("관리자 키 없이 답변하면 401이고 답변이 저장되지 않는다")
    void answer_rejects_missing_admin_key() {
        client.patch()
                .uri(answerUri(waiting.getInquiryKey()))
                .contentType(MediaType.APPLICATION_JSON)
                .body(answerBody(InquiryFixture.ANSWER))
                .exchange()
                .expectStatus().isUnauthorized();

        assertThat(reload(waiting).getStatus()).isEqualTo(InquiryStatus.WAITING);
        assertThat(reload(waiting).getAnswer()).isNull();
    }

    @Test
    @DisplayName("틀린 관리자 키로 답변하면 401이고 답변이 저장되지 않는다")
    void answer_rejects_wrong_admin_key() {
        client.patch()
                .uri(answerUri(waiting.getInquiryKey()))
                .header(ADMIN_KEY_HEADER, InquiryFixture.WRONG_ADMIN_API_KEY)
                .contentType(MediaType.APPLICATION_JSON)
                .body(answerBody(InquiryFixture.ANSWER))
                .exchange()
                .expectStatus().isUnauthorized();

        assertThat(reload(waiting).getStatus()).isEqualTo(InquiryStatus.WAITING);
        assertThat(reload(waiting).getAnswer()).isNull();
    }

    @Test
    @DisplayName("회원 액세스 토큰으로는 운영자 답변 API를 쓸 수 없다")
    void answer_rejects_member_jwt() {
        client.patch()
                .uri(answerUri(waiting.getInquiryKey()))
                .header(HttpHeaders.AUTHORIZATION, bearerToken(guardian.getMemberKey(), guardian.getRole()))
                .contentType(MediaType.APPLICATION_JSON)
                .body(answerBody(InquiryFixture.ANSWER))
                .exchange()
                .expectStatus().isUnauthorized();

        assertThat(reload(waiting).getAnswer()).isNull();
    }

    @Test
    @DisplayName("없는 문의에 답변하면 E11003이 반환된다")
    void answer_rejects_unknown_inquiry() {
        client.patch()
                .uri(answerUri(InquiryFixture.UNKNOWN_INQUIRY_KEY))
                .header(ADMIN_KEY_HEADER, InquiryFixture.ADMIN_API_KEY)
                .contentType(MediaType.APPLICATION_JSON)
                .body(answerBody(InquiryFixture.ANSWER))
                .exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.error.errorCode").isEqualTo("E11003");
    }

    @Test
    @DisplayName("답변이 2000자면 등록된다")
    void answer_accepts_answer_of_2000_chars() {
        client.patch()
                .uri(answerUri(waiting.getInquiryKey()))
                .header(ADMIN_KEY_HEADER, InquiryFixture.ADMIN_API_KEY)
                .contentType(MediaType.APPLICATION_JSON)
                .body(answerBody(InquiryFixture.ANSWER_2000_CHARS))
                .exchange()
                .expectStatus().isOk();

        assertThat(reload(waiting).getAnswer()).isEqualTo(InquiryFixture.ANSWER_2000_CHARS);
    }

    @Test
    @DisplayName("답변이 2001자면 E11002로 거부되고 저장되지 않는다")
    void answer_rejects_answer_of_2001_chars() {
        client.patch()
                .uri(answerUri(waiting.getInquiryKey()))
                .header(ADMIN_KEY_HEADER, InquiryFixture.ADMIN_API_KEY)
                .contentType(MediaType.APPLICATION_JSON)
                .body(answerBody(InquiryFixture.ANSWER_2001_CHARS))
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.error.errorCode").isEqualTo("E11002");

        assertThat(reload(waiting).getAnswer()).isNull();
    }

    // ── GET /api/v1/admin/inquiries ─────────────────────────────────────────

    @Test
    @DisplayName("답변 대기 목록은 답변된 문의를 빼고 오래된 순으로 반환된다")
    void getInquiries_returns_waiting_oldest_first() {
        inquiryRepository.save(InquiryFixture.createAnsweredInquiry(guardian.getMemberKey(), InquiryFixture.TITLE));
        Inquiry newerWaiting = inquiryRepository.save(
                InquiryFixture.createInquiry(guardian.getMemberKey(), InquiryFixture.OTHER_TITLE));

        client.get()
                .uri(ADMIN_INQUIRIES_URI + "?status=WAITING")
                .header(ADMIN_KEY_HEADER, InquiryFixture.ADMIN_API_KEY)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.length()").isEqualTo(2)
                .jsonPath("$.data[0].inquiryKey").isEqualTo(waiting.getInquiryKey())
                .jsonPath("$.data[0].memberKey").isEqualTo(guardian.getMemberKey())
                .jsonPath("$.data[1].inquiryKey").isEqualTo(newerWaiting.getInquiryKey());
    }

    private String answerUri(String inquiryKey) {
        return ADMIN_INQUIRIES_URI + "/" + inquiryKey + "/answer";
    }

    private String answerBody(String answer) {
        return """
                {"answer": "%s"}
                """.formatted(answer);
    }

    private Inquiry reload(Inquiry inquiry) {
        return inquiryRepository.findByInquiryKey(inquiry.getInquiryKey()).orElseThrow();
    }
}
