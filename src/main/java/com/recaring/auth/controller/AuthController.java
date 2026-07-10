package com.recaring.auth.controller;

import com.recaring.auth.business.CookieService;
import com.recaring.auth.business.LocalAuthService;
import com.recaring.auth.business.OAuthService;
import com.recaring.auth.business.TokenRefreshService;
import com.recaring.auth.controller.request.EmailRequest;
import com.recaring.auth.controller.request.NewPasswordRequest;
import com.recaring.auth.controller.request.OauthLinkRequest;
import com.recaring.auth.controller.request.OauthSignInRequest;
import com.recaring.auth.controller.request.SignInRequest;
import com.recaring.auth.controller.request.SignOutRequest;
import com.recaring.auth.controller.request.SignUpRequest;
import com.recaring.auth.controller.response.MaskEmailResponse;
import com.recaring.auth.controller.response.SignInResponse;
import com.recaring.auth.vo.LocalEmail;
import com.recaring.auth.vo.OAuthProvider;
import com.recaring.auth.vo.Password;
import com.recaring.security.vo.AuthMember;
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
import org.springframework.http.HttpStatus;
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
                    카카오 Access Token으로 로그인합니다. Access Token은 응답 바디, Refresh Token은 Cookie로 발급됩니다.
                    연동되지 않은 카카오 계정이면 로그인에 실패합니다. 먼저 로컬 회원가입 후 `/oauth/link/kakao`로 연동해야 합니다.
                    """
    )
    @PostMapping("/sign-in/kakao")
    public ResponseEntity<ApiResponse<SignInResponse>> signInByKakao(
            @Valid @RequestBody OauthSignInRequest request,
            HttpServletResponse response
    ) {
        Jwt jwt = oAuthService.signIn(request.accessToken(), OAuthProvider.KAKAO);
        response.addHeader(HttpHeaders.SET_COOKIE, cookieService.create(jwt.refreshToken()).toString());
        return ResponseEntity.ok(ApiResponse.success(new SignInResponse(jwt.accessToken())));
    }

    @Operation(
            summary = "네이버 로그인",
            description = """
                    네이버 Access Token으로 로그인합니다. Access Token은 응답 바디, Refresh Token은 Cookie로 발급됩니다.
                    연동되지 않은 네이버 계정이면 로그인에 실패합니다. 먼저 로컬 회원가입 후 `/oauth/link/naver`로 연동해야 합니다.
                    """
    )
    @PostMapping("/sign-in/naver")
    public ResponseEntity<ApiResponse<SignInResponse>> signInByNaver(
            @Valid @RequestBody OauthSignInRequest request,
            HttpServletResponse response
    ) {
        Jwt jwt = oAuthService.signIn(request.accessToken(), OAuthProvider.NAVER);
        response.addHeader(HttpHeaders.SET_COOKIE, cookieService.create(jwt.refreshToken()).toString());
        return ResponseEntity.ok(ApiResponse.success(new SignInResponse(jwt.accessToken())));
    }

    @Operation(summary = "카카오 연동", description = "로그인한 계정에 카카오 계정을 연동합니다. (JWT 인증 필요)")
    @PostMapping("/oauth/link/kakao")
    public ResponseEntity<ApiResponse<Void>> linkKakao(
            @AuthMember String memberKey,
            @Valid @RequestBody OauthLinkRequest request
    ) {
        oAuthService.link(memberKey, OAuthProvider.KAKAO, request.accessToken());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success());
    }

    @Operation(summary = "네이버 연동", description = "로그인한 계정에 네이버 계정을 연동합니다. (JWT 인증 필요)")
    @PostMapping("/oauth/link/naver")
    public ResponseEntity<ApiResponse<Void>> linkNaver(
            @AuthMember String memberKey,
            @Valid @RequestBody OauthLinkRequest request
    ) {
        oAuthService.link(memberKey, OAuthProvider.NAVER, request.accessToken());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success());
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
    public ResponseEntity<ApiResponse<Void>> resetPassword(@Valid @RequestBody NewPasswordRequest request) {
        localAuthService.resetPassword(request.smsToken(), new Password(request.password()));
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

    @Operation(
            summary = "로그아웃",
            description = "Refresh Token을 만료시키고 Cookie를 삭제합니다. fcmToken을 함께 보내면 해당 기기의 FCM 토큰도 삭제합니다."
    )
    @PostMapping("/sign-out")
    public ResponseEntity<ApiResponse<Void>> signOut(
            @Valid @RequestBody(required = false) SignOutRequest signOutRequest,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        String refreshToken = cookieService.extract(request);
        String fcmToken = signOutRequest == null ? null : signOutRequest.fcmToken();
        localAuthService.signOut(refreshToken, fcmToken);
        response.addHeader(HttpHeaders.SET_COOKIE, cookieService.expire().toString());
        return ResponseEntity.ok(ApiResponse.success());
    }
}
