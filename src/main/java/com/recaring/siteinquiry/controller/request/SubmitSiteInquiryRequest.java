package com.recaring.siteinquiry.controller.request;

import com.recaring.inquiry.vo.InquiryContent;
import com.recaring.inquiry.vo.InquiryTitle;
import com.recaring.siteinquiry.business.command.SubmitSiteInquiryCommand;
import com.recaring.siteinquiry.dataaccess.entity.SiteInquiryCategory;
import com.recaring.siteinquiry.vo.SiteInquiryPassword;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SubmitSiteInquiryRequest(
        @Schema(description = "문의 유형", example = "FEATURE")
        @NotNull SiteInquiryCategory category,
        @Schema(description = "제목 (앞뒤 공백 제외 1~50자)", example = "보호자를 여러 명 등록할 수 있나요?")
        String title,
        @Schema(description = "내용 (앞뒤 공백 제외 1~1000자)", example = "부모님을 형제와 함께 돌보고 싶어요.")
        String content,
        @Schema(description = "답변 받을 이메일", example = "user@example.com")
        @NotBlank @Email @Size(max = 100) String email,
        @Schema(description = "연락처 (선택, 숫자와 하이픈 최대 13자)", example = "010-1234-5678")
        @Pattern(regexp = "^[0-9-]{0,13}$") String phone,
        @Schema(description = "비밀글 여부")
        Boolean isSecret,
        @Schema(description = "비밀글 비밀번호 (비밀글이면 필수, 숫자 4~6자리)", example = "1234")
        String password
) {
    public SubmitSiteInquiryCommand toCommand() {
        return new SubmitSiteInquiryCommand(
                category,
                new InquiryTitle(title),
                new InquiryContent(content),
                email.strip(),
                phone == null || phone.isBlank() ? null : phone,
                Boolean.TRUE.equals(isSecret) ? new SiteInquiryPassword(password) : null
        );
    }

    @Override
    public String toString() {
        return "SubmitSiteInquiryRequest[category=" + category + ", isSecret=" + isSecret + "]";
    }
}
