package com.recaring.siteinquiry.controller.request;

import io.swagger.v3.oas.annotations.media.Schema;

public record ViewSiteInquiryRequest(
        @Schema(description = "비밀글 비밀번호. 공개글이면 생략", example = "1234")
        String password
) {
    @Override
    public String toString() {
        return "ViewSiteInquiryRequest[****]";
    }
}
