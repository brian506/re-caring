package com.recaring.care.controller;

import com.recaring.care.business.CareRelationshipService;
import com.recaring.care.business.CareInvitationService;
import com.recaring.care.controller.request.AddCaregiverRequest;
import com.recaring.care.controller.request.AddWardRequest;
import com.recaring.care.controller.response.CaregiverResponse;
import com.recaring.care.controller.response.ReceivedCareRequestResponse;
import com.recaring.care.controller.response.WardResponse;
import com.recaring.care.dataaccess.entity.CareRole;
import com.recaring.security.vo.AuthMember;
import com.recaring.support.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/care")
@RequiredArgsConstructor
public class CareController {

    private final CareInvitationService careInvitationService;
    private final CareRelationshipService careRelationshipService;

    @PostMapping("/requests/ward")
    public ResponseEntity<ApiResponse<Void>> requestAddWard(
            @AuthMember String memberKey,
            @Valid @RequestBody AddWardRequest request
    ) {
        careInvitationService.sendWardInvitation(request.toCommand(memberKey));
        return ResponseEntity.ok(ApiResponse.success());
    }

    @PostMapping("/requests/manager")
    public ResponseEntity<ApiResponse<Void>> requestAddManager(
            @AuthMember String memberKey,
            @Valid @RequestBody AddCaregiverRequest request
    ) {
        careInvitationService.sendManagerInvitation(request.toCommand(memberKey, CareRole.MANAGER));
        return ResponseEntity.ok(ApiResponse.success());
    }

    /**
     * 받은 케어 요청 목록 조회 (PENDING 상태만)
     */
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

    @PatchMapping("/requests/{requestKey}/accept")
    public ResponseEntity<ApiResponse<Void>> acceptRequestAsWard(
            @AuthMember String memberKey,
            @PathVariable String requestKey
    ) {
        careInvitationService.accept(requestKey, memberKey);
        return ResponseEntity.ok(ApiResponse.success());
    }

    @PatchMapping("/requests/{requestKey}/reject")
    public ResponseEntity<ApiResponse<Void>> rejectRequest(
            @AuthMember String memberKey,
            @PathVariable String requestKey
    ) {
        careInvitationService.reject(requestKey, memberKey);
        return ResponseEntity.ok(ApiResponse.success());
    }

    /**
     * 내 보호 대상자 목록 조회
     */
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

    /**
     * 특정 보호 대상자의 보호자/관리자 목록 조회
     */
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
}
