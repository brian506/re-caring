package com.recaring.siteinquiry.controller;

import com.recaring.siteinquiry.business.SiteInquiryService;
import com.recaring.siteinquiry.controller.request.AnswerSiteInquiryRequest;
import com.recaring.siteinquiry.controller.response.AdminSiteInquiryResponse;
import com.recaring.siteinquiry.dataaccess.entity.SiteInquiryStatus;
import com.recaring.support.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/site-inquiries")
@RequiredArgsConstructor
@Tag(name = "Admin Site Inquiry", description = "Operator website inquiry API (X-Admin-Key header)")
public class AdminSiteInquiryController {

    private final SiteInquiryService siteInquiryService;

    @Operation(
            summary = "[운영] 웹사이트 문의 목록 조회",
            description = """
                    X-Admin-Key 헤더가 필요합니다. 이메일·연락처를 포함합니다.
                    status=WAITING이면 미답변 문의를 오래된 순으로, 생략하면 전체 문의를 최신순으로 반환합니다.
                    """
    )
    @GetMapping
    public ResponseEntity<ApiResponse<List<AdminSiteInquiryResponse>>> getInquiries(
            @Parameter(description = "WAITING 또는 ANSWERED. 생략하면 전체")
            @RequestParam(required = false) SiteInquiryStatus status
    ) {
        List<AdminSiteInquiryResponse> response = siteInquiryService.getInquiries(status).stream()
                .map(AdminSiteInquiryResponse::from)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(
            summary = "[운영] 웹사이트 문의 답변 등록",
            description = "X-Admin-Key 헤더가 필요합니다. 다시 호출하면 답변을 덮어씁니다."
    )
    @PatchMapping("/{inquiryKey}/answer")
    public ResponseEntity<ApiResponse<AdminSiteInquiryResponse>> answer(
            @Parameter(description = "답변할 문의의 inquiryKey")
            @PathVariable String inquiryKey,
            @RequestBody AnswerSiteInquiryRequest request
    ) {
        AdminSiteInquiryResponse response = AdminSiteInquiryResponse.from(
                siteInquiryService.answer(inquiryKey, request.toAnswer()));
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(
            summary = "[운영] 웹사이트 문의 답변 삭제",
            description = "X-Admin-Key 헤더가 필요합니다. 답변을 지우고 status를 WAITING으로 되돌립니다."
    )
    @DeleteMapping("/{inquiryKey}/answer")
    public ResponseEntity<ApiResponse<AdminSiteInquiryResponse>> clearAnswer(
            @Parameter(description = "답변을 삭제할 문의의 inquiryKey")
            @PathVariable String inquiryKey
    ) {
        AdminSiteInquiryResponse response = AdminSiteInquiryResponse.from(siteInquiryService.clearAnswer(inquiryKey));
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(
            summary = "[운영] 웹사이트 문의 삭제",
            description = "X-Admin-Key 헤더가 필요합니다. 문의를 완전히 삭제합니다."
    )
    @DeleteMapping("/{inquiryKey}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @Parameter(description = "삭제할 문의의 inquiryKey")
            @PathVariable String inquiryKey
    ) {
        siteInquiryService.delete(inquiryKey);
        return ResponseEntity.ok(ApiResponse.success());
    }
}
