package com.recaring.member.controller;

import com.recaring.member.controller.request.SearchByPhonesRequest;
import com.recaring.member.controller.response.ContactMemberResponse;
import com.recaring.member.implement.MemberReader;
import com.recaring.support.response.ApiResponse;
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
public class MemberController {

    private final MemberReader memberReader;

    /**
     * 연락처 기반 가입 회원 조회
     * 클라이언트에서 기기 연락처 전화번호 목록을 보내면, 가입된 회원만 필터링해서 반환
     */
    @PostMapping("/phones")
    public ResponseEntity<ApiResponse<List<ContactMemberResponse>>> searchByPhones(
            @Valid @RequestBody SearchByPhonesRequest request
    ) {
        List<ContactMemberResponse> responses = memberReader.findAllByPhones(request.phones())
                .stream()
                .map(ContactMemberResponse::from)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(responses));
    }
}
