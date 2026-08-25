package com.samsam55.trip.global.config;

import com.samsam55.trip.auth.argumentresolver.CurrentActorArgumentResolver;
import com.samsam55.trip.auth.argumentresolver.LoginUserArgumentResolver;
import com.samsam55.trip.auth.argumentresolver.ParticipantArgumentResolver;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final LoginUserArgumentResolver loginUserArgumentResolver;
    private final ParticipantArgumentResolver participantArgumentResolver;
    private final CurrentActorArgumentResolver currentActorArgumentResolver;

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(loginUserArgumentResolver);
        resolvers.add(participantArgumentResolver);
        resolvers.add(currentActorArgumentResolver);
    }
}
