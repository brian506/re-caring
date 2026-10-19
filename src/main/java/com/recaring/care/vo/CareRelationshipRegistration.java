package com.recaring.care.vo;


public record CareRelationshipRegistration(
        String wardMemberKey,
        String caregiverKey,
        CareRole careRole
) {}
