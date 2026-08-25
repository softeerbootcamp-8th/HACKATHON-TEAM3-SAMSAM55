package com.samsam55.trip.trip.ai;

import org.springframework.stereotype.Component;

/**
 * 실제 AI 연동 전까지 사용하는 임시 스텁이다. 팀 확인 후 실제 AI 클라이언트로 교체한다.
 */
@Component
public class StubVoteOptionDescriptionGenerator implements VoteOptionDescriptionGenerator {

    @Override
    public String generate(String optionName) {
        return optionName + "에 대한 AI STUB 응답";
    }
}
