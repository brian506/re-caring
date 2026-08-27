package com.recaring.member.implement;

import com.recaring.member.dataaccess.entity.Member;
import com.recaring.member.dataaccess.repository.MemberRepository;
import com.recaring.member.fixture.MemberFixture;
import com.recaring.sms.vo.PhoneNumber;
import com.recaring.support.exception.AppException;
import com.recaring.support.exception.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("MemberReader 단위 테스트")
class MemberReaderTest {

    @InjectMocks
    private MemberReader memberReader;

    @Mock
    private MemberRepository memberRepository;

    @Test
    @DisplayName("존재하지 않는 memberKey로 조회하면 NOT_FOUND_ACCOUNT 예외가 발생한다")
    void findByMemberKey_throws_when_absent() {
        given(memberRepository.findByMemberKey(MemberFixture.MEMBER_KEY)).willReturn(Optional.empty());

        // SPEC ErrorType.NOT_FOUND_ACCOUNT(E2016): 존재하지 않는 계정 정보입니다
        assertThatThrownBy(() -> memberReader.findByMemberKey(MemberFixture.MEMBER_KEY))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.NOT_FOUND_ACCOUNT);
    }

    @Test
    @DisplayName("가입되지 않은 전화번호로 조회하면 NOT_FOUND_ACCOUNT 예외가 발생한다")
    void findByPhone_throws_when_absent() {
        given(memberRepository.findByPhone(MemberFixture.UNREGISTERED_PHONE)).willReturn(Optional.empty());

        // SPEC ErrorType.NOT_FOUND_ACCOUNT(E2016)
        assertThatThrownBy(() -> memberReader.findByPhone(new PhoneNumber(MemberFixture.UNREGISTERED_PHONE)))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.NOT_FOUND_ACCOUNT);
    }

    @Test
    @DisplayName("이름·생년월일·전화번호가 모두 일치하는 계정이 없으면 NOT_FOUND_ACCOUNT 예외가 발생한다")
    void findAccount_throws_when_absent() {
        given(memberRepository.findAccount(MemberFixture.NAME, MemberFixture.BIRTH, MemberFixture.PHONE))
                .willReturn(Optional.empty());

        // SPEC ErrorType.NOT_FOUND_ACCOUNT(E2016)
        assertThatThrownBy(() -> memberReader.findAccount(MemberFixture.NAME, MemberFixture.BIRTH, MemberFixture.PHONE))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.NOT_FOUND_ACCOUNT);
    }

    @Test
    @DisplayName("비관적 락 조회 대상이 없으면 NOT_FOUND_ACCOUNT 예외가 발생한다")
    void findForUpdate_throws_when_absent() {
        given(memberRepository.findForUpdate(MemberFixture.MEMBER_KEY)).willReturn(Optional.empty());

        // SPEC ErrorType.NOT_FOUND_ACCOUNT(E2016)
        assertThatThrownBy(() -> memberReader.findForUpdate(MemberFixture.MEMBER_KEY))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.NOT_FOUND_ACCOUNT);
    }

    @Test
    @DisplayName("여러 memberKey 조회 결과는 각 회원의 memberKey를 키로 하는 맵으로 돌려준다")
    void findAllByMemberKeys_is_keyed_by_member_key() {
        Member first = MemberFixture.createMemberWithKey(MemberFixture.MEMBER_KEY, MemberFixture.PHONE);
        Member second = MemberFixture.createMemberWithKey(MemberFixture.OTHER_MEMBER_KEY, MemberFixture.OTHER_PHONE);
        List<String> keys = List.of(MemberFixture.MEMBER_KEY, MemberFixture.OTHER_MEMBER_KEY);
        given(memberRepository.findAllByMemberKeyIn(keys)).willReturn(List.of(first, second));

        Map<String, Member> result = memberReader.findAllByMemberKeys(keys);

        // SPEC architecture.md 외부 식별자: 회원 조회 결과는 memberKey로 찾는다
        assertThat(result).hasSize(2);
        assertThat(result.get(MemberFixture.MEMBER_KEY).getPhone()).isEqualTo(MemberFixture.PHONE);
        assertThat(result.get(MemberFixture.OTHER_MEMBER_KEY).getPhone()).isEqualTo(MemberFixture.OTHER_PHONE);
    }

    @Test
    @DisplayName("요청한 memberKey 중 일부만 존재하면 존재하는 회원만 맵에 담긴다")
    void findAllByMemberKeys_omits_missing_keys() {
        Member first = MemberFixture.createMemberWithKey(MemberFixture.MEMBER_KEY, MemberFixture.PHONE);
        List<String> keys = List.of(MemberFixture.MEMBER_KEY, MemberFixture.OTHER_MEMBER_KEY);
        given(memberRepository.findAllByMemberKeyIn(keys)).willReturn(List.of(first));

        Map<String, Member> result = memberReader.findAllByMemberKeys(keys);

        // IMPL: 존재하지 않는 키는 예외 없이 누락된다 — 스펙 근거 없음
        assertThat(result).containsOnlyKeys(MemberFixture.MEMBER_KEY);
    }
}
