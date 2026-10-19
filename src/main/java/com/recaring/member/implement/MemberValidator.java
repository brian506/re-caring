package com.recaring.member.implement;

import com.recaring.member.dataaccess.entity.SubscriptionType;
import com.recaring.member.vo.MemberInfo;
import com.recaring.support.exception.AppException;
import com.recaring.support.exception.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MemberValidator {
    private final MemberReader memberReader;

    public void validateSubscription(String memberKey) {
        MemberInfo member = memberReader.findByMemberKey(memberKey);
        if(member.subscriptionType() == null) {
            throw new AppException(ErrorType.SUBSCRIPTION_ONLY);
        }
    }

    public void validatePremium(String memberKey) {
        MemberInfo member = memberReader.findByMemberKey(memberKey);
        if(member.subscriptionType() != SubscriptionType.PREMIUM) {
            throw new AppException(ErrorType.PREMIUM_ONLY);
        }
    }

}
