package com.samsam55.trip.trip.ai;

/**
 * 실제 AI 연동 전까지 쓰는 임시 스텁이다. 스프링 빈 등록은
 * {@link VoteOptionDescriptionGeneratorConfig}가 {@code @ConditionalOnMissingBean}으로
 * 담당한다 — 실제 AI 클라이언트를 구현한 빈이 등록되면 이 스텁은 자동으로 빠진다.
 */
public class StubVoteOptionDescriptionGenerator implements VoteOptionDescriptionGenerator {

    @Override
    public String generate(String optionName) {
        return "상세 설명을 입력해주세요";
    }

    @Override
    public String getSource() {
        return "HOST";
    }
}
