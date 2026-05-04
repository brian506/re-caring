package com.recaring.device.controller;

import com.recaring.device.business.DeviceTokenService;
import com.recaring.device.controller.response.DeviceTokenResponse;
import com.recaring.security.vo.AuthMember;
import com.recaring.support.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/device")
@RequiredArgsConstructor
@Tag(name = "Device", description = "Device Token 발급 API")
public class DeviceTokenController {

    private final DeviceTokenService deviceTokenService;

    @Operation(
            summary = "Device Token 발급",
            description = "보호대상자(WARD)가 GPS 전송용 Device Token을 발급합니다. [WARD 전용]"
    )
    @PostMapping("/token")
    public ResponseEntity<ApiResponse<DeviceTokenResponse>> issueToken(
            @AuthMember String memberKey
    ) {
        String token = deviceTokenService.issueToken(memberKey);
        return ResponseEntity.ok(ApiResponse.success(new DeviceTokenResponse(token)));
    }
}
