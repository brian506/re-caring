package com.recaring.notification.controller;

import com.recaring.notification.business.FcmDeviceTokenService;
import com.recaring.notification.controller.request.UpsertFcmDeviceTokenRequest;
import com.recaring.notification.controller.response.FcmDeviceTokenResponse;
import com.recaring.security.vo.AuthMember;
import com.recaring.support.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
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
            summary = "Upsert FCM device token",
            description = "Registers or updates a guardian or manager FCM device token. [GUARDIAN, MANAGER]"
    )
    @PutMapping
    public ResponseEntity<ApiResponse<FcmDeviceTokenResponse>> upsert(
            @Parameter(hidden = true)
            @AuthMember String memberKey,
            @RequestBody UpsertFcmDeviceTokenRequest request
    ) {
        FcmDeviceTokenResponse response = FcmDeviceTokenResponse.from(
                fcmDeviceTokenService.upsert(request.toCommand(memberKey))
        );
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
