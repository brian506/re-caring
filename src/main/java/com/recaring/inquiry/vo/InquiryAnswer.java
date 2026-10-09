package com.recaring.inquiry.vo;

import com.recaring.support.exception.AppException;
import com.recaring.support.exception.ErrorType;

public record InquiryAnswer(String value) {

    private static final int MAX_LENGTH = 2000;

    public InquiryAnswer {
        if (value == null) {
            throw new AppException(ErrorType.INVALID_INQUIRY_ANSWER);
        }
        value = value.strip();
        if (value.isEmpty() || value.length() > MAX_LENGTH) {
            throw new AppException(ErrorType.INVALID_INQUIRY_ANSWER);
        }
    }
}
