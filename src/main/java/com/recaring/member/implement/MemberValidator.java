package com.recaring.member.implement;

import com.recaring.member.dataaccess.entity.Member;
import com.recaring.member.dataaccess.entity.SubscriptionType;
import com.recaring.support.exception.AppException;
import com.recaring.support.exception.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MemberValidator {
    private final MemberReader memberReader;

    public void validateSubscription(String memberKey) {
        Member member = memberReader.findByMemberKey(memberKey);
        if(member.getSubscriptionType() == null) {
            throw new AppException(ErrorType.SUBSCRIPTION_ONLY);
        }
    }

    public void validatePremium(String memberKey) {
        Member member = memberReader.findByMemberKey(memberKey);
        if(member.getSubscriptionType() != SubscriptionType.PREMIUM) {
            throw new AppException(ErrorType.PREMIUM_ONLY);
        }
    }

}
