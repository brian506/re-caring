package com.recaring.auth.vo;

import java.util.regex.Pattern;

public record LocalEmail(String email) {
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");


    public LocalEmail {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("이메일은 필수 입력값입니다.");
        }
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            throw new IllegalArgumentException("올바른 이메일 형식이 아닙니다.");
        }
    }
}
