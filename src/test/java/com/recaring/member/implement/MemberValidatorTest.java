package com.recaring.member.implement;

import com.recaring.member.dataaccess.entity.SubscriptionType;
import com.recaring.member.fixture.MemberFixture;
import com.recaring.support.exception.AppException;
import com.recaring.support.exception.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;

@ExtendWith(MockitoExtension.class)
@DisplayName("MemberValidator 단위 테스트")
class MemberValidatorTest {

    @InjectMocks
    private MemberValidator memberValidator;

    @Mock
    private MemberReader memberReader;

    @Test
    @DisplayName("PREMIUM 회원은 프리미엄 전용 기능 검증을 통과한다")
    void validatePremium_passes_for_premium_member() {
        given(memberReader.findByMemberKey(MemberFixture.MEMBER_KEY))
                .willReturn(MemberFixture.createMemberWithSubscription(SubscriptionType.PREMIUM));

        // SPEC ErrorType.PREMIUM_ONLY(E3003): 프리미엄 구독 회원만 접근 가능
        assertThatCode(() -> memberValidator.validatePremium(MemberFixture.MEMBER_KEY))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("BASIC 회원이 프리미엄 전용 기능에 접근하면 PREMIUM_ONLY 예외가 발생한다")
    void validatePremium_throws_for_basic_member() {
        given(memberReader.findByMemberKey(MemberFixture.MEMBER_KEY))
                .willReturn(MemberFixture.createMemberWithSubscription(SubscriptionType.BASIC));

        // SPEC ErrorType.PREMIUM_ONLY(E3003)
        assertThatThrownBy(() -> memberValidator.validatePremium(MemberFixture.MEMBER_KEY))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.PREMIUM_ONLY);
    }

    @Test
    @DisplayName("존재하지 않는 회원의 프리미엄 검증은 NOT_FOUND_ACCOUNT 예외로 끝난다")
    void validatePremium_propagates_not_found() {
        willThrow(new AppException(ErrorType.NOT_FOUND_ACCOUNT))
                .given(memberReader).findByMemberKey(MemberFixture.MEMBER_KEY);

        // SPEC ErrorType.NOT_FOUND_ACCOUNT(E2016)
        assertThatThrownBy(() -> memberValidator.validatePremium(MemberFixture.MEMBER_KEY))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.NOT_FOUND_ACCOUNT);
    }

    @Test
    @DisplayName("신규 가입 상태(BASIC)의 회원은 구독 검증을 통과한다")
    void validateSubscription_passes_for_newly_registered_member() {
        given(memberReader.findByMemberKey(MemberFixture.MEMBER_KEY)).willReturn(MemberFixture.createMember());

        // IMPL: SUBSCRIPTION_ONLY 분기는 subscriptionType null에서만 동작하는데 컬럼이 NOT NULL이라
        // 실서비스에서 도달 불가 (audit A-10). 정책 결정 전까지 현재 동작만 고정한다.
        assertThatCode(() -> memberValidator.validateSubscription(MemberFixture.MEMBER_KEY))
                .doesNotThrowAnyException();
    }
}
