package com.recaring.inquiry.event;

public record InquiryAnsweredEvent(String inquiryKey, String memberKey, String title) {}
