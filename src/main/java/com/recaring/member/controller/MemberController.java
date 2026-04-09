package com.recaring.member.controller;

import com.recaring.member.business.MemberService;
import com.recaring.member.controller.request.SearchByPhonesRequest;
import com.recaring.member.controller.response.ContactMemberResponse;
import com.recaring.support.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
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
}
