package com.recaring.siteinquiry.fixture;

import com.recaring.inquiry.fixture.InquiryFixture;
import com.recaring.inquiry.vo.InquiryContent;
import com.recaring.inquiry.vo.InquiryTitle;
import com.recaring.siteinquiry.business.command.SubmitSiteInquiryCommand;
import com.recaring.siteinquiry.dataaccess.entity.SiteInquiry;
import com.recaring.siteinquiry.dataaccess.entity.SiteInquiryCategory;
import com.recaring.siteinquiry.dataaccess.entity.SiteInquiryStatus;
import com.recaring.siteinquiry.vo.SiteInquiryInfo;
import com.recaring.siteinquiry.vo.SiteInquiryPassword;

import java.time.LocalDateTime;

public class SiteInquiryFixture {

    public static final String ADMIN_API_KEY = InquiryFixture.ADMIN_API_KEY;

    public static final String INQUIRY_KEY = "test-site-inquiry-key";
    public static final String UNKNOWN_INQUIRY_KEY = "unknown-site-inquiry-key";

    public static final SiteInquiryCategory CATEGORY = SiteInquiryCategory.FEATURE;
    public static final String TITLE = "보호자를 여러 명 등록할 수 있나요?";
    public static final String OTHER_TITLE = "탈퇴는 어떻게 하나요?";
    public static final String CONTENT = "부모님을 형제와 함께 돌보고 싶어요.";
    public static final String EMAIL = "visitor@example.com";
    public static final String INVALID_EMAIL = "not-an-email";
    public static final String PHONE = "010-1234-5678";
    public static final String ANSWER = InquiryFixture.ANSWER;
    public static final LocalDateTime ANSWERED_AT = InquiryFixture.ANSWERED_AT;
    public static final LocalDateTime SUBMITTED_AT = InquiryFixture.SUBMITTED_AT;

    public static final String TITLE_51_CHARS = InquiryFixture.TITLE_51_CHARS;
    public static final String ANSWER_2001_CHARS = InquiryFixture.ANSWER_2001_CHARS;

    public static final String PASSWORD = "1234";
    public static final String WRONG_PASSWORD = "9999";
    public static final String PASSWORD_HASH = "$2a$10$encodedPasswordHashForTest";
    public static final String PASSWORD_4_DIGITS = "1234";
    public static final String PASSWORD_6_DIGITS = "123456";
    public static final String PASSWORD_3_DIGITS = "123";
    public static final String PASSWORD_7_DIGITS = "1234567";
    public static final String PASSWORD_WITH_LETTER = "12a4";

    public static final String CLIENT_IP = "203.0.113.10";
    public static final String OTHER_CLIENT_IP = "203.0.113.20";

    public static SiteInquiry createPublicInquiry(String title) {
        return SiteInquiry.builder()
                .category(CATEGORY)
                .title(title)
                .content(CONTENT)
                .email(EMAIL)
                .phone(PHONE)
                .secret(false)
                .build();
    }

    public static SiteInquiry createSecretInquiry(String passwordHash) {
        return SiteInquiry.builder()
                .category(CATEGORY)
                .title(TITLE)
                .content(CONTENT)
                .email(EMAIL)
                .phone(PHONE)
                .secret(true)
                .passwordHash(passwordHash)
                .build();
    }

    public static SiteInquiry createAnsweredInquiry(String title) {
        SiteInquiry inquiry = createPublicInquiry(title);
        inquiry.answer(ANSWER, ANSWERED_AT);
        return inquiry;
    }

    public static SiteInquiryPassword createPassword() {
        return new SiteInquiryPassword(PASSWORD);
    }

    public static SubmitSiteInquiryCommand createSecretSubmitCommand() {
        return new SubmitSiteInquiryCommand(CATEGORY, new InquiryTitle(TITLE), new InquiryContent(CONTENT),
                EMAIL, PHONE, createPassword());
    }

    public static SiteInquiryInfo createSubmittedInfo() {
        return new SiteInquiryInfo(INQUIRY_KEY, CATEGORY, TITLE, CONTENT, EMAIL, PHONE, true,
                SiteInquiryStatus.WAITING, SUBMITTED_AT, null, null);
    }
}
