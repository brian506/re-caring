package com.recaring.moremenu.implement;

import com.recaring.moremenu.business.MoreMenuInfo;
import com.recaring.moremenu.business.MoreMenuItemInfo;
import com.recaring.moremenu.business.MoreMenuItemKey;
import com.recaring.moremenu.business.MoreMenuSectionInfo;
import com.recaring.moremenu.business.MoreMenuSectionKey;
import com.recaring.moremenu.business.MoreMenuTargetType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.recaring.moremenu.business.MoreMenuContextType.GUARDIAN;
import static com.recaring.moremenu.business.MoreMenuContextType.MANAGER;
import static com.recaring.moremenu.business.MoreMenuContextType.WARD;
import static com.recaring.moremenu.business.MoreMenuItemKey.APP_VERSION;
import static com.recaring.moremenu.business.MoreMenuItemKey.CAREGIVER_SETTING;
import static com.recaring.moremenu.business.MoreMenuItemKey.FAQ;
import static com.recaring.moremenu.business.MoreMenuItemKey.INQUIRY;
import static com.recaring.moremenu.business.MoreMenuItemKey.LOCATION_COLLECTION_SETTING;
import static com.recaring.moremenu.business.MoreMenuItemKey.NOTIFICATION_SETTING;
import static com.recaring.moremenu.business.MoreMenuItemKey.PROTECTOR_SETTING;
import static com.recaring.moremenu.business.MoreMenuItemKey.SAFE_ZONE_SETTING;
import static com.recaring.moremenu.business.MoreMenuItemKey.SIGN_OUT;
import static com.recaring.moremenu.business.MoreMenuItemKey.TERMS;
import static com.recaring.moremenu.business.MoreMenuItemKey.WARD_SETTING;
import static com.recaring.moremenu.business.MoreMenuItemKey.WITHDRAW_ACCOUNT;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("더보기 메뉴 팩토리 단위 테스트")
class MoreMenuFactoryTest {

    private final MoreMenuFactory moreMenuFactory = new MoreMenuFactory();

    @Test
    @DisplayName("보호 대상자 메뉴는 알림 설정과 보호자 설정을 반환한다")
    void getMenu_returns_ward_items() {
        MoreMenuInfo result = moreMenuFactory.getMenu(WARD);

        assertThat(settingItems(result))
                .extracting(MoreMenuItemInfo::itemKey)
                .containsExactly(NOTIFICATION_SETTING, PROTECTOR_SETTING);
        assertCommonMoreItems(result);
    }

    @Test
    @DisplayName("관리자 메뉴는 위치 관련 설정을 비활성화한다")
    void getMenu_returns_manager_items() {
        List<MoreMenuItemInfo> settingItems = settingItems(moreMenuFactory.getMenu(MANAGER));

        assertThat(settingItems)
                .extracting(MoreMenuItemInfo::itemKey)
                .containsExactly(
                        LOCATION_COLLECTION_SETTING,
                        NOTIFICATION_SETTING,
                        SAFE_ZONE_SETTING,
                        WARD_SETTING,
                        CAREGIVER_SETTING
                );
        assertThat(find(settingItems, LOCATION_COLLECTION_SETTING).enabled()).isFalse();
        assertThat(find(settingItems, SAFE_ZONE_SETTING).enabled()).isFalse();
        assertThat(find(settingItems, NOTIFICATION_SETTING).enabled()).isTrue();
        assertThat(find(settingItems, WARD_SETTING).enabled()).isTrue();
        assertThat(find(settingItems, CAREGIVER_SETTING).enabled()).isTrue();
    }

    @Test
    @DisplayName("주보호자 메뉴는 모든 설정 항목을 활성화한다")
    void getMenu_returns_guardian_items() {
        List<MoreMenuItemInfo> settingItems = settingItems(moreMenuFactory.getMenu(GUARDIAN));

        assertThat(settingItems)
                .extracting(MoreMenuItemInfo::itemKey)
                .containsExactly(
                        LOCATION_COLLECTION_SETTING,
                        NOTIFICATION_SETTING,
                        SAFE_ZONE_SETTING,
                        WARD_SETTING,
                        CAREGIVER_SETTING
                );
        assertThat(settingItems).allMatch(MoreMenuItemInfo::enabled);
    }

    @Test
    @DisplayName("공통 더보기 메뉴는 고객지원 항목과 계정 액션을 반환한다")
    void getMenu_returns_common_more_items() {
        MoreMenuInfo result = moreMenuFactory.getMenu(GUARDIAN);

        assertThat(moreItems(result))
                .extracting(MoreMenuItemInfo::itemKey)
                .containsExactly(FAQ, TERMS, INQUIRY, APP_VERSION, SIGN_OUT, WITHDRAW_ACCOUNT);
        assertThat(find(moreItems(result), FAQ).targetType()).isEqualTo(MoreMenuTargetType.WEBVIEW);
        assertThat(find(moreItems(result), TERMS).targetType()).isEqualTo(MoreMenuTargetType.WEBVIEW);
        assertThat(find(moreItems(result), INQUIRY).targetType()).isEqualTo(MoreMenuTargetType.EXTERNAL_LINK);
        assertThat(find(moreItems(result), APP_VERSION).targetType()).isEqualTo(MoreMenuTargetType.CLIENT_ACTION);
        assertThat(find(moreItems(result), SIGN_OUT).targetType()).isEqualTo(MoreMenuTargetType.ACTION);
    }

    private void assertCommonMoreItems(MoreMenuInfo result) {
        assertThat(moreItems(result))
                .extracting(MoreMenuItemInfo::itemKey)
                .containsExactly(FAQ, TERMS, INQUIRY, APP_VERSION, SIGN_OUT, WITHDRAW_ACCOUNT);
    }

    private List<MoreMenuItemInfo> settingItems(MoreMenuInfo menu) {
        return sectionItems(menu, MoreMenuSectionKey.SETTING);
    }

    private List<MoreMenuItemInfo> moreItems(MoreMenuInfo menu) {
        return sectionItems(menu, MoreMenuSectionKey.MORE);
    }

    private List<MoreMenuItemInfo> sectionItems(MoreMenuInfo menu, MoreMenuSectionKey sectionKey) {
        return menu.sections()
                .stream()
                .filter(section -> section.sectionKey() == sectionKey)
                .findFirst()
                .map(MoreMenuSectionInfo::items)
                .orElseThrow();
    }

    private MoreMenuItemInfo find(List<MoreMenuItemInfo> items, MoreMenuItemKey itemKey) {
        return items.stream()
                .filter(item -> item.itemKey() == itemKey)
                .findFirst()
                .orElseThrow();
    }
}
