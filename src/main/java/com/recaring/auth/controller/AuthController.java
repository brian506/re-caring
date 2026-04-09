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
import com.recaring.auth.vo.LocalEmail;
import com.recaring.auth.vo.OAuthProvider;
import com.recaring.auth.vo.Password;
import com.recaring.security.vo.Jwt;
import com.recaring.sms.vo.PhoneNumber;
import com.recaring.support.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Auth", description = "인증 API (회원가입, 로그인, 토큰 갱신, 로그아웃)")
public class AuthController {

    private final LocalAuthService localAuthService;
    private final OAuthService oAuthService;
    private final CookieService cookieService;
    private final TokenRefreshService refreshService;

    @Operation(summary = "로컬 회원가입", description = "이메일/비밀번호로 회원가입합니다. SMS 인증 토큰이 필요합니다.")
    @PostMapping("/sign-up")
    public ResponseEntity<ApiResponse<Void>> signUp(@Valid @RequestBody SignUpRequest request) {
        localAuthService.signUp(request.toCommand());
        return ResponseEntity.ok(ApiResponse.success());
    }

    @Operation(summary = "로컬 로그인", description = "이메일/비밀번호로 로그인합니다. Access Token은 응답 바디, Refresh Token은 HttpOnly Cookie로 발급됩니다.")
    @PostMapping("/sign-in/local")
    public ResponseEntity<ApiResponse<SignInResponse>> signInByLocal(
            @Valid @RequestBody SignInRequest request,
            HttpServletResponse response
    ) {
        Jwt jwt = localAuthService.signIn(new LocalEmail(request.email()), new Password(request.password()));
        response.addHeader(HttpHeaders.SET_COOKIE, cookieService.create(jwt.refreshToken()).toString());
        return ResponseEntity.ok(ApiResponse.success(new SignInResponse(jwt.accessToken())));
    }

    @Operation(
            summary = "카카오 로그인",
            description = """
                    카카오 Access Token으로 로그인합니다.

                    - `status: SUCCESS` → 로그인 성공. Access Token 반환, Refresh Token은 Cookie 발급
                    - `status: NEED_SIGN_UP` → 미가입 회원. `providerMemberId`를 받아 `/sign-up/kakao`로 이동
                    """
    )
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

    @Operation(
            summary = "네이버 로그인",
            description = """
                    네이버 Access Token으로 로그인합니다.

                    - `status: SUCCESS` → 로그인 성공. Access Token 반환, Refresh Token은 Cookie 발급
                    - `status: NEED_SIGN_UP` → 미가입 회원. `providerMemberId`를 받아 `/sign-up/naver`로 이동
                    """
    )
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

    @Operation(summary = "카카오 회원가입", description = "카카오 로그인 후 NEED_SIGN_UP 상태일 때 추가 정보를 입력해 회원가입합니다.")
    @PostMapping("/sign-up/kakao")
    public ResponseEntity<ApiResponse<SignInResponse>> signUpByKakao(
            @Valid @RequestBody OauthSignUpRequest request,
            HttpServletResponse response
    ) {
        Jwt jwt = oAuthService.signUp(OAuthProvider.KAKAO, request.toCommand());
        response.addHeader(HttpHeaders.SET_COOKIE, cookieService.create(jwt.refreshToken()).toString());
        return ResponseEntity.ok(ApiResponse.success(new SignInResponse(jwt.accessToken())));
    }

    @Operation(summary = "네이버 회원가입", description = "네이버 로그인 후 NEED_SIGN_UP 상태일 때 추가 정보를 입력해 회원가입합니다.")
    @PostMapping("/sign-up/naver")
    public ResponseEntity<ApiResponse<SignInResponse>> signUpByNaver(
            @Valid @RequestBody OauthSignUpRequest request,
            HttpServletResponse response
    ) {
        Jwt jwt = oAuthService.signUp(OAuthProvider.NAVER, request.toCommand());
        response.addHeader(HttpHeaders.SET_COOKIE, cookieService.create(jwt.refreshToken()).toString());
        return ResponseEntity.ok(ApiResponse.success(new SignInResponse(jwt.accessToken())));
    }

    @Operation(summary = "이메일 찾기", description = "이름, 생년월일, 전화번호로 가입된 이메일을 마스킹하여 반환합니다.")
    @GetMapping("/email")
    public ResponseEntity<ApiResponse<MaskEmailResponse>> findEmail(
            @Valid @ModelAttribute EmailRequest request
    ) {
        String maskEmail = localAuthService.findEmail(request.name(), request.birth(), new PhoneNumber(request.phone()));
        return ResponseEntity.ok(ApiResponse.success(new MaskEmailResponse(maskEmail)));
    }

    @Operation(summary = "비밀번호 재설정", description = "SMS 인증 후 발급받은 smsToken으로 새 비밀번호를 설정합니다.")
    @PostMapping("/password")
    public ResponseEntity<ApiResponse<Void>> findPassword(@Valid @RequestBody NewPasswordRequest request) {
        localAuthService.findPassword(request.smsToken(), new Password(request.password()));
        return ResponseEntity.ok(ApiResponse.success());
    }

    @Operation(
            summary = "토큰 갱신",
            description = "HttpOnly Cookie의 Refresh Token을 사용해 새 Access Token과 Refresh Token을 발급합니다."
    )
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

    @Operation(summary = "로그아웃", description = "Refresh Token을 만료시키고 Cookie를 삭제합니다.")
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
