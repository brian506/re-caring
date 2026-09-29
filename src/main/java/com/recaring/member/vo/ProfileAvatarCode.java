package com.recaring.member.vo;

import com.recaring.member.dataaccess.entity.Gender;
import com.recaring.member.dataaccess.entity.MemberRole;
import com.recaring.support.exception.AppException;
import com.recaring.support.exception.ErrorType;

import java.util.Set;

/**
 * 앱이 번들로 가진 기본 프로필 일러스트 식별자. 서버는 이미지가 아니라 이 코드만 저장한다.
 * value가 null이면 "기본 코드로 되돌리기" 요청이며, 저장 시 역할·성별·memberKey 기준 기본 코드로 바뀐다.
 */
public record ProfileAvatarCode(String value) {

    private static final int VARIANT_COUNT = 4;

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
     * 빈 문자열은 기본 코드로 되돌리기다. 앱의 JSON 직렬화가 null 필드를 생략해 null로는 해제 신호를 보낼 수 없다.
     */
    public static ProfileAvatarCode from(String raw) {
        if (raw == null || raw.isBlank()) {
            return new ProfileAvatarCode(null);
        }
        return new ProfileAvatarCode(raw.trim());
    }

    public static ProfileAvatarCode defaultFor(String memberKey, MemberRole role, Gender gender) {
        String age = role == MemberRole.WARD ? "senior" : "adult";
        String sex = gender == Gender.FEMALE ? "female" : "male";
        int variant = Math.floorMod(memberKey.hashCode(), VARIANT_COUNT) + 1;
        return new ProfileAvatarCode(age + "_" + sex + "_" + variant);
    }

    public boolean isCleared() {
        return value == null;
    }
}
