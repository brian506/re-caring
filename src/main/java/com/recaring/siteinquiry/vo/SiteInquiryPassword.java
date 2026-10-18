package com.recaring.siteinquiry.vo;

import com.recaring.support.exception.AppException;
import com.recaring.support.exception.ErrorType;

import java.util.regex.Pattern;

public record SiteInquiryPassword(String value) {

    private static final Pattern PASSWORD_PATTERN = Pattern.compile("^\\d{4,6}$");

    public SiteInquiryPassword {
        if (value == null || !PASSWORD_PATTERN.matcher(value).matches()) {
            throw new AppException(ErrorType.INVALID_SITE_INQUIRY_PASSWORD);
        }
    }

    @Override
    public String toString() {
        return "SiteInquiryPassword[****]";
    }
}
