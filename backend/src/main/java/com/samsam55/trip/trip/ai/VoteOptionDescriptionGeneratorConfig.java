package com.samsam55.trip.trip.ai;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * {@code VoteOptionDescriptionGenerator}를 구현하는 실제 AI 클라이언트 빈이 없을 때만
 * {@link StubVoteOptionDescriptionGenerator}를 등록한다. 나중에 실제 구현체를
 * {@code @Component}로 추가하면 이 스텁은 자동으로 빠지고 그 구현체가 쓰인다.
 */
@Configuration
public class VoteOptionDescriptionGeneratorConfig {

    @Bean
    @ConditionalOnMissingBean(VoteOptionDescriptionGenerator.class)
    public VoteOptionDescriptionGenerator voteOptionDescriptionGenerator() {
        return new StubVoteOptionDescriptionGenerator();
    }
}
