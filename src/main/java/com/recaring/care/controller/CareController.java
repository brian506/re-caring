package com.recaring.care.controller;

import com.recaring.care.business.CareInvitationService;
import com.recaring.care.business.CareRelationshipService;
import com.recaring.care.controller.request.AddCaregiverRequest;
import com.recaring.care.controller.request.AddWardRequest;
import com.recaring.care.controller.request.DesignateAvatarRequest;
import com.recaring.care.controller.request.UpdateWardNicknameRequest;
import com.recaring.care.controller.response.CaregiverResponse;
import com.recaring.care.controller.response.ReceivedCareRequestResponse;
import com.recaring.care.controller.response.SentCareRequestResponse;
import com.recaring.care.dataaccess.entity.CareInvitationStatus;
import com.recaring.care.controller.response.WardResponse;
import com.recaring.security.vo.AuthMember;
import com.recaring.support.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
            description = "보호자(GUARDIAN)가 전화번호로 보호 대상자에게 케어 요청을 전송합니다. "
                    + "이미 다른 보호자가 있는 대상자에게도 보낼 수 있으며, 대상자가 수락하면 GUARDIAN으로 연결됩니다. "
                    + "같은 요청자가 같은 대상자에게 보낸 대기 요청이 있으면 E5009. [GUARDIAN 전용]"
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
            summary = "보호자(Guardian) 추가 요청",
            description = "대상자의 기존 보호자가 신규 보호자에게 추가 요청을 보냅니다. 신규 보호자가 수락하면 GUARDIAN으로 연결됩니다. "
                    + "해당 대상자의 보호자라면 누구나 보낼 수 있습니다. [GUARDIAN 전용]"
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
            summary = "보낸 케어 요청 목록 조회",
            description = "내가 보낸 케어 요청 목록을 최신순으로 반환합니다. status 기본값은 PENDING입니다. "
                    + "대상자 추가 요청(targetRole=WARD)은 수락된 뒤에만 wardKey·wardName을 채웁니다. [GUARDIAN 전용]"
    )
    @GetMapping("/requests/sent")
    public ResponseEntity<ApiResponse<List<SentCareRequestResponse>>> getSentRequests(
            @AuthMember String memberKey,
            @RequestParam(defaultValue = "PENDING") CareInvitationStatus status
    ) {
        List<SentCareRequestResponse> responses = careInvitationService.getSentRequests(memberKey, status)
                .stream()
                .map(SentCareRequestResponse::from)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    @Operation(
            summary = "케어 요청 재전송",
            description = "내가 보낸 대기 중인 케어 요청의 알림을 다시 보냅니다. 요청마다 5분에 한 번만 가능합니다. [GUARDIAN 전용]"
    )
    @PostMapping("/requests/{requestKey}/resend")
    public ResponseEntity<ApiResponse<Void>> resendRequest(
            @AuthMember String memberKey,
            @PathVariable String requestKey
    ) {
        careInvitationService.resend(requestKey, memberKey);
        return ResponseEntity.ok(ApiResponse.success());
    }

    @Operation(
            summary = "케어 요청 취소",
            description = "내가 보낸 대기 중인 케어 요청을 취소(삭제)합니다. [GUARDIAN 전용]"
    )
    @DeleteMapping("/requests/{requestKey}")
    public ResponseEntity<ApiResponse<Void>> cancelRequest(
            @AuthMember String memberKey,
            @PathVariable String requestKey
    ) {
        careInvitationService.cancel(requestKey, memberKey);
        return ResponseEntity.ok(ApiResponse.success());
    }

    @Operation(
            summary = "케어 요청 수락",
            description = "수신한 케어 요청을 수락합니다. 수락 시 케어 관계가 생성됩니다. [GUARDIAN, WARD 공통]"
    )
    @PatchMapping("/requests/{requestKey}/accept")
    public ResponseEntity<ApiResponse<Void>> acceptRequest(
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
            @AuthMember String memberKey,
            @PathVariable String requestKey
    ) {
        careInvitationService.reject(requestKey, memberKey);
        return ResponseEntity.ok(ApiResponse.success());
    }

    @Operation(
            summary = "내 보호 대상자 목록 조회",
            description = "내가 보호자인 보호 대상자(Ward) 목록을 반환합니다. [GUARDIAN 전용]"
    )
    @GetMapping("/wards")
    public ResponseEntity<ApiResponse<List<WardResponse>>> getMyWards(
            @AuthMember String memberKey
    ) {
        List<WardResponse> responses = careRelationshipService.getMyWards(memberKey)
                .stream()
                .map(WardResponse::from)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    @Operation(
            summary = "특정 보호 대상자의 보호자 목록 조회",
            description = "특정 보호 대상자(wardKey)에 연결된 보호자 목록을 반환합니다. [GUARDIAN, WARD 공통]"
    )
    @GetMapping("/wards/{wardKey}/caregivers")
    public ResponseEntity<ApiResponse<List<CaregiverResponse>>> getCaregivers(
            @AuthMember String memberKey,
            @PathVariable String wardKey
    ) {
        List<CaregiverResponse> responses = careRelationshipService.getCaregivers(wardKey, memberKey)
                .stream()
                .map(CaregiverResponse::from)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    @Operation(
            summary = "보호 대상자(Ward) 케어 관계 삭제",
            description = """
                    보호자가 자신과 특정 보호 대상자 사이의 케어 관계를 삭제합니다(본인 이탈).
                    남은 보호자의 케어 관계는 유지되며, 내가 보낸 대기 중인 보호자 추가 요청은 함께 삭제됩니다.
                    다른 보호자를 내보내는 기능은 없습니다.
                    [케어 관계가 있는 회원 전용]
                    """
    )
    @DeleteMapping("/wards/{wardKey}")
    public ResponseEntity<ApiResponse<Void>> removeWard(
            @AuthMember String memberKey,
            @Parameter(description = "보호 대상자 memberKey") @PathVariable String wardKey
    ) {
        careRelationshipService.removeWard(memberKey, wardKey);
        return ResponseEntity.ok(ApiResponse.success());
    }

    @Operation(
            summary = "보호 대상자(Ward) 별명 수정",
            description = """
                    보호자가 자신에게만 보이는 보호 대상자의 별명을 설정합니다.
                    대상자의 실명은 그대로 유지되며, 조회 응답의 wardName(실명)과 wardNickname(별명)은 분리되어 내려갑니다.
                    nickname을 비우거나 생략하면 별명이 해제되어 실명이 표시됩니다. [보호 대상자와 케어 관계가 있는 회원 전용]
                    """
    )
    @PatchMapping("/wards/{wardKey}/nickname")
    public ResponseEntity<ApiResponse<Void>> updateWardNickname(
            @AuthMember String memberKey,
            @Parameter(description = "보호 대상자 memberKey") @PathVariable String wardKey,
            @Valid @RequestBody UpdateWardNicknameRequest request
    ) {
        careRelationshipService.updateWardNickname(memberKey, wardKey, request.nickname());
        return ResponseEntity.ok(ApiResponse.success());
    }

    @Operation(
            summary = "보호 대상자 프로필 아바타 지정",
            description = """
                    내가 보는 보호 대상자의 얼굴을 앱 번들 일러스트 16종 중 하나로 지정합니다. 나에게만 보이며,
                    대상자 본인이 고른 얼굴(wardProfileAvatarCode)이나 다른 보호자의 화면은 바뀌지 않습니다.
                    앱은 designatedProfileAvatarCode > wardProfileAvatarCode 순으로 표시합니다. wardProfileAvatarCode는 항상 채워집니다.
                    허용 코드 외 값은 400(E3008)이며, 빈 문자열이나 생략은 지정 해제입니다.
                    [보호 대상자와 케어 관계가 있는 회원 전용]
                    """
    )
    @PatchMapping("/wards/{wardKey}/avatar")
    public ResponseEntity<ApiResponse<Void>> designateWardAvatar(
            @AuthMember String memberKey,
            @Parameter(description = "보호 대상자 memberKey") @PathVariable String wardKey,
            @Valid @RequestBody DesignateAvatarRequest request
    ) {
        careRelationshipService.designateWardAvatar(memberKey, wardKey, request.profileAvatarCode());
        return ResponseEntity.ok(ApiResponse.success());
    }

    @Operation(
            summary = "보호자 프로필 아바타 지정",
            description = """
                    보호 대상자 화면에서 내가 보는 특정 보호자의 얼굴을 지정합니다. 나에게만 보이며,
                    그 사람이 직접 고른 얼굴(profileAvatarCode)이나 다른 회원의 화면은 바뀌지 않습니다.
                    허용 코드 외 값은 400(E3008)이며, 빈 문자열이나 생략은 지정 해제입니다.
                    [보호자 목록을 볼 수 있는 회원 전용: 대상자 본인 또는 GUARDIAN 계열]
                    """
    )
    @PatchMapping("/wards/{wardKey}/caregivers/{caregiverKey}/avatar")
    public ResponseEntity<ApiResponse<Void>> designateCaregiverAvatar(
            @AuthMember String memberKey,
            @Parameter(description = "보호 대상자 memberKey") @PathVariable String wardKey,
            @Parameter(description = "얼굴을 지정할 보호자 memberKey") @PathVariable String caregiverKey,
            @Valid @RequestBody DesignateAvatarRequest request
    ) {
        careRelationshipService.designateCaregiverAvatar(memberKey, wardKey, caregiverKey, request.profileAvatarCode());
        return ResponseEntity.ok(ApiResponse.success());
    }
}
