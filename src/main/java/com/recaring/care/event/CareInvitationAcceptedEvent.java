package com.recaring.care.event;

import com.recaring.care.vo.CarePartyRole;

public record CareInvitationAcceptedEvent(String requestKey, String acceptorMemberKey, String requesterMemberKey, CarePartyRole targetRole) {}
