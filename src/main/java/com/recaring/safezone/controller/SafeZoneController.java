package com.recaring.safezone.controller;

import com.recaring.safezone.business.SafeZoneService;
import com.recaring.safezone.controller.request.CreateSafeZoneRequest;
import com.recaring.safezone.controller.request.UpdateSafeZoneRequest;
import com.recaring.safezone.controller.response.SafeZoneResponse;
import com.recaring.security.vo.AuthMember;
import com.recaring.support.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/care/wards/{wardKey}/safe-zones")
@RequiredArgsConstructor
@Tag(name = "SafeZone", description = "안심존 API (등록, 조회, 수정, 삭제)")
public class SafeZoneController {

    private final SafeZoneService safeZoneService;

    @Operation(summary = "안심존 추가", description = "보호자/관계자가 보호대상자의 안심존을 추가합니다.")
    @PostMapping
    public ResponseEntity<ApiResponse<Void>> addSafeZone(
            @AuthMember String memberKey,
            @PathVariable String wardKey,
            @Valid @RequestBody CreateSafeZoneRequest request
    ) {
        safeZoneService.addSafeZone(memberKey, request.toCommand(wardKey));
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success());
    }

    @Operation(summary = "안심존 목록 조회", description = "보호대상자에게 등록된 안심존 목록을 조회합니다.")
    @GetMapping
    public ResponseEntity<ApiResponse<List<SafeZoneResponse>>> getSafeZones(
            @AuthMember String memberKey,
            @PathVariable String wardKey
    ) {
        List<SafeZoneResponse> responses = safeZoneService.getSafeZones(memberKey, wardKey)
                .stream()
                .map(SafeZoneResponse::from)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    @Operation(summary = "안심존 상세 조회", description = "안심존 하나의 상세 정보를 조회합니다.")
    @GetMapping("/{safeZoneKey}")
    public ResponseEntity<ApiResponse<SafeZoneResponse>> getSafeZone(
            @AuthMember String memberKey,
            @PathVariable String wardKey,
            @PathVariable String safeZoneKey
    ) {
        SafeZoneResponse response = SafeZoneResponse.from(
                safeZoneService.getSafeZone(memberKey, wardKey, safeZoneKey)
        );
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "안심존 수정", description = "안심존의 이름, 주소, 위도, 경도, 반경을 수정합니다.")
    @PatchMapping("/{safeZoneKey}")
    public ResponseEntity<ApiResponse<Void>> updateSafeZone(
            @AuthMember String memberKey,
            @PathVariable String wardKey,
            @PathVariable String safeZoneKey,
            @Valid @RequestBody UpdateSafeZoneRequest request
    ) {
        safeZoneService.updateSafeZone(memberKey, wardKey, safeZoneKey, request.toCommand());
        return ResponseEntity.ok(ApiResponse.success());
    }

    @Operation(summary = "안심존 삭제", description = "안심존을 삭제합니다.")
    @DeleteMapping("/{safeZoneKey}")
    public ResponseEntity<ApiResponse<Void>> deleteSafeZone(
            @AuthMember String memberKey,
            @PathVariable String wardKey,
            @PathVariable String safeZoneKey
    ) {
        safeZoneService.deleteSafeZone(memberKey, wardKey, safeZoneKey);
        return ResponseEntity.ok(ApiResponse.success());
    }
}
