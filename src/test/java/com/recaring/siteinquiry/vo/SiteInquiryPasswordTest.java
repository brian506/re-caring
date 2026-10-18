package com.recaring.siteinquiry.vo;

import com.recaring.siteinquiry.fixture.SiteInquiryFixture;
import com.recaring.support.exception.AppException;
import com.recaring.support.exception.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("SiteInquiryPassword 단위 테스트")
class SiteInquiryPasswordTest {

    @ParameterizedTest
    @ValueSource(strings = {SiteInquiryFixture.PASSWORD_4_DIGITS, SiteInquiryFixture.PASSWORD_6_DIGITS})
    @DisplayName("숫자 4자리와 6자리 비밀번호는 허용한다")
    void accepts_four_and_six_digits(String value) {
        assertThatCode(() -> new SiteInquiryPassword(value)).doesNotThrowAnyException();
    }

    @ParameterizedTest
    @ValueSource(strings = {SiteInquiryFixture.PASSWORD_3_DIGITS, SiteInquiryFixture.PASSWORD_7_DIGITS})
    @DisplayName("숫자 3자리와 7자리 비밀번호는 거부한다")
    void rejects_three_and_seven_digits(String value) {
        assertThatThrownBy(() -> new SiteInquiryPassword(value))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.INVALID_SITE_INQUIRY_PASSWORD);
    }

    @Test
    @DisplayName("숫자가 아닌 문자가 섞인 비밀번호는 거부한다")
    void rejects_non_digit_characters() {
        assertThatThrownBy(() -> new SiteInquiryPassword(SiteInquiryFixture.PASSWORD_WITH_LETTER))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.INVALID_SITE_INQUIRY_PASSWORD);
    }
}
