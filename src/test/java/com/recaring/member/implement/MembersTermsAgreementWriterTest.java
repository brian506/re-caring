package com.recaring.member.implement;

import com.recaring.member.dataaccess.entity.MembersTermsAgreement;
import com.recaring.member.dataaccess.repository.MembersTermsAgreementRepository;
import com.recaring.member.fixture.MemberFixture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("MembersTermsAgreementWriter 단위 테스트")
class MembersTermsAgreementWriterTest {

    @InjectMocks
    private MembersTermsAgreementWriter membersTermsAgreementWriter;

    @Mock
    private MembersTermsAgreementRepository membersTermsAgreementRepository;

    @Test
    @DisplayName("약관 동의 이력은 가입한 회원의 memberKey로 저장된다")
    void register_stores_agreement_under_that_member_key() {
        membersTermsAgreementWriter.register(MemberFixture.MEMBER_KEY);

        ArgumentCaptor<MembersTermsAgreement> captor = ArgumentCaptor.forClass(MembersTermsAgreement.class);
        then(membersTermsAgreementRepository).should().save(captor.capture());
        // SPEC snapshot.md 엔티티 MembersTermsAgreement: memberKey, agreedAt
        assertThat(captor.getValue().getMemberKey()).isEqualTo(MemberFixture.MEMBER_KEY);
    }
}
