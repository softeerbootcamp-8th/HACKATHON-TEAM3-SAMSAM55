package com.samsam55.trip.global.config;

import com.samsam55.trip.auth.annotation.CurrentParticipant;
import com.samsam55.trip.auth.annotation.Login;
import org.springdoc.core.utils.SpringDocUtils;
import org.springframework.context.annotation.Configuration;

/**
 * {@code @Login}, {@code @CurrentParticipant}는 커스텀 argument resolver가 세션에서
 * 값을 채우는 파라미터라 클라이언트가 보내는 값이 아니다. springdoc이 이를 모르고
 * 필수 쿼리 파라미터로 문서화(그리고 Orval이 그대로 생성)하는 것을 막는다.
 */
@Configuration
public class SpringDocConfig {

    static {
        SpringDocUtils.getConfig().addAnnotationsToIgnore(Login.class, CurrentParticipant.class);
    }
}
