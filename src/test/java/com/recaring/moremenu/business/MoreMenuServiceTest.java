package com.recaring.moremenu.business;

import com.recaring.care.dataaccess.entity.CareRole;
import com.recaring.care.fixture.CareFixture;
import com.recaring.care.implement.CareRelationshipReader;
import com.recaring.member.implement.MemberReader;
import com.recaring.moremenu.implement.MoreMenuFactory;
import com.recaring.support.exception.AppException;
import com.recaring.support.exception.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
@DisplayName("더보기 메뉴 서비스 단위 테스트")
class MoreMenuServiceTest {

    @InjectMocks
    private MoreMenuService moreMenuService;

    @Mock
    private MemberReader memberReader;

    @Mock
    private CareRelationshipReader careRelationshipReader;

    @Mock
    private MoreMenuFactory moreMenuFactory;

    @Test
    @DisplayName("보호 대상자는 wardKey 없이 보호 대상자 메뉴를 조회한다")
    void getMenu_returns_ward_menu() {
        MoreMenuInfo expected = menu(MoreMenuContextType.WARD);

        given(memberReader.findByMemberKey(CareFixture.WARD_MEMBER_KEY))
                .willReturn(CareFixture.createWardMember());
        given(moreMenuFactory.getMenu(MoreMenuContextType.WARD)).willReturn(expected);

        MoreMenuInfo result = moreMenuService.getMenu(CareFixture.WARD_MEMBER_KEY, null);

        assertThat(result).isEqualTo(expected);
        then(careRelationshipReader).shouldHaveNoInteractions();
        then(moreMenuFactory).should(times(1)).getMenu(MoreMenuContextType.WARD);
    }

    @Test
    @DisplayName("관리자는 wardKey로 관리자 메뉴를 조회한다")
    void getMenu_returns_manager_menu() {
        MoreMenuInfo expected = menu(MoreMenuContextType.MANAGER);
        var manager = CareFixture.createGuardianMember(CareFixture.MANAGER_PHONE);

        given(memberReader.findByMemberKey(CareFixture.MANAGER_MEMBER_KEY))
                .willReturn(manager);
        given(careRelationshipReader.findCareRole(CareFixture.WARD_MEMBER_KEY, manager.getMemberKey()))
                .willReturn(CareRole.MANAGER);
        given(moreMenuFactory.getMenu(MoreMenuContextType.MANAGER)).willReturn(expected);

        MoreMenuInfo result = moreMenuService.getMenu(CareFixture.MANAGER_MEMBER_KEY, CareFixture.WARD_MEMBER_KEY);

        assertThat(result).isEqualTo(expected);
        then(moreMenuFactory).should(times(1)).getMenu(MoreMenuContextType.MANAGER);
    }

    @Test
    @DisplayName("주보호자는 wardKey로 주보호자 메뉴를 조회한다")
    void getMenu_returns_guardian_menu() {
        MoreMenuInfo expected = menu(MoreMenuContextType.GUARDIAN);
        var guardian = CareFixture.createGuardianMember();

        given(memberReader.findByMemberKey(CareFixture.GUARDIAN_MEMBER_KEY))
                .willReturn(guardian);
        given(careRelationshipReader.findCareRole(CareFixture.WARD_MEMBER_KEY, guardian.getMemberKey()))
                .willReturn(CareRole.GUARDIAN);
        given(moreMenuFactory.getMenu(MoreMenuContextType.GUARDIAN)).willReturn(expected);

        MoreMenuInfo result = moreMenuService.getMenu(CareFixture.GUARDIAN_MEMBER_KEY, CareFixture.WARD_MEMBER_KEY);

        assertThat(result).isEqualTo(expected);
        then(moreMenuFactory).should(times(1)).getMenu(MoreMenuContextType.GUARDIAN);
    }

    @Test
    @DisplayName("보호자는 wardKey가 없으면 예외가 발생한다")
    void getMenu_throws_exception_when_ward_key_is_missing() {
        given(memberReader.findByMemberKey(CareFixture.GUARDIAN_MEMBER_KEY))
                .willReturn(CareFixture.createGuardianMember());

        assertThatThrownBy(() -> moreMenuService.getMenu(CareFixture.GUARDIAN_MEMBER_KEY, null))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.WARD_KEY_REQUIRED);

        then(careRelationshipReader).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("보호자는 wardKey가 공백이면 예외가 발생한다")
    void getMenu_throws_exception_when_ward_key_is_blank() {
        given(memberReader.findByMemberKey(CareFixture.GUARDIAN_MEMBER_KEY))
                .willReturn(CareFixture.createGuardianMember());

        assertThatThrownBy(() -> moreMenuService.getMenu(CareFixture.GUARDIAN_MEMBER_KEY, " "))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.WARD_KEY_REQUIRED);

        then(careRelationshipReader).shouldHaveNoInteractions();
    }

    private MoreMenuInfo menu(MoreMenuContextType contextType) {
        return new MoreMenuInfo(contextType, List.of());
    }
}
