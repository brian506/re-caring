package com.recaring.location.controller;

import com.recaring.location.business.LocationSettingService;
import com.recaring.location.controller.request.UpdateLocationCollectionIntervalRequest;
import com.recaring.location.controller.response.LocationCollectionIntervalSettingResponse;
import com.recaring.location.controller.response.WardLocationCollectionIntervalResponse;
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
@RequestMapping("/api/v1/location/settings")
@RequiredArgsConstructor
@Tag(name = "Location Setting", description = "Location collection setting API")
public class LocationSettingController {

    private final LocationSettingService locationSettingService;

    @Operation(
            summary = "위치 수집 주기 조회",
            description = "피보호자의 현재 위치 수집 주기와 선택 가능한 옵션 목록을 반환합니다. [GUARDIAN 전용]"
    )
    @GetMapping("/{wardKey}/collection-interval")
    public ResponseEntity<ApiResponse<LocationCollectionIntervalSettingResponse>> getCollectionInterval(
            @Parameter(hidden = true)
            @AuthMember String memberKey,
            @PathVariable String wardKey
    ) {
        LocationCollectionIntervalSettingResponse response =
                LocationCollectionIntervalSettingResponse.from(
                        locationSettingService.getCollectionInterval(memberKey, wardKey)
                );
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(
            summary = "위치 수집 주기 변경",
            description = "피보호자의 위치 수집 주기를 변경합니다. [GUARDIAN 전용]"
    )
    @PatchMapping("/{wardKey}/collection-interval")
    public ResponseEntity<ApiResponse<Void>> updateCollectionInterval(
            @Parameter(hidden = true)
            @AuthMember String memberKey,
            @PathVariable String wardKey,
            @Valid @RequestBody UpdateLocationCollectionIntervalRequest request
    ) {
        locationSettingService.updateCollectionInterval(memberKey, wardKey, request.toInterval());
        return ResponseEntity.ok(ApiResponse.success());
    }

    @Operation(
            summary = "내 위치 수집 주기 조회",
            description = "피보호자 디바이스 앱에서 현재 설정된 위치 수집 주기를 조회합니다. [WARD Device Token 전용]"
    )
    @GetMapping("/collection-interval/me")
    public ResponseEntity<ApiResponse<WardLocationCollectionIntervalResponse>> getMyCollectionInterval(
            @Parameter(hidden = true)
            @AuthMember String wardKey
    ) {
        WardLocationCollectionIntervalResponse response =
                WardLocationCollectionIntervalResponse.from(
                        locationSettingService.getMyCollectionInterval(wardKey)
                );
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
