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

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("1:1 문의 API HTTP 통합 테스트")
class InquiryControllerTest extends AbstractIntegrationTest {

    private static final String INQUIRIES_URI = "/api/v1/inquiries";
    private static final String MY_INQUIRIES_URI = "/api/v1/inquiries/me";

    @Autowired private MemberRepository memberRepository;
    @Autowired private InquiryRepository inquiryRepository;

    private Member guardian;
    private Member ward;
    private String guardianAuth;
    private String wardAuth;

    @BeforeEach
    void setUp() {
        guardian = memberRepository.save(CareFixture.createGuardianMember());
        ward = memberRepository.save(CareFixture.createWardMember());
        guardianAuth = bearerToken(guardian.getMemberKey(), guardian.getRole());
        wardAuth = bearerToken(ward.getMemberKey(), ward.getRole());
    }

    @AfterEach
    void tearDown() {
        inquiryRepository.deleteAllInBatch();
        memberRepository.deleteAllInBatch();
    }

    // ── POST /api/v1/inquiries ──────────────────────────────────────────────

    @Test
    @DisplayName("문의를 접수하면 답변 대기 상태로 저장되고 응답에 문의 키가 담긴다")
    void submit_persists_waiting_inquiry() {
        AtomicReference<String> inquiryKey = new AtomicReference<>();
        client.post()
                .uri(INQUIRIES_URI)
                .header(HttpHeaders.AUTHORIZATION, guardianAuth)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body(InquiryFixture.TITLE, InquiryFixture.CONTENT))
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.resultType").isEqualTo("SUCCESS")
                .jsonPath("$.data.status").isEqualTo("WAITING")
                .jsonPath("$.data.category").isEqualTo(InquiryFixture.CATEGORY.name())
                .jsonPath("$.data.answer").isEmpty()
                .jsonPath("$.data.answeredAt").isEmpty()
                .jsonPath("$.data.inquiryKey").value(key -> inquiryKey.set((String) key));

        Inquiry saved = onlyInquiryOf(guardian);
        assertThat(saved.getInquiryKey()).isEqualTo(inquiryKey.get());
        assertThat(saved.getStatus()).isEqualTo(InquiryStatus.WAITING);
        assertThat(saved.getCategory()).isEqualTo(InquiryFixture.CATEGORY);
        assertThat(saved.getTitle()).isEqualTo(InquiryFixture.TITLE);
        assertThat(saved.getContent()).isEqualTo(InquiryFixture.CONTENT);
    }

    @Test
    @DisplayName("보호 대상자도 문의를 접수할 수 있다")
    void submit_allows_ward() {
        client.post()
                .uri(INQUIRIES_URI)
                .header(HttpHeaders.AUTHORIZATION, wardAuth)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body(InquiryFixture.TITLE, InquiryFixture.CONTENT))
                .exchange()
                .expectStatus().isCreated();

        assertThat(onlyInquiryOf(ward).getTitle()).isEqualTo(InquiryFixture.TITLE);
    }

    @Test
    @DisplayName("제목과 내용의 앞뒤 공백은 잘라서 저장한다")
    void submit_strips_surrounding_whitespace() {
        client.post()
                .uri(INQUIRIES_URI)
                .header(HttpHeaders.AUTHORIZATION, guardianAuth)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body("  " + InquiryFixture.TITLE + "  ", "\\n" + InquiryFixture.CONTENT + "  "))
                .exchange()
                .expectStatus().isCreated();

        Inquiry saved = onlyInquiryOf(guardian);
        assertThat(saved.getTitle()).isEqualTo(InquiryFixture.TITLE);
        assertThat(saved.getContent()).isEqualTo(InquiryFixture.CONTENT);
    }

    @Test
    @DisplayName("앞뒤 공백을 제외한 제목이 50자면 접수된다")
    void submit_accepts_title_of_50_chars() {
        client.post()
                .uri(INQUIRIES_URI)
                .header(HttpHeaders.AUTHORIZATION, guardianAuth)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body(" " + InquiryFixture.TITLE_50_CHARS + " ", InquiryFixture.CONTENT))
                .exchange()
                .expectStatus().isCreated();

        assertThat(onlyInquiryOf(guardian).getTitle()).isEqualTo(InquiryFixture.TITLE_50_CHARS);
    }

    @Test
    @DisplayName("제목이 51자면 E11000으로 거부되고 저장되지 않는다")
    void submit_rejects_title_of_51_chars() {
        client.post()
                .uri(INQUIRIES_URI)
                .header(HttpHeaders.AUTHORIZATION, guardianAuth)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body(InquiryFixture.TITLE_51_CHARS, InquiryFixture.CONTENT))
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.error.errorCode").isEqualTo("E11000");

        assertThat(inquiryRepository.findAll()).isEmpty();
    }

    @Test
    @DisplayName("내용이 1000자면 접수된다")
    void submit_accepts_content_of_1000_chars() {
        client.post()
                .uri(INQUIRIES_URI)
                .header(HttpHeaders.AUTHORIZATION, guardianAuth)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body(InquiryFixture.TITLE, InquiryFixture.CONTENT_1000_CHARS))
                .exchange()
                .expectStatus().isCreated();

        assertThat(onlyInquiryOf(guardian).getContent()).isEqualTo(InquiryFixture.CONTENT_1000_CHARS);
    }

    @Test
    @DisplayName("내용이 1001자면 E11001로 거부되고 저장되지 않는다")
    void submit_rejects_content_of_1001_chars() {
        client.post()
                .uri(INQUIRIES_URI)
                .header(HttpHeaders.AUTHORIZATION, guardianAuth)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body(InquiryFixture.TITLE, InquiryFixture.CONTENT_1001_CHARS))
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.error.errorCode").isEqualTo("E11001");

        assertThat(inquiryRepository.findAll()).isEmpty();
    }

    @Test
    @DisplayName("공백뿐인 제목은 E11000으로 거부되고 저장되지 않는다")
    void submit_rejects_blank_title() {
        client.post()
                .uri(INQUIRIES_URI)
                .header(HttpHeaders.AUTHORIZATION, guardianAuth)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body("   ", InquiryFixture.CONTENT))
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.error.errorCode").isEqualTo("E11000");

        assertThat(inquiryRepository.findAll()).isEmpty();
    }

    @Test
    @DisplayName("공백뿐인 내용은 E11001로 거부되고 저장되지 않는다")
    void submit_rejects_blank_content() {
        client.post()
                .uri(INQUIRIES_URI)
                .header(HttpHeaders.AUTHORIZATION, guardianAuth)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body(InquiryFixture.TITLE, "   "))
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.error.errorCode").isEqualTo("E11001");

        assertThat(inquiryRepository.findAll()).isEmpty();
    }

    @Test
    @DisplayName("정의되지 않은 문의 유형은 400으로 거부되고 저장되지 않는다")
    void submit_rejects_unknown_category() {
        client.post()
                .uri(INQUIRIES_URI)
                .header(HttpHeaders.AUTHORIZATION, guardianAuth)
                .contentType(MediaType.APPLICATION_JSON)
                .body("""
                        {"category": "PAYMENT", "title": "%s", "content": "%s"}
                        """.formatted(InquiryFixture.TITLE, InquiryFixture.CONTENT))
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.error.errorCode").isEqualTo("E400");

        assertThat(inquiryRepository.findAll()).isEmpty();
    }

    // ── GET /api/v1/inquiries/me ────────────────────────────────────────────

    @Test
    @DisplayName("내 문의 목록은 다른 회원의 문의 없이 최신순으로 반환된다")
    void getMyInquiries_returns_only_own_latest_first() {
        Inquiry older = inquiryRepository.save(InquiryFixture.createInquiry(guardian.getMemberKey(), InquiryFixture.TITLE));
        inquiryRepository.save(InquiryFixture.createInquiry(ward.getMemberKey(), InquiryFixture.TITLE));
        Inquiry newer = inquiryRepository.save(InquiryFixture.createInquiry(guardian.getMemberKey(), InquiryFixture.OTHER_TITLE));

        client.get()
                .uri(MY_INQUIRIES_URI)
                .header(HttpHeaders.AUTHORIZATION, guardianAuth)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.length()").isEqualTo(2)
                .jsonPath("$.data[0].inquiryKey").isEqualTo(newer.getInquiryKey())
                .jsonPath("$.data[0].title").isEqualTo(InquiryFixture.OTHER_TITLE)
                .jsonPath("$.data[1].inquiryKey").isEqualTo(older.getInquiryKey());
    }

    @Test
    @DisplayName("답변된 문의는 목록에서 답변 완료 상태와 답변·답변 시각이 보인다")
    void getMyInquiries_shows_answer() {
        inquiryRepository.save(InquiryFixture.createAnsweredInquiry(guardian.getMemberKey(), InquiryFixture.TITLE));

        client.get()
                .uri(MY_INQUIRIES_URI)
                .header(HttpHeaders.AUTHORIZATION, guardianAuth)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data[0].status").isEqualTo("ANSWERED")
                .jsonPath("$.data[0].answer").isEqualTo(InquiryFixture.ANSWER)
                .jsonPath("$.data[0].answeredAt").isNotEmpty();
    }

    private String body(String title, String content) {
        return """
                {"category": "%s", "title": "%s", "content": "%s"}
                """.formatted(InquiryFixture.CATEGORY.name(), title, content);
    }

    private Inquiry onlyInquiryOf(Member member) {
        List<Inquiry> inquiries = inquiryRepository.findByMemberKeyOrderByIdDesc(member.getMemberKey());
        assertThat(inquiries).hasSize(1);
        return inquiries.getFirst();
    }
}
