package com.recaring.notification.controller;

import com.recaring.common.controller.ApiControllerAdvice;
import com.recaring.notification.business.NotificationQueryService;
import com.recaring.notification.fixture.NotificationFixture;
import com.recaring.security.vo.AuthMember;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationController MVC test")
class NotificationControllerWebMvcTest {

    private static final String MEMBER_KEY = "member-key";

    private MockMvc mockMvc;

    @Mock
    private NotificationQueryService notificationQueryService;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new NotificationController(notificationQueryService))
                .setControllerAdvice(new ApiControllerAdvice())
                .setCustomArgumentResolvers(authMemberArgumentResolver())
                .build();
    }

    @Test
    @DisplayName("Returns the authenticated member's notification list")
    void getMyNotifications_returns_list() throws Exception {
        given(notificationQueryService.getMyNotifications(MEMBER_KEY)).willReturn(List.of(
                NotificationFixture.notificationItem("BATTERY_LOW", "배터리 부족", "배터리가 부족합니다. 잔량은 40% 입니다.")
        ));

        mockMvc.perform(get("/api/v1/notifications")
                        .requestAttr("memberKey", MEMBER_KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultType").value("SUCCESS"))
                .andExpect(jsonPath("$.data[0].notificationKey").value("notification-key-001"))
                .andExpect(jsonPath("$.data[0].eventType").value("BATTERY_LOW"))
                .andExpect(jsonPath("$.data[0].title").value("배터리 부족"))
                .andExpect(jsonPath("$.data[0].body").value("배터리가 부족합니다. 잔량은 40% 입니다."))
                .andExpect(jsonPath("$.data[0].dataPayload.type").value("BATTERY_LOW"));

        then(notificationQueryService).should().getMyNotifications(MEMBER_KEY);
    }

    @Test
    @DisplayName("Returns an empty list when there are no notifications")
    void getMyNotifications_returns_empty_list() throws Exception {
        given(notificationQueryService.getMyNotifications(MEMBER_KEY)).willReturn(List.of());

        mockMvc.perform(get("/api/v1/notifications")
                        .requestAttr("memberKey", MEMBER_KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultType").value("SUCCESS"))
                .andExpect(jsonPath("$.data").isEmpty());
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
