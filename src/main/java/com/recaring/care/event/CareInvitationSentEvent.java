package com.recaring.care.event;

public record CareInvitationSentEvent(String requestKey, String targetMemberKey, String requesterMemberKey) {}
