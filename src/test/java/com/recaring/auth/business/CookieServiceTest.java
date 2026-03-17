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

    private CookieService cookieService;

    private static final long REFRESH_EXPIRATION_MS = 1209600000L; // 14일

    @BeforeEach
    void setUp() {
        cookieService = new CookieService();
        ReflectionTestUtils.setField(cookieService, "refreshExpiration", REFRESH_EXPIRATION_MS);
    }

    @Test
    @DisplayName("extract()는 요청에서 refresh_token 쿠키 값을 추출한다")
    void extract_success() {
        // given
        MockHttpServletRequest request = new MockHttpServletRequest();
        Cookie cookie = new Cookie("refresh_token", "my-refresh-token");
        request.setCookies(cookie);
        // when
        String token = cookieService.extract(request);
        // then
        assertThat(token).isEqualTo("my-refresh-token");
    }

    @Test
    @DisplayName("쿠키가 없으면 REQUIRED_AUTH 예외가 발생한다")
    void extract_fail_when_no_cookies() {
        // given
        MockHttpServletRequest request = new MockHttpServletRequest();

        // then
        assertThatThrownBy(() -> cookieService.extract(request))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorType())
                .isEqualTo(ErrorType.REQUIRED_AUTH);
    }

    @Test
    @DisplayName("refresh_token 쿠키가 없으면 REQUIRED_AUTH 예외가 발생한다")
    void extract_fail_when_refresh_token_cookie_not_found() {

        // given
        MockHttpServletRequest request = new MockHttpServletRequest();
        Cookie otherCookie = new Cookie("other_cookie", "value");
        request.setCookies(otherCookie);

        // then
        assertThatThrownBy(() -> cookieService.extract(request))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorType())
                .isEqualTo(ErrorType.REQUIRED_AUTH);
    }

    @Test
    @DisplayName("expire()는 maxAge=0인 만료 쿠키를 생성한다")
    void expire_success() {
        ResponseCookie expiredCookie = cookieService.expire();

        assertThat(expiredCookie.getName()).isEqualTo("refresh_token");
        assertThat(expiredCookie.getValue()).isEmpty();
        assertThat(expiredCookie.getMaxAge().getSeconds()).isZero();
        assertThat(expiredCookie.isHttpOnly()).isTrue();
        assertThat(expiredCookie.isSecure()).isTrue();
    }
}
