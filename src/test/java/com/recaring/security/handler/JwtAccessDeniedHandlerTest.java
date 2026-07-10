package com.recaring.security.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.recaring.support.exception.ErrorType;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
@DisplayName("JwtAccessDeniedHandler 단위 테스트")
class JwtAccessDeniedHandlerTest {

    private final JwtAccessDeniedHandler jwtAccessDeniedHandler = new JwtAccessDeniedHandler(new ObjectMapper());

    @Mock
    private HttpServletRequest request;

    @Test
    @DisplayName("접근 거부 시 403과 FORBIDDEN_ROLE 에러 바디를 반환한다")
    void handle_writes_forbidden_response() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        jwtAccessDeniedHandler.handle(request, response, new AccessDeniedException("no access"));

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentType()).startsWith(MediaType.APPLICATION_JSON_VALUE);
        assertThat(response.getContentAsString()).contains(ErrorType.FORBIDDEN_ROLE.getErrorCode().name());
        assertThat(response.getContentAsString()).contains("\"resultType\":\"ERROR\"");
    }
}
