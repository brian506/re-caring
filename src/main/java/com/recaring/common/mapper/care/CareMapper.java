package com.recaring.common.mapper.care;

import com.recaring.care.dataaccess.entity.CareInvitation;
import com.recaring.care.dataaccess.entity.CareRelationship;
import com.recaring.care.dataaccess.entity.CareRole;
import com.recaring.care.vo.NewCareInvitation;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class CareMapper {

    public CareRelationship toCareRelationship(String wardMemberKey, String caregiverMemberKey, CareRole careRole) {
        return CareRelationship.builder()
                .wardMemberKey(wardMemberKey)
                .caregiverMemberKey(caregiverMemberKey)
                .careRole(careRole)
                .build();
    }

    public CareInvitation toCareInvitation(NewCareInvitation invitation) {
        return CareInvitation.builder()
                .requesterMemberKey(invitation.requesterMemberKey())
                .targetMemberKey(invitation.targetMemberKey())
                .wardMemberKey(invitation.wardMemberKey())
                .careRole(invitation.careRole())
                .build();
    }
}

