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
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationSettingController MVC 테스트")
class NotificationSettingControllerWebMvcTest {

    private static final String MEMBER_KEY = "member-key";
    private static final String WARD_KEY = "ward-key";

    private MockMvc mockMvc;

    @Mock
    private NotificationSettingService notificationSettingService;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new NotificationSettingController(notificationSettingService))
                .setControllerAdvice(new ApiControllerAdvice())
                .setCustomArgumentResolvers(authMemberArgumentResolver())
                .build();
    }

    @Test
    @DisplayName("GET /api/v1/notifications/settings/{wardKey} - 알림 설정 전체를 반환한다")
    void getSetting_returns_notification_setting() throws Exception {
        given(notificationSettingService.getSetting(MEMBER_KEY, WARD_KEY))
                .willReturn(NotificationSettingInfo.from(setting()));

        mockMvc.perform(get("/api/v1/notifications/settings/{wardKey}", WARD_KEY)
                        .requestAttr("memberKey", MEMBER_KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultType").value("SUCCESS"))
                .andExpect(jsonPath("$.data.safeZone.entryEnabled").value(true))
                .andExpect(jsonPath("$.data.safeZone.exitEnabled").value(false))
                .andExpect(jsonPath("$.data.anomaly.sensitivity").value("HIGH"))
                .andExpect(jsonPath("$.data.anomaly.sensitivityOptions[0]").value("VERY_LOW"))
                .andExpect(jsonPath("$.data.emergencyCall.enabled").value(true))
                .andExpect(jsonPath("$.data.battery.lowBatteryEnabled").value(false))
                .andExpect(jsonPath("$.data.battery.thresholdPercent").value(40))
                .andExpect(jsonPath("$.data.battery.thresholdOptions[0]").value(10))
                .andExpect(jsonPath("$.data.battery.thresholdOptions[18]").value(100));

        then(notificationSettingService).should().getSetting(MEMBER_KEY, WARD_KEY);
    }

    @Test
    @DisplayName("PATCH /safe-zone - 안심존 알림 설정을 즉시 저장한다")
    void updateSafeZone_updates_setting() throws Exception {
        mockMvc.perform(patch("/api/v1/notifications/settings/{wardKey}/safe-zone", WARD_KEY)
                        .requestAttr("memberKey", MEMBER_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"entryEnabled": false, "exitEnabled": true}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultType").value("SUCCESS"));

        then(notificationSettingService).should().updateSafeZone(
                org.mockito.ArgumentMatchers.eq(MEMBER_KEY),
                argThat(command -> command.wardKey().equals(WARD_KEY)
                        && !command.entryEnabled()
                        && command.exitEnabled())
        );
    }

    @Test
    @DisplayName("PATCH /anomaly - 이상탐지 알림 설정을 즉시 저장한다")
    void updateAnomaly_updates_setting() throws Exception {
        mockMvc.perform(patch("/api/v1/notifications/settings/{wardKey}/anomaly", WARD_KEY)
                        .requestAttr("memberKey", MEMBER_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "routeDeviationEnabled": true,
                                  "speedAnomalyEnabled": false,
                                  "wanderingAnomalyEnabled": true,
                                  "sensitivity": "LOW"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultType").value("SUCCESS"));

        then(notificationSettingService).should().updateAnomaly(
                org.mockito.ArgumentMatchers.eq(MEMBER_KEY),
                argThat(command -> command.wardKey().equals(WARD_KEY)
                        && command.routeDeviationEnabled()
                        && !command.speedAnomalyEnabled()
                        && command.wanderingAnomalyEnabled()
                        && command.sensitivity() == AnomalySensitivity.LOW)
        );
    }

    @Test
    @DisplayName("PATCH /battery - 지원하지 않는 배터리 임계값이면 400을 반환한다")
    void updateBattery_returns_400_for_invalid_threshold() throws Exception {
        mockMvc.perform(patch("/api/v1/notifications/settings/{wardKey}/battery", WARD_KEY)
                        .requestAttr("memberKey", MEMBER_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"lowBatteryEnabled": true, "thresholdPercent": 12}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.resultType").value("ERROR"))
                .andExpect(jsonPath("$.error.errorCode").value("E8001"));
    }

    @Test
    @DisplayName("응답 DTO는 비즈니스 정보를 API 응답으로 변환한다")
    void response_from_maps_business_info() {
        NotificationSettingResponse response = NotificationSettingResponse.from(
                NotificationSettingInfo.from(setting())
        );

        assertThat(response.safeZone().entryEnabled()).isTrue();
        assertThat(response.anomaly().sensitivity()).isEqualTo("HIGH");
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
