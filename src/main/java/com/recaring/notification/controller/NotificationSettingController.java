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
            summary = "알림 설정 조회",
            description = "wardKey에 해당하는 피보호자의 알림 설정을 반환합니다."
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
            summary = "안심존 알림 설정 변경",
            description = "안심존 진입/이탈 알림 활성화 여부를 변경합니다."
    )
    @PatchMapping("/{wardKey}/safe-zone")
    public ResponseEntity<ApiResponse<Void>> updateSafeZone(
            @Parameter(hidden = true)
            @AuthMember String memberKey,
            @PathVariable String wardKey,
            @Valid @RequestBody UpdateSafeZoneNotificationSettingRequest request
    ) {
        notificationSettingService.updateSafeZone(memberKey, wardKey, request.entryEnabled(), request.exitEnabled());
        return ResponseEntity.ok(ApiResponse.success());
    }

    @Operation(
            summary = "이상탐지 알림 설정 변경",
            description = "경로 이탈/속도 이상/배회 알림 활성화 여부와 알림 민감도를 변경합니다."
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
            summary = "응급호출 알림 설정 변경",
            description = "응급호출 알림 활성화 여부를 변경합니다."
    )
    @PatchMapping("/{wardKey}/emergency-call")
    public ResponseEntity<ApiResponse<Void>> updateEmergencyCall(
            @Parameter(hidden = true)
            @AuthMember String memberKey,
            @PathVariable String wardKey,
            @Valid @RequestBody UpdateEmergencyCallNotificationSettingRequest request
    ) {
        notificationSettingService.updateEmergencyCall(memberKey, wardKey, request.enabled());
        return ResponseEntity.ok(ApiResponse.success());
    }

    @Operation(
            summary = "배터리 부족 알림 설정 변경",
            description = "배터리 부족 알림 활성화 여부와 알림 기준 배터리 수치(%)를 변경합니다."
    )
    @PatchMapping("/{wardKey}/battery")
    public ResponseEntity<ApiResponse<Void>> updateBattery(
            @Parameter(hidden = true)
            @AuthMember String memberKey,
            @PathVariable String wardKey,
            @Valid @RequestBody UpdateBatteryNotificationSettingRequest request
    ) {
        notificationSettingService.updateBattery(memberKey, wardKey, request.lowBatteryEnabled(), request.toThreshold());
        return ResponseEntity.ok(ApiResponse.success());
    }
}
