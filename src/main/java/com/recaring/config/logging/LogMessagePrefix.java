package com.recaring.config.logging;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public record LogMessagePrefix(String category, String detail) {

    private static final Pattern PREFIX_PATTERN = Pattern.compile("^\\[([^\\]:]+?)(?:\\s*:\\s*([^\\]]+?))?\\s*\\]");

    public static LogMessagePrefix parse(String message) {
        if (message == null || message.isEmpty() || message.charAt(0) != '[') {
            return null;
        }
        Matcher matcher = PREFIX_PATTERN.matcher(message);
        if (!matcher.lookingAt()) {
            return null;
        }
        String category = matcher.group(1).trim();
        if (category.isEmpty()) {
            return null;
        }
        String detail = matcher.group(2) == null ? null : matcher.group(2).trim();
        return new LogMessagePrefix(category, detail);
    }
}
