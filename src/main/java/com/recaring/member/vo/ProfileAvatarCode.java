package com.recaring.member.vo;

import com.recaring.support.exception.AppException;
import com.recaring.support.exception.ErrorType;

import java.util.Set;

/**
 * 앱이 번들로 가진 기본 프로필 일러스트 식별자. 서버는 이미지가 아니라 이 코드만 저장한다.
 * value가 null이면 "직접 고른 얼굴 없음"이며, 앱이 성별·memberKey 기준으로 자동 배정한다.
 */
public record ProfileAvatarCode(String value) {

    private static final Set<String> ALLOWED_CODES = Set.of(
            "senior_female_1", "senior_female_2", "senior_female_3", "senior_female_4",
            "senior_male_1", "senior_male_2", "senior_male_3", "senior_male_4",
            "adult_female_1", "adult_female_2", "adult_female_3", "adult_female_4",
            "adult_male_1", "adult_male_2", "adult_male_3", "adult_male_4"
    );

    public ProfileAvatarCode {
        if (value != null && !ALLOWED_CODES.contains(value)) {
            throw new AppException(ErrorType.INVALID_PROFILE_AVATAR_CODE);
        }
    }

    /**
     * 빈 문자열은 자동 배정으로 되돌리기다. 앱의 JSON 직렬화가 null 필드를 생략해 null로는 해제 신호를 보낼 수 없다.
     */
    public static ProfileAvatarCode from(String raw) {
        if (raw == null || raw.isBlank()) {
            return new ProfileAvatarCode(null);
        }
        return new ProfileAvatarCode(raw.trim());
    }

    public boolean isCleared() {
        return value == null;
    }
}
