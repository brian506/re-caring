package com.recaring.member.business;

import com.recaring.auth.fixture.AuthFixture;
import com.recaring.auth.implement.local.LocalAuthAuthenticator;
import com.recaring.auth.implement.local.LocalAuthManager;
import com.recaring.auth.implement.local.LocalAuthReader;
import com.recaring.auth.vo.Password;
import com.recaring.member.controller.response.ContactMemberResponse;
import com.recaring.member.controller.response.MyInfoResponse;
import com.recaring.member.dataaccess.entity.Member;
import com.recaring.member.dataaccess.entity.MemberRole;
import com.recaring.member.dataaccess.entity.MembersTermsAgreement;
import com.recaring.member.fixture.MemberFixture;
import com.recaring.member.implement.MemberReader;
import com.recaring.member.implement.MemberWithdrawalManager;
import com.recaring.member.implement.MemberWriter;
import com.recaring.member.implement.MembersTermsAgreementReader;
import com.recaring.safezone.fixture.SafeZoneFixture;
import com.recaring.safezone.implement.SafeZoneReader;
import com.recaring.safezone.vo.SafeZoneInfo;
import com.recaring.support.exception.AppException;
import com.recaring.support.exception.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.groups.Tuple.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
@DisplayName("MemberService 단위 테스트")
class MemberServiceTest {

    private static final String MEMBER_KEY = MemberFixture.MEMBER_KEY;

    @InjectMocks
    private MemberService memberService;

    @Mock private MemberReader memberReader;
    @Mock private MemberWriter memberWriter;
    @Mock private LocalAuthReader localAuthReader;
    @Mock private LocalAuthAuthenticator localAuthAuthenticator;
    @Mock private LocalAuthManager localAuthManager;
    @Mock private MembersTermsAgreementReader membersTermsAgreementReader;
    @Mock private MemberWithdrawalManager memberWithdrawalManager;
    @Mock private SafeZoneReader safeZoneReader;

    // ── findByPhones ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("연락처 조회 응답은 가입 회원의 memberKey·이름·전화번호·역할만 노출한다")
    void findByPhones_exposes_public_identifier_and_role() {
        Member guardian = MemberFixture.createMemberWithKey(MEMBER_KEY, MemberFixture.PHONE);
        Member ward = MemberFixture.createWardMember(MemberFixture.OTHER_PHONE);
        List<String> phones = List.of(MemberFixture.PHONE, MemberFixture.OTHER_PHONE, MemberFixture.UNREGISTERED_PHONE);
        given(memberReader.findByPhones(phones)).willReturn(List.of(guardian, ward));

        List<ContactMemberResponse> result = memberService.findByPhones(phones);

        // SPEC POST /api/v1/members/phones: 가입된 회원만 필터링해 반환, 외부 식별자는 memberKey
        assertThat(result).extracting(ContactMemberResponse::memberKey, ContactMemberResponse::phone, ContactMemberResponse::role)
                .containsExactly(
                        tuple(MEMBER_KEY, MemberFixture.PHONE, MemberRole.GUARDIAN),
                        tuple(ward.getMemberKey(), MemberFixture.OTHER_PHONE, MemberRole.WARD));
    }

    // ── getMyInfo ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("내 정보는 회원·이메일·약관 동의·안심존을 요청자의 memberKey 하나로 모아 만든다")
    void getMyInfo_assembles_all_sources_for_requester() {
        Member member = MemberFixture.createMemberWithKey(MEMBER_KEY, MemberFixture.PHONE);
        MembersTermsAgreement terms = MemberFixture.createTermsAgreement();
        SafeZoneInfo safeZone = SafeZoneFixture.createSafeZoneInfo("safe-zone-key-1");
        given(memberReader.findByMemberKey(MEMBER_KEY)).willReturn(member);
        given(localAuthReader.findEmailByMemberKey(MEMBER_KEY)).willReturn(MemberFixture.EMAIL);
        given(membersTermsAgreementReader.findByMemberKey(MEMBER_KEY)).willReturn(terms);
        given(safeZoneReader.findAllByWardMemberKey(MEMBER_KEY)).willReturn(List.of(safeZone));

        MyInfoResponse result = memberService.getMyInfo(MEMBER_KEY);

        // SPEC GET /api/v1/members/me: Member + 이메일 + 약관 + 안심존 통합
        assertThat(result.memberKey()).isEqualTo(MEMBER_KEY);
        assertThat(result.phone()).isEqualTo(MemberFixture.PHONE);
        assertThat(result.email()).isEqualTo(MemberFixture.EMAIL);
        assertThat(result.termsServiceAgreedAt()).isEqualTo(terms.getTermsServiceAgreedAt());
        assertThat(result.safeZones()).containsExactly(safeZone);
    }

    @Test
    @DisplayName("존재하지 않는 회원의 내 정보 조회는 NOT_FOUND_ACCOUNT로 끝나고 다른 조회는 시도하지 않는다")
    void getMyInfo_stops_at_member_lookup_when_absent() {
        willThrow(new AppException(ErrorType.NOT_FOUND_ACCOUNT))
                .given(memberReader).findByMemberKey(MEMBER_KEY);

        // SPEC ErrorType.NOT_FOUND_ACCOUNT(E2016)
        assertThatThrownBy(() -> memberService.getMyInfo(MEMBER_KEY))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.NOT_FOUND_ACCOUNT);
        verifyNoInteractions(localAuthReader, membersTermsAgreementReader, safeZoneReader);
    }

    // ── updateMyInfo ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("새 비밀번호가 있으면 현재 비밀번호로 검증한 뒤 새 비밀번호의 해시로 교체한다")
    void updateMyInfo_verifies_current_then_stores_encoded_new_password() {
        given(localAuthAuthenticator.encodePassword(new Password(MemberFixture.NEW_PASSWORD)))
                .willReturn(AuthFixture.createEncodedPassword());

        memberService.updateMyInfo(MEMBER_KEY, MemberFixture.UPDATED_NAME, MemberFixture.UPDATED_BIRTH,
                MemberFixture.CURRENT_PASSWORD, MemberFixture.NEW_PASSWORD);

        // SPEC PATCH /api/v1/members/me: 비밀번호 변경 시 현재 비밀번호(currentPassword) 확인이 필요하다
        InOrder inOrder = inOrder(localAuthAuthenticator, localAuthManager);
        inOrder.verify(localAuthAuthenticator).verifyPassword(MEMBER_KEY, new Password(MemberFixture.CURRENT_PASSWORD));
        inOrder.verify(localAuthManager).changePassword(MEMBER_KEY, AuthFixture.ENCODED_PASSWORD);
        // SPEC PATCH /api/v1/members/me: 이름·생년월일도 함께 반영
        then(memberWriter).should().updateProfile(MEMBER_KEY, MemberFixture.UPDATED_NAME, MemberFixture.UPDATED_BIRTH);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    @DisplayName("새 비밀번호가 비어 있으면 프로필만 수정하고 비밀번호 검증·변경은 건너뛴다")
    void updateMyInfo_skips_password_flow_when_new_password_blank(String blankNewPassword) {
        memberService.updateMyInfo(MEMBER_KEY, MemberFixture.UPDATED_NAME, null, null, blankNewPassword);

        // SPEC PATCH /api/v1/members/me: 요청에 포함된 필드만 반영, null 또는 빈 값은 무시
        then(memberWriter).should().updateProfile(MEMBER_KEY, MemberFixture.UPDATED_NAME, null);
        verifyNoInteractions(localAuthAuthenticator, localAuthManager);
    }

    @Test
    @DisplayName("현재 비밀번호가 틀리면 INVALID_PASSWORD 예외가 발생하고 비밀번호는 바뀌지 않는다")
    void updateMyInfo_throws_and_keeps_password_when_current_mismatch() {
        willThrow(new AppException(ErrorType.INVALID_PASSWORD))
                .given(localAuthAuthenticator).verifyPassword(MEMBER_KEY, new Password(MemberFixture.WRONG_PASSWORD));

        // SPEC ErrorType.INVALID_PASSWORD(E2017)
        assertThatThrownBy(() -> memberService.updateMyInfo(MEMBER_KEY, null, null,
                MemberFixture.WRONG_PASSWORD, MemberFixture.NEW_PASSWORD))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.INVALID_PASSWORD);
        then(localAuthAuthenticator).should(never()).encodePassword(any());
        then(localAuthManager).should(never()).changePassword(anyString(), anyString());
    }

    @Test
    @DisplayName("새 비밀번호만 보내고 현재 비밀번호를 생략하면 PASSWORD_IS_NULL 예외가 발생한다")
    void updateMyInfo_throws_when_current_password_missing() {
        // SPEC Password VO: 비밀번호는 null일 수 없다 (E2011)
        assertThatThrownBy(() -> memberService.updateMyInfo(MEMBER_KEY, null, null, null, MemberFixture.NEW_PASSWORD))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.PASSWORD_IS_NULL);
        verifyNoInteractions(localAuthManager);
    }

    @Test
    @DisplayName("새 비밀번호가 형식에 맞지 않으면 INVALID_PASSWORD_FORMAT 예외가 발생하고 비밀번호는 바뀌지 않는다")
    void updateMyInfo_throws_when_new_password_has_invalid_format() {
        // SPEC Password VO: 영문과 숫자를 포함해야 한다 (E2015)
        assertThatThrownBy(() -> memberService.updateMyInfo(MEMBER_KEY, null, null,
                MemberFixture.CURRENT_PASSWORD, "onlyletters"))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.INVALID_PASSWORD_FORMAT);
        verifyNoInteractions(localAuthManager);
    }
}
