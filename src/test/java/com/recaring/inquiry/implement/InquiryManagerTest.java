package com.recaring.inquiry.implement;

import com.recaring.inquiry.dataaccess.repository.InquiryRepository;
import com.recaring.inquiry.event.InquiryAnsweredEvent;
import com.recaring.inquiry.fixture.InquiryFixture;
import com.recaring.inquiry.vo.InquiryAnswer;
import com.recaring.support.exception.AppException;
import com.recaring.support.exception.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.never;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("문의 매니저 단위 테스트")
class InquiryManagerTest {

    @InjectMocks
    private InquiryManager inquiryManager;

    @Mock
    private InquiryRepository inquiryRepository;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Test
    @DisplayName("답변을 등록하면 작성자와 문의 제목을 담아 답변 알림 이벤트를 발행한다")
    void publishes_answered_event_with_author_and_title() {
        var inquiry = InquiryFixture.createInquiry(InquiryFixture.MEMBER_KEY);
        given(inquiryRepository.findByInquiryKey(inquiry.getInquiryKey())).willReturn(Optional.of(inquiry));

        inquiryManager.answer(inquiry.getInquiryKey(), new InquiryAnswer(InquiryFixture.ANSWER));

        then(eventPublisher).should().publishEvent(
                new InquiryAnsweredEvent(inquiry.getInquiryKey(), InquiryFixture.MEMBER_KEY, InquiryFixture.TITLE));
    }

    @Test
    @DisplayName("없는 문의에 답변하면 예외가 나고 알림 이벤트를 발행하지 않는다")
    void does_not_publish_event_when_inquiry_missing() {
        given(inquiryRepository.findByInquiryKey(InquiryFixture.UNKNOWN_INQUIRY_KEY)).willReturn(Optional.empty());

        assertThatThrownBy(() -> inquiryManager.answer(
                InquiryFixture.UNKNOWN_INQUIRY_KEY, new InquiryAnswer(InquiryFixture.ANSWER)))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.INQUIRY_NOT_FOUND);

        then(eventPublisher).should(never()).publishEvent(any(Object.class));
    }
}
