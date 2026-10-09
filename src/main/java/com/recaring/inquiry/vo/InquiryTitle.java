package com.recaring.inquiry.vo;

import com.recaring.support.exception.AppException;
import com.recaring.support.exception.ErrorType;

public record InquiryTitle(String value) {

    private static final int MAX_LENGTH = 50;

    public InquiryTitle {
        if (value == null) {
            throw new AppException(ErrorType.INVALID_INQUIRY_TITLE);
        }
        value = value.strip();
        if (value.isEmpty() || value.length() > MAX_LENGTH) {
            throw new AppException(ErrorType.INVALID_INQUIRY_TITLE);
        }
    }
}
