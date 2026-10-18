package com.recaring.siteinquiry.controller;

import com.recaring.siteinquiry.dataaccess.entity.SiteInquiry;
import com.recaring.siteinquiry.dataaccess.repository.SiteInquiryRepository;
import com.recaring.siteinquiry.fixture.SiteInquiryFixture;
import com.recaring.support.AbstractIntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("웹사이트 문의 API HTTP 통합 테스트")
class SiteInquiryControllerTest extends AbstractIntegrationTest {

    private static final String REAL_IP_HEADER = "X-Real-IP";
    private static final String SITE_INQUIRIES_URI = "/api/v1/site/inquiries";

    @Autowired private SiteInquiryRepository siteInquiryRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private StringRedisTemplate redisTemplate;

    @AfterEach
    void tearDown() {
        siteInquiryRepository.deleteAllInBatch();
        redisTemplate.getConnectionFactory().getConnection().serverCommands().flushAll();
    }

    // ── POST /api/v1/site/inquiries ─────────────────────────────────────────

    @Test
    @DisplayName("로그인 없이 비밀글을 등록하면 저장되고 비밀번호는 평문이 아닌 해시로 저장된다")
    void submit_persists_inquiry_with_hashed_password() {
        client.post()
                .uri(SITE_INQUIRIES_URI)
                .header(REAL_IP_HEADER, SiteInquiryFixture.CLIENT_IP)
                .contentType(MediaType.APPLICATION_JSON)
                .body(secretBody(SiteInquiryFixture.TITLE, SiteInquiryFixture.EMAIL, SiteInquiryFixture.PASSWORD))
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.data.status").isEqualTo("WAITING")
                .jsonPath("$.data.isSecret").isEqualTo(true);

        List<SiteInquiry> saved = siteInquiryRepository.findAll();
        assertThat(saved).hasSize(1);
        assertThat(saved.getFirst().getEmail()).isEqualTo(SiteInquiryFixture.EMAIL);
        assertThat(saved.getFirst().getPhone()).isEqualTo(SiteInquiryFixture.PHONE);
        assertThat(saved.getFirst().getPasswordHash()).isNotEqualTo(SiteInquiryFixture.PASSWORD);
        assertThat(passwordEncoder.matches(SiteInquiryFixture.PASSWORD, saved.getFirst().getPasswordHash())).isTrue();
    }

    @Test
    @DisplayName("비밀글인데 비밀번호가 없으면 E11004로 거부되고 저장되지 않는다")
    void submit_rejects_secret_without_password() {
        client.post()
                .uri(SITE_INQUIRIES_URI)
                .header(REAL_IP_HEADER, SiteInquiryFixture.CLIENT_IP)
                .contentType(MediaType.APPLICATION_JSON)
                .body(secretBodyWithoutPassword())
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.error.errorCode").isEqualTo("E11004");

        assertThat(siteInquiryRepository.count()).isZero();
    }

    @Test
    @DisplayName("제목이 51자면 E11000으로 거부되고 저장되지 않는다")
    void submit_rejects_title_of_51_chars() {
        client.post()
                .uri(SITE_INQUIRIES_URI)
                .header(REAL_IP_HEADER, SiteInquiryFixture.CLIENT_IP)
                .contentType(MediaType.APPLICATION_JSON)
                .body(publicBody(SiteInquiryFixture.TITLE_51_CHARS, SiteInquiryFixture.EMAIL))
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.error.errorCode").isEqualTo("E11000");

        assertThat(siteInquiryRepository.count()).isZero();
    }

    @Test
    @DisplayName("이메일 형식이 틀리면 400으로 거부되고 저장되지 않는다")
    void submit_rejects_invalid_email() {
        client.post()
                .uri(SITE_INQUIRIES_URI)
                .header(REAL_IP_HEADER, SiteInquiryFixture.CLIENT_IP)
                .contentType(MediaType.APPLICATION_JSON)
                .body(publicBody(SiteInquiryFixture.TITLE, SiteInquiryFixture.INVALID_EMAIL))
                .exchange()
                .expectStatus().isBadRequest();

        assertThat(siteInquiryRepository.count()).isZero();
    }

    @Test
    @DisplayName("같은 IP의 6번째 등록은 E11007로 거부되고 다른 IP는 한도를 공유하지 않는다")
    void submit_rejects_sixth_request_from_same_ip_only() {
        submitPublicFrom(SiteInquiryFixture.CLIENT_IP);
        submitPublicFrom(SiteInquiryFixture.CLIENT_IP);
        submitPublicFrom(SiteInquiryFixture.CLIENT_IP);
        submitPublicFrom(SiteInquiryFixture.CLIENT_IP);
        submitPublicFrom(SiteInquiryFixture.CLIENT_IP);

        client.post()
                .uri(SITE_INQUIRIES_URI)
                .header(REAL_IP_HEADER, SiteInquiryFixture.CLIENT_IP)
                .contentType(MediaType.APPLICATION_JSON)
                .body(publicBody(SiteInquiryFixture.TITLE, SiteInquiryFixture.EMAIL))
                .exchange()
                .expectStatus().isEqualTo(429)
                .expectBody()
                .jsonPath("$.error.errorCode").isEqualTo("E11007");

        assertThat(siteInquiryRepository.count()).isEqualTo(5);
        submitPublicFrom(SiteInquiryFixture.OTHER_CLIENT_IP);
        assertThat(siteInquiryRepository.count()).isEqualTo(6);
    }

    // ── GET /api/v1/site/inquiries ──────────────────────────────────────────

    @Test
    @DisplayName("목록은 최신순이고 내용·이메일·연락처를 포함하지 않는다")
    void getInquiries_returns_latest_first_without_private_fields() {
        SiteInquiry older = siteInquiryRepository.save(SiteInquiryFixture.createPublicInquiry(SiteInquiryFixture.TITLE));
        SiteInquiry newer = siteInquiryRepository.save(SiteInquiryFixture.createPublicInquiry(SiteInquiryFixture.OTHER_TITLE));

        client.get()
                .uri(SITE_INQUIRIES_URI)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.length()").isEqualTo(2)
                .jsonPath("$.data[0].inquiryKey").isEqualTo(newer.getInquiryKey())
                .jsonPath("$.data[1].inquiryKey").isEqualTo(older.getInquiryKey())
                .jsonPath("$.data[0].content").doesNotExist()
                .jsonPath("$.data[0].email").doesNotExist()
                .jsonPath("$.data[0].phone").doesNotExist();
    }

    // ── POST /api/v1/site/inquiries/{inquiryKey}/view ───────────────────────

    @Test
    @DisplayName("공개글 상세는 비밀번호 없이 내용과 답변을 돌려주고 연락처는 포함하지 않는다")
    void view_public_inquiry_without_password() {
        SiteInquiry answered = siteInquiryRepository.save(SiteInquiryFixture.createAnsweredInquiry(SiteInquiryFixture.TITLE));

        client.post()
                .uri(viewUri(answered.getInquiryKey()))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.content").isEqualTo(SiteInquiryFixture.CONTENT)
                .jsonPath("$.data.answer").isEqualTo(SiteInquiryFixture.ANSWER)
                .jsonPath("$.data.status").isEqualTo("ANSWERED")
                .jsonPath("$.data.email").doesNotExist()
                .jsonPath("$.data.phone").doesNotExist();
    }

    @Test
    @DisplayName("비밀글을 틀린 비밀번호로 조회하면 E11005로 거부된다")
    void view_secret_inquiry_rejects_wrong_password() {
        SiteInquiry secret = saveSecretInquiry();

        client.post()
                .uri(viewUri(secret.getInquiryKey()))
                .contentType(MediaType.APPLICATION_JSON)
                .body(passwordBody(SiteInquiryFixture.WRONG_PASSWORD))
                .exchange()
                .expectStatus().isForbidden()
                .expectBody()
                .jsonPath("$.error.errorCode").isEqualTo("E11005");
    }

    @Test
    @DisplayName("비밀번호를 5번 틀린 뒤에는 맞는 비밀번호로도 E11008로 거부된다")
    void view_secret_inquiry_locks_after_five_failures() {
        SiteInquiry secret = saveSecretInquiry();
        viewWithWrongPassword(secret);
        viewWithWrongPassword(secret);
        viewWithWrongPassword(secret);
        viewWithWrongPassword(secret);
        viewWithWrongPassword(secret);

        client.post()
                .uri(viewUri(secret.getInquiryKey()))
                .contentType(MediaType.APPLICATION_JSON)
                .body(passwordBody(SiteInquiryFixture.PASSWORD))
                .exchange()
                .expectStatus().isEqualTo(429)
                .expectBody()
                .jsonPath("$.error.errorCode").isEqualTo("E11008");
    }

    @Test
    @DisplayName("없는 문의를 조회하면 E11006이 반환된다")
    void view_rejects_unknown_inquiry() {
        client.post()
                .uri(viewUri(SiteInquiryFixture.UNKNOWN_INQUIRY_KEY))
                .exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.error.errorCode").isEqualTo("E11006");
    }

    private SiteInquiry saveSecretInquiry() {
        return siteInquiryRepository.save(
                SiteInquiryFixture.createSecretInquiry(passwordEncoder.encode(SiteInquiryFixture.PASSWORD)));
    }

    private void submitPublicFrom(String clientIp) {
        client.post()
                .uri(SITE_INQUIRIES_URI)
                .header(REAL_IP_HEADER, clientIp)
                .contentType(MediaType.APPLICATION_JSON)
                .body(publicBody(SiteInquiryFixture.TITLE, SiteInquiryFixture.EMAIL))
                .exchange()
                .expectStatus().isCreated();
    }

    private void viewWithWrongPassword(SiteInquiry inquiry) {
        client.post()
                .uri(viewUri(inquiry.getInquiryKey()))
                .contentType(MediaType.APPLICATION_JSON)
                .body(passwordBody(SiteInquiryFixture.WRONG_PASSWORD))
                .exchange()
                .expectStatus().isForbidden();
    }

    private String viewUri(String inquiryKey) {
        return SITE_INQUIRIES_URI + "/" + inquiryKey + "/view";
    }

    private String publicBody(String title, String email) {
        return """
                {"category": "%s", "title": "%s", "content": "%s", "email": "%s", "phone": "%s", "isSecret": false}
                """.formatted(SiteInquiryFixture.CATEGORY, title, SiteInquiryFixture.CONTENT, email, SiteInquiryFixture.PHONE);
    }

    private String secretBody(String title, String email, String password) {
        return """
                {"category": "%s", "title": "%s", "content": "%s", "email": "%s", "phone": "%s", "isSecret": true, "password": "%s"}
                """.formatted(SiteInquiryFixture.CATEGORY, title, SiteInquiryFixture.CONTENT, email, SiteInquiryFixture.PHONE, password);
    }

    private String secretBodyWithoutPassword() {
        return """
                {"category": "%s", "title": "%s", "content": "%s", "email": "%s", "isSecret": true}
                """.formatted(SiteInquiryFixture.CATEGORY, SiteInquiryFixture.TITLE, SiteInquiryFixture.CONTENT, SiteInquiryFixture.EMAIL);
    }

    private String passwordBody(String password) {
        return """
                {"password": "%s"}
                """.formatted(password);
    }
}
