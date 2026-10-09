package com.recaring.inquiry.controller.request;

import com.recaring.inquiry.business.command.SubmitInquiryCommand;
import com.recaring.inquiry.dataaccess.entity.InquiryCategory;
import com.recaring.inquiry.vo.InquiryContent;
import com.recaring.inquiry.vo.InquiryTitle;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record SubmitInquiryRequest(
        @Schema(description = "문의 유형", example = "ALERT_EMERGENCY")
        @NotNull InquiryCategory category,
        @Schema(description = "제목 (앞뒤 공백 제외 1~50자)", example = "알림이 안 와요")
        String title,
        @Schema(description = "내용 (앞뒤 공백 제외 1~1000자)", example = "어제부터 안심존 이탈 알림이 오지 않아요.")
        String content
) {
    public SubmitInquiryCommand toCommand() {
        return new SubmitInquiryCommand(category, new InquiryTitle(title), new InquiryContent(content));
    }
}
