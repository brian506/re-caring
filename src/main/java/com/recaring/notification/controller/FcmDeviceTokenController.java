package com.recaring.notification.controller;

import com.recaring.notification.business.FcmDeviceTokenService;
import com.recaring.notification.controller.request.UpsertFcmDeviceTokenRequest;
import com.recaring.notification.controller.response.FcmDeviceTokenResponse;
import com.recaring.security.vo.AuthMember;
import com.recaring.support.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/notifications/device-tokens")
@RequiredArgsConstructor
@Tag(name = "FCM Device Token", description = "FCM device token API")
public class FcmDeviceTokenController {

    private final FcmDeviceTokenService fcmDeviceTokenService;

    @Operation(
            summary = "FCM device token 생성 및 업데이트",
            description = "FCM 토큰 없으면 생성하고 있으면 수정. [GUARDIAN, MANAGER, WARD]"
    )
    @PutMapping
    public ResponseEntity<ApiResponse<FcmDeviceTokenResponse>> upsert(
            @Parameter(hidden = true)
            @AuthMember String memberKey,
            @Valid @RequestBody UpsertFcmDeviceTokenRequest request
    ) {
        FcmDeviceTokenResponse response = FcmDeviceTokenResponse.from(
                fcmDeviceTokenService.upsert(memberKey, request.token(), request.careRole(), request.platform())
        );
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
