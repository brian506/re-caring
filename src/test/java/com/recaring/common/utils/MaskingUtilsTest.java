package com.recaring.common.utils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.recaring.common.fixture.CommonFixture.EXTERNAL_ERROR_BODY;
import static com.recaring.common.fixture.CommonFixture.MASKED_EXTERNAL_ERROR_BODY;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("외부 API 응답 로그 마스킹")
class MaskingUtilsTest {

    @Test
    @DisplayName("외부 응답에 담긴 전화번호와 토큰은 가리고 나머지 진단 정보는 남긴다")
    void masks_phone_and_token_in_external_text() {
        assertThat(MaskingUtils.maskExternalText(EXTERNAL_ERROR_BODY)).isEqualTo(MASKED_EXTERNAL_ERROR_BODY);
    }
}
