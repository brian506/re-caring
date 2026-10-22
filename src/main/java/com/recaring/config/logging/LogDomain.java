package com.recaring.config.logging;

import java.util.Set;

public final class LogDomain {

    public static final String FIELD = "domain";

    private static final String APP_PACKAGE_PREFIX = "com.recaring.";
    private static final Set<String> SHARED_PACKAGES = Set.of("common", "support", "config");

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

    public static String resolveOrigin(StackTraceElement[] stackTrace) {
        if (stackTrace == null) {
            return null;
        }
        for (StackTraceElement frame : stackTrace) {
            String className = frame.getClassName();
            if (!className.startsWith(APP_PACKAGE_PREFIX)) {
                continue;
            }
            String domain = segment(className, APP_PACKAGE_PREFIX.length());
            if (domain != null && !SHARED_PACKAGES.contains(domain)) {
                return domain;
            }
        }
        return null;
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
