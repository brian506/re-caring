package com.recaring.member.implement;

import com.recaring.member.dataaccess.entity.MembersTermsAgreement;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
@DisplayName("MembersTermsAgreementReader 단위 테스트")
class MembersTermsAgreementReaderTest {

    @InjectMocks
    private MembersTermsAgreementReader membersTermsAgreementReader;

    @Mock
    private MembersTermsAgreementRepository membersTermsAgreementRepository;

    @Test
    @DisplayName("memberKey로 약관 동의 정보 조회 성공")
    void findByMemberKey_success() {
        MembersTermsAgreement terms = MemberFixture.createTermsAgreement();
        given(membersTermsAgreementRepository.findByMemberKey(MemberFixture.MEMBER_KEY))
                .willReturn(Optional.of(terms));

        MembersTermsAgreement result = membersTermsAgreementReader.findByMemberKey(MemberFixture.MEMBER_KEY);

        assertThat(result.getMemberKey()).isEqualTo(MemberFixture.MEMBER_KEY);
        then(membersTermsAgreementRepository).should(times(1)).findByMemberKey(MemberFixture.MEMBER_KEY);
    }

    @Test
    @DisplayName("존재하지 않는 memberKey로 조회 시 NOT_FOUND_ACCOUNT 예외가 발생한다")
    void findByMemberKey_throws_when_not_found() {
        given(membersTermsAgreementRepository.findByMemberKey(MemberFixture.MEMBER_KEY))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> membersTermsAgreementReader.findByMemberKey(MemberFixture.MEMBER_KEY))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.NOT_FOUND_ACCOUNT);
    }
}
