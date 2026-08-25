package com.samsam55.trip.trip.ai;

/**
 * 선택지 이름으로 AI 설명을 생성한다. 실제 AI 클라이언트로 교체하기 전까지는
 * {@link StubVoteOptionDescriptionGenerator}를 사용한다.
 */
public interface VoteOptionDescriptionGenerator {

    String SOURCE = "AI";

    String generate(String optionName);
}
