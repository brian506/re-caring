package com.recaring.common.fixture;

public class CommonFixture {

    public static final String REQUEST_ID_HEADER = "X-Request-Id";
    public static final String UUID_PATTERN = "^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$";

    public static final String CLIENT_REQUEST_ID = "app-bug-report-7f3a9c";
    public static final String REQUEST_ID_8_CHARS = "abcd-123";
    public static final String REQUEST_ID_7_CHARS = "abcd-12";
    public static final String REQUEST_ID_64_CHARS = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";
    public static final String REQUEST_ID_65_CHARS = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef0";
}
