package com.samsam55.trip.auth.argumentresolver;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.samsam55.trip.auth.annotation.Login;
import com.samsam55.trip.auth.exception.AuthErrorType;
import com.samsam55.trip.auth.service.AuthService;
import com.samsam55.trip.global.exception.ApplicationException;
import com.samsam55.trip.global.config.WebMvcConfig;
import jakarta.servlet.http.HttpSession;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;

class LoginUserArgumentResolverTest {

    private final LoginUserArgumentResolver resolver = new LoginUserArgumentResolver();

    @Test
    @DisplayName("@Login이 붙은 Long 파라미터를 지원한다")
    void Login이_붙은_Long_파라미터를_지원한다() throws NoSuchMethodException {
        assertThat(resolver.supportsParameter(parameterOf("login", Long.class))).isTrue();
        assertThat(resolver.supportsParameter(parameterOf("withoutAnnotation", Long.class))).isFalse();
    }

    @Test
    @DisplayName("세션의 회원 식별자를 @Login 파라미터로 전달한다")
    void 세션의_회원_식별자를_Login_파라미터로_전달한다() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        HttpSession session = request.getSession(true);
        session.setAttribute(AuthService.LOGIN_USER_ID_SESSION_ATTRIBUTE, 42L);

        Object result = resolver.resolveArgument(
                parameterOf("login", Long.class),
                null,
                new ServletWebRequest(request),
                null
        );

        assertThat(result).isEqualTo(42L);
    }

    @Test
    @DisplayName("세션이 없으면 로그인 필요 에러를 던진다")
    void 세션이_없으면_로그인_필요_에러를_던진다() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();

        assertThatThrownBy(() -> resolver.resolveArgument(
                parameterOf("login", Long.class),
                null,
                new ServletWebRequest(request),
                null
        ))
                .isInstanceOfSatisfying(ApplicationException.class, exception ->
                        assertThat(exception.getErrorType()).isEqualTo(AuthErrorType.LOGIN_REQUIRED));
    }

    @Test
    @DisplayName("WebMvc 설정에 로그인 사용자 resolver를 등록한다")
    void WebMvc_설정에_로그인_사용자_resolver를_등록한다() {
        WebMvcConfig webMvcConfig = new WebMvcConfig(resolver);
        List<HandlerMethodArgumentResolver> resolvers = new ArrayList<>();

        webMvcConfig.addArgumentResolvers(resolvers);

        assertThat(resolvers).containsExactly(resolver);
    }

    private MethodParameter parameterOf(String methodName, Class<?> parameterType) throws NoSuchMethodException {
        Method method = LoginTarget.class.getDeclaredMethod(methodName, parameterType);
        return new MethodParameter(method, 0);
    }

    private static class LoginTarget {

        private void login(@Login Long userId) {
        }

        private void withoutAnnotation(Long userId) {
        }
    }
}
