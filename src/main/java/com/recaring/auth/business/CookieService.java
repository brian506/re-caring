package com.recaring.auth.business;

import com.recaring.support.exception.AppException;
import com.recaring.support.exception.ErrorType;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

import java.util.Arrays;

@Service
public class CookieService {

    private static final String COOKIE_NAME = "refresh_token";

    @Value("${jwt.refresh.expiration}")
    private long refreshExpiration; // ms 단위

    public ResponseCookie create(String token) {
        return ResponseCookie.from(COOKIE_NAME, token)
                .httpOnly(true)
                .secure(true)
                .sameSite("Lax")
                .path("/api/v1/auth")
                .maxAge(refreshExpiration / 1000)
                .build();
    }

    public String extract(HttpServletRequest request) {
        if (request.getCookies() == null) {
            throw new AppException(ErrorType.REQUIRED_AUTH);
        }
        return Arrays.stream(request.getCookies())
                .filter(cookie -> COOKIE_NAME.equals(cookie.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElseThrow(() -> new AppException(ErrorType.REQUIRED_AUTH));
    }

    // 쿠키 만료
    public ResponseCookie expire() {
        return ResponseCookie.from(COOKIE_NAME, "")
                .httpOnly(true)
                .secure(true)
                .sameSite("Lax")
                .path("/api/v1/auth")
                .maxAge(0)
                .build();
    }
}
