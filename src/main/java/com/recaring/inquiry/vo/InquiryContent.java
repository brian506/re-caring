package com.recaring.inquiry.vo;

import com.recaring.support.exception.AppException;
import com.recaring.support.exception.ErrorType;

public record InquiryContent(String value) {

    private static final int MAX_LENGTH = 1000;

    public InquiryContent {
        if (value == null) {
            throw new AppException(ErrorType.INVALID_INQUIRY_CONTENT);
        }
        value = value.strip();
        if (value.isEmpty() || value.length() > MAX_LENGTH) {
            throw new AppException(ErrorType.INVALID_INQUIRY_CONTENT);
        }
    }
}
