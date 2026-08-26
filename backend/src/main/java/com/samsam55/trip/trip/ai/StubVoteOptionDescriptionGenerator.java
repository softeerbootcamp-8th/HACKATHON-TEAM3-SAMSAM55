package com.samsam55.trip.trip.ai;

/**
 * 실제 AI 연동 전까지 쓰는 임시 스텁이다. 스프링 빈 등록은
 * {@link VoteOptionDescriptionGeneratorConfig}가 {@code @ConditionalOnMissingBean}으로
 * 담당한다 — 실제 AI 클라이언트를 구현한 빈이 등록되면 이 스텁은 자동으로 빠진다.
 * 실제로 생성한 설명이 없으므로 {@code null}을 반환한다 — 안내 문구 같은 placeholder를
 * 채워 넣으면 방장이 직접 쓴 설명과 구분이 안 되고 그대로 화면에 노출된다.
 */
public class StubVoteOptionDescriptionGenerator implements VoteOptionDescriptionGenerator {

    @Override
    public String generate(String optionName) {
        return null;
    }

    @Override
    public String getSource() {
        return "HOST";
    }
}
