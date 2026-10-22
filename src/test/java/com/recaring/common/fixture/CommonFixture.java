package com.recaring.common.fixture;

public class CommonFixture {

    public static final String REQUEST_ID_HEADER = "X-Request-Id";
    public static final String UUID_PATTERN = "^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$";

    public static final String CLIENT_REQUEST_ID = "app-bug-report-7f3a9c";
    public static final String REQUEST_ID_8_CHARS = "abcd-123";
    public static final String REQUEST_ID_7_CHARS = "abcd-12";
    public static final String REQUEST_ID_64_CHARS = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";
    public static final String REQUEST_ID_65_CHARS = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef0";

    public static final String APP_EXCEPTION_THROWER = "com.recaring.care.implement.CareInvitationManager";
    public static final String APP_EXCEPTION_DOMAIN = "care";
    public static final String ROOT_CAUSE_MESSAGE = "redis connection refused";

    public static final String EXTERNAL_ERROR_BODY =
            "{\"error\":\"invalid recipient 010-1234-5678\",\"token\":\"dGVzdC1mY20tdG9rZW46QVBBOTFiSGFiY2RlZmdoaWprbG1ub3A\"}";
    public static final String MASKED_EXTERNAL_ERROR_BODY =
            "{\"error\":\"invalid recipient 010****5678\",\"token\":\"[REDACTED]\"}";

    public static StackTraceElement[] appExceptionStackTrace() {
        return new StackTraceElement[]{
                new StackTraceElement(APP_EXCEPTION_THROWER, "accept", "CareInvitationManager.java", 42)
        };
    }
}
