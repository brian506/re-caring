package com.recaring.location.controller;

import com.recaring.location.business.LocationService;
import com.recaring.location.controller.request.GpsRequest;
import com.recaring.location.controller.response.GpsHistoryResponse;
import com.recaring.security.vo.AuthMember;
import com.recaring.support.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/location")
@RequiredArgsConstructor
@Tag(name = "Location", description = "GPS 위치 수신 및 실시간 위치 조회 API")
public class LocationController {

    private final LocationService locationService;

    @Operation(
            summary = "GPS 좌표 전송",
            description = "보호대상자(WARD)가 현재 GPS 좌표를 전송합니다. [WARD 전용]"
    )
    @PostMapping("/gps")
    public ResponseEntity<ApiResponse<Void>> receiveGps(
            @AuthMember String memberKey,
            @Valid @RequestBody GpsRequest request
    ) {
        locationService.receiveGps(memberKey, request.latitude(), request.longitude());
        return ResponseEntity.ok(ApiResponse.success());
    }

    @Operation(
            summary = "실시간 위치 SSE 스트림",
            description = "보호자(GUARDIAN/MANAGER)가 보호대상자의 실시간 위치를 구독합니다. [GUARDIAN 전용]"
    )
    @GetMapping(value = "/stream/{wardKey}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamLocation(
            @AuthMember String memberKey,
            @PathVariable String wardKey
    ) {
        return locationService.streamLocation(memberKey, wardKey);
    }

    @Operation(
            summary = "날짜별 이동 경로 히스토리 조회",
            description = "보호자(GUARDIAN/MANAGER)가 보호대상자의 날짜별 이동 경로를 조회합니다. [GUARDIAN 전용]"
    )
    @GetMapping("/history/{wardKey}")
    public ResponseEntity<ApiResponse<List<GpsHistoryResponse>>> getHistory(
            @AuthMember String memberKey,
            @PathVariable String wardKey,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        List<GpsHistoryResponse> responses = locationService.getHistory(memberKey, wardKey, date)
                .stream()
                .map(GpsHistoryResponse::from)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(responses));
    }
}
