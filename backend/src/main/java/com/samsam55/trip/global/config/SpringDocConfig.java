package com.samsam55.trip.global.config;

import com.samsam55.trip.auth.annotation.CurrentParticipant;
import com.samsam55.trip.auth.annotation.Login;
import org.springdoc.core.customizers.ParameterCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * API 문서(OpenAPI)에서 세션으로 인증되는 파라미터를 감춘다.
 *
 * <p>{@link Login}, {@link CurrentParticipant}가 붙은 파라미터는 클라이언트가 보내는 값이 아니라
 * 서버가 세션에서 직접 읽는다. 이 커스터마이저가 없으면 springdoc이 커스텀
 * {@code HandlerMethodArgumentResolver}를 인식하지 못해 필수 쿼리 파라미터로 잘못
 * 노출하고, 프론트엔드 API 클라이언트 생성(Orval) 결과에도 그대로 반영된다.
 */
@Configuration
public class SpringDocConfig {

    @Bean
    public ParameterCustomizer hideSessionResolvedParameterCustomizer() {
        return (parameterModel, methodParameter) -> {
            if (methodParameter.hasParameterAnnotation(Login.class)
                    || methodParameter.hasParameterAnnotation(CurrentParticipant.class)) {
                return null;
            }
            return parameterModel;
        };
    }
}
