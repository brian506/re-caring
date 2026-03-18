package com.recaring.auth.controller;

import com.recaring.auth.business.CookieService;
import com.recaring.auth.business.LocalAuthService;
import com.recaring.auth.business.OAuthService;
import com.recaring.auth.business.TokenRefreshService;
import com.recaring.auth.controller.request.EmailRequest;
import com.recaring.auth.controller.request.NewPasswordRequest;
import com.recaring.auth.controller.request.OauthSignInRequest;
import com.recaring.auth.controller.request.OauthSignUpRequest;
import com.recaring.auth.controller.request.SignInRequest;
import com.recaring.auth.controller.request.SignUpRequest;
import com.recaring.auth.controller.response.MaskEmailResponse;
import com.recaring.auth.controller.response.OAuthSignInResponse;
import com.recaring.auth.controller.response.SignInResponse;
import com.recaring.auth.vo.OAuthProvider;
import com.recaring.auth.vo.Password;
import com.recaring.security.vo.Jwt;
import com.recaring.sms.vo.PhoneNumber;
import com.recaring.support.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final LocalAuthService localAuthService;
    private final OAuthService oAuthService;
    private final CookieService cookieService;
    private final TokenRefreshService refreshService;

    @PostMapping("/sign-up")
    public ResponseEntity<ApiResponse<Void>> signUp(@Valid @RequestBody SignUpRequest request) {
        localAuthService.signUp(request.toCommand());
        return ResponseEntity.ok(ApiResponse.success());
    }

    @PostMapping("/sign-in/local")
    public ResponseEntity<ApiResponse<SignInResponse>> signInByLocal(
            @Valid @RequestBody SignInRequest request,
            HttpServletResponse response
    ) {
        Jwt jwt = localAuthService.signIn(request.toCommand());
        response.addHeader(HttpHeaders.SET_COOKIE, cookieService.create(jwt.refreshToken()).toString());
        return ResponseEntity.ok(ApiResponse.success(new SignInResponse(jwt.accessToken())));
    }

    @PostMapping("/sign-in/kakao")
    public ResponseEntity<ApiResponse<OAuthSignInResponse>> signInByKakao(
            @Valid @RequestBody OauthSignInRequest request,
            HttpServletResponse response
    ) {
        OAuthSignInResponse result = oAuthService.signIn(request.accessToken(), OAuthProvider.KAKAO);
        if (OAuthSignInResponse.SUCCESS.equals(result.status())) {
            response.addHeader(HttpHeaders.SET_COOKIE, cookieService.create(result.refreshToken()).toString());
        }
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @PostMapping("/sign-in/naver")
    public ResponseEntity<ApiResponse<OAuthSignInResponse>> signInByNaver(
            @Valid @RequestBody OauthSignInRequest request,
            HttpServletResponse response
    ) {
        OAuthSignInResponse result = oAuthService.signIn(request.accessToken(), OAuthProvider.NAVER);
        if (OAuthSignInResponse.SUCCESS.equals(result.status())) {
            response.addHeader(HttpHeaders.SET_COOKIE, cookieService.create(result.refreshToken()).toString());
        }
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @PostMapping("/sign-up/kakao")
    public ResponseEntity<ApiResponse<SignInResponse>> signUpByKakao(
            @Valid @RequestBody OauthSignUpRequest request,
            HttpServletResponse response
    ) {
        Jwt jwt = oAuthService.signUp(OAuthProvider.KAKAO, request.toCommand());
        response.addHeader(HttpHeaders.SET_COOKIE, cookieService.create(jwt.refreshToken()).toString());
        return ResponseEntity.ok(ApiResponse.success(new SignInResponse(jwt.accessToken())));
    }

    @PostMapping("/sign-up/naver")
    public ResponseEntity<ApiResponse<SignInResponse>> signUpByNaver(
            @Valid @RequestBody OauthSignUpRequest request,
            HttpServletResponse response
    ) {
        Jwt jwt = oAuthService.signUp(OAuthProvider.NAVER, request.toCommand());
        response.addHeader(HttpHeaders.SET_COOKIE, cookieService.create(jwt.refreshToken()).toString());
        return ResponseEntity.ok(ApiResponse.success(new SignInResponse(jwt.accessToken())));
    }

    @GetMapping("/email")
    public ResponseEntity<ApiResponse<MaskEmailResponse>> findEmail(@Valid @ModelAttribute EmailRequest request) {
        String maskEmail = localAuthService.findEmail(request.name(), request.birth(), new PhoneNumber(request.phone()));
        return ResponseEntity.ok(ApiResponse.success(new MaskEmailResponse(maskEmail)));
    }

    @PostMapping("/password")
    public ResponseEntity<ApiResponse<Void>> findPassword(@Valid @RequestBody NewPasswordRequest request) {
        localAuthService.findPassword(request.smsToken(), new Password(request.password()));
        return ResponseEntity.ok(ApiResponse.success());
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<SignInResponse>> refresh(
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        String refreshToken = cookieService.extract(request);
        Jwt jwt = refreshService.refresh(refreshToken);
        response.addHeader(HttpHeaders.SET_COOKIE, cookieService.create(jwt.refreshToken()).toString());
        return ResponseEntity.ok(ApiResponse.success(new SignInResponse(jwt.accessToken())));
    }

    @PostMapping("/sign-out")
    public ResponseEntity<ApiResponse<Void>> signOut(
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        String refreshToken = cookieService.extract(request);
        localAuthService.signOut(refreshToken);
        response.addHeader(HttpHeaders.SET_COOKIE, cookieService.expire().toString());
        return ResponseEntity.ok(ApiResponse.success());
    }
}
