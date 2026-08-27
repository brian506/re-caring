package com.recaring.member.implement;

import com.recaring.member.dataaccess.repository.MembersTermsAgreementRepository;
import com.recaring.member.fixture.MemberFixture;
import com.recaring.support.exception.AppException;
import com.recaring.support.exception.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("MembersTermsAgreementReader 단위 테스트")
class MembersTermsAgreementReaderTest {

    @InjectMocks
    private MembersTermsAgreementReader membersTermsAgreementReader;

    @Mock
    private MembersTermsAgreementRepository membersTermsAgreementRepository;

    @Test
    @DisplayName("약관 동의 이력이 없는 회원은 NOT_FOUND_ACCOUNT 예외가 발생한다")
    void findByMemberKey_throws_when_absent() {
        given(membersTermsAgreementRepository.findByMemberKey(MemberFixture.MEMBER_KEY))
                .willReturn(Optional.empty());

        // SPEC ErrorType.NOT_FOUND_ACCOUNT(E2016): 약관 동의는 가입 시 반드시 생성되므로 없으면 계정 부재로 본다
        assertThatThrownBy(() -> membersTermsAgreementReader.findByMemberKey(MemberFixture.MEMBER_KEY))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.NOT_FOUND_ACCOUNT);
    }
}
