package com.recaring.care.event;

import com.recaring.care.vo.CarePartyRole;

public record CareInvitationSentEvent(String requestKey, String targetMemberKey, String requesterMemberKey, CarePartyRole targetRole) {}
