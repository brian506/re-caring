package com.recaring.care.event;

import com.recaring.care.dataaccess.entity.CarePartyRole;

public record CareInvitationAcceptedEvent(String requestKey, String acceptorMemberKey, String requesterMemberKey, CarePartyRole targetRole) {}
