package com.recaring.care.controller;

import com.recaring.care.business.CareInvitationService;
import com.recaring.care.business.CareRelationshipService;
import com.recaring.care.controller.request.AddCaregiverRequest;
import com.recaring.care.controller.request.AddWardRequest;
import com.recaring.care.controller.response.CaregiverResponse;
import com.recaring.care.controller.response.ReceivedCareRequestResponse;
import com.recaring.care.controller.response.WardResponse;
import com.recaring.security.vo.AuthMember;
import com.recaring.support.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/care")
@RequiredArgsConstructor
@Tag(name = "Care", description = "케어 관계 API (보호 대상자 추가, 케어 요청 관리, 관계 조회)")
public class CareController {

    private final CareInvitationService careInvitationService;
    private final CareRelationshipService careRelationshipService;

    @Operation(
            summary = "보호 대상자(Ward) 추가 요청",
            description = "보호자(GUARDIAN)가 전화번호로 보호 대상자에게 케어 요청을 전송합니다. [GUARDIAN 전용]"
    )
    @PostMapping("/requests/ward")
    public ResponseEntity<ApiResponse<Void>> requestAddWard(
            @AuthMember String memberKey,
            @Valid @RequestBody AddWardRequest request
    ) {
        careInvitationService.sendWardInvitation(memberKey, request.phoneNumber());
        return ResponseEntity.ok(ApiResponse.success());
    }

    @Operation(
            summary = "관리자(Manager) 추가 요청",
            description = "보호자(GUARDIAN)가 보호 대상자를 대신해 다른 관리자를 추가 요청합니다. [GUARDIAN 전용]"
    )
    @PostMapping("/requests/manager")
    public ResponseEntity<ApiResponse<Void>> requestAddManager(
            @AuthMember String memberKey,
            @Valid @RequestBody AddCaregiverRequest request
    ) {
        careInvitationService.sendManagerInvitation(memberKey, request.phoneNumber(), request.wardMemberKey());
        return ResponseEntity.ok(ApiResponse.success());
    }

    @Operation(
            summary = "보호자(Guardian) 추가 요청",
            description = "보호자(GUARDIAN)가 보호 대상자를 대신해 다른 보호자를 추가 요청합니다. [GUARDIAN 전용]"
    )
    @PostMapping("/requests/guardian")
    public ResponseEntity<ApiResponse<Void>> requestAddGuardian(
            @AuthMember String memberKey,
            @Valid @RequestBody AddCaregiverRequest request
    ) {
        careInvitationService.sendGuardianInvitation(memberKey, request.phoneNumber(), request.wardMemberKey());
        return ResponseEntity.ok(ApiResponse.success());
    }

    @Operation(
            summary = "받은 케어 요청 목록 조회",
            description = "내가 수신한 케어 요청 중 PENDING(대기 중) 상태인 목록을 반환합니다. [GUARDIAN, WARD 공통]"
    )
    @GetMapping("/requests/received")
    public ResponseEntity<ApiResponse<List<ReceivedCareRequestResponse>>> getReceivedRequests(
            @AuthMember String memberKey
    ) {
        List<ReceivedCareRequestResponse> responses = careInvitationService.getReceivedRequests(memberKey)
                .stream()
                .map(ReceivedCareRequestResponse::from)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    @Operation(
            summary = "케어 요청 수락",
            description = "수신한 케어 요청을 수락합니다. 수락 시 케어 관계가 생성됩니다. [GUARDIAN, WARD 공통]"
    )
    @PatchMapping("/requests/{requestKey}/accept")
    public ResponseEntity<ApiResponse<Void>> acceptRequestAsWard(
            @AuthMember String memberKey,
            @PathVariable String requestKey
    ) {
        careInvitationService.accept(requestKey, memberKey);
        return ResponseEntity.ok(ApiResponse.success());
    }

    @Operation(
            summary = "케어 요청 거절",
            description = "수신한 케어 요청을 거절합니다. [GUARDIAN, WARD 공통]"
    )
    @PatchMapping("/requests/{requestKey}/reject")
    public ResponseEntity<ApiResponse<Void>> rejectRequest(
            @Parameter(hidden = true) @AuthMember String memberKey,
            @PathVariable String requestKey
    ) {
        careInvitationService.reject(requestKey, memberKey);
        return ResponseEntity.ok(ApiResponse.success());
    }

    @Operation(
            summary = "내 보호 대상자 목록 조회",
            description = "내가 보호자/관리자인 보호 대상자(Ward) 목록을 반환합니다. [GUARDIAN 전용]"
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "보호 대상자 목록 반환"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "GUARDIAN 권한 없음")
    })
    @GetMapping("/wards")
    public ResponseEntity<ApiResponse<List<WardResponse>>> getMyWards(
            @Parameter(hidden = true) @AuthMember String memberKey
    ) {
        List<WardResponse> responses = careRelationshipService.getMyWards(memberKey)
                .stream()
                .map(WardResponse::from)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    @Operation(
            summary = "특정 보호 대상자의 보호자/관리자 목록 조회",
            description = "특정 보호 대상자(wardKey)에 연결된 보호자 및 관리자 목록을 반환합니다. [GUARDIAN, WARD 공통]"
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "보호자/관리자 목록 반환"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "해당 보호 대상자와 케어 관계가 없음")
    })
    @GetMapping("/wards/{wardKey}/caregivers")
    public ResponseEntity<ApiResponse<List<CaregiverResponse>>> getCaregivers(
            @Parameter(hidden = true) @AuthMember String memberKey,
            @Parameter(description = "보호 대상자 memberKey", example = "abc123-uuid") @PathVariable String wardKey
    ) {
        List<CaregiverResponse> responses = careRelationshipService.getCaregivers(wardKey, memberKey)
                .stream()
                .map(CaregiverResponse::from)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(responses));
    }
}
