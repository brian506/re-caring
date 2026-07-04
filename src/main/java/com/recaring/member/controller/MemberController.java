package com.recaring.member.controller;

import com.recaring.auth.vo.Password;
import com.recaring.member.business.MemberService;
import com.recaring.member.controller.request.SearchByPhonesRequest;
import com.recaring.member.controller.request.UpdateMyInfoRequest;
import com.recaring.member.controller.request.WithdrawRequest;
import com.recaring.member.controller.response.ContactMemberResponse;
import com.recaring.member.controller.response.MyInfoResponse;
import com.recaring.security.vo.AuthMember;
import com.recaring.support.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/members")
@RequiredArgsConstructor
@Tag(name = "Member", description = "회원 조회 API")
public class MemberController {

    private final MemberService memberService;

    @Operation(
            summary = "연락처 기반 가입 회원 조회",
            description = """
                    클라이언트 기기의 연락처 전화번호 목록을 전송하면, 해당 번호 중 re;caRing에 가입된 회원만 필터링해 반환합니다.
                    보호 대상자 추가 시 연락처에서 가입 회원을 찾는 용도로 사용합니다. [GUARDIAN 전용]
                    """
    )
    @PostMapping("/phones")
    public ResponseEntity<ApiResponse<List<ContactMemberResponse>>> searchByPhones(
            @Valid @RequestBody SearchByPhonesRequest request
    ) {
        List<ContactMemberResponse> responses = memberService.findByPhoneNumbers(request.phones());
        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    @Operation(
            summary = "내 정보 조회",
            description = """
                    JWT 인증 기반으로 본인의 회원 정보를 조회합니다.
                    회원 기본 정보, 이메일, 약관 동의 시각을 반환합니다.
                    """
    )
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<MyInfoResponse>> getMyInfo(@AuthMember String memberKey) {
        MyInfoResponse response = memberService.getMyInfo(memberKey);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(
            summary = "내 정보 수정",
            description = """
                    JWT 인증 기반으로 본인의 이름·생년월일·비밀번호를 부분 수정합니다.
                    요청에 포함된 필드만 반영하며, null 또는 빈 값은 무시합니다.
                    비밀번호 변경 시 현재 비밀번호(currentPassword) 확인이 필요합니다.
                    """
    )
    @PatchMapping("/me")
    public ResponseEntity<ApiResponse<Void>> updateMyInfo(
            @AuthMember String memberKey,
            @Valid @RequestBody UpdateMyInfoRequest request
    ) {
        memberService.updateMyInfo(memberKey, request.name(), request.birth(), request.currentPassword(), request.newPassword());
        return ResponseEntity.ok(ApiResponse.success());
    }

    @Operation(
            summary = "회원 탈퇴",
            description = """
                    비밀번호 확인 후 본인 계정 및 연관된 모든 데이터를 삭제합니다.
                    """
    )
    @DeleteMapping("/me")
    public ResponseEntity<ApiResponse<Void>> withdraw(
            @AuthMember String memberKey,
            @Valid @RequestBody WithdrawRequest request
    ) {
        memberService.withdraw(memberKey, new Password(request.password()));
        return ResponseEntity.ok(ApiResponse.success());
    }
}
