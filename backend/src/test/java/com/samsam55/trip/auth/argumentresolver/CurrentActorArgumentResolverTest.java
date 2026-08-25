package com.samsam55.trip.auth.argumentresolver;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.samsam55.trip.auth.annotation.CurrentActor;
import com.samsam55.trip.auth.dto.ActorPrincipal;
import com.samsam55.trip.auth.service.AuthService;
import java.lang.reflect.Method;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.ServletWebRequest;

@ExtendWith(MockitoExtension.class)
class CurrentActorArgumentResolverTest {

    @Mock
    private AuthService authService;

    private CurrentActorArgumentResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new CurrentActorArgumentResolver(authService);
    }

    @Test
    @DisplayName("@CurrentActor가 붙은 ActorPrincipal 파라미터를 지원한다")
    void CurrentActor가_붙은_ActorPrincipal_파라미터를_지원한다() throws NoSuchMethodException {
        assertThat(resolver.supportsParameter(parameterOf("withAnnotation", ActorPrincipal.class))).isTrue();
        assertThat(resolver.supportsParameter(parameterOf("withoutAnnotation", ActorPrincipal.class))).isFalse();
        assertThat(resolver.supportsParameter(parameterOf("wrongType", Long.class))).isFalse();
    }

    @Test
    @DisplayName("인증 주체를 @CurrentActor 파라미터로 전달한다")
    void 인증_주체를_CurrentActor_파라미터로_전달한다() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        ActorPrincipal actor = ActorPrincipal.ofHost(5L);
        when(authService.resolveActor(request)).thenReturn(actor);

        Object result = resolver.resolveArgument(
                parameterOf("withAnnotation", ActorPrincipal.class),
                null,
                new ServletWebRequest(request),
                null
        );

        assertThat(result).isEqualTo(actor);
        verify(authService).resolveActor(request);
    }

    private MethodParameter parameterOf(String methodName, Class<?> parameterType) throws NoSuchMethodException {
        Method method = ActorTarget.class.getDeclaredMethod(methodName, parameterType);
        return new MethodParameter(method, 0);
    }

    private static class ActorTarget {

        private void withAnnotation(@CurrentActor ActorPrincipal actor) {
        }

        private void withoutAnnotation(ActorPrincipal actor) {
        }

        private void wrongType(@CurrentActor Long actor) {
        }
    }
}
