package com.samsam55.trip.auth.argumentresolver;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.samsam55.trip.auth.annotation.CurrentParticipant;
import com.samsam55.trip.auth.dto.ParticipantPrincipal;
import com.samsam55.trip.auth.service.ParticipantSessionResolver;
import com.samsam55.trip.global.exception.ApplicationException;
import com.samsam55.trip.trip.exception.TripErrorType;
import java.lang.reflect.Method;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.ServletWebRequest;

@ExtendWith(MockitoExtension.class)
class ParticipantArgumentResolverTest {

    @Mock
    private ParticipantSessionResolver participantSessionResolver;

    private ParticipantArgumentResolver resolver;

    @Test
    @DisplayName("@CurrentParticipant가 붙은 ParticipantPrincipal 파라미터를 지원한다")
    void CurrentParticipant가_붙은_ParticipantPrincipal_파라미터를_지원한다() throws NoSuchMethodException {
        resolver = new ParticipantArgumentResolver(participantSessionResolver);

        assertThat(resolver.supportsParameter(parameterOf("withAnnotation"))).isTrue();
        assertThat(resolver.supportsParameter(parameterOf("withoutAnnotation"))).isFalse();
    }

    @Test
    @DisplayName("세션 또는 쿠키로 식별된 참여자를 파라미터로 전달한다")
    void 식별된_참여자를_파라미터로_전달한다() throws Exception {
        resolver = new ParticipantArgumentResolver(participantSessionResolver);
        MockHttpServletRequest request = new MockHttpServletRequest();
        ParticipantPrincipal principal = new ParticipantPrincipal(12L, 1L);
        when(participantSessionResolver.resolve(request)).thenReturn(Optional.of(principal));

        Object result = resolver.resolveArgument(
                parameterOf("withAnnotation"), null, new ServletWebRequest(request), null
        );

        assertThat(result).isEqualTo(principal);
    }

    @Test
    @DisplayName("식별에 실패하면 참여자 인증 필요 에러를 던진다")
    void 식별에_실패하면_인증_필요_에러를_던진다() throws Exception {
        resolver = new ParticipantArgumentResolver(participantSessionResolver);
        MockHttpServletRequest request = new MockHttpServletRequest();
        when(participantSessionResolver.resolve(request)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> resolver.resolveArgument(
                parameterOf("withAnnotation"), null, new ServletWebRequest(request), null
        ))
                .isInstanceOfSatisfying(ApplicationException.class, exception ->
                        assertThat(exception.getErrorType()).isEqualTo(TripErrorType.PARTICIPANT_LOGIN_REQUIRED));
    }

    private MethodParameter parameterOf(String methodName) throws NoSuchMethodException {
        Method method = ParticipantTarget.class.getDeclaredMethod(methodName, ParticipantPrincipal.class);
        return new MethodParameter(method, 0);
    }

    private static class ParticipantTarget {

        private void withAnnotation(@CurrentParticipant ParticipantPrincipal principal) {
        }

        private void withoutAnnotation(ParticipantPrincipal principal) {
        }
    }
}
