package com.recaring.auth.business;

import com.recaring.support.exception.AppException;
import com.recaring.support.exception.ErrorType;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseCookie;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("CookieService 단위 테스트")
class CookieServiceTest {

    private static final long REFRESH_EXPIRATION_MS = 1_209_600_000L;
    private static final long REFRESH_EXPIRATION_SECONDS = 1_209_600L;
    private static final String COOKIE_NAME = "refresh_token";
    private static final String COOKIE_PATH = "/api/v1/auth";

    private CookieService cookieService;

    @BeforeEach
    void setUp() {
        cookieService = new CookieService();
        ReflectionTestUtils.setField(cookieService, "refreshExpiration", REFRESH_EXPIRATION_MS);
    }

    @Test
    @DisplayName("create()는 설정된 만료시간(ms)을 초 단위로 환산한 HttpOnly 쿠키를 만든다")
    void create_success() {
        // when
        ResponseCookie cookie = cookieService.create("my-refresh-token");

        // then
        assertThat(cookie.getName()).isEqualTo(COOKIE_NAME);
        assertThat(cookie.getValue()).isEqualTo("my-refresh-token");
        assertThat(cookie.getMaxAge().getSeconds()).isEqualTo(REFRESH_EXPIRATION_SECONDS);
        assertThat(cookie.isHttpOnly()).isTrue();
        assertThat(cookie.isSecure()).isTrue();
        assertThat(cookie.getPath()).isEqualTo(COOKIE_PATH);
    }

    @Test
    @DisplayName("extract()는 요청에서 refresh_token 쿠키 값을 추출한다")
    void extract_success() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(
                new Cookie("other_cookie", "other-value"),
                new Cookie(COOKIE_NAME, "my-refresh-token")
        );

        // when
        String token = cookieService.extract(request);

        // then
        assertThat(token).isEqualTo("my-refresh-token");
    }

    @Test
    @DisplayName("쿠키가 하나도 없으면 REQUIRED_AUTH 예외가 발생한다")
    void extract_fail_when_no_cookies() {
        // given
        MockHttpServletRequest request = new MockHttpServletRequest();

        // when & then
        assertThatThrownBy(() -> cookieService.extract(request))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.REQUIRED_AUTH);
    }

    @Test
    @DisplayName("refresh_token 쿠키만 없으면 REQUIRED_AUTH 예외가 발생한다")
    void extract_fail_when_refresh_token_cookie_not_found() {
        // given
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie("other_cookie", "value"));

        // when & then
        assertThatThrownBy(() -> cookieService.extract(request))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.REQUIRED_AUTH);
    }

    @Test
    @DisplayName("expire()는 maxAge=0인 만료 쿠키를 생성한다")
    void expire_success() {
        // when
        ResponseCookie expiredCookie = cookieService.expire();

        // then
        assertThat(expiredCookie.getName()).isEqualTo(COOKIE_NAME);
        assertThat(expiredCookie.getValue()).isEmpty();
        assertThat(expiredCookie.getMaxAge().getSeconds()).isZero();
        assertThat(expiredCookie.isHttpOnly()).isTrue();
        assertThat(expiredCookie.isSecure()).isTrue();
        assertThat(expiredCookie.getPath()).isEqualTo(COOKIE_PATH);
    }
}
