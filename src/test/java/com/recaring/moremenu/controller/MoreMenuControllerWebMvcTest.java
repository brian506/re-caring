package com.recaring.moremenu.controller;

import com.recaring.moremenu.business.MoreMenuContextType;
import com.recaring.moremenu.business.MoreMenuInfo;
import com.recaring.moremenu.business.MoreMenuItemInfo;
import com.recaring.moremenu.business.MoreMenuItemKey;
import com.recaring.moremenu.business.MoreMenuSectionInfo;
import com.recaring.moremenu.business.MoreMenuSectionKey;
import com.recaring.moremenu.business.MoreMenuService;
import com.recaring.moremenu.business.MoreMenuTargetType;
import com.recaring.security.vo.AuthMember;
import com.recaring.common.controller.ApiControllerAdvice;
import com.recaring.support.exception.AppException;
import com.recaring.support.exception.ErrorType;
import io.swagger.v3.oas.annotations.Operation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.lang.reflect.Method;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("더보기 메뉴 컨트롤러 MVC 테스트")
class MoreMenuControllerWebMvcTest {

    private static final String MEMBER_KEY = "member-key";
    private static final String WARD_KEY = "ward-key";

    private MockMvc mockMvc;

    @Mock
    private MoreMenuService moreMenuService;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new MoreMenuController(moreMenuService))
                .setControllerAdvice(new ApiControllerAdvice())
                .setCustomArgumentResolvers(authMemberArgumentResolver())
                .build();
    }

    @Test
    @DisplayName("인증 principal과 wardKey를 서비스로 전달하고 API 응답으로 감싼다")
    void getMenu_returns_api_response() throws Exception {
        given(moreMenuService.getMenu(MEMBER_KEY, WARD_KEY))
                .willReturn(menu());

        mockMvc.perform(get("/api/v1/more/menu")
                        .param("wardKey", WARD_KEY)
                        .requestAttr("memberKey", MEMBER_KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultType").value("SUCCESS"))
                .andExpect(jsonPath("$.data.contextType").value("GUARDIAN"))
                .andExpect(jsonPath("$.data.sections[0].sectionKey").value("SETTING"))
                .andExpect(jsonPath("$.data.sections[0].items[0].itemKey").value("NOTIFICATION_SETTING"))
                .andExpect(jsonPath("$.data.sections[0].items[0].enabled").value(true))
                .andExpect(jsonPath("$.data.sections[0].items[0].targetType").value("SCREEN"))
                .andExpect(jsonPath("$.data.sections[0].items[0].target").value("ST-003"));

        then(moreMenuService).should().getMenu(MEMBER_KEY, WARD_KEY);
    }

    @Test
    @DisplayName("서비스 예외는 공통 에러 응답으로 변환된다")
    void getMenu_returns_error_response_when_service_throws_exception() throws Exception {
        given(moreMenuService.getMenu(MEMBER_KEY, null))
                .willThrow(new AppException(ErrorType.WARD_KEY_REQUIRED));

        mockMvc.perform(get("/api/v1/more/menu")
                        .requestAttr("memberKey", MEMBER_KEY))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.resultType").value("ERROR"))
                .andExpect(jsonPath("$.error.errorCode").value("E5010"));
    }

    @Test
    @DisplayName("Swagger 문서는 인증 principal을 숨기고 wardKey만 요청 파라미터로 노출한다")
    void getMenu_has_swagger_contract() throws NoSuchMethodException {
        Method method = MoreMenuController.class.getMethod("getMenu", String.class, String.class);

        Operation operation = method.getAnnotation(Operation.class);
        io.swagger.v3.oas.annotations.Parameter authParameter =
                method.getParameters()[0].getAnnotation(io.swagger.v3.oas.annotations.Parameter.class);
        io.swagger.v3.oas.annotations.Parameter wardKeyParameter =
                method.getParameters()[1].getAnnotation(io.swagger.v3.oas.annotations.Parameter.class);

        assertThat(operation.summary()).isEqualTo("Get more menu");
        assertThat(authParameter.hidden()).isTrue();
        assertThat(wardKeyParameter.required()).isFalse();
        assertThat(wardKeyParameter.description()).contains("Ward member key");
    }

    private MoreMenuInfo menu() {
        return new MoreMenuInfo(
                MoreMenuContextType.GUARDIAN,
                List.of(new MoreMenuSectionInfo(
                        MoreMenuSectionKey.SETTING,
                        List.of(new MoreMenuItemInfo(
                                MoreMenuItemKey.NOTIFICATION_SETTING,
                                true,
                                MoreMenuTargetType.SCREEN,
                                "ST-003"
                        ))
                ))
        );
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
