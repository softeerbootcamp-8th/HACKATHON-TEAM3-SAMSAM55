package com.samsam55.trip.trip.ai;

/**
 * 선택지 이름으로 AI 설명을 생성한다. 실제 AI 클라이언트로 교체하기 전까지는
 * {@link StubVoteOptionDescriptionGenerator}를 사용한다.
 */
public interface VoteOptionDescriptionGenerator {

    String generate(String optionName);

    /**
     * 생성한 설명의 출처를 나타낸다. 실제 AI 클라이언트는 "AI"를, 그 전까지 쓰는 스텁은
     * AI가 실제로 글을 쓴 게 아니므로 그와 다른 값을 반환해야 한다.
     */
    String getSource();
}
