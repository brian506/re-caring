package com.recaring.config.logging;

public final class LogDomain {

    private static final String APP_PACKAGE_PREFIX = "com.recaring.";

    private LogDomain() {
    }

    public static String resolve(String loggerName) {
        if (loggerName == null || loggerName.isEmpty()) {
            return null;
        }
        if (loggerName.startsWith(APP_PACKAGE_PREFIX)) {
            return segment(loggerName, APP_PACKAGE_PREFIX.length());
        }
        int firstDot = loggerName.indexOf('.');
        if (firstDot < 0) {
            return null;
        }
        String library = segment(loggerName, firstDot + 1);
        return "springframework".equals(library) ? "spring" : library;
    }

    private static String segment(String loggerName, int start) {
        int end = loggerName.indexOf('.', start);
        if (end < 0) {
            return null;
        }
        String segment = loggerName.substring(start, end);
        return segment.isEmpty() ? null : segment;
    }
}
