package com.recaring.notification.controller;

import com.recaring.common.controller.ApiControllerAdvice;
import com.recaring.notification.business.NotificationSettingInfo;
import com.recaring.notification.business.NotificationSettingService;
import com.recaring.notification.controller.response.NotificationSettingResponse;
import com.recaring.notification.dataaccess.entity.NotificationSetting;
import com.recaring.notification.fixture.NotificationFixture;
import com.recaring.notification.vo.AnomalySensitivity;
import com.recaring.security.vo.AuthMember;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationSettingController MVC test")
class NotificationSettingControllerWebMvcTest {

    private static final String MEMBER_KEY = "member-key";
    private static final String WARD_KEY = "ward-key";

    private MockMvc mockMvc;

    @Mock
    private NotificationSettingService notificationSettingService;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders.standaloneSetup(new NotificationSettingController(notificationSettingService))
                .setControllerAdvice(new ApiControllerAdvice())
                .setCustomArgumentResolvers(authMemberArgumentResolver())
                .setValidator(validator)
                .build();
    }

    @Test
    @DisplayName("Returns all notification settings")
    void getSetting_returns_notification_setting() throws Exception {
        given(notificationSettingService.getSetting(MEMBER_KEY, WARD_KEY))
                .willReturn(NotificationSettingInfo.from(setting()));

        mockMvc.perform(get("/api/v1/notifications/settings/{wardKey}", WARD_KEY)
                        .requestAttr("memberKey", MEMBER_KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultType").value("SUCCESS"))
                .andExpect(jsonPath("$.data.safeZone.entryEnabled").value(true))
                .andExpect(jsonPath("$.data.safeZone.exitEnabled").value(false))
                .andExpect(jsonPath("$.data.anomaly.routeDeviationSensitivity").value("HIGH"))
                .andExpect(jsonPath("$.data.anomaly.speedAnomalySensitivity").value("LOW"))
                .andExpect(jsonPath("$.data.anomaly.wanderingAnomalySensitivity").value("VERY_HIGH"))
                .andExpect(jsonPath("$.data.anomaly.sensitivityOptions[0]").value("VERY_LOW"))
                .andExpect(jsonPath("$.data.emergencyCall.enabled").value(true))
                .andExpect(jsonPath("$.data.battery.lowBatteryEnabled").value(false))
                .andExpect(jsonPath("$.data.battery.thresholdPercent").value(40))
                .andExpect(jsonPath("$.data.battery.thresholdOptions[0]").value(10))
                .andExpect(jsonPath("$.data.battery.thresholdOptions[18]").value(100));

        then(notificationSettingService).should().getSetting(MEMBER_KEY, WARD_KEY);
    }

    @Test
    @DisplayName("Updates safe zone notification settings")
    void updateSafeZone_updates_setting() throws Exception {
        mockMvc.perform(patch("/api/v1/notifications/settings/{wardKey}/safe-zone", WARD_KEY)
                        .requestAttr("memberKey", MEMBER_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"entryEnabled": false, "exitEnabled": true}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultType").value("SUCCESS"));

        then(notificationSettingService).should().updateSafeZone(MEMBER_KEY, WARD_KEY, false, true);
    }

    @Test
    @DisplayName("Returns 400 when a required safe zone boolean is missing")
    void updateSafeZone_returns_400_when_boolean_is_missing() throws Exception {
        mockMvc.perform(patch("/api/v1/notifications/settings/{wardKey}/safe-zone", WARD_KEY)
                        .requestAttr("memberKey", MEMBER_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"exitEnabled": true}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.resultType").value("ERROR"));

        then(notificationSettingService).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("Updates anomaly notification settings")
    void updateAnomaly_updates_setting() throws Exception {
        mockMvc.perform(patch("/api/v1/notifications/settings/{wardKey}/anomaly", WARD_KEY)
                        .requestAttr("memberKey", MEMBER_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "routeDeviationEnabled": true,
                                  "speedAnomalyEnabled": false,
                                  "wanderingAnomalyEnabled": true,
                                  "routeDeviationSensitivity": "LOW",
                                  "speedAnomalySensitivity": "VERY_HIGH",
                                  "wanderingAnomalySensitivity": "VERY_LOW"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultType").value("SUCCESS"));

        then(notificationSettingService).should().updateAnomaly(
                eq(MEMBER_KEY),
                argThat(command -> command.wardKey().equals(WARD_KEY)
                        && command.routeDeviationEnabled()
                        && !command.speedAnomalyEnabled()
                        && command.wanderingAnomalyEnabled()
                        && command.routeDeviationSensitivity() == AnomalySensitivity.LOW
                        && command.speedAnomalySensitivity() == AnomalySensitivity.VERY_HIGH
                        && command.wanderingAnomalySensitivity() == AnomalySensitivity.VERY_LOW)
        );
    }

    @Test
    @DisplayName("Returns 400 when battery threshold is invalid")
    void updateBattery_returns_400_for_invalid_threshold() throws Exception {
        mockMvc.perform(patch("/api/v1/notifications/settings/{wardKey}/battery", WARD_KEY)
                        .requestAttr("memberKey", MEMBER_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"lowBatteryEnabled": true, "thresholdPercent": 12}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.resultType").value("ERROR"))
                .andExpect(jsonPath("$.error.errorCode").value("E9001"));
    }

    @Test
    @DisplayName("Maps business info to API response")
    void response_from_maps_business_info() {
        NotificationSettingResponse response = NotificationSettingResponse.from(
                NotificationSettingInfo.from(setting())
        );

        assertThat(response.safeZone().entryEnabled()).isTrue();
        assertThat(response.anomaly().routeDeviationSensitivity()).isEqualTo("HIGH");
        assertThat(response.anomaly().speedAnomalySensitivity()).isEqualTo("LOW");
        assertThat(response.anomaly().wanderingAnomalySensitivity()).isEqualTo("VERY_HIGH");
        assertThat(response.battery().thresholdOptions()).contains(10, 25, 100);
    }

    private NotificationSetting setting() {
        return NotificationFixture.createSetting(WARD_KEY);
    }

    private HandlerMethodArgumentResolver authMemberArgumentResolver() {
        return new HandlerMethodArgumentResolver() {
            @Override
            public boolean supportsParameter(MethodParameter parameter) {
                return parameter.hasParameterAnnotation(AuthMember.class)
                        && parameter.getParameterType().equals(String.class);
            }

            @Override
            public Object resolveArgument(
                    MethodParameter parameter,
                    ModelAndViewContainer mavContainer,
                    NativeWebRequest webRequest,
                    WebDataBinderFactory binderFactory
            ) {
                return webRequest.getAttribute("memberKey", RequestAttributes.SCOPE_REQUEST);
            }
        };
    }
}
