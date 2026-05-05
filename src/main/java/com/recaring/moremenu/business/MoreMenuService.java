package com.recaring.moremenu.business;

import com.recaring.care.dataaccess.entity.CareRole;
import com.recaring.care.implement.CareRelationshipReader;
import com.recaring.member.dataaccess.entity.Member;
import com.recaring.member.dataaccess.entity.MemberRole;
import com.recaring.member.implement.MemberReader;
import com.recaring.moremenu.implement.MoreMenuFactory;
import com.recaring.support.exception.AppException;
import com.recaring.support.exception.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MoreMenuService {

    private final MemberReader memberReader;
    private final CareRelationshipReader careRelationshipReader;
    private final MoreMenuFactory moreMenuFactory;

    public MoreMenuInfo getMenu(String memberKey, String wardKey) {
        Member member = memberReader.findByMemberKey(memberKey);
        MoreMenuContextType contextType = resolveContextType(member, memberKey, wardKey);
        return moreMenuFactory.getMenu(contextType);
    }

    private MoreMenuContextType resolveContextType(Member member, String memberKey, String wardKey) {
        if (member.getRole() == MemberRole.WARD) {
            return MoreMenuContextType.WARD;
        }
        if (wardKey == null || wardKey.isBlank()) {
            throw new AppException(ErrorType.WARD_KEY_REQUIRED);
        }

        CareRole careRole = careRelationshipReader.findCareRole(wardKey, memberKey);
        return switch (careRole) {
            case MANAGER -> MoreMenuContextType.MANAGER;
            case GUARDIAN -> MoreMenuContextType.GUARDIAN;
        };
    }
}
