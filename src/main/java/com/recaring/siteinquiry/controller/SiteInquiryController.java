package com.recaring.siteinquiry.controller;

import com.recaring.common.utils.NetworkUtils;
import com.recaring.siteinquiry.business.SiteInquiryService;
import com.recaring.siteinquiry.controller.request.SubmitSiteInquiryRequest;
import com.recaring.siteinquiry.controller.request.ViewSiteInquiryRequest;
import com.recaring.siteinquiry.controller.response.SiteInquiryDetailResponse;
import com.recaring.siteinquiry.controller.response.SiteInquirySummaryResponse;
import com.recaring.support.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/site/inquiries")
@RequiredArgsConstructor
@Tag(name = "Site Inquiry", description = "Website inquiry board API (no login)")
public class SiteInquiryController {

    private final SiteInquiryService siteInquiryService;

    @Operation(
            summary = "웹사이트 문의 등록",
            description = """
                    로그인 없이 문의를 등록합니다.
                    category는 FEATURE, USAGE, ACCOUNT_DELETION, OTHER 중 하나입니다.
                    title은 앞뒤 공백 제외 1~50자(E11000), content는 1~1000자(E11001)입니다.
                    isSecret=true면 password(숫자 4~6자리, E11004)가 필요합니다.
                    같은 IP에서 10분에 5건까지 등록할 수 있습니다(E11007).
                    """
    )
    @PostMapping
    public ResponseEntity<ApiResponse<SiteInquirySummaryResponse>> submit(
            @Valid @RequestBody SubmitSiteInquiryRequest request,
            @Parameter(hidden = true) HttpServletRequest httpRequest
    ) {
        SiteInquirySummaryResponse response = SiteInquirySummaryResponse.from(
                siteInquiryService.submit(request.toCommand(), NetworkUtils.getProxiedClientIp(httpRequest)));
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @Operation(
            summary = "웹사이트 문의 목록 조회",
            description = "전체 문의를 최신순으로 반환합니다. 내용·연락처는 포함하지 않습니다."
    )
    @GetMapping
    public ResponseEntity<ApiResponse<List<SiteInquirySummaryResponse>>> getInquiries() {
        List<SiteInquirySummaryResponse> response = siteInquiryService.getInquiries(null).stream()
                .map(SiteInquirySummaryResponse::from)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(
            summary = "웹사이트 문의 상세 조회",
            description = """
                    문의 내용과 답변을 반환합니다. 비밀글이면 password가 일치해야 합니다(E11005).
                    비밀번호를 10분 안에 5번 틀리면 해당 문의 조회가 잠깁니다(E11008).
                    """
    )
    @PostMapping("/{inquiryKey}/view")
    public ResponseEntity<ApiResponse<SiteInquiryDetailResponse>> view(
            @Parameter(description = "조회할 문의의 inquiryKey")
            @PathVariable String inquiryKey,
            @RequestBody(required = false) ViewSiteInquiryRequest request
    ) {
        String password = request == null ? null : request.password();
        SiteInquiryDetailResponse response = SiteInquiryDetailResponse.from(siteInquiryService.view(inquiryKey, password));
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
