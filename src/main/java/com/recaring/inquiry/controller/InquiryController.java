package com.recaring.inquiry.controller;

import com.recaring.inquiry.business.InquiryService;
import com.recaring.inquiry.controller.request.SubmitInquiryRequest;
import com.recaring.inquiry.controller.response.InquiryResponse;
import com.recaring.security.vo.AuthMember;
import com.recaring.support.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/inquiries")
@RequiredArgsConstructor
@Tag(name = "Inquiry", description = "1:1 inquiry API")
public class InquiryController {

    private final InquiryService inquiryService;

    @Operation(
            summary = "1:1 문의 접수",
            description = """
                    로그인한 회원 명의로 문의를 접수합니다. 보호자·대상자 모두 가능합니다.
                    category는 LOCATION_SAFE_ZONE, ALERT_EMERGENCY, CONNECTION_ACCOUNT, ERROR_OTHER 중 하나입니다.
                    title은 앞뒤 공백을 제외하고 1~50자(E11000), content는 1~1000자(E11001)여야 합니다.
                    """
    )
    @PostMapping
    public ResponseEntity<ApiResponse<InquiryResponse>> submit(
            @Parameter(hidden = true)
            @AuthMember String memberKey,
            @Valid @RequestBody SubmitInquiryRequest request
    ) {
        InquiryResponse response = InquiryResponse.from(inquiryService.submit(memberKey, request.toCommand()));
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @Operation(
            summary = "내 문의 목록 조회",
            description = """
                    내가 보낸 문의 전체를 최신순으로 반환합니다. 답변이 있으면 status가 ANSWERED이고
                    answer·answeredAt이 채워집니다. 답변 전에는 status가 WAITING이고 두 필드는 null입니다.
                    """
    )
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<List<InquiryResponse>>> getMyInquiries(
            @Parameter(hidden = true)
            @AuthMember String memberKey
    ) {
        List<InquiryResponse> response = inquiryService.getMyInquiries(memberKey).stream()
                .map(InquiryResponse::from)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
