package com.recaring.place.vo;

import com.recaring.support.exception.AppException;
import com.recaring.support.exception.ErrorType;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public record PlaceKeyword(String value) {

    private static final int MAX_LENGTH = 100;
    private static final int MIN_TOKEN_LENGTH = 2;
    private static final String WHITESPACE = "\\s+";

    public PlaceKeyword {
        if (value == null || value.isBlank()) {
            throw new AppException(ErrorType.INVALID_PLACE_QUERY);
        }
        value = value.trim();
        if (value.length() > MAX_LENGTH) {
            throw new AppException(ErrorType.INVALID_PLACE_QUERY);
        }
    }

    public boolean matches(String placeName) {
        if (placeName == null || placeName.isBlank()) {
            return false;
        }
        String target = normalize(placeName);
        return tokens().stream().anyMatch(target::contains);
    }

    private List<String> tokens() {
        List<String> tokens = Arrays.stream(value.split(WHITESPACE))
                .map(PlaceKeyword::normalize)
                .filter(token -> token.length() >= MIN_TOKEN_LENGTH)
                .toList();

        return tokens.isEmpty() ? List.of(normalize(value)) : tokens;
    }

    private static String normalize(String text) {
        return text.replaceAll(WHITESPACE, "").toLowerCase(Locale.ROOT);
    }
}
