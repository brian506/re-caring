package com.recaring.auth.controller;

import com.recaring.auth.business.LocalAuthService;
import com.recaring.auth.controller.request.SignUpRequest;
import com.recaring.support.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NullMarked;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@NullMarked
public class AuthController {

    private final LocalAuthService localAuthService;

    @PostMapping("/sign-up")
    public ResponseEntity<ApiResponse<Void>> signUp(@Valid @RequestBody SignUpRequest request) {
        localAuthService.signUp(request.toCommand());
        return ResponseEntity.ok(ApiResponse.success());
    }

}
