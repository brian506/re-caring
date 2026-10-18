package com.recaring.inquiry.fixture;

import com.recaring.inquiry.dataaccess.entity.Inquiry;
import com.recaring.inquiry.dataaccess.entity.InquiryCategory;
import com.recaring.inquiry.dataaccess.entity.InquiryStatus;
import com.recaring.inquiry.event.InquiryAnsweredEvent;
import com.recaring.inquiry.vo.InquiryInfo;

import java.time.LocalDateTime;

public class InquiryFixture {

    public static final String ADMIN_API_KEY = "test-admin-api-key";
    public static final String WRONG_ADMIN_API_KEY = "wrong-admin-api-key";

    public static final String INQUIRY_KEY = "test-inquiry-key";
    public static final String MEMBER_KEY = "inquiry-member-key";
    public static final String UNKNOWN_INQUIRY_KEY = "unknown-inquiry-key";

    public static final InquiryCategory CATEGORY = InquiryCategory.ALERT_EMERGENCY;
    public static final String TITLE = "알림이 안 와요";
    public static final String OTHER_TITLE = "안심존이 이상해요";
    public static final String CONTENT = "어제부터 안심존 이탈 알림이 오지 않아요.";
    public static final String ANSWER = "알림 설정을 다시 켜 주세요.";
    public static final String UPDATED_ANSWER = "앱을 최신 버전으로 업데이트해 주세요.";
    public static final LocalDateTime ANSWERED_AT = LocalDateTime.of(2026, 1, 1, 9, 0);

    public static final String TITLE_50_CHARS = "가".repeat(50);
    public static final String TITLE_51_CHARS = "가".repeat(51);
    public static final String CONTENT_1000_CHARS = "나".repeat(1000);
    public static final String CONTENT_1001_CHARS = "나".repeat(1001);
    public static final String ANSWER_2000_CHARS = "다".repeat(2000);
    public static final String ANSWER_2001_CHARS = "다".repeat(2001);

    public static final String SLACK_WEBHOOK_URL = "https://hooks.slack.com/services/T000/B000/test";
    public static final LocalDateTime SUBMITTED_AT = LocalDateTime.of(2026, 10, 17, 8, 24, 31);
    public static final String SUBMITTED_AT_TEXT = "2026-10-17 08:24";
    public static final String CATEGORY_DESCRIPTION = "알림·응급";
    public static final String MENTION_TITLE = "<!channel> 급해요 & 확인 부탁";
    public static final String ESCAPED_MENTION_TITLE = "&lt;!channel&gt; 급해요 &amp; 확인 부탁";

    public static Inquiry createInquiry(String memberKey) {
        return createInquiry(memberKey, TITLE);
    }

    public static Inquiry createInquiry(String memberKey, String title) {
        return Inquiry.builder()
                .memberKey(memberKey)
                .category(CATEGORY)
                .title(title)
                .content(CONTENT)
                .build();
    }

    public static Inquiry createAnsweredInquiry(String memberKey, String title) {
        Inquiry inquiry = createInquiry(memberKey, title);
        inquiry.answer(ANSWER, ANSWERED_AT);
        return inquiry;
    }

    public static InquiryInfo createSubmittedInfo(String title) {
        return new InquiryInfo(INQUIRY_KEY, MEMBER_KEY, CATEGORY, title, CONTENT,
                InquiryStatus.WAITING, SUBMITTED_AT, null, null);
    }

    public static InquiryAnsweredEvent createAnsweredEvent() {
        return new InquiryAnsweredEvent(INQUIRY_KEY, MEMBER_KEY, TITLE);
    }
}
