package com.recaring.sms.controller;

import com.recaring.sms.business.PhoneVerificationService;
import com.recaring.sms.controller.request.SendCodeRequest;
import com.recaring.sms.controller.request.VerifyCodeRequest;
import com.recaring.sms.controller.response.VerifyCodeResponse;
import com.recaring.sms.vo.PhoneNumber;
import com.recaring.sms.vo.SmsCode;
import com.recaring.support.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth/phone")
@RequiredArgsConstructor
@Tag(name = "Phone Verification", description = "전화번호 SMS 인증 API")
public class PhoneVerificationController {

    private final PhoneVerificationService phoneVerificationService;

    @Operation(summary = "인증 코드 발송", description = "입력한 전화번호로 6자리 SMS 인증 코드를 발송합니다.")
    @PostMapping("/send-code")
    public ResponseEntity<ApiResponse<Void>> sendCode(@Valid @RequestBody SendCodeRequest request) {
        phoneVerificationService.sendCode(new PhoneNumber(request.phone()));
        return ResponseEntity.ok(ApiResponse.success());
    }

    @Operation(
            summary = "인증 코드 확인",
            description = "발송된 SMS 인증 코드를 검증하고, 이후 회원가입/비밀번호 재설정에 사용할 `verificationToken`을 반환합니다."
    )
    @PostMapping("/verify")
    public ResponseEntity<ApiResponse<VerifyCodeResponse>> verifyCode(@Valid @RequestBody VerifyCodeRequest request) {
        String smsToken = phoneVerificationService.verifyCode(new PhoneNumber(request.phone()), new SmsCode(request.code()));
        return ResponseEntity.ok(ApiResponse.success(new VerifyCodeResponse(smsToken)));
    }
}
