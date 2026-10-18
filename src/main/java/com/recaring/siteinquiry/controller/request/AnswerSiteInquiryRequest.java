package com.recaring.siteinquiry.controller.request;

import com.recaring.inquiry.vo.InquiryAnswer;
import io.swagger.v3.oas.annotations.media.Schema;

public record AnswerSiteInquiryRequest(
        @Schema(description = "답변 (앞뒤 공백 제외 1~2000자). 다시 등록하면 덮어씁니다.")
        String answer
) {
    public InquiryAnswer toAnswer() {
        return new InquiryAnswer(answer);
    }
}
