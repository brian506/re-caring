package com.recaring.sms.controller;

import com.recaring.sms.business.PhoneVerificationService;
import com.recaring.sms.controller.request.SendCodeRequest;
import com.recaring.sms.controller.request.VerifyCodeRequest;
import com.recaring.sms.controller.response.VerifyCodeResponse;
import com.recaring.support.response.ApiResponse;
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
public class PhoneVerificationController {

    private final PhoneVerificationService phoneVerificationService;

    @PostMapping("/send-code")
    public ResponseEntity<ApiResponse<Void>> sendCode(@Valid @RequestBody SendCodeRequest request) {
        phoneVerificationService.sendCode(request.toCommand());
        return ResponseEntity.ok(ApiResponse.success());
    }

    @PostMapping("/verify")
    public ResponseEntity<ApiResponse<VerifyCodeResponse>> verifyCode(@Valid @RequestBody VerifyCodeRequest request) {
        String smsToken = phoneVerificationService.verifyCode(request.toCommand());
        return ResponseEntity.ok(ApiResponse.success(new VerifyCodeResponse(smsToken)));
    }
}
