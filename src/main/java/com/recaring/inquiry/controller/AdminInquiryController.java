package com.recaring.inquiry.controller;

import com.recaring.inquiry.business.InquiryService;
import com.recaring.inquiry.controller.request.AnswerInquiryRequest;
import com.recaring.inquiry.controller.response.AdminInquiryResponse;
import com.recaring.inquiry.dataaccess.entity.InquiryStatus;
import com.recaring.support.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/inquiries")
@RequiredArgsConstructor
@Tag(name = "Admin Inquiry", description = "Operator inquiry API (X-Admin-Key header)")
public class AdminInquiryController {

    private final InquiryService inquiryService;

    @Operation(
            summary = "[운영] 문의 목록 조회",
            description = """
                    X-Admin-Key 헤더가 필요합니다.
                    status=WAITING이면 미답변 문의를 오래된 순으로, 생략하면 전체 문의를 최신순으로 반환합니다.
                    """
    )
    @GetMapping
    public ResponseEntity<ApiResponse<List<AdminInquiryResponse>>> getInquiries(
            @Parameter(description = "WAITING 또는 ANSWERED. 생략하면 전체")
            @RequestParam(required = false) InquiryStatus status
    ) {
        List<AdminInquiryResponse> response = inquiryService.getInquiries(status).stream()
                .map(AdminInquiryResponse::from)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(
            summary = "[운영] 문의 답변 등록",
            description = """
                    X-Admin-Key 헤더가 필요합니다.
                    답변을 등록하면 status가 ANSWERED가 되고 answeredAt이 기록되며, 작성자에게 INQUIRY_ANSWERED 푸시가 갑니다.
                    다시 호출하면 답변을 덮어쓰고 푸시도 다시 보냅니다.
                    """
    )
    @PatchMapping("/{inquiryKey}/answer")
    public ResponseEntity<ApiResponse<AdminInquiryResponse>> answer(
            @Parameter(description = "답변할 문의의 inquiryKey")
            @PathVariable String inquiryKey,
            @RequestBody AnswerInquiryRequest request
    ) {
        AdminInquiryResponse response = AdminInquiryResponse.from(inquiryService.answer(inquiryKey, request.toAnswer()));
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
