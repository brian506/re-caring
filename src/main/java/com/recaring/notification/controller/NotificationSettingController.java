package com.recaring.notification.controller;

import com.recaring.notification.business.NotificationSettingService;
import com.recaring.notification.controller.request.UpdateAnomalyNotificationSettingRequest;
import com.recaring.notification.controller.request.UpdateBatteryNotificationSettingRequest;
import com.recaring.notification.controller.request.UpdateEmergencyCallNotificationSettingRequest;
import com.recaring.notification.controller.request.UpdateSafeZoneNotificationSettingRequest;
import com.recaring.notification.controller.response.NotificationSettingResponse;
import com.recaring.security.vo.AuthMember;
import com.recaring.support.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/notifications/settings")
@RequiredArgsConstructor
@Tag(name = "Notification Setting", description = "Notification setting API")
public class NotificationSettingController {

    private final NotificationSettingService notificationSettingService;

    @Operation(
            summary = "Get notification settings",
            description = "Returns ST-003 notification settings for a ward. [WARD, GUARDIAN, MANAGER]"
    )
    @GetMapping("/{wardKey}")
    public ResponseEntity<ApiResponse<NotificationSettingResponse>> getSetting(
            @Parameter(hidden = true)
            @AuthMember String memberKey,
            @PathVariable String wardKey
    ) {
        NotificationSettingResponse response = NotificationSettingResponse.from(
                notificationSettingService.getSetting(memberKey, wardKey)
        );
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(
            summary = "Update safe zone notification settings",
            description = "Updates ST-003-1 safe zone notification settings for a ward. [WARD, GUARDIAN, MANAGER]"
    )
    @PatchMapping("/{wardKey}/safe-zone")
    public ResponseEntity<ApiResponse<Void>> updateSafeZone(
            @Parameter(hidden = true)
            @AuthMember String memberKey,
            @PathVariable String wardKey,
            @Valid @RequestBody UpdateSafeZoneNotificationSettingRequest request
    ) {
        notificationSettingService.updateSafeZone(memberKey, request.toCommand(wardKey));
        return ResponseEntity.ok(ApiResponse.success());
    }

    @Operation(
            summary = "Update anomaly notification settings",
            description = "Updates ST-003-2 anomaly notification settings for a ward. [WARD, GUARDIAN, MANAGER]"
    )
    @PatchMapping("/{wardKey}/anomaly")
    public ResponseEntity<ApiResponse<Void>> updateAnomaly(
            @Parameter(hidden = true)
            @AuthMember String memberKey,
            @PathVariable String wardKey,
            @Valid @RequestBody UpdateAnomalyNotificationSettingRequest request
    ) {
        notificationSettingService.updateAnomaly(memberKey, request.toCommand(wardKey));
        return ResponseEntity.ok(ApiResponse.success());
    }

    @Operation(
            summary = "Update emergency call notification settings",
            description = "Updates ST-003-3 emergency call notification settings for a ward. [WARD, GUARDIAN, MANAGER]"
    )
    @PatchMapping("/{wardKey}/emergency-call")
    public ResponseEntity<ApiResponse<Void>> updateEmergencyCall(
            @Parameter(hidden = true)
            @AuthMember String memberKey,
            @PathVariable String wardKey,
            @Valid @RequestBody UpdateEmergencyCallNotificationSettingRequest request
    ) {
        notificationSettingService.updateEmergencyCall(memberKey, request.toCommand(wardKey));
        return ResponseEntity.ok(ApiResponse.success());
    }

    @Operation(
            summary = "Update battery notification settings",
            description = "Updates ST-003-4 battery notification settings for a ward. [WARD, GUARDIAN, MANAGER]"
    )
    @PatchMapping("/{wardKey}/battery")
    public ResponseEntity<ApiResponse<Void>> updateBattery(
            @Parameter(hidden = true)
            @AuthMember String memberKey,
            @PathVariable String wardKey,
            @Valid @RequestBody UpdateBatteryNotificationSettingRequest request
    ) {
        notificationSettingService.updateBattery(memberKey, request.toCommand(wardKey));
        return ResponseEntity.ok(ApiResponse.success());
    }
}
