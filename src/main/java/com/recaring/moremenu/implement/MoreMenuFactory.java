package com.recaring.moremenu.implement;

import com.recaring.moremenu.business.MoreMenuContextType;
import com.recaring.moremenu.business.MoreMenuInfo;
import com.recaring.moremenu.business.MoreMenuItemInfo;
import com.recaring.moremenu.business.MoreMenuItemKey;
import com.recaring.moremenu.business.MoreMenuSectionInfo;
import com.recaring.moremenu.business.MoreMenuSectionKey;
import com.recaring.moremenu.business.MoreMenuTargetType;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class MoreMenuFactory {

    private static final String LOCATION_COLLECTION_SETTING_TARGET = "ST-002";
    private static final String NOTIFICATION_SETTING_TARGET = "ST-003";
    private static final String SAFE_ZONE_SETTING_TARGET = "ZN-001";
    private static final String WARD_SETTING_TARGET = "ST-004";
    private static final String CARE_RELATIONSHIP_SETTING_TARGET = "ST-005";
    private static final String FAQ_TARGET = "CS-001";
    private static final String TERMS_TARGET = "TERMS";
    private static final String INQUIRY_TARGET = "APP_INQUIRY";
    private static final String APP_VERSION_TARGET = "SHOW_APP_VERSION";
    private static final String SIGN_OUT_TARGET = "/api/v1/auth/sign-out";
    private static final String WITHDRAW_ACCOUNT_TARGET = "WITHDRAW_ACCOUNT";

    public MoreMenuInfo getMenu(MoreMenuContextType contextType) {
        return new MoreMenuInfo(
                contextType,
                List.of(
                        settingSection(contextType),
                        moreSection()
                )
        );
    }

    private MoreMenuSectionInfo settingSection(MoreMenuContextType contextType) {
        return switch (contextType) {
            case WARD -> new MoreMenuSectionInfo(
                    MoreMenuSectionKey.SETTING,
                    List.of(
                            screen(MoreMenuItemKey.NOTIFICATION_SETTING, true, NOTIFICATION_SETTING_TARGET),
                            screen(MoreMenuItemKey.PROTECTOR_SETTING, true, CARE_RELATIONSHIP_SETTING_TARGET)
                    )
            );
            case MANAGER -> caregiverSettingSection(false);
            case GUARDIAN -> caregiverSettingSection(true);
        };
    }

    private MoreMenuSectionInfo caregiverSettingSection(boolean locationSettingsEnabled) {
        return new MoreMenuSectionInfo(
                MoreMenuSectionKey.SETTING,
                List.of(
                        screen(MoreMenuItemKey.LOCATION_COLLECTION_SETTING, locationSettingsEnabled, LOCATION_COLLECTION_SETTING_TARGET),
                        screen(MoreMenuItemKey.NOTIFICATION_SETTING, true, NOTIFICATION_SETTING_TARGET),
                        screen(MoreMenuItemKey.SAFE_ZONE_SETTING, locationSettingsEnabled, SAFE_ZONE_SETTING_TARGET),
                        screen(MoreMenuItemKey.WARD_SETTING, true, WARD_SETTING_TARGET),
                        screen(MoreMenuItemKey.CAREGIVER_SETTING, true, CARE_RELATIONSHIP_SETTING_TARGET)
                )
        );
    }

    private MoreMenuSectionInfo moreSection() {
        return new MoreMenuSectionInfo(
                MoreMenuSectionKey.MORE,
                List.of(
                        webview(MoreMenuItemKey.FAQ, FAQ_TARGET),
                        webview(MoreMenuItemKey.TERMS, TERMS_TARGET),
                        externalLink(MoreMenuItemKey.INQUIRY, INQUIRY_TARGET),
                        clientAction(MoreMenuItemKey.APP_VERSION, APP_VERSION_TARGET),
                        action(MoreMenuItemKey.SIGN_OUT, SIGN_OUT_TARGET),
                        screen(MoreMenuItemKey.WITHDRAW_ACCOUNT, true, WITHDRAW_ACCOUNT_TARGET)
                )
        );
    }

    private MoreMenuItemInfo screen(MoreMenuItemKey itemKey, boolean enabled, String target) {
        return item(itemKey, enabled, MoreMenuTargetType.SCREEN, target);
    }

    private MoreMenuItemInfo webview(MoreMenuItemKey itemKey, String target) {
        return item(itemKey, true, MoreMenuTargetType.WEBVIEW, target);
    }

    private MoreMenuItemInfo externalLink(MoreMenuItemKey itemKey, String target) {
        return item(itemKey, true, MoreMenuTargetType.EXTERNAL_LINK, target);
    }

    private MoreMenuItemInfo clientAction(MoreMenuItemKey itemKey, String target) {
        return item(itemKey, true, MoreMenuTargetType.CLIENT_ACTION, target);
    }

    private MoreMenuItemInfo action(MoreMenuItemKey itemKey, String target) {
        return item(itemKey, true, MoreMenuTargetType.ACTION, target);
    }

    private MoreMenuItemInfo item(MoreMenuItemKey itemKey, boolean enabled, MoreMenuTargetType targetType, String target) {
        return new MoreMenuItemInfo(itemKey, enabled, targetType, target);
    }
}

