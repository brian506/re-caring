package com.recaring.place.vo;

import com.recaring.support.exception.AppException;
import com.recaring.support.exception.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("PlaceKeyword 단위 테스트")
class PlaceKeywordTest {

    private static final int MAX_LENGTH = 100;

    @Test
    @DisplayName("검색어가 장소 이름에 들어 있으면 검색어에 걸린 결과로 본다")
    void matches_when_place_name_contains_keyword() {
        PlaceKeyword keyword = new PlaceKeyword("망원역");

        assertThat(keyword.matches("망원역 6호선")).isTrue();
    }

    @Test
    @DisplayName("공백과 대소문자 차이는 무시하고 판정한다")
    void matches_ignoring_whitespace_and_case() {
        PlaceKeyword keyword = new PlaceKeyword("star bucks");

        assertThat(keyword.matches("STARBUCKS 합정점")).isTrue();
    }

    @Test
    @DisplayName("여러 단어로 검색하면 한 단어라도 이름에 걸릴 때 검색어에 걸린 결과로 본다")
    void matches_when_any_token_is_contained() {
        PlaceKeyword keyword = new PlaceKeyword("강남역 스타벅스");

        assertThat(keyword.matches("스타벅스 강남GT타워점")).isTrue();
    }

    @Test
    @DisplayName("검색어와 무관한 이름은 검색어에 걸리지 않은 결과로 본다")
    void does_not_match_unrelated_place_name() {
        PlaceKeyword keyword = new PlaceKeyword("해운대역");

        assertThat(keyword.matches("해운대연탄생갈비 부평갈산역점")).isFalse();
    }

    @Test
    @DisplayName("한 글자 검색어도 이름 포함 여부로 판정한다")
    void matches_single_character_keyword() {
        PlaceKeyword keyword = new PlaceKeyword("봄");

        assertThat(keyword.matches("봄카페 망원점")).isTrue();
    }

    @Test
    @DisplayName("검색어는 100자까지 허용한다")
    void allows_keyword_of_max_length() {
        String keyword = "가".repeat(MAX_LENGTH);

        assertThatCode(() -> new PlaceKeyword(keyword)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("검색어가 100자를 넘으면 INVALID_PLACE_QUERY 예외가 발생한다")
    void throws_when_keyword_exceeds_max_length() {
        String keyword = "가".repeat(MAX_LENGTH + 1);

        assertThatThrownBy(() -> new PlaceKeyword(keyword))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.INVALID_PLACE_QUERY);
    }
}
